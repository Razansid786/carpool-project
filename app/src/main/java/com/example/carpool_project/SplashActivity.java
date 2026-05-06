package com.example.carpool_project;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 123;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();

        checkNotificationPermission();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) {
                checkNavigation();
            }
        }, 1500);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void checkNavigation() {
//        SharedPreferences preferences = getSharedPreferences("onboarding", MODE_PRIVATE);
//        boolean isFirstTime = preferences.getBoolean("isFirstTime", true);
//
//        if (isFirstTime) {
//            startActivity(new Intent(SplashActivity.this, OnboardingActivity.class));
//        } else {
//            FirebaseUser currentUser = mAuth.getCurrentUser();
//            if (currentUser != null) {
//                // User is signed in, go to MainActivity
//                startActivity(new Intent(SplashActivity.this, MainActivity.class));
//            } else {
//                // No user is signed in, go to LoginActivity
//                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
//            }
//        }
        startActivity(new Intent(SplashActivity.this, OnboardingActivity.class));

        finish();
    }
}
