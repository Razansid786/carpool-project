package com.example.carpool_project;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
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

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SignupActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etCity, etCountry, etPassword;
    private EditText etWorkplace, etWorkplaceAddress, etHomeAddress;
    private Spinner spinnerRole;
    private TextView tvRoleLabel;
    private Button btnSignup;
    private TextView tvLogin;
    private ImageView ivAnimatedCar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

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

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Send automated verification email via Firebase
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verificationTask -> {
                                        if (verificationTask.isSuccessful()) {
                                            Log.d("Signup", "Verification email sent to " + email);
                                        }
                                    });
                            saveUserToFirestore(user.getUid(), name, email, phone, role, city, country, password, workplace, workplaceAddr, homeAddr);
                        }
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                        Toast.makeText(SignupActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String userId, String name, String email, String phone, String role, String city, String country, String password, String workplace, String workplaceAddr, String homeAddr) {
        Person person = new Person(userId, name, email, role, "", password, phone, city, country, workplace, workplaceAddr, homeAddr);
        
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> workLocs = geocoder.getFromLocationName(workplaceAddr + ", " + city, 1);
            if (workLocs != null && !workLocs.isEmpty()) {
                person.workplaceLat = workLocs.get(0).getLatitude();
                person.workplaceLng = workLocs.get(0).getLongitude();
            }
            List<Address> homeLocs = geocoder.getFromLocationName(homeAddr + ", " + city, 1);
            if (homeLocs != null && !homeLocs.isEmpty()) {
                person.homeLat = homeLocs.get(0).getLatitude();
                person.homeLng = homeLocs.get(0).getLongitude();
            }
        } catch (IOException e) {
            Log.e("Signup", "Geocoding failed", e);
        }

        db.collection("users").document(userId)
                .set(person)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "User profile saved: " + userId);
                    sendNotificationToFirestore(userId, "Welcome to Carpool!", "Successfully signed up. Please check your email for verification.");
                    NotificationHelper.showNotification(this, "Welcome!", "Signup successful. Check email.");
                    
                    // Fallback Intent-based email if needed, but Firebase verification is more "receiving"
                    // sendWelcomeEmail(email); 

                    Toast.makeText(SignupActivity.this, "Signup successful! Verification email sent.", Toast.LENGTH_LONG).show();
                    new android.os.Handler().postDelayed(() -> {
                        startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                        finish();
                    }, 1500);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignupActivity.this, "Error saving user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendNotificationToFirestore(String userId, String title, String message) {
        String id = UUID.randomUUID().toString();
        Notification notification = new Notification(id, userId, title, message, System.currentTimeMillis());
        db.collection("notifications").document(id).set(notification);
    }
}
