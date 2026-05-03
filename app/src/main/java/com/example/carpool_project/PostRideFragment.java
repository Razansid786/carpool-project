package com.example.carpool_project;

import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PostRideFragment extends Fragment implements OnMapReadyCallback {

    private EditText etFrom, etTo, etSeats, etTime;
    private ImageButton btnClearFrom, btnClearTo;
    private ChipGroup chipGroupDays;
    private TabLayout tabLayoutRideType;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private GoogleMap mMap;
    private LatLng startLatLng, endLatLng;
    private boolean isSelectingFrom = true;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private TextWatcher fromTextWatcher, toTextWatcher;
    private boolean isMapInitialized = false;
    private String workplaceAddress = "", homeAddress = "";
    private LatLng workplaceLatLng = null, homeLatLng = null;
    private String userCity = "";
    private String userCountry = "";
    private RecyclerView rvSuggestions;
    private boolean isSelfChange = false;

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
        btnClearFrom = view.findViewById(R.id.btnClearFrom);
        btnClearTo = view.findViewById(R.id.btnClearTo);
        etSeats = view.findViewById(R.id.etSeats);
        etTime = view.findViewById(R.id.etTime);
        chipGroupDays = view.findViewById(R.id.chipGroupDays);
        Button btnPostCommute = view.findViewById(R.id.btnPostCommute);
        tabLayoutRideType = view.findViewById(R.id.tabLayoutRideType);
        rvSuggestions = view.findViewById(R.id.rvSuggestions);

        fetchUserProfile();

        etTime.setOnClickListener(v -> showTimePicker());
        btnPostCommute.setOnClickListener(v -> handlePostRide());

        btnClearFrom.setOnClickListener(v -> {
            if (etFrom.isEnabled()) {
                etFrom.setText("");
                startLatLng = null;
                updateMapMarkers();
            }
        });

        btnClearTo.setOnClickListener(v -> {
            if (etTo.isEnabled()) {
                etTo.setText("");
                endLatLng = null;
                updateMapMarkers();
            }
        });

        etFrom.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && etFrom.isEnabled()) {
                isSelectingFrom = true;
                ensureMapInitialized();
            } else {
                rvSuggestions.setVisibility(View.GONE);
            }
        });

        etTo.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && etTo.isEnabled()) {
                isSelectingFrom = false;
                ensureMapInitialized();
            } else {
                rvSuggestions.setVisibility(View.GONE);
            }
        });

        tabLayoutRideType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateFieldsForRideType(tab.getPosition());
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        setupSearchListeners();
    }

    private void fetchUserProfile() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                workplaceAddress = doc.getString("workplaceAddress");
                if (workplaceAddress == null) workplaceAddress = doc.getString("workplace");
                
                Double wLat = doc.getDouble("workplaceLat");
                Double wLng = doc.getDouble("workplaceLng");
                if (wLat != null && wLng != null && wLat != 0) {
                    workplaceLatLng = new LatLng(wLat, wLng);
                }

                homeAddress = doc.getString("homeAddress");
                Double hLat = doc.getDouble("homeLat");
                Double hLng = doc.getDouble("homeLng");
                if (hLat != null && hLng != null && hLat != 0) {
                    homeLatLng = new LatLng(hLat, hLng);
                }
                
                userCity = doc.getString("city");
                userCountry = doc.getString("country");
                
                updateFieldsForRideType(tabLayoutRideType.getSelectedTabPosition());
            }
        });
    }

    private void updateFieldsForRideType(int position) {
        isSelfChange = true;
        if (position == 0) {
            etTo.setText(workplaceAddress != null ? workplaceAddress : "");
            etTo.setEnabled(false);
            btnClearTo.setVisibility(View.GONE);
            etFrom.setText(homeAddress != null ? homeAddress : "");
            etFrom.setEnabled(true);
            btnClearFrom.setVisibility(View.VISIBLE);
            startLatLng = homeLatLng;
            endLatLng = workplaceLatLng;
        } else {
            etFrom.setText(workplaceAddress != null ? workplaceAddress : "");
            etFrom.setEnabled(false);
            btnClearFrom.setVisibility(View.GONE);
            etTo.setText(homeAddress != null ? homeAddress : "");
            etTo.setEnabled(true);
            btnClearTo.setVisibility(View.VISIBLE);
            startLatLng = workplaceLatLng;
            endLatLng = homeLatLng;
        }
        isSelfChange = false;
        ensureMapInitialized();
        updateMapMarkers();
    }

    private void showTimePicker() {
        if (getContext() == null) return;
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
                if (isSelfChange) return;
                searchHandler.removeCallbacksAndMessages(null);
                if (s.length() > 2) {
                    searchHandler.postDelayed(() -> performSearch(s.toString(), isFrom), 400);
                } else {
                    rvSuggestions.setVisibility(View.GONE);
                }
            }
        };
    }

    private void performSearch(String query, boolean isFrom) {
        if (getContext() == null) return;
        new Thread(() -> {
            Geocoder coder = new Geocoder(getContext(), Locale.getDefault());
            try {
                StringBuilder biasedQuery = new StringBuilder(query);
                if (!userCity.isEmpty() && !query.toLowerCase().contains(userCity.toLowerCase())) {
                    biasedQuery.append(", ").append(userCity);
                }
                List<Address> addresses = coder.getFromLocationName(biasedQuery.toString(), 15);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (addresses != null && !addresses.isEmpty()) {
                            sortAndShowSuggestions(addresses, isFrom, query);
                        } else {
                            rvSuggestions.setVisibility(View.GONE);
                        }
                    });
                }
            } catch (IOException e) {
                Log.e("PostRide", "Search failed", e);
            }
        }).start();
    }

    private void sortAndShowSuggestions(List<Address> addresses, boolean isFrom, String query) {
        rvSuggestions.setVisibility(View.VISIBLE);
        rvSuggestions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSuggestions.setAdapter(new SuggestionAdapter(addresses, address -> {
            LatLng loc = new LatLng(address.getLatitude(), address.getLongitude());
            isSelfChange = true;
            String readableName = address.getFeatureName();
            if (readableName == null || readableName.length() < 3) readableName = address.getAddressLine(0);
            
            if (isFrom) {
                startLatLng = loc;
                etFrom.setText(readableName);
            } else {
                endLatLng = loc;
                etTo.setText(readableName);
            }
            isSelfChange = false;
            rvSuggestions.setVisibility(View.GONE);
            updateMapMarkers();
        }));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        LatLng defaultLoc = new LatLng(31.5204, 74.3587);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 12));

        mMap.setOnMapClickListener(latLng -> {
            if (isSelectingFrom && etFrom.isEnabled()) {
                startLatLng = latLng;
                updateLocationText(latLng, etFrom);
            } else if (!isSelectingFrom && etTo.isEnabled()) {
                endLatLng = latLng;
                updateLocationText(latLng, etTo);
            }
            updateMapMarkers();
        });
        
        updateMapMarkers();
    }

    private void updateLocationText(LatLng latLng, EditText editText) {
        new Thread(() -> {
            String address = getAddressFromLocation(latLng);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    isSelfChange = true;
                    editText.setText(address);
                    isSelfChange = false;
                    updateMapMarkers();
                });
            }
        }).start();
    }

    private void updateMapMarkers() {
        if (mMap == null) return;
        mMap.clear();
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        boolean hasMarkers = false;

        if (startLatLng != null) {
            mMap.addMarker(new MarkerOptions().position(startLatLng).title("Start").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            builder.include(startLatLng);
            hasMarkers = true;
        }
        if (endLatLng != null) {
            mMap.addMarker(new MarkerOptions().position(endLatLng).title("Destination").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            builder.include(endLatLng);
            hasMarkers = true;
        }

        if (startLatLng != null && endLatLng != null) {
            mMap.addPolyline(new PolylineOptions().add(startLatLng, endLatLng).width(10).color(Color.parseColor("#6366F1")).geodesic(true));
            try { mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150)); } catch (Exception ignored) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 15));
            }
        } else if (hasMarkers) {
            LatLng point = (startLatLng != null) ? startLatLng : endLatLng;
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 15));
        }
    }

    private String getAddressFromLocation(LatLng latLng) {
        if (getContext() == null) return latLng.latitude + ", " + latLng.longitude;
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String feature = addresses.get(0).getFeatureName();
                return (feature != null && feature.length() > 3) ? feature : addresses.get(0).getAddressLine(0);
            }
        } catch (Exception ignored) {}
        return latLng.latitude + ", " + latLng.longitude;
    }

    private void handlePostRide() {
        String from = etFrom.getText().toString().trim();
        String to = etTo.getText().toString().trim();
        String seatsStr = etSeats.getText().toString().trim();
        String selectedTime = etTime.getText().toString();
        String rideType = tabLayoutRideType.getSelectedTabPosition() == 0 ? "pickup" : "dropoff";
        
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

        // Geocoding fallback
        if (startLatLng == null || endLatLng == null) {
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            try {
                if (startLatLng == null) {
                    List<Address> list = geocoder.getFromLocationName(from + ", " + userCity, 1);
                    if (list != null && !list.isEmpty()) startLatLng = new LatLng(list.get(0).getLatitude(), list.get(0).getLongitude());
                }
                if (endLatLng == null) {
                    List<Address> list = geocoder.getFromLocationName(to + ", " + userCity, 1);
                    if (list != null && !list.isEmpty()) endLatLng = new LatLng(list.get(0).getLatitude(), list.get(0).getLongitude());
                }
            } catch (IOException ignored) {}
        }

        int seats = Integer.parseInt(seatsStr);
        String rideId = UUID.randomUUID().toString();
        if (mAuth.getCurrentUser() == null) return;
        String driverId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(driverId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("name");
                String email = doc.getString("email");
                String phone = doc.getString("phoneNumber");
                
                Ride ride = new Ride(rideId, driverId, name, email != null ? email : "", "", phone != null ? phone : "", 4.5, from, to,
                        startLatLng != null ? startLatLng.latitude : 0, startLatLng != null ? startLatLng.longitude : 0,
                        endLatLng != null ? endLatLng.latitude : 0, endLatLng != null ? endLatLng.longitude : 0,
                        selectedTime, days, seats, "active", rideType);

                db.collection("rides").document(rideId).set(ride)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Ride Posted!", Toast.LENGTH_SHORT).show();
                            if (getActivity() != null) {
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.fragment_container, new DiscoverFragment())
                                        .commit();
                            }
                        });
            }
        });
    }

    private static class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {
        private final List<Address> addresses;
        private final OnAddressClickListener listener;
        interface OnAddressClickListener { void onAddressClick(Address address); }
        SuggestionAdapter(List<Address> addresses, OnAddressClickListener listener) {
            this.addresses = addresses;
            this.listener = listener;
        }
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Address address = addresses.get(position);
            holder.tv1.setText(address.getFeatureName() != null ? address.getFeatureName() : "");
            holder.tv2.setText(address.getAddressLine(0));
            holder.tv1.setTextColor(0xFFFFFFFF);
            holder.tv2.setTextColor(0xFFAAAAAA);
            holder.itemView.setOnClickListener(v -> listener.onAddressClick(address));
        }
        @Override
        public int getItemCount() { return addresses.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tv1, tv2;
            ViewHolder(View v) { super(v); tv1 = v.findViewById(android.R.id.text1); tv2 = v.findViewById(android.R.id.text2); }
        }
    }
}
