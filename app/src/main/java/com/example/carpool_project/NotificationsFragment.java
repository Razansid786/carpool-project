package com.example.carpool_project;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private LinearLayout layoutEmpty;
    private MaterialButton btnClearAll;
    private FirebaseFirestore db;
    private ListenerRegistration notificationListener;
    private boolean isOptimisticClearing = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);
        db = FirebaseFirestore.getInstance();
        
        recyclerView = view.findViewById(R.id.rvNotifications);
        layoutEmpty = view.findViewById(R.id.layoutEmptyNotifications);
        btnClearAll = view.findViewById(R.id.btnClearNotifications);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        btnClearAll.setOnClickListener(v -> showClearAllConfirmation());

        setupSwipeToDelete();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Notification notification = notificationList.get(position);
                    deleteSingleNotification(notification, position);
                }
            }
        };

        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);
    }

    private void deleteSingleNotification(Notification notification, int position) {
        notificationList.remove(position);
        adapter.notifyItemRemoved(position);
        updateUIState();

        db.collection("notifications").document(notification.id).delete()
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error: Failed to delete", Toast.LENGTH_SHORT).show();
                        startListening(); 
                    }
                });
    }

    private void startListening() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        if (notificationListener != null) notificationListener.remove();

        notificationListener = db.collection("notifications")
                .whereEqualTo("recipientId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded() || getContext() == null || isOptimisticClearing) return;
                    
                    if (error != null) {
                        Log.e("Notifications", "Listen failed, attempting fallback", error);
                        loadWithoutSorting(uid);
                        return;
                    }

                    if (value != null) {
                        notificationList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Notification n = doc.toObject(Notification.class);
                            if (n != null) notificationList.add(n);
                        }
                        adapter.notifyDataSetChanged();
                        updateUIState();
                    }
                });
    }

    private void loadWithoutSorting(String uid) {
        if (notificationListener != null) notificationListener.remove();
        notificationListener = db.collection("notifications")
                .whereEqualTo("recipientId", uid)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded() || value == null || isOptimisticClearing) return;
                    notificationList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Notification n = doc.toObject(Notification.class);
                        if (n != null) notificationList.add(n);
                    }
                    notificationList.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                    adapter.notifyDataSetChanged();
                    updateUIState();
                });
    }

    private void updateUIState() {
        if (notificationList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            btnClearAll.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            btnClearAll.setVisibility(View.VISIBLE);
        }
    }

    private void showClearAllConfirmation() {
        if (getContext() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear All Notifications")
                .setMessage("Delete all notifications permanently?")
                .setPositiveButton("Clear All", (dialog, which) -> clearAllNotifications())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAllNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || notificationList.isEmpty()) return;

        isOptimisticClearing = true;
        
        final List<String> idsToDelete = new ArrayList<>();
        for (Notification n : notificationList) {
            idsToDelete.add(n.id);
        }

        notificationList.clear();
        adapter.notifyDataSetChanged();
        updateUIState();

        WriteBatch batch = db.batch();
        for (String id : idsToDelete) {
            batch.delete(db.collection("notifications").document(id));
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            isOptimisticClearing = false;
            if (isAdded()) {
                Toast.makeText(getContext(), "Notifications cleared", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            isOptimisticClearing = false;
            if (isAdded()) {
                Toast.makeText(getContext(), "Failed to clear notifications", Toast.LENGTH_SHORT).show();
                startListening();
            }
        });
    }
}
