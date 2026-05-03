package com.example.carpool_project;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookingOfferBottomSheet extends BottomSheetDialogFragment implements OnMapReadyCallback {

    private Ride ride;
    private GoogleMap mMap;
    private EditText etPickup, etDropoff, etOfferPrice;
    private Spinner spinnerSeats;
    private TextView tvMinRate, tvInstruction;
    private LatLng pickupLatLng, dropoffLatLng;
    private boolean selectingPickup = true;
    private double calculatedMinPrice = 0.0;
    private List<BookingOffer> acceptedOffers = new ArrayList<>();
    private boolean isSelfChange = false;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private boolean isCameraSearching = false;
    private String userCity = "";
    private String userCountry = "";
    private RecyclerView rvSuggestions;

    public static BookingOfferBottomSheet newInstance(Ride ride) {
        BookingOfferBottomSheet fragment = new BookingOfferBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable("ride", ride);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        return inflater.inflate(R.layout.layout_booking_offer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ride = (Ride) getArguments().getSerializable("ride");

        etPickup = view.findViewById(R.id.etPickup);
        etDropoff = view.findViewById(R.id.etDropoff);
        etOfferPrice = view.findViewById(R.id.etOfferPrice);
        spinnerSeats = view.findViewById(R.id.spinnerSeats);
        tvMinRate = view.findViewById(R.id.tvMinRate);
        tvInstruction = view.findViewById(R.id.tvBookingInstruction);
        rvSuggestions = view.findViewById(R.id.rvSuggestions);
        Button btnConfirm = view.findViewById(R.id.btnConfirmBooking);

        dropoffLatLng = new LatLng(ride.endLat, ride.endLng);
        etDropoff.setText(ride.destination);
        etDropoff.setEnabled(false);
        etDropoff.setAlpha(0.7f);

        fetchUserProfile();
        setupSeatsSpinner();
        loadAcceptedOffers();
        setupSearchFunctionality();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.bookingMapContainer);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        etPickup.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                selectingPickup = true;
                tvInstruction.setText("Tap map or type to select Pickup point");
            } else {
                rvSuggestions.setVisibility(View.GONE);
            }
        });

        btnConfirm.setOnClickListener(v -> fetchUserDetailsAndSendOffer());
    }

    private void fetchUserProfile() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userCity = documentSnapshot.getString("city");
                        userCountry = documentSnapshot.getString("country");
                        if (userCity == null) userCity = "";
                        if (userCountry == null) userCountry = "";
                    }
                });
    }

    private void setupSearchFunctionality() {
        TextWatcher searchWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (isSelfChange) return;
                String query = s.toString();
                if (query.length() > 2) {
                    mainHandler.removeCallbacks(searchRunnable);
                    searchRunnable = () -> searchLocation(query);
                    mainHandler.postDelayed(searchRunnable, 400);
                } else {
                    rvSuggestions.setVisibility(View.GONE);
                }
            }
        };
        etPickup.addTextChangedListener(searchWatcher);
        etPickup.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                mainHandler.removeCallbacks(searchRunnable);
                searchLocation(etPickup.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void searchLocation(String locationName) {
        if (TextUtils.isEmpty(locationName)) return;
        executorService.execute(() -> {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            try {
                StringBuilder biasedQuery = new StringBuilder(locationName);
                if (!userCity.isEmpty() && !locationName.toLowerCase().contains(userCity.toLowerCase())) {
                    biasedQuery.append(", ").append(userCity);
                }
                if (!userCountry.isEmpty() && !locationName.toLowerCase().contains(userCountry.toLowerCase())) {
                    biasedQuery.append(", ").append(userCountry);
                }
                List<Address> addresses = geocoder.getFromLocationName(biasedQuery.toString(), 25);
                mainHandler.post(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        sortAndShowSuggestions(addresses, locationName);
                    } else {
                        rvSuggestions.setVisibility(View.GONE);
                    }
                });
            } catch (IOException ignored) {}
        });
    }

    private void sortAndShowSuggestions(List<Address> addresses, String query) {
        String lowerQuery = query.toLowerCase().trim();
        
        Collections.sort(addresses, (a, b) -> {
            String f1 = a.getFeatureName() != null ? a.getFeatureName().toLowerCase() : "";
            String f2 = b.getFeatureName() != null ? b.getFeatureName().toLowerCase() : "";
            String full1 = a.getAddressLine(0).toLowerCase();
            String full2 = b.getAddressLine(0).toLowerCase();

            boolean exact1 = f1.equals(lowerQuery);
            boolean exact2 = f2.equals(lowerQuery);
            if (exact1 && !exact2) return -1;
            if (!exact1 && exact2) return 1;

            boolean starts1 = f1.startsWith(lowerQuery);
            boolean starts2 = f2.startsWith(lowerQuery);
            if (starts1 && !starts2) return -1;
            if (!starts1 && starts2) return 1;

            boolean fullStarts1 = full1.startsWith(lowerQuery);
            boolean fullStarts2 = full2.startsWith(lowerQuery);
            if (fullStarts1 && !fullStarts2) return -1;
            if (!fullStarts1 && fullStarts2) return 1;

            return full1.length() - full2.length();
        });

        rvSuggestions.setVisibility(View.VISIBLE);
        rvSuggestions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSuggestions.setAdapter(new SuggestionAdapter(addresses, address -> {
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            isSelfChange = true;
            String readableName = address.getFeatureName();
            if (readableName == null || readableName.length() < 3) readableName = address.getAddressLine(0);
            
            etPickup.setText(readableName);
            isSelfChange = false;
            
            pickupLatLng = latLng;
            isCameraSearching = true;
            rvSuggestions.setVisibility(View.GONE);
            
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
                updateMarkers();
                calculatePrice();
            }
        }));
    }

    private void setupSeatsSpinner() {
        List<Integer> seats = new ArrayList<>();
        for (int i = 1; i <= ride.seatsAvailable; i++) seats.add(i);
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, seats);
        spinnerSeats.setAdapter(adapter);
    }

    private void loadAcceptedOffers() {
        FirebaseFirestore.getInstance().collection("offers")
                .whereEqualTo("rideId", ride.rideId)
                .whereEqualTo("status", "accepted")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    acceptedOffers.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        acceptedOffers.add(doc.toObject(BookingOffer.class));
                    }
                    if (mMap != null) updateMarkers();
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        updateMarkers();
        mMap.setOnMapClickListener(latLng -> {
            isCameraSearching = false;
            pickupLatLng = latLng;
            updateAddress(latLng, etPickup);
            updateMarkers();
            calculatePrice();
        });
    }

    private void updateAddress(LatLng latLng, EditText et) {
        isSelfChange = true;
        executorService.execute(() -> {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (!addresses.isEmpty()) {
                    String readable = addresses.get(0).getFeatureName();
                    if (readable == null || readable.length() < 3) readable = addresses.get(0).getAddressLine(0);
                    
                    final String addressLine = readable;
                    mainHandler.post(() -> {
                        et.setText(addressLine);
                        isSelfChange = false;
                    });
                }
            } catch (IOException ignored) {
                isSelfChange = false;
            }
        });
    }

    private void updateMarkers() {
        if (mMap == null) return;
        mMap.clear();
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        boolean hasPoints = false;
        if (ride.startLat != 0 && ride.startLng != 0) {
            LatLng start = new LatLng(ride.startLat, ride.startLng);
            LatLng end = new LatLng(ride.endLat, ride.endLng);
            mMap.addMarker(new MarkerOptions().position(start).title("Ride Start").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            mMap.addMarker(new MarkerOptions().position(end).title("Ride End").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            mMap.addPolyline(new PolylineOptions().add(start, end).color(0x7FFF0000).width(5));
            builder.include(start); builder.include(end);
            hasPoints = true;
        }
        if (pickupLatLng != null && dropoffLatLng != null) {
            mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Your Pickup").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            mMap.addMarker(new MarkerOptions().position(dropoffLatLng).title("Destination").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            mMap.addPolyline(new PolylineOptions().add(pickupLatLng, dropoffLatLng).color(0x7F00FF00).width(8));
            builder.include(pickupLatLng); builder.include(dropoffLatLng);
            hasPoints = true;
        }
        if (hasPoints && !isCameraSearching) {
            try { mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100)); } catch (IllegalStateException ignored) {}
        }
    }

    private void calculatePrice() {
        if (pickupLatLng != null && dropoffLatLng != null) {
            float[] results = new float[1];
            android.location.Location.distanceBetween(pickupLatLng.latitude, pickupLatLng.longitude, 
                    dropoffLatLng.latitude, dropoffLatLng.longitude, results);
            double distanceKm = results[0] / 1000.0;
            double estimatedTimeMin = distanceKm * 1.5; 
            calculatedMinPrice = 300 + (distanceKm * 10) + (estimatedTimeMin * 5);
            tvMinRate.setText(String.format(Locale.US, "Min Offer: Rs. %.0f", calculatedMinPrice));
        }
    }

    private void fetchUserDetailsAndSendOffer() {
        String priceStr = etOfferPrice.getText().toString();
        if (pickupLatLng == null || dropoffLatLng == null || TextUtils.isEmpty(priceStr)) {
            Toast.makeText(getContext(), "Please select a pickup point", Toast.LENGTH_SHORT).show();
            return;
        }
        double offeredPrice = Double.parseDouble(priceStr);
        if (offeredPrice < calculatedMinPrice) {
            Toast.makeText(getContext(), "Offer must be at least Rs. " + (int)calculatedMinPrice, Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        sendOffer(userId, name, email, offeredPrice);
                    } else {
                        sendOffer(userId, "Passenger", "No email", offeredPrice);
                    }
                })
                .addOnFailureListener(e -> {
                    sendOffer(userId, "Passenger", "No email", offeredPrice);
                });
    }

    private void sendOffer(String userId, String name, String email, double offeredPrice) {
        String offerId = UUID.randomUUID().toString();
        BookingOffer offer = new BookingOffer(offerId, ride.rideId, userId, name, email, 
                ride.driverId, pickupLatLng.latitude, pickupLatLng.longitude, dropoffLatLng.latitude, dropoffLatLng.longitude,
                etPickup.getText().toString(), etDropoff.getText().toString(), offeredPrice, 
                (Integer) spinnerSeats.getSelectedItem(), "pending", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("offers").document(offerId).set(offer)
                .addOnSuccessListener(aVoid -> {
                    sendNotification(ride.driverId, "New Booking Offer", "You have a new offer for your ride to " + ride.destination);
                    sendNotification(userId, "Offer Sent", "Your offer for the ride to " + ride.destination + " has been sent.");
                    dismiss();
                });
    }

    private void sendNotification(String recipientId, String title, String message) {
        String id = UUID.randomUUID().toString();
        Notification notification = new Notification(id, recipientId, title, message, System.currentTimeMillis());
        FirebaseFirestore.getInstance().collection("notifications").document(id).set(notification);
    }

    @Override
    public void onDestroy() { super.onDestroy(); executorService.shutdown(); }

    private static class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {
        private final List<Address> addresses;
        private final OnAddressClickListener listener;

        interface OnAddressClickListener { void onAddressClick(Address address); }

        SuggestionAdapter(List<Address> addresses, OnAddressClickListener listener) {
            this.addresses = addresses;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Address address = addresses.get(position);
            String mainText = address.getFeatureName() != null ? address.getFeatureName() : "";
            String subText = address.getAddressLine(0);
            
            holder.tv1.setText(mainText);
            holder.tv2.setText(subText);
            holder.tv1.setTextColor(0xFFFFFFFF);
            holder.tv2.setTextColor(0xFFAAAAAA);
            
            holder.itemView.setOnClickListener(v -> listener.onAddressClick(address));
        }

        @Override
        public int getItemCount() { return addresses.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tv1, tv2;
            ViewHolder(View v) { 
                super(v);
                tv1 = v.findViewById(android.R.id.text1);
                tv2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}
