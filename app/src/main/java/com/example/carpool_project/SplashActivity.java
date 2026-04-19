package com.example.carpool_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView carImage = findViewById(R.id.car_image);

        // Initial position: off-screen to the left
        carImage.setTranslationX(-1000f);

        // Slide in animation
        carImage.animate()
                .translationX(0f)
                .setDuration(1000)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    // Wait for 1 second in the center
                    new Handler().postDelayed(() -> {
                        // Slide out to the right
                        carImage.animate()
                                .translationX(1000f)
                                .setDuration(1000)
                                .setInterpolator(new AccelerateInterpolator())
                                .withEndAction(() -> {
                                    navigateToNext();
                                })
                                .start();
                    }, 1000);
                })
                .start();
    }

    private void navigateToNext() {
        SharedPreferences preferences = getSharedPreferences("onboarding", MODE_PRIVATE);
        boolean isFirstTime = preferences.getBoolean("isFirstTime", true);

//        if (isFirstTime) {
//            startActivity(new Intent(SplashActivity.this, OnboardingActivity.class));
//        } else {
//            // Later this will go to Login/Signup, for now let's go to MainActivity
//            startActivity(new Intent(SplashActivity.this, MainActivity.class));
//        }
        startActivity(new Intent(SplashActivity.this, OnboardingActivity.class));
        finish();
    }
}