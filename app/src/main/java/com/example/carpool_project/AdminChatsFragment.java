package com.example.carpool_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminChatsFragment extends Fragment {

    private RecyclerView rvChats;
    private ChatListAdaper adapter;
    private List<SupportChatEntry> chatEntries;
    private DatabaseReference supportRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_chats, container, false);
        
        rvChats = view.findViewById(R.id.rvAdminChats);
        chatEntries = new ArrayList<>();
        adapter = new ChatListAdaper(chatEntries);
        
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
                        entry.userEmail = doc.getString("email");
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    static class SupportChatEntry {
        String userId;
        String userName;
        String userEmail;
        SupportChatEntry(String userId) { this.userId = userId; }
    }

    class ChatListAdaper extends RecyclerView.Adapter<ChatListAdaper.ViewHolder> {
        private List<SupportChatEntry> list;
        ChatListAdaper(List<SupportChatEntry> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_chat, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SupportChatEntry entry = list.get(position);
            holder.tvName.setText(entry.userName != null ? entry.userName : "Loading...");
            holder.tvEmail.setText(entry.userEmail != null ? entry.userEmail : entry.userId);
            holder.btnChat.setOnClickListener(v -> {
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
            TextView tvName, tvEmail;
            ImageButton btnChat;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvChatUserName);
                tvEmail = v.findViewById(R.id.tvChatUserEmail);
                btnChat = v.findViewById(R.id.btnUserChat);
            }
        }
    }
}
