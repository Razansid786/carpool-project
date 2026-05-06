package com.example.carpool_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_chats, container, false);
        
        rvChats = view.findViewById(R.id.rvAdminChats);
        chatEntries = new ArrayList<>();
        adapter = new ChatListAdapter(chatEntries);
        
        rvChats.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChats.setAdapter(adapter);
        
        supportRef = FirebaseDatabase.getInstance().getReference("support_chats");
        loadSupportChats();
        
        return view;
    }

    private void loadSupportChats() {
        supportRef.addValueEventListener(new ValueEventListener() {
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
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchUserName(SupportChatEntry entry) {
        FirebaseFirestore.getInstance().collection("users").document(entry.userId)
                .get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        entry.userName = doc.getString("name");
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void fetchLastMessage(SupportChatEntry entry) {
        Query lastMsgQuery = supportRef.child(entry.userId).orderByKey().limitToLast(1);
        lastMsgQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
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
