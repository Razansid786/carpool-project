package com.example.carpool_project;

import android.content.Intent;
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

public class SignupActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etPassword;
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
        etPassword = findViewById(R.id.etPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        tvRoleLabel = findViewById(R.id.tvRoleLabel);
        btnSignup = findViewById(R.id.btnSignup);
        tvLogin = findViewById(R.id.tvLogin);
        ivAnimatedCar = findViewById(R.id.ivAnimatedCar);

        // Setup Spinner
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

        // Start Car Animation
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
        String password = etPassword.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email is required");
            return;
        }
        if (TextUtils.isEmpty(phone) || phone.length() < 10) {
            etPhone.setError("Valid phone number is required");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user.getUid(), name, email, phone, role, password);
                        }
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                        Toast.makeText(SignupActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String userId, String name, String email, String phone, String role, String password) {
        // Create an instance of the Person class with all data
        Person person = new Person(userId, name, email, role, "", password, phone);
        
        // Save the entire object to the "users" collection in Firestore
        db.collection("users").document(userId)
                .set(person)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "User profile saved for: " + userId);
                    Toast.makeText(SignupActivity.this, "Signup successful! Please login.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error saving user", e);
                    Toast.makeText(SignupActivity.this, "Error saving user data", Toast.LENGTH_SHORT).show();
                });
    }
}
