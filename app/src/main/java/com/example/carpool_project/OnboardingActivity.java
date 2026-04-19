package com.example.carpool_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private OnboardingAdapter onboardingAdapter;
    private ViewPager2 viewPager;
    private Button btnGetStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        btnGetStarted = findViewById(R.id.btnGetStarted);
        viewPager = findViewById(R.id.viewPager);

        setupOnboardingItems();

        viewPager.setAdapter(onboardingAdapter);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            // No text needed for dots
        }).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == onboardingAdapter.getItemCount() - 1) {
                    btnGetStarted.setVisibility(View.VISIBLE);
                } else {
                    btnGetStarted.setVisibility(View.INVISIBLE);
                }
            }
        });

        btnGetStarted.setOnClickListener(v -> {
            markOnboardingFinished();
            startActivity(new Intent(OnboardingActivity.this, MainActivity.class));
            finish();
        });
    }

    private void setupOnboardingItems() {
        List<OnboardingItem> onboardingItems = new ArrayList<>();

        onboardingItems.add(new OnboardingItem(
                R.drawable.ic_onboarding_1,
                "Split Costs & Save",
                "Share fuel expenses with commuters."
        ));

        onboardingItems.add(new OnboardingItem(
                R.drawable.ic_onboarding_2,
                "Reduce Traffic & Pollution",
                "Help declog roads and clean the air."
        ));

        onboardingItems.add(new OnboardingItem(
                R.drawable.ic_onboarding_3,
                "Match with Commuters",
                "Find reliable neighbors going your way."
        ));

        onboardingAdapter = new OnboardingAdapter(onboardingItems);
    }

    private void markOnboardingFinished() {
        SharedPreferences preferences = getSharedPreferences("onboarding", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isFirstTime", false);
        editor.apply();
    }
}