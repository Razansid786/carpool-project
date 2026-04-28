package com.example.carpool_project;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DiscoverFragment extends Fragment {

    private RecyclerView recyclerView;
    private RideAdapter adapter;
    private List<Ride> rideList;
    private FirebaseFirestore db;
    private ListenerRegistration rideListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recyclerViewRides);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        rideList = new ArrayList<>();
        adapter = new RideAdapter(rideList);
        recyclerView.setAdapter(adapter);

        ImageView ivNotification = view.findViewById(R.id.ivNotificationBell);
        if (ivNotification != null) {
            ivNotification.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openNotifications();
                }
            });
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadActiveRides();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }
    }

    private void loadActiveRides() {
        rideListener = db.collection("rides")
                .whereEqualTo("status", "active")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        rideList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                Ride ride = doc.toObject(Ride.class);
                                rideList.add(ride);
                            } catch (Exception e) {
                                Log.e("Firestore", "Error parsing ride", e);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}
