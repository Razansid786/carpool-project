package com.example.carpool_project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;

public class RideDetailsBottomSheet extends BottomSheetDialogFragment implements OnMapReadyCallback {

    private Ride ride;
    private GoogleMap mMap;
    private boolean isMyRideTab = false;

    public RideDetailsBottomSheet() {
        // Required empty public constructor
    }

    public static RideDetailsBottomSheet newInstance(Ride ride) {
        return newInstance(ride, false);
    }

    public static RideDetailsBottomSheet newInstance(Ride ride, boolean isMyRideTab) {
        RideDetailsBottomSheet fragment = new RideDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable("ride", ride);
        args.putBoolean("isMyRideTab", isMyRideTab);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_ride_details_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            ride = (Ride) getArguments().getSerializable("ride");
            isMyRideTab = getArguments().getBoolean("isMyRideTab", false);
        }

        if (ride == null) {
            dismiss();
            return;
        }

        TextView tvDriverName = view.findViewById(R.id.tvDetailDriverName);
        TextView tvRating = view.findViewById(R.id.tvDetailRating);
        TextView tvRoute = view.findViewById(R.id.tvDetailRoute);
        TextView tvSchedule = view.findViewById(R.id.tvDetailSchedule);
        Button btnChat = view.findViewById(R.id.btnChat);
        Button btnBookSeat = view.findViewById(R.id.btnBookSeat);

        tvDriverName.setText(ride.driverName != null ? ride.driverName : "Unknown");
        tvRating.setText(ride.driverRating + " ★");
        tvRoute.setText(ride.origin + " ➔ " + ride.destination);
        tvSchedule.setText(ride.time + ", " + ride.recurringDays);

        String currentUserId = FirebaseAuth.getInstance().getUid();

        // New Logic: 
        // 1. My Post: Hide all buttons
        // 2. My Ride Tab: Show only Chat
        // 3. Discover Feed: Show only Book Seat (No Chat as requested)
        
        if (ride.driverId != null && ride.driverId.equals(currentUserId)) {
            btnChat.setVisibility(View.GONE);
            btnBookSeat.setVisibility(View.GONE);
        } else if (isMyRideTab) {
            btnBookSeat.setVisibility(View.GONE);
            btnChat.setVisibility(View.VISIBLE);
        } else {
            btnChat.setVisibility(View.GONE); // No chat on feed
            btnBookSeat.setVisibility(View.VISIBLE);
            btnBookSeat.setText("Book Seat");
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapDetailContainer);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        btnChat.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("rideId", ride.rideId);
            intent.putExtra("otherUserId", ride.driverId);
            startActivity(intent);
        });

        btnBookSeat.setOnClickListener(v -> {
            // Open the new Negotiation / Stop selection bottom sheet
            BookingOfferBottomSheet offerSheet = BookingOfferBottomSheet.newInstance(ride);
            offerSheet.show(getParentFragmentManager(), "BookingOffer");
            dismiss();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (ride != null) {
            LatLng start = new LatLng(ride.startLat, ride.startLng);
            LatLng end = new LatLng(ride.endLat, ride.endLng);
            mMap.addMarker(new MarkerOptions().position(start).title("Start"));
            mMap.addMarker(new MarkerOptions().position(end).title("End"));
            mMap.addPolyline(new PolylineOptions().add(start, end).width(10).color(Color.parseColor("#2196F3")));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 12));
        }
    }
}
