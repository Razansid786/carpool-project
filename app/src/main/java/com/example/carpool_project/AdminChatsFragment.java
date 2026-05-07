package com.example.carpool_project;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminChatsFragment extends Fragment {

    private RecyclerView rvChats;
    private ChatListAdapter adapter;
    private List<SupportChatEntry> chatEntries;
    private DatabaseReference supportRef;
    private ValueEventListener supportListener;
    private boolean isHardcodedAdmin = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_chats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getArguments() != null) {
            isHardcodedAdmin = getArguments().getBoolean("isHardcodedAdmin", false);
        }

        rvChats = view.findViewById(R.id.rvAdminChats);
        chatEntries = new ArrayList<>();
        adapter = new ChatListAdapter(chatEntries);
        
        rvChats.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChats.setAdapter(adapter);
        
        checkAdminAndLoad();
    }

    private void checkAdminAndLoad() {
        // 1. Primary check: If we are in the AdminMainActivity dashboard, grant access.
        if (isHardcodedAdmin || getActivity() instanceof AdminMainActivity) {
            Log.d("AdminChats", "Access granted via Admin Dashboard context");
            loadData();
            return;
        }

        // 2. Fallback check for Firebase User
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Check hardcoded email
            if ("admin@gmail.com".equals(user.getEmail())) {
                loadData();
                return;
            }

            // Check Firestore role as last resort
            FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (isAdded()) {
                            if (doc.exists() && "admin".equals(doc.getString("role"))) {
                                loadData();
                            } else {
                                Log.w("AdminChats", "Unauthorized user email: " + user.getEmail());
                                Toast.makeText(getContext(), "Access Denied: Admin role not found", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            Log.e("AdminChats", "Firestore verification failed", e);
                            Toast.makeText(getContext(), "Error verifying admin status", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            if (isAdded()) {
                Log.w("AdminChats", "No user logged in and not in Admin context");
                Toast.makeText(getContext(), "Access Denied: Please log in as admin", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadData() {
        supportRef = FirebaseDatabase.getInstance().getReference("support_chats");
        loadSupportChats();
    }

    private void loadSupportChats() {
        if (supportRef == null) return;
        
        if (supportListener != null) {
            supportRef.removeEventListener(supportListener);
        }

        supportListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatEntries.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String userId = ds.getKey();
                    SupportChatEntry entry = new SupportChatEntry(userId);
                    chatEntries.add(entry);
                    fetchUserName(entry);
                    fetchLastMessage(entry);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AdminChats", "Database Access Blocked: " + error.getMessage());
                if (isAdded()) {
                    // This error means Firebase Realtime Database Security Rules are rejecting the request.
                    Toast.makeText(getContext(), "Permission Denied: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        };
        supportRef.addValueEventListener(supportListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (supportRef != null && supportListener != null) {
            supportRef.removeEventListener(supportListener);
        }
    }

    private void fetchUserName(SupportChatEntry entry) {
        FirebaseFirestore.getInstance().collection("users").document(entry.userId)
                .get().addOnSuccessListener(doc -> {
                    if (doc.exists() && isAdded()) {
                        entry.userName = doc.getString("name");
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void fetchLastMessage(SupportChatEntry entry) {
        Query lastMsgQuery = supportRef.child(entry.userId).orderByKey().limitToLast(1);
        lastMsgQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ChatMessage msg = ds.getValue(ChatMessage.class);
                    if (msg != null) {
                        entry.lastMessage = msg.message;
                        adapter.notifyDataSetChanged();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    static class SupportChatEntry {
        String userId;
        String userName;
        String lastMessage = "No messages yet";
        SupportChatEntry(String userId) { this.userId = userId; }
    }

    class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {
        private List<SupportChatEntry> list;
        ChatListAdapter(List<SupportChatEntry> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_chat, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SupportChatEntry entry = list.get(position);
            holder.tvName.setText(entry.userName != null ? entry.userName : "User: " + entry.userId);
            holder.tvLastMsg.setText(entry.lastMessage);
            
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), ChatActivity.class);
                intent.putExtra("rideId", "support");
                intent.putExtra("otherUserId", entry.userId);
                intent.putExtra("isAdmin", true);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvLastMsg;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvAdminChatUserName);
                tvLastMsg = v.findViewById(R.id.tvAdminChatLastMsg);
            }
        }
    }
}
