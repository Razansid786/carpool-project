package com.example.carpool_project;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Using a simple layout to avoid any animation issues causing a black screen
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) {
                checkNavigation();
            }
        }, 1500);
    }

    private void checkNavigation() {
        try {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            if (mAuth.getCurrentUser() != null) {
                Log.d("Splash", "User logged in, going to MainActivity");
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                Log.d("Splash", "User not logged in, going to LoginActivity");
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
        } catch (Exception e) {
            Log.e("Splash", "Error in navigation", e);
            // Fallback
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }
    }
}
