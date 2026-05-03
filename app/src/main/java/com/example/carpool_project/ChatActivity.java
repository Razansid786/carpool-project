package com.example.carpool_project;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private EditText etMessage;
    private MaterialButton btnSendMessage;
    private TextView tvChatWith;
    private String rideId, otherUserId, chatRoomId;
    private DatabaseReference chatRef;
    private List<ChatMessage> messageList;
    private ChatAdapter adapter;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rideId = getIntent().getStringExtra("rideId");
        otherUserId = getIntent().getStringExtra("otherUserId");
        currentUserId = FirebaseAuth.getInstance().getUid();

        if (rideId == null || otherUserId == null || currentUserId == null) {
            Toast.makeText(this, "Error initializing chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (currentUserId.compareTo(otherUserId) < 0) {
            chatRoomId = currentUserId + "_" + otherUserId;
        } else {
            chatRoomId = otherUserId + "_" + currentUserId;
        }

        rvChat = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        tvChatWith = findViewById(R.id.tvChatWith);
        ImageView btnBack = findViewById(R.id.btnChatBack);

        btnBack.setOnClickListener(v -> finish());

        messageList = new ArrayList<>();
        adapter = new ChatAdapter(messageList, currentUserId);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);

        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(rideId).child(chatRoomId);

        loadOtherUserInfo();
        loadMessages();

        btnSendMessage.setOnClickListener(v -> sendMessage());

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void loadOtherUserInfo() {
        FirebaseFirestore.getInstance().collection("users").document(otherUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        if (name != null) tvChatWith.setText(name);
                    }
                });
    }

    private void loadMessages() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ChatMessage msg = ds.getValue(ChatMessage.class);
                    if (msg != null) {
                        messageList.add(msg);
                    }
                }
                adapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    rvChat.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatActivity", "Database error: " + error.getMessage());
            }
        });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (!TextUtils.isEmpty(text)) {
            ChatMessage msg = new ChatMessage(currentUserId, text, System.currentTimeMillis());
            chatRef.push().setValue(msg).addOnFailureListener(e -> {
                Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
            });
            etMessage.setText("");
        }
    }
}

class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;
    private final List<ChatMessage> list;
    private final String currentUserId;

    public ChatAdapter(List<ChatMessage> list, String currentUserId) {
        this.list = list;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        if (list.get(position).senderId.equals(currentUserId)) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_received, parent, false);
        }
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = list.get(position);
        holder.tvMessage.setText(msg.message);
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }
}
