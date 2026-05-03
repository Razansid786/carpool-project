package com.example.carpool_project;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment implements OnMapReadyCallback {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText etWorkplace, etWorkplaceAddress, etHomeAddress, etCity, etCountry;
    private TextView tvName;
    private MaterialButton btnSave, btnLogout, btnEditProfile;
    private ImageView btnSearchWorkLocation, btnSearchHomeLocation;
    private MapView mapView;
    private GoogleMap googleMap;
    private MaterialCardView cardProfileMap;
    private NestedScrollView profileScrollView;
    private RecyclerView rvSuggestions;
    private double workplaceLat = 0, workplaceLng = 0;
    private double homeLat = 0, homeLng = 0;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private boolean isSelfChange = false;
    private String userCity = "", userCountry = "";
    private boolean isEditing = false;
    private boolean isSearchingWork = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvName = view.findViewById(R.id.tvProfileName);
        etCity = view.findViewById(R.id.etCity);
        etCountry = view.findViewById(R.id.etCountry);
        etWorkplace = view.findViewById(R.id.etWorkplace);
        etWorkplaceAddress = view.findViewById(R.id.etWorkplaceAddress);
        etHomeAddress = view.findViewById(R.id.etHomeAddress);
        btnSearchWorkLocation = view.findViewById(R.id.btnSearchWorkLocation);
        btnSearchHomeLocation = view.findViewById(R.id.btnSearchHomeLocation);
        btnSave = view.findViewById(R.id.btnSaveProfile);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        mapView = view.findViewById(R.id.profileMapView);
        cardProfileMap = view.findViewById(R.id.cardProfileMap);
        profileScrollView = view.findViewById(R.id.profileScrollView);
        rvSuggestions = view.findViewById(R.id.rvSuggestions);

        mapView.onCreate(savedInstanceState);
        
        loadProfileData();
        setupAddressSearch();
        fixMapScrolling();

        btnEditProfile.setOnClickListener(v -> toggleEditMode());
        btnSave.setOnClickListener(v -> saveProfileData());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });

        return view;
    }

    private void toggleEditMode() {
        isEditing = !isEditing;
        etCity.setEnabled(isEditing);
        etCountry.setEnabled(isEditing);
        etWorkplace.setEnabled(isEditing);
        etWorkplaceAddress.setEnabled(isEditing);
        etHomeAddress.setEnabled(isEditing);
        btnSave.setVisibility(isEditing ? View.VISIBLE : View.GONE);
        btnEditProfile.setText(isEditing ? "Cancel" : "Edit");
        if (!isEditing) rvSuggestions.setVisibility(View.GONE);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void fixMapScrolling() {
        mapView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    profileScrollView.requestDisallowInterceptTouchEvent(true);
                    return false;
                case MotionEvent.ACTION_UP:
                    profileScrollView.requestDisallowInterceptTouchEvent(false);
                    return true;
                default:
                    return true;
            }
        });
    }

    private void setupAddressSearch() {
        etWorkplaceAddress.addTextChangedListener(createWatcher(true));
        etHomeAddress.addTextChangedListener(createWatcher(false));

        etWorkplaceAddress.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && isEditing) {
                isSearchingWork = true;
                showMapFor(workplaceLat, workplaceLng);
            }
        });

        etHomeAddress.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && isEditing) {
                isSearchingWork = false;
                showMapFor(homeLat, homeLng);
            }
        });

        btnSearchWorkLocation.setOnClickListener(v -> {
            if (isEditing) performSearch(etWorkplaceAddress.getText().toString(), true);
        });

        btnSearchHomeLocation.setOnClickListener(v -> {
            if (isEditing) performSearch(etHomeAddress.getText().toString(), false);
        });
    }

    private TextWatcher createWatcher(boolean forWork) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (isSelfChange || !isEditing) return;
                searchHandler.removeCallbacksAndMessages(null);
                if (s.length() > 2) {
                    searchHandler.postDelayed(() -> performSearch(s.toString(), forWork), 400);
                } else {
                    rvSuggestions.setVisibility(View.GONE);
                }
            }
        };
    }

    private void showMapFor(double lat, double lng) {
        cardProfileMap.setVisibility(View.VISIBLE);
        if (googleMap != null && lat != 0) {
            LatLng loc = new LatLng(lat, lng);
            updateMarker(loc);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15));
        } else {
            mapView.getMapAsync(this);
        }
    }

    private void performSearch(String query, boolean forWork) {
        if (query.isEmpty()) return;
        Context context = getContext();
        if (context == null) return;
        
        new Thread(() -> {
            Geocoder coder = new Geocoder(context, Locale.getDefault());
            try {
                StringBuilder biasedQuery = new StringBuilder(query);
                if (!userCity.isEmpty() && !query.toLowerCase().contains(userCity.toLowerCase())) {
                    biasedQuery.append(", ").append(userCity);
                }
                if (!userCountry.isEmpty() && !query.toLowerCase().contains(userCountry.toLowerCase())) {
                    biasedQuery.append(", ").append(userCountry);
                }
                
                List<Address> addresses = coder.getFromLocationName(biasedQuery.toString(), 25);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (addresses != null && !addresses.isEmpty()) {
                            sortAndShowSuggestions(addresses, query, forWork);
                        } else {
                            rvSuggestions.setVisibility(View.GONE);
                        }
                    });
                }
            } catch (IOException e) {
                Log.e("Profile", "Search failed", e);
            }
        }).start();
    }

    private void sortAndShowSuggestions(List<Address> addresses, String query, boolean forWork) {
        String lowerQuery = query.toLowerCase().trim();
        Collections.sort(addresses, (a, b) -> {
            String f1 = a.getFeatureName() != null ? a.getFeatureName().toLowerCase() : "";
            String f2 = b.getFeatureName() != null ? b.getFeatureName().toLowerCase() : "";
            String full1 = a.getAddressLine(0).toLowerCase();
            String full2 = b.getAddressLine(0).toLowerCase();
            boolean exact1 = f1.equals(lowerQuery);
            boolean exact2 = f2.equals(lowerQuery);
            if (exact1 && !exact2) return -1;
            if (!exact1 && exact2) return 1;
            boolean startF1 = f1.startsWith(lowerQuery);
            boolean startF2 = f2.startsWith(lowerQuery);
            if (startF1 && !startF2) return -1;
            if (!startF1 && startF2) return 1;
            return full1.length() - full2.length();
        });

        rvSuggestions.setVisibility(View.VISIBLE);
        rvSuggestions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSuggestions.setAdapter(new SuggestionAdapter(addresses, address -> {
            LatLng loc = new LatLng(address.getLatitude(), address.getLongitude());
            isSelfChange = true;
            if (forWork) {
                workplaceLat = loc.latitude;
                workplaceLng = loc.longitude;
                etWorkplaceAddress.setText(address.getAddressLine(0));
            } else {
                homeLat = loc.latitude;
                homeLng = loc.longitude;
                etHomeAddress.setText(address.getAddressLine(0));
            }
            isSelfChange = false;
            rvSuggestions.setVisibility(View.GONE);
            updateMarker(loc);
            if (googleMap != null) googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15));
        }));
    }

    private void loadProfileData() {
        if (mAuth.getCurrentUser() == null) return;
        db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                tvName.setText(doc.getString("name"));
                userCity = doc.getString("city");
                userCountry = doc.getString("country");
                if (userCity == null) userCity = "";
                if (userCountry == null) userCountry = "";
                etCity.setText(userCity);
                etCountry.setText(userCountry);
                etWorkplace.setText(doc.getString("workplace"));
                
                isSelfChange = true;
                etWorkplaceAddress.setText(doc.getString("workplaceAddress"));
                etHomeAddress.setText(doc.getString("homeAddress"));
                isSelfChange = false;
                
                workplaceLat = doc.getDouble("workplaceLat") != null ? doc.getDouble("workplaceLat") : 0;
                workplaceLng = doc.getDouble("workplaceLng") != null ? doc.getDouble("workplaceLng") : 0;
                homeLat = doc.getDouble("homeLat") != null ? doc.getDouble("homeLat") : 0;
                homeLng = doc.getDouble("homeLng") != null ? doc.getDouble("homeLng") : 0;
                
                if (workplaceLat != 0) {
                    cardProfileMap.setVisibility(View.VISIBLE);
                    mapView.getMapAsync(this);
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        LatLng startLoc = new LatLng(31.5204, 74.3587);
        if (isSearchingWork && workplaceLat != 0) startLoc = new LatLng(workplaceLat, workplaceLng);
        else if (!isSearchingWork && homeLat != 0) startLoc = new LatLng(homeLat, homeLng);
        
        updateMarker(startLoc);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLoc, 12));

        googleMap.setOnMapClickListener(latLng -> {
            if (!isEditing) return;
            if (isSearchingWork) {
                workplaceLat = latLng.latitude;
                workplaceLng = latLng.longitude;
                updateAddressFromLatLng(latLng, etWorkplaceAddress);
            } else {
                homeLat = latLng.latitude;
                homeLng = latLng.longitude;
                updateAddressFromLatLng(latLng, etHomeAddress);
            }
            updateMarker(latLng);
        });
    }

    private void updateMarker(LatLng latLng) {
        if (googleMap == null) return;
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
    }

    private void updateAddressFromLatLng(LatLng latLng, EditText et) {
        if (getContext() == null) return;
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                isSelfChange = true;
                et.setText(addresses.get(0).getAddressLine(0));
                isSelfChange = false;
            }
        } catch (Exception e) {
            Log.e("Profile", "Geocoding failed", e);
        }
    }

    private void saveProfileData() {
        if (mAuth.getCurrentUser() == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("city", etCity.getText().toString().trim());
        updates.put("country", etCountry.getText().toString().trim());
        updates.put("workplace", etWorkplace.getText().toString().trim());
        updates.put("workplaceAddress", etWorkplaceAddress.getText().toString().trim());
        updates.put("workplaceLat", workplaceLat);
        updates.put("workplaceLng", workplaceLng);
        updates.put("homeAddress", etHomeAddress.getText().toString().trim());
        updates.put("homeLat", homeLat);
        updates.put("homeLng", homeLng);

        db.collection("users").document(mAuth.getCurrentUser().getUid()).update(updates)
                .addOnSuccessListener(aVoid -> {
                    userCity = etCity.getText().toString().trim();
                    userCountry = etCountry.getText().toString().trim();
                    toggleEditMode();
                    Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onStart() { super.onStart(); if(mapView != null) mapView.onStart(); }
    @Override
    public void onStop() { super.onStop(); if(mapView != null) mapView.onStop(); }
    @Override
    public void onResume() { super.onResume(); if(mapView != null) mapView.onResume(); }
    @Override
    public void onPause() { super.onPause(); if(mapView != null) mapView.onPause(); }
    @Override
    public void onDestroy() { super.onDestroy(); if(mapView != null) mapView.onDestroy(); }
    @Override
    public void onLowMemory() { super.onLowMemory(); if(mapView != null) mapView.onLowMemory(); }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mapView != null) mapView.onSaveInstanceState(outState);
    }

    private static class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {
        private final List<Address> addresses;
        private final OnAddressClickListener listener;
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
            holder.tv1.setText(address.getFeatureName());
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
        interface OnAddressClickListener { void onAddressClick(Address address); }
    }
}
