package com.example.carpool_project;

import androidx.core.widget.NestedScrollView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SignupActivity extends AppCompatActivity implements OnMapReadyCallback {

    private EditText etName, etEmail, etPhone, etCity, etCountry, etPassword;
    private EditText etWorkplace, etWorkplaceAddress, etHomeAddress;
    private Spinner spinnerRole;
    private TextView tvRoleLabel;
    private Button btnSignup;
    private TextView tvLogin;
    private ImageView ivAnimatedCar, btnSearchWorkLocation, btnSearchHomeLocation;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private MapView mapView;
    private GoogleMap googleMap;
    private MaterialCardView cardSignupMap;
    private NestedScrollView signupScrollView;
    private RecyclerView rvSuggestions;
    
    private double workplaceLat = 0, workplaceLng = 0;
    private double homeLat = 0, homeLng = 0;
    private boolean isSearchingWork = true;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private boolean isSelfChange = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etCity = findViewById(R.id.etCity);
        etCountry = findViewById(R.id.etCountry);
        etPassword = findViewById(R.id.etPassword);
        
        etWorkplace = findViewById(R.id.etWorkplace);
        etWorkplaceAddress = findViewById(R.id.etWorkplaceAddress);
        etHomeAddress = findViewById(R.id.etHomeAddress);
        
        spinnerRole = findViewById(R.id.spinnerRole);
        tvRoleLabel = findViewById(R.id.tvRoleLabel);
        btnSignup = findViewById(R.id.btnSignup);
        tvLogin = findViewById(R.id.tvLogin);
        ivAnimatedCar = findViewById(R.id.ivAnimatedCar);

        btnSearchWorkLocation = findViewById(R.id.btnSearchWorkLocation);
        btnSearchHomeLocation = findViewById(R.id.btnSearchHomeLocation);
        mapView = findViewById(R.id.signupMapView);
        cardSignupMap = findViewById(R.id.cardSignupMap);
        signupScrollView = findViewById(R.id.signupScrollView);
        rvSuggestions = findViewById(R.id.rvSuggestions);

        mapView.onCreate(savedInstanceState);

        setupAddressSearch();
        fixMapScrolling();

        String[] roles = {"Student", "Teacher"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRole = parent.getItemAtPosition(position).toString();
                tvRoleLabel.setText("Selected Role: " + selectedRole);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                tvRoleLabel.setText("Your Role");
            }
        });

        Animation carAnim = AnimationUtils.loadAnimation(this, R.anim.car_animation);
        if (ivAnimatedCar != null) {
            ivAnimatedCar.startAnimation(carAnim);
        }

        btnSignup.setOnClickListener(v -> handleSignup());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void fixMapScrolling() {
        mapView.setOnTouchListener((v, event) -> {
            if (signupScrollView == null) return false;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    signupScrollView.requestDisallowInterceptTouchEvent(true);
                    return false;
                case MotionEvent.ACTION_UP:
                    signupScrollView.requestDisallowInterceptTouchEvent(false);
                    return false;
                default:
                    return false;
            }
        });
    }

    private void setupAddressSearch() {
        etWorkplaceAddress.addTextChangedListener(createWatcher(true));
        etHomeAddress.addTextChangedListener(createWatcher(false));

        etWorkplaceAddress.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                isSearchingWork = true;
                showMapFor(workplaceLat, workplaceLng);
            }
        });

        etHomeAddress.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                isSearchingWork = false;
                showMapFor(homeLat, homeLng);
            }
        });

        btnSearchWorkLocation.setOnClickListener(v -> performSearch(etWorkplaceAddress.getText().toString(), true));
        btnSearchHomeLocation.setOnClickListener(v -> performSearch(etHomeAddress.getText().toString(), false));
    }

    private TextWatcher createWatcher(boolean forWork) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isSelfChange) return;
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
        cardSignupMap.setVisibility(View.VISIBLE);
        if (googleMap != null) {
            if (lat != 0) {
                LatLng loc = new LatLng(lat, lng);
                updateMarker(loc);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15));
            }
        } else {
            mapView.getMapAsync(this);
        }
    }

    private void performSearch(String query, boolean forWork) {
        if (query.isEmpty()) return;
        
        new Thread(() -> {
            Geocoder coder = new Geocoder(this, Locale.getDefault());
            try {
                String city = etCity.getText().toString().trim();
                String country = etCountry.getText().toString().trim();
                StringBuilder biasedQuery = new StringBuilder(query);
                if (!city.isEmpty() && !query.toLowerCase().contains(city.toLowerCase())) {
                    biasedQuery.append(", ").append(city);
                }
                if (!country.isEmpty() && !query.toLowerCase().contains(country.toLowerCase())) {
                    biasedQuery.append(", ").append(country);
                }
                
                List<Address> addresses = coder.getFromLocationName(biasedQuery.toString(), 10);
                
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        sortAndShowSuggestions(addresses, query, forWork);
                    } else {
                        rvSuggestions.setVisibility(View.GONE);
                    }
                });
            } catch (IOException e) {
                Log.e("Signup", "Search failed", e);
            }
        }).start();
    }

    private void sortAndShowSuggestions(List<Address> addresses, String query, boolean forWork) {
        String lowerQuery = query.toLowerCase().trim();
        Collections.sort(addresses, (a, b) -> {
            String f1 = a.getFeatureName() != null ? a.getFeatureName().toLowerCase() : "";
            String f2 = b.getFeatureName() != null ? b.getFeatureName().toLowerCase() : "";
            boolean exact1 = f1.equals(lowerQuery);
            boolean exact2 = f2.equals(lowerQuery);
            if (exact1 && !exact2) return -1;
            if (!exact1 && exact2) return 1;
            return 0;
        });

        rvSuggestions.setVisibility(View.VISIBLE);
        rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
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

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        LatLng startLoc = new LatLng(31.5204, 74.3587); // Default
        
        if (isSearchingWork && workplaceLat != 0) startLoc = new LatLng(workplaceLat, workplaceLng);
        else if (!isSearchingWork && homeLat != 0) startLoc = new LatLng(homeLat, homeLng);
        
        updateMarker(startLoc);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLoc, 12));

        googleMap.setOnMapClickListener(latLng -> {
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
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                isSelfChange = true;
                et.setText(addresses.get(0).getAddressLine(0));
                isSelfChange = false;
            }
        } catch (Exception e) {
            Log.e("Signup", "Geocoding failed", e);
        }
    }

    private void handleSignup() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String country = etCountry.getText().toString().trim();
        String workplace = etWorkplace.getText().toString().trim();
        String workplaceAddr = etWorkplaceAddress.getText().toString().trim();
        String homeAddr = etHomeAddress.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();

        if (TextUtils.isEmpty(name)) { etName.setError("Name required"); return; }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.setError("Valid email required"); return; }
        if (TextUtils.isEmpty(phone) || phone.length() < 10) { etPhone.setError("Valid phone required"); return; }
        if (TextUtils.isEmpty(city)) { etCity.setError("City required"); return; }
        if (TextUtils.isEmpty(country)) { etCountry.setError("Country required"); return; }
        if (TextUtils.isEmpty(workplace)) { etWorkplace.setError("Workplace required"); return; }
        if (TextUtils.isEmpty(workplaceAddr)) { etWorkplaceAddress.setError("Workplace address required"); return; }
        if (TextUtils.isEmpty(homeAddr)) { etHomeAddress.setError("Home address required"); return; }
        if (TextUtils.isEmpty(password) || password.length() < 6) { etPassword.setError("Min 6 characters"); return; }

        btnSignup.setEnabled(false);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verificationTask -> {
                                        if (verificationTask.isSuccessful()) {
                                            Log.d("Signup", "Verification email sent to " + email);
                                        }
                                    });
                            saveUserToFirestore(user.getUid(), name, email, phone, role, city, country, password, workplace, workplaceAddr, homeAddr);
                        }
                    } else {
                        btnSignup.setEnabled(true);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                        Toast.makeText(SignupActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String userId, String name, String email, String phone, String role, String city, String country, String password, String workplace, String workplaceAddr, String homeAddr) {
        Person person = new Person(userId, name, email, role, "", password, phone, city, country, workplace, workplaceAddr, homeAddr);
        person.workplaceLat = workplaceLat;
        person.workplaceLng = workplaceLng;
        person.homeLat = homeLat;
        person.homeLng = homeLng;

        db.collection("users").document(userId)
                .set(person)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "User profile saved: " + userId);
                    sendNotificationToFirestore(userId, "Welcome to Carpool!", "Successfully signed up. Please check your email for verification.");
                    NotificationHelper.showNotification(this, "Welcome!", "Signup successful. Check email.");
                    
                    Toast.makeText(SignupActivity.this, "Signup successful! Verification email sent.", Toast.LENGTH_LONG).show();
                    new android.os.Handler().postDelayed(() -> {
                        startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                        finish();
                    }, 1500);
                })
                .addOnFailureListener(e -> {
                    btnSignup.setEnabled(true);
                    Toast.makeText(SignupActivity.this, "Error saving user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendNotificationToFirestore(String userId, String title, String message) {
        String id = UUID.randomUUID().toString();
        Notification notification = new Notification(id, userId, title, message, System.currentTimeMillis());
        db.collection("notifications").document(id).set(notification);
    }

    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override protected void onStop() { super.onStop(); mapView.onStop(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    private static class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {
        private final List<Address> addresses;
        private final OnAddressClickListener listener;
        SuggestionAdapter(List<Address> addresses, OnAddressClickListener listener) {
            this.addresses = addresses;
            this.listener = listener;
        }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Address address = addresses.get(position);
            holder.tv1.setText(address.getFeatureName());
            holder.tv2.setText(address.getAddressLine(0));
            holder.tv1.setTextColor(0xFFFFFFFF);
            holder.tv2.setTextColor(0xFFAAAAAA);
            holder.itemView.setOnClickListener(v -> listener.onAddressClick(address));
        }
        @Override public int getItemCount() { return addresses.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tv1, tv2;
            ViewHolder(View v) { super(v); tv1 = v.findViewById(android.R.id.text1); tv2 = v.findViewById(android.R.id.text2); }
        }
        interface OnAddressClickListener { void onAddressClick(Address address); }
    }
}
