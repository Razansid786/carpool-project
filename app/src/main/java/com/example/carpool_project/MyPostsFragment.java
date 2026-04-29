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

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyPostsFragment extends Fragment {
    private RecyclerView recyclerView;
    private RideAdapter adapter;
    private List<Ride> allRides;
    private List<Ride> filteredRides;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration rideListener;
    private TabLayout tabLayoutDays;
    private String selectedDay = "Mon";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_posts, container, false);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        tabLayoutDays = view.findViewById(R.id.tabLayoutDays);
        setupTabs();

        recyclerView = view.findViewById(R.id.rvMyPosts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        allRides = new ArrayList<>();
        filteredRides = new ArrayList<>();
        adapter = new RideAdapter(filteredRides);
        adapter.setMyPosts(true, ride -> showDeleteConfirmation(ride));
        
        recyclerView.setAdapter(adapter);
        return view;
    }

    private void setupTabs() {
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String day : days) {
            tabLayoutDays.addTab(tabLayoutDays.newTab().setText(day));
        }

        tabLayoutDays.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedDay = tab.getText().toString();
                filterByDay();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
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
                        allRides.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                Ride ride = doc.toObject(Ride.class);
                                allRides.add(ride);
                            } catch (Exception e) {
                                Log.e("MyPostsFragment", "Error parsing ride", e);
                            }
                        }
                        filterByDay();
                    }
                });
    }

    private void filterByDay() {
        filteredRides.clear();
        for (Ride ride : allRides) {
            if (ride.recurringDays != null && ride.recurringDays.contains(selectedDay)) {
                filteredRides.add(ride);
            }
        }
        adapter.notifyDataSetChanged();
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
        db.collection("rides").document(ride.rideId).delete();
        db.collection("offers")
                .whereEqualTo("rideId", ride.rideId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                });
        Toast.makeText(getContext(), "Ride deleted", Toast.LENGTH_SHORT).show();
    }
}
