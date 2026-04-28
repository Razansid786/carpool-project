package com.example.carpool_project;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyPostsFragment extends Fragment {
    private RecyclerView recyclerView;
    private RideAdapter adapter;
    private List<Ride> myRides;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration rideListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_posts, container, false);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        recyclerView = view.findViewById(R.id.rvMyPosts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        myRides = new ArrayList<>();
        
        adapter = new RideAdapter(myRides);
        adapter.setMyPosts(true, ride -> showDeleteConfirmation(ride));
        
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadMyRides();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }
    }

    private void loadMyRides() {
        if (mAuth.getCurrentUser() == null) return;
        
        String uid = mAuth.getCurrentUser().getUid();
        rideListener = db.collection("rides")
                .whereEqualTo("driverId", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("MyPostsFragment", "Listen failed.", error);
                        return;
                    }
                    if (value != null) {
                        myRides.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                Ride ride = doc.toObject(Ride.class);
                                myRides.add(ride);
                            } catch (Exception e) {
                                Log.e("MyPostsFragment", "Error parsing ride", e);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void showDeleteConfirmation(Ride ride) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Ride")
                .setMessage("Are you sure you want to delete this ride post?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRide(ride))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRide(Ride ride) {
        db.collection("rides").document(ride.rideId)
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Ride deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show());
    }
}
