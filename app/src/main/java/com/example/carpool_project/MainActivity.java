package com.example.carpool_project;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Example: Saving a new ride
        saveRide(new Ride("1", "user_123", "Downtown", "University", "08:00 AM", "Mon-Fri",4));
    }

    private void saveRide(Ride ride) {
        db.collection("rides").document(ride.rideId)
                .set(ride)
                .addOnSuccessListener(aVoid -> Log.d("DB", "Ride saved!"))
                .addOnFailureListener(e -> Log.w("DB", "Error saving ride", e));
    }
}
