package com.example.carpool_project;

import android.app.TimePickerDialog;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PostRideFragment extends Fragment implements OnMapReadyCallback {

    private EditText etFrom, etTo, etSeats, etTime;
    private ChipGroup chipGroupDays;
    private Button btnPostCommute;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private GoogleMap mMap;
    private LatLng startLatLng, endLatLng;
    private boolean isSelectingFrom = true;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private TextWatcher fromTextWatcher, toTextWatcher;
    private boolean isMapInitialized = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post_ride, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etFrom = view.findViewById(R.id.etFrom);
        etTo = view.findViewById(R.id.etTo);
        etSeats = view.findViewById(R.id.etSeats);
        etTime = view.findViewById(R.id.etTime);
        chipGroupDays = view.findViewById(R.id.chipGroupDays);
        btnPostCommute = view.findViewById(R.id.btnPostCommute);

        etTime.setOnClickListener(v -> showTimePicker());

        btnPostCommute.setOnClickListener(v -> handlePostRide());

        etFrom.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                isSelectingFrom = true;
                ensureMapInitialized();
            }
        });

        etTo.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                isSelectingFrom = false;
                ensureMapInitialized();
            }
        });

        setupSearchListeners();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minuteOfHour) -> {
            String amPm = (hourOfDay < 12) ? "AM" : "PM";
            int displayHour = (hourOfDay > 12) ? hourOfDay - 12 : (hourOfDay == 0 ? 12 : hourOfDay);
            etTime.setText(String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minuteOfHour, amPm));
        }, hour, minute, false);
        timePickerDialog.show();
    }

    private void ensureMapInitialized() {
        if (!isMapInitialized) {
            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                    .findFragmentById(R.id.map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
                isMapInitialized = true;
            }
        }
    }

    private void setupSearchListeners() {
        fromTextWatcher = createTextWatcher(true);
        toTextWatcher = createTextWatcher(false);
        etFrom.addTextChangedListener(fromTextWatcher);
        etTo.addTextChangedListener(toTextWatcher);
    }

    private TextWatcher createTextWatcher(boolean isFrom) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                searchHandler.removeCallbacksAndMessages(null);
                if (s.length() > 3) {
                    searchHandler.postDelayed(() -> {
                        ensureMapInitialized();
                        performSearch(s.toString(), isFrom);
                    }, 1000);
                }
            }
        };
    }

    private void performSearch(String query, boolean isFrom) {
        if (!Geocoder.isPresent()) return;
        new Thread(() -> {
            LatLng loc = getLocationFromAddress(query);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (loc != null) {
                        if (isFrom) startLatLng = loc;
                        else endLatLng = loc;
                        updateMapMarkers();
                        if (mMap != null) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15));
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        LatLng defaultLoc = new LatLng(31.5204, 74.3587); // Lahore
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 12));

        mMap.setOnMapClickListener(latLng -> {
            if (isSelectingFrom) {
                startLatLng = latLng;
                updateLocationText(latLng, etFrom, fromTextWatcher);
            } else {
                endLatLng = latLng;
                updateLocationText(latLng, etTo, toTextWatcher);
            }
            updateMapMarkers();
        });
        
        updateMapMarkers();
    }

    private void updateLocationText(LatLng latLng, EditText editText, TextWatcher watcher) {
        new Thread(() -> {
            String address = getAddressFromLocation(latLng);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    editText.removeTextChangedListener(watcher);
                    editText.setText(address);
                    editText.addTextChangedListener(watcher);
                });
            }
        }).start();
    }

    private void updateMapMarkers() {
        if (mMap == null) return;
        mMap.clear();
        if (startLatLng != null) mMap.addMarker(new MarkerOptions().position(startLatLng).title("Start"));
        if (endLatLng != null) mMap.addMarker(new MarkerOptions().position(endLatLng).title("Destination"));
    }

    private String getAddressFromLocation(LatLng latLng) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) return addresses.get(0).getAddressLine(0);
        } catch (Exception e) { 
            Log.e("PostRide", "Geocoding failed", e);
        }
        return latLng.latitude + ", " + latLng.longitude;
    }

    private LatLng getLocationFromAddress(String strAddress) {
        Geocoder coder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> address = coder.getFromLocationName(strAddress, 1);
            if (address != null && !address.isEmpty()) return new LatLng(address.get(0).getLatitude(), address.get(0).getLongitude());
        } catch (Exception e) { 
            Log.e("PostRide", "Geocoding failed", e);
        }
        return null;
    }

    private void handlePostRide() {
        String from = etFrom.getText().toString().trim();
        String to = etTo.getText().toString().trim();
        String seatsStr = etSeats.getText().toString().trim();
        String selectedTime = etTime.getText().toString();
        
        StringBuilder daysBuilder = new StringBuilder();
        List<Integer> ids = chipGroupDays.getCheckedChipIds();
        for (Integer id : ids) {
            Chip chip = chipGroupDays.findViewById(id);
            if (daysBuilder.length() > 0) daysBuilder.append(", ");
            daysBuilder.append(chip.getText().toString());
        }
        String days = daysBuilder.toString();

        if (TextUtils.isEmpty(from) || TextUtils.isEmpty(to) || TextUtils.isEmpty(seatsStr) || TextUtils.isEmpty(days) || TextUtils.isEmpty(selectedTime)) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int seats = Integer.parseInt(seatsStr);
        String rideId = UUID.randomUUID().toString();
        String driverId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(driverId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("name");
                String phone = doc.getString("phoneNumber");
                Ride ride = new Ride(rideId, driverId, name, "", phone, 4.5, from, to, 
                        startLatLng != null ? startLatLng.latitude : 0, startLatLng != null ? startLatLng.longitude : 0,
                        endLatLng != null ? endLatLng.latitude : 0, endLatLng != null ? endLatLng.longitude : 0,
                        selectedTime, days, seats, "active");

                db.collection("rides").document(rideId).set(ride)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Ride Posted!", Toast.LENGTH_SHORT).show();
                            if (getActivity() != null) getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new DiscoverFragment()).commit();
                        });
            }
        });
    }
}
