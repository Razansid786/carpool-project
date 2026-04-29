package com.example.carpool_project;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class MyRidesFragment extends Fragment {
    private RecyclerView recyclerView;
    private RideAdapter adapter;
    private List<Ride> myBookedRides;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_rides, container, false);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        recyclerView = view.findViewById(R.id.rvMyRides);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        myBookedRides = new ArrayList<>();
        
        adapter = new RideAdapter(myBookedRides);
        adapter.setMyRidesTab(true); 
        
        recyclerView.setAdapter(adapter);
        loadBookedRides();
        return view;
    }

    private void loadBookedRides() {
        String uid = mAuth.getCurrentUser().getUid();
        // Listen to offers instead of bookings to show pending status
        db.collection("offers")
                .whereEqualTo("passengerId", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        List<String> rideIds = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            String status = doc.getString("status");
                            // Show pending and accepted. Hide rejected as requested.
                            if ("pending".equals(status) || "accepted".equals(status)) {
                                rideIds.add(doc.getString("rideId"));
                            }
                        }
                        if (!rideIds.isEmpty()) {
                            fetchRides(rideIds);
                        } else {
                            myBookedRides.clear();
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void fetchRides(List<String> rideIds) {
        db.collection("rides")
                .whereIn("rideId", rideIds)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        myBookedRides.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            myBookedRides.add(doc.toObject(Ride.class));
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}
