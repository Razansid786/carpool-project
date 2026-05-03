package com.example.carpool_project;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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
        ImageButton btnOpenInMaps = view.findViewById(R.id.btnDetailOpenInMaps);
        ImageButton btnZoomIn = view.findViewById(R.id.btnDetailZoomIn);
        ImageButton btnZoomOut = view.findViewById(R.id.btnDetailZoomOut);

        tvDriverName.setText(ride.driverName != null ? ride.driverName : "Unknown");
        tvRating.setText(ride.driverRating + " ★");
        tvRoute.setText(ride.origin + " ➔ " + ride.destination);
        tvSchedule.setText(ride.time + ", " + ride.recurringDays);

        String currentUserId = FirebaseAuth.getInstance().getUid();

        if (ride.driverId != null && ride.driverId.equals(currentUserId)) {
            btnChat.setVisibility(View.GONE);
            btnBookSeat.setVisibility(View.GONE);
        } else if (isMyRideTab) {
            btnBookSeat.setVisibility(View.GONE);
            btnChat.setVisibility(View.VISIBLE);
        } else {
            btnChat.setVisibility(View.GONE);
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
            BookingOfferBottomSheet offerSheet = BookingOfferBottomSheet.newInstance(ride);
            offerSheet.show(getParentFragmentManager(), "BookingOffer");
            dismiss();
        });

        if (btnOpenInMaps != null) {
            btnOpenInMaps.setOnClickListener(v -> {
                if (ride.startLat != 0 && ride.endLat != 0) {
                    String uri = "https://www.google.com/maps/dir/?api=1&origin=" + ride.startLat + "," + ride.startLng + 
                                "&destination=" + ride.endLat + "," + ride.endLng + "&travelmode=driving";
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    intent.setPackage("com.google.android.apps.maps");
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), "Location not available", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        if (btnZoomIn != null) btnZoomIn.setOnClickListener(v -> { if(mMap != null) mMap.animateCamera(CameraUpdateFactory.zoomIn()); });
        if (btnZoomOut != null) btnZoomOut.setOnClickListener(v -> { if(mMap != null) mMap.animateCamera(CameraUpdateFactory.zoomOut()); });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        if (ride != null && ride.startLat != 0) {
            LatLng start = new LatLng(ride.startLat, ride.startLng);
            LatLng end = new LatLng(ride.endLat, ride.endLng);
            mMap.addMarker(new MarkerOptions().position(start).title("Start").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            mMap.addMarker(new MarkerOptions().position(end).title("End").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            mMap.addPolyline(new PolylineOptions().add(start, end).width(10).color(Color.parseColor("#2196F3")));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 12));
        }
    }
}
