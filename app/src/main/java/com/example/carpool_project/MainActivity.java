package com.example.carpool_project;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class MainActivity extends AppCompatActivity {

    private com.google.firebase.firestore.ListenerRegistration notificationListener;
    private static final int PERMISSION_REQUEST_CODE = 123;
    private long appStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        appStartTime = System.currentTimeMillis();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DiscoverFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();
            if (id == R.id.nav_discover) {
                selectedFragment = new DiscoverFragment();
            } else if (id == R.id.nav_post) {
                selectedFragment = new PostRideFragment();
            } else if (id == R.id.nav_rides) {
                selectedFragment = new RidesFragment();
            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        checkNotificationPermission();
        startNotificationListener();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void startNotificationListener() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        notificationListener = FirebaseFirestore.getInstance().collection("notifications")
                .whereEqualTo("recipientId", uid)
                .whereEqualTo("read", false)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("MainActivity", "Notification listener failed", error);
                        return;
                    }

                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Notification notification = dc.getDocument().toObject(Notification.class);
                                if (notification.timestamp >= appStartTime) {
                                    NotificationHelper.showNotification(this, notification.title, notification.message);
                                }
                            }
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }
    
    public void openNotifications() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new NotificationsFragment())
                .addToBackStack(null)
                .commit();
    }
}
