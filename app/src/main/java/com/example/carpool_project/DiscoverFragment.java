package com.example.carpool_project;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DiscoverFragment extends Fragment {

    private RecyclerView recyclerView;
    private RideAdapter adapter;
    private List<Ride> allRides;
    private List<Ride> filteredRides;
    private FirebaseFirestore db;
    private ListenerRegistration rideListener;

    private EditText etSearchDestination;
    private MaterialButton btnFilterDays, btnFilterTime, btnClearFilters;

    private List<String> selectedDays = new ArrayList<>();
    private String selectedTime = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recyclerViewRides);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        allRides = new ArrayList<>();
        filteredRides = new ArrayList<>();
        adapter = new RideAdapter(filteredRides);
        recyclerView.setAdapter(adapter);

        etSearchDestination = view.findViewById(R.id.etSearchDestination);
        btnFilterDays = view.findViewById(R.id.btnFilterDays);
        btnFilterTime = view.findViewById(R.id.btnFilterTime);
        btnClearFilters = view.findViewById(R.id.btnClearFilters);

        ImageView ivNotification = view.findViewById(R.id.ivNotificationBell);
        if (ivNotification != null) {
            ivNotification.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openNotifications();
                }
            });
        }

        setupFilters();

        return view;
    }

    private void setupFilters() {
        etSearchDestination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnFilterDays.setOnClickListener(v -> showDaysFilterDialog());
        btnFilterTime.setOnClickListener(v -> showTimeFilterDialog());
        btnClearFilters.setOnClickListener(v -> {
            etSearchDestination.setText("");
            selectedDays.clear();
            selectedTime = "";
            btnFilterDays.setText("Select Days");
            btnFilterTime.setText("Select Time");
            btnClearFilters.setVisibility(View.GONE);
            applyFilters();
        });
    }

    private void showDaysFilterDialog() {
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        boolean[] checkedDays = new boolean[days.length];
        for (int i = 0; i < days.length; i++) {
            if (selectedDays.contains(days[i])) checkedDays[i] = true;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Days")
                .setMultiChoiceItems(days, checkedDays, (dialog, which, isChecked) -> {
                    if (isChecked) selectedDays.add(days[which]);
                    else selectedDays.remove(days[which]);
                })
                .setPositiveButton("OK", (dialog, which) -> {
                    if (selectedDays.isEmpty()) btnFilterDays.setText("Select Days");
                    else btnFilterDays.setText(String.join(", ", selectedDays));
                    btnClearFilters.setVisibility(View.VISIBLE);
                    applyFilters();
                })
                .show();
    }

    private void showTimeFilterDialog() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(), (view, hourOfDay, minuteOfHour) -> {
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
            btnFilterTime.setText(selectedTime);
            btnClearFilters.setVisibility(View.VISIBLE);
            applyFilters();
        }, hour, minute, false);
        timePickerDialog.show();
    }

    private void applyFilters() {
        String query = etSearchDestination.getText().toString().toLowerCase().trim();
        filteredRides.clear();

        for (Ride ride : allRides) {
            boolean matchesDestination = ride.destination.toLowerCase().contains(query);
            boolean matchesDays = selectedDays.isEmpty();
            if (!selectedDays.isEmpty() && ride.recurringDays != null) {
                for (String day : selectedDays) {
                    if (ride.recurringDays.contains(day)) {
                        matchesDays = true;
                        break;
                    }
                }
            }

            boolean matchesTime = selectedTime.isEmpty() || (ride.time != null && ride.time.contains(selectedTime));

            if (matchesDestination && matchesDays && matchesTime) {
                filteredRides.add(ride);
            }
        }
        adapter.notifyDataSetChanged();
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
                        allRides.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                Ride ride = doc.toObject(Ride.class);
                                allRides.add(ride);
                            } catch (Exception e) {
                                Log.e("Firestore", "Error parsing ride", e);
                            }
                        }
                        applyFilters();
                    }
                });
    }
}
