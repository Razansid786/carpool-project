package com.example.carpool_project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RideAdapter extends RecyclerView.Adapter<RideAdapter.RideViewHolder> {

    private List<Ride> rideList;
    private boolean isMyPosts = false;
    private boolean isMyRidesTab = false;
    private OnDeleteClickListener deleteClickListener;
    private int expandedPosition = -1;
    private Map<String, LocationCallback> activeLocationCallbacks = new HashMap<>();
    private Map<Integer, ListenerRegistration> activeListeners = new HashMap<>();

    public interface OnDeleteClickListener {
        void onDeleteClick(Ride ride);
    }

    public RideAdapter(List<Ride> rideList) {
        this.rideList = rideList;
    }

    public void setMyPosts(boolean myPosts, OnDeleteClickListener listener) {
        this.isMyPosts = myPosts;
        this.deleteClickListener = listener;
    }

    public void setMyRidesTab(boolean myRidesTab) {
        this.isMyRidesTab = myRidesTab;
    }

    @NonNull
    @Override
    public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MapsInitializer.initialize(parent.getContext().getApplicationContext());
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ride_card, parent, false);
        return new RideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
        final Ride ride = rideList.get(position);
        if (ride == null) return;
        
        holder.tvDriverName.setText(ride.driverName != null ? ride.driverName : "Unknown Driver");
        holder.tvDriverEmail.setText(ride.driverEmail != null ? ride.driverEmail : "No email available");
        holder.tvRating.setText(ride.driverRating + " ★");
        
        if (holder.tvOrigin != null) holder.tvOrigin.setText(ride.origin);
        if (holder.tvDestination != null) holder.tvDestination.setText(ride.destination);
        holder.tvRoute.setText(ride.origin + " ➔ " + ride.destination);
        
        holder.tvTimeDays.setText(ride.time + ", " + ride.recurringDays);
        holder.tvSeatsBadge.setText(ride.seatsAvailable + " Seats Available");

        if (activeListeners.containsKey(holder.hashCode())) {
            activeListeners.get(holder.hashCode()).remove();
            activeListeners.remove(holder.hashCode());
        }

        holder.btnOpenInMaps.setOnClickListener(v -> {
            if (isMyPosts && expandedPosition == holder.getBindingAdapterPosition()) {
                loadRideOffersForMaps(ride, v.getContext());
            } else {
                openInGoogleMaps(v.getContext(), ride, new ArrayList<>());
            }
        });

        if (isMyRidesTab) {
            updateMyRideStatus(ride, holder);
        } else if (isMyPosts) {
            holder.layoutPendingStatus.setVisibility(View.GONE);
            holder.layoutConfirmedStatus.setVisibility(View.GONE);
            holder.layoutSafetyFeatures.setVisibility(View.GONE);
            holder.btnRideAction.setVisibility(View.GONE);
            
            boolean isExpanded = position == expandedPosition;
            holder.layoutUsers.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            
            if (ride.startLat != 0) {
                holder.cardRideMap.setVisibility(View.VISIBLE);
                holder.rideMapView.getMapAsync(googleMap -> {
                    if (googleMap != null) {
                        setupMapControls(googleMap, holder);
                        loadRideOffers(ride, holder, googleMap);
                    }
                });
            } else {
                holder.cardRideMap.setVisibility(View.GONE);
            }

            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteClickListener != null) {
                    deleteClickListener.onDeleteClick(ride);
                }
            });

            holder.itemView.setOnClickListener(v -> {
                int previousExpandedPosition = expandedPosition;
                expandedPosition = isExpanded ? -1 : holder.getBindingAdapterPosition();
                notifyItemChanged(previousExpandedPosition);
                notifyItemChanged(expandedPosition);
            });

        } else {
            if (ride.startLat != 0 && ride.endLat != 0) {
                holder.cardRideMap.setVisibility(View.VISIBLE);
                holder.rideMapView.getMapAsync(googleMap -> {
                    if (googleMap != null) {
                        setupRideMap(googleMap, ride, new ArrayList<>(), holder.itemView.getContext(), false);
                        setupMapControls(googleMap, holder);
                    }
                });
            } else {
                holder.cardRideMap.setVisibility(View.GONE);
            }

            holder.btnDelete.setVisibility(View.GONE);
            holder.layoutUsers.setVisibility(View.GONE);
            
            holder.itemView.setOnClickListener(v -> {
                AppCompatActivity activity = getAppCompatActivity(v.getContext());
                if (activity != null) {
                    RideDetailsBottomSheet.newInstance(ride, isMyRidesTab).show(activity.getSupportFragmentManager(), "RideDetails");
                }
            });
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupMapControls(GoogleMap googleMap, RideViewHolder holder) {
        if (holder.btnZoomIn != null) {
            holder.btnZoomIn.setOnClickListener(v -> googleMap.animateCamera(CameraUpdateFactory.zoomIn()));
        }
        if (holder.btnZoomOut != null) {
            holder.btnZoomOut.setOnClickListener(v -> googleMap.animateCamera(CameraUpdateFactory.zoomOut()));
        }
        
        holder.rideMapView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            v.onTouchEvent(event);
            return true;
        });
    }

    private void openInGoogleMaps(Context context, Ride ride, List<BookingOffer> acceptedOffers) {
        if (ride.startLat != 0 && ride.endLat != 0) {
            StringBuilder uriBuilder = new StringBuilder("https://www.google.com/maps/dir/?api=1");
            uriBuilder.append("&origin=").append(ride.startLat).append(",").append(ride.startLng);
            uriBuilder.append("&destination=").append(ride.endLat).append(",").append(ride.endLng);
            uriBuilder.append("&travelmode=driving");

            if (!acceptedOffers.isEmpty()) {
                uriBuilder.append("&waypoints=");
                for (int i = 0; i < acceptedOffers.size(); i++) {
                    BookingOffer o = acceptedOffers.get(i);
                    uriBuilder.append(o.pickupLat).append(",").append(o.pickupLng);
                    if (i < acceptedOffers.size() - 1) uriBuilder.append("|");
                }
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriBuilder.toString()));
            intent.setPackage("com.google.android.apps.maps");
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "Location coordinates not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadRideOffersForMaps(Ride ride, Context context) {
        FirebaseFirestore.getInstance().collection("offers")
                .whereEqualTo("rideId", ride.rideId)
                .whereEqualTo("status", "accepted")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<BookingOffer> acceptedOffers = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        acceptedOffers.add(doc.toObject(BookingOffer.class));
                    }
                    openInGoogleMaps(context, ride, acceptedOffers);
                });
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RideViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        try { holder.rideMapView.onResume(); } catch (Exception ignored) {}
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RideViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        try { holder.rideMapView.onPause(); } catch (Exception ignored) {}
        if (activeListeners.containsKey(holder.hashCode())) {
            activeListeners.get(holder.hashCode()).remove();
            activeListeners.remove(holder.hashCode());
        }
    }

    private void updateMyRideStatus(Ride ride, RideViewHolder holder) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        ListenerRegistration lr = FirebaseFirestore.getInstance().collection("offers")
                .whereEqualTo("rideId", ride.rideId)
                .whereEqualTo("passengerId", uid)
                .addSnapshotListener((value, error) -> {
                    if (value != null && !value.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) value.getDocuments().get(0);
                        String status = doc.getString("status");
                        String rideFlowStatus = doc.getString("rideFlowStatus");
                        BookingOffer myOffer = doc.toObject(BookingOffer.class);

                        if (ride.startLat != 0) {
                            holder.cardRideMap.setVisibility(View.VISIBLE);
                            holder.rideMapView.getMapAsync(googleMap -> {
                                if (googleMap != null) {
                                    List<BookingOffer> myAcceptedOffer = new ArrayList<>();
                                    if ("accepted".equals(status)) {
                                        myAcceptedOffer.add(myOffer);
                                    }
                                    setupRideMap(googleMap, ride, myAcceptedOffer, holder.itemView.getContext(), true);
                                    setupMapControls(googleMap, holder);
                                }
                            });
                            
                            holder.btnOpenInMaps.setOnClickListener(v -> {
                                List<BookingOffer> waypoints = new ArrayList<>();
                                if ("accepted".equals(status)) waypoints.add(myOffer);
                                openInGoogleMaps(v.getContext(), ride, waypoints);
                            });
                        } else {
                            holder.cardRideMap.setVisibility(View.GONE);
                        }

                        if ("pending".equals(status)) {
                            holder.layoutPendingStatus.setVisibility(View.VISIBLE);
                            holder.layoutConfirmedStatus.setVisibility(View.GONE);
                            holder.btnRideAction.setVisibility(View.GONE);
                            holder.layoutSafetyFeatures.setVisibility(View.GONE);
                        } else if ("accepted".equals(status)) {
                            holder.layoutPendingStatus.setVisibility(View.GONE);
                            holder.layoutConfirmedStatus.setVisibility(View.VISIBLE);
                            holder.layoutSafetyFeatures.setVisibility(View.VISIBLE);

                            if (holder.btnChatDriver != null) {
                                holder.btnChatDriver.setOnClickListener(v -> {
                                    Intent intent = new Intent(v.getContext(), ChatActivity.class);
                                    intent.putExtra("rideId", ride.rideId);
                                    intent.putExtra("otherUserId", ride.driverId);
                                    v.getContext().startActivity(intent);
                                });
                            }

                            if (holder.btnComplaint != null) {
                                holder.btnComplaint.setOnClickListener(v -> openComplaintChat(v.getContext(), ride));
                            }

                            if ("ongoing".equals(rideFlowStatus)) {
                                holder.btnCallPolice.setVisibility(View.VISIBLE);
                                holder.btnShareRide.setVisibility(View.VISIBLE);
                                holder.btnComplaint.setVisibility(View.VISIBLE);
                                holder.btnRideAction.setVisibility(View.VISIBLE);
                                holder.btnRideAction.setText("End Ride");
                                holder.btnRideAction.setEnabled(true);
                                holder.btnRideAction.setOnClickListener(v -> {
                                    doc.getReference().update("rideFlowStatus", "completed");
                                    sendNotification(ride.driverId, "Ride Completed", "Passenger has ended the ride.");
                                });

                                holder.btnCallPolice.setOnClickListener(v -> {
                                    Intent intent = new Intent(Intent.ACTION_DIAL);
                                    intent.setData(Uri.parse("tel:15"));
                                    v.getContext().startActivity(intent);
                                });
                                holder.btnShareRide.setOnClickListener(v -> shareLiveLocation(v.getContext(), ride));
                            } else if ("completed".equals(rideFlowStatus)) {
                                holder.layoutSafetyFeatures.setVisibility(View.GONE);
                                holder.btnRideAction.setVisibility(View.VISIBLE);
                                holder.btnRideAction.setText("Ride Finished");
                                holder.btnRideAction.setEnabled(false);
                            } else {
                                holder.btnCallPolice.setVisibility(View.GONE);
                                holder.btnShareRide.setVisibility(View.GONE);
                                holder.btnComplaint.setVisibility(View.GONE);
                                holder.btnRideAction.setVisibility(View.VISIBLE);
                                holder.btnRideAction.setText("Confirm Start");
                                holder.btnRideAction.setEnabled(true);
                                holder.btnRideAction.setOnClickListener(v -> {
                                    doc.getReference().update("rideFlowStatus", "ongoing");
                                    sendNotification(ride.driverId, "Ride Started", "Passenger has confirmed the ride start.");
                                });
                            }
                        }
                    } else {
                        holder.layoutPendingStatus.setVisibility(View.GONE);
                        holder.layoutConfirmedStatus.setVisibility(View.GONE);
                        holder.cardRideMap.setVisibility(View.GONE);
                        holder.layoutSafetyFeatures.setVisibility(View.GONE);
                    }
                });
        activeListeners.put(holder.hashCode(), lr);
    }

    private void openComplaintChat(Context context, Ride ride) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String complaintMsg = "Automated Complaint Report:\n" +
                "Ride ID: " + ride.rideId + "\n" +
                "Route: " + ride.origin + " to " + ride.destination + "\n" +
                "Driver: " + ride.driverName + "\n" +
                "Driver Email: " + ride.driverEmail + "\n" +
                "Driver Phone: " + ride.driverPhone;

        ChatMessage msg = new ChatMessage(uid, complaintMsg, System.currentTimeMillis());
        FirebaseDatabase.getInstance().getReference("support_chats").child(uid).push().setValue(msg);

        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("rideId", "support");
        intent.putExtra("otherUserId", "admin");
        context.startActivity(intent);
    }

    private void shareLiveLocation(Context context, Ride ride) {
        String locationText = "I'm on my way using Carpool! Follow my live trip: \n" +
                "Route: " + ride.origin + " to " + ride.destination + "\n" +
                "Live tracking: https://www.google.com/maps/search/?api=1&query=" + ride.currentLat + "," + ride.currentLng;
        
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, locationText);
        sendIntent.setType("text/plain");
        context.startActivity(Intent.createChooser(sendIntent, "Share Live Ride Info"));
    }

    private void loadRideOffers(Ride ride, RideViewHolder holder, GoogleMap googleMap) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        ListenerRegistration lr = db.collection("offers")
                .whereEqualTo("rideId", ride.rideId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    
                    List<BookingOffer> offers = new ArrayList<>();
                    List<BookingOffer> acceptedOffers = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            BookingOffer offer = doc.toObject(BookingOffer.class);
                            offers.add(offer);
                            if ("accepted".equals(offer.status)) {
                                acceptedOffers.add(offer);
                            }
                        }
                    }

                    // Sort offers: completed at the bottom
                    Collections.sort(offers, (o1, o2) -> {
                        int s1 = "completed".equals(o1.rideFlowStatus) ? 1 : 0;
                        int s2 = "completed".equals(o2.rideFlowStatus) ? 1 : 0;
                        return Integer.compare(s1, s2);
                    });
                    
                    OfferAdapter adapter = new OfferAdapter(offers, ride);
                    holder.rvUsers.setLayoutManager(new LinearLayoutManager(holder.rvUsers.getContext()));
                    holder.rvUsers.setAdapter(adapter);

                    if (googleMap != null) {
                        setupRideMap(googleMap, ride, acceptedOffers, holder.itemView.getContext(), false);
                    }
                });
        activeListeners.put(holder.hashCode(), lr);
    }

    private void setupRideMap(GoogleMap googleMap, Ride ride, List<BookingOffer> acceptedOffers, Context context, boolean isPassengerView) {
        if (googleMap == null) return;
        try {
            googleMap.clear();
            googleMap.getUiSettings().setZoomControlsEnabled(false); 
            googleMap.getUiSettings().setAllGesturesEnabled(true);
            googleMap.getUiSettings().setMapToolbarEnabled(true);
            googleMap.getUiSettings().setCompassEnabled(true);
            
            LatLng start = new LatLng(ride.startLat, ride.startLng);
            LatLng end = new LatLng(ride.endLat, ride.endLng);
            
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            int pointsCount = 0;

            if (ride.startLat != 0 && ride.startLng != 0) {
                googleMap.addMarker(new MarkerOptions().position(start).title("Start: " + ride.origin).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                builder.include(start);
                pointsCount++;
            }
            if (ride.endLat != 0 && ride.endLng != 0) {
                googleMap.addMarker(new MarkerOptions().position(end).title("End: " + ride.destination).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                builder.include(end);
                pointsCount++;
            }
            
            if (ride.currentLat != 0 && ride.currentLng != 0) {
                LatLng current = new LatLng(ride.currentLat, ride.currentLng);
                googleMap.addMarker(new MarkerOptions()
                    .position(current)
                    .title(isPassengerView ? "Driver Live" : "My Location")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car))); 
                builder.include(current);
                pointsCount++;
            }

            PolylineOptions routeLines = new PolylineOptions().color(Color.BLUE).width(8).geodesic(true);
            if (ride.startLat != 0) routeLines.add(start);

            for (BookingOffer offer : acceptedOffers) {
                if (offer.pickupLat != 0) {
                    LatLng p = new LatLng(offer.pickupLat, offer.pickupLng);
                    googleMap.addMarker(new MarkerOptions().position(p).title(offer.passengerName + " (Pickup)")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    builder.include(p);
                    routeLines.add(p);
                    pointsCount++;
                }
            }
            
            if (ride.endLat != 0) routeLines.add(end);
            if (pointsCount >= 2) {
                googleMap.addPolyline(routeLines);
            } else if (ride.startLat != 0 && ride.endLat != 0) {
                googleMap.addPolyline(new PolylineOptions().add(start, end).color(0x7FFF0000).width(5));
            }

            if (pointsCount > 0) {
                if (pointsCount == 1) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(builder.build().getCenter(), 14));
                } else {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AppCompatActivity getAppCompatActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof AppCompatActivity) return (AppCompatActivity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    private void sendNotification(String recipientId, String title, String message) {
        String id = UUID.randomUUID().toString();
        Notification notification = new Notification(id, recipientId, title, message, System.currentTimeMillis());
        FirebaseFirestore.getInstance().collection("notifications").document(id).set(notification);
    }

    @Override
    public int getItemCount() { return rideList != null ? rideList.size() : 0; }

    static class RideViewHolder extends RecyclerView.ViewHolder {
        TextView tvDriverName, tvDriverEmail, tvRating, tvRoute, tvOrigin, tvDestination, tvTimeDays, tvSeatsBadge;
        ImageView ivDriver, ivBottomCar;
        ImageButton btnDelete, btnOpenInMaps, btnZoomIn, btnZoomOut;
        LinearLayout layoutUsers, layoutPendingStatus, layoutConfirmedStatus, layoutSafetyFeatures;
        Button btnRideAction, btnCallPolice, btnShareRide, btnChatDriver, btnComplaint;
        RecyclerView rvUsers;
        MapView rideMapView;
        View cardRideMap;

        public RideViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDriver = itemView.findViewById(R.id.ivDriver);
            ivBottomCar = itemView.findViewById(R.id.ivBottomCar);
            tvDriverName = itemView.findViewById(R.id.tvDriverName);
            tvDriverEmail = itemView.findViewById(R.id.tvDriverEmail);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvOrigin = itemView.findViewById(R.id.tvOrigin);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvTimeDays = itemView.findViewById(R.id.tvTimeDays);
            tvSeatsBadge = itemView.findViewById(R.id.tvSeatsBadge);
            btnDelete = itemView.findViewById(R.id.btnDeleteRide);
            btnOpenInMaps = itemView.findViewById(R.id.btnOpenInMaps);
            btnZoomIn = itemView.findViewById(R.id.btnZoomIn);
            btnZoomOut = itemView.findViewById(R.id.btnZoomOut);
            layoutUsers = itemView.findViewById(R.id.layoutUsers);
            layoutPendingStatus = itemView.findViewById(R.id.layoutPendingStatus);
            layoutConfirmedStatus = itemView.findViewById(R.id.layoutConfirmedStatus);
            layoutSafetyFeatures = itemView.findViewById(R.id.layoutSafetyFeatures);
            btnRideAction = itemView.findViewById(R.id.btnRideAction);
            btnCallPolice = itemView.findViewById(R.id.btnCallPolice);
            btnShareRide = itemView.findViewById(R.id.btnShareRide);
            btnChatDriver = itemView.findViewById(R.id.btnChatDriver);
            btnComplaint = itemView.findViewById(R.id.btnComplaint);
            rvUsers = itemView.findViewById(R.id.rvUsers);
            rideMapView = itemView.findViewById(R.id.rideMapView);
            cardRideMap = itemView.findViewById(R.id.cardRideMap);
            
            if (rideMapView != null) {
                rideMapView.onCreate(null);
            }
        }
    }

    class OfferAdapter extends RecyclerView.Adapter<OfferAdapter.ViewHolder> {
        private List<BookingOffer> offers;
        private Ride ride;

        public OfferAdapter(List<BookingOffer> offers, Ride ride) {
            this.offers = offers;
            this.ride = ride;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_offer, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BookingOffer offer = offers.get(position);
            holder.tvName.setText(offer.passengerName);
            holder.tvEmail.setText(offer.passengerEmail != null ? offer.passengerEmail : "No email available");
            holder.tvDetails.setText("Pickup: " + offer.pickupAddress + "\nPrice: Rs. " + (int)offer.offeredPrice + " | Seats: " + offer.seatsRequested);
            holder.tvStatus.setText(offer.status.toUpperCase());
            
            holder.tvEmail.setVisibility(View.VISIBLE);
            holder.tvName.setVisibility(View.VISIBLE);

            holder.btnChat.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ChatActivity.class);
                intent.putExtra("rideId", ride.rideId);
                intent.putExtra("otherUserId", offer.passengerId);
                v.getContext().startActivity(intent);
            });

            holder.mapView.setVisibility(View.GONE);
            View cardMap = holder.itemView.findViewById(R.id.cardMap);
            if (cardMap != null) cardMap.setVisibility(View.GONE);

            if (offer.status.equals("pending")) {
                holder.layoutActions.setVisibility(View.VISIBLE);
                holder.layoutAccepted.setVisibility(View.GONE);
                holder.btnRideAction.setVisibility(View.GONE);
                holder.btnAccept.setOnClickListener(v -> handleOffer(offer, "accepted"));
                holder.btnReject.setOnClickListener(v -> handleOffer(offer, "rejected"));
            } else if (offer.status.equals("accepted")) {
                holder.layoutActions.setVisibility(View.GONE);
                holder.layoutAccepted.setVisibility(View.VISIBLE);
                holder.btnRideAction.setVisibility(View.VISIBLE);

                holder.ivAnimatedCar.setVisibility(View.INVISIBLE); 
                
                FirebaseFirestore.getInstance().collection("offers").document(offer.offerId)
                    .addSnapshotListener((snapshot, e) -> {
                        if (snapshot != null && snapshot.exists()) {
                            String flowStatus = snapshot.getString("rideFlowStatus");
                            if ("ongoing".equals(flowStatus)) {
                                holder.btnRideAction.setText("End Ride");
                                holder.btnRideAction.setEnabled(true);
                            } else if ("starting".equals(flowStatus)) {
                                holder.btnRideAction.setText("Waiting...");
                                holder.btnRideAction.setEnabled(false);
                            } else if ("completed".equals(flowStatus)) {
                                holder.btnRideAction.setText("Ride Finished");
                                holder.btnRideAction.setEnabled(false);
                            } else {
                                holder.btnRideAction.setText("Start Ride");
                                holder.btnRideAction.setEnabled(true);
                            }
                        }
                    });
                
                holder.btnRideAction.setOnClickListener(v -> {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    String currentText = holder.btnRideAction.getText().toString();
                    if ("Start Ride".equals(currentText)) {
                        db.collection("offers").document(offer.offerId).update("rideFlowStatus", "starting");
                        sendNotification(offer.passengerId, "Driver waiting", "Driver has arrived at your location.");
                        startLocationUpdates(v.getContext(), ride.rideId);
                    } else if ("End Ride".equals(currentText)) {
                        db.collection("offers").document(offer.offerId).update("rideFlowStatus", "completed");
                        sendNotification(offer.passengerId, "Ride Completed", "Hope you had a great trip!");
                        stopLocationUpdates(ride.rideId);
                    }
                });
            } else {
                holder.layoutActions.setVisibility(View.GONE);
                holder.layoutAccepted.setVisibility(View.GONE);
                holder.btnRideAction.setVisibility(View.GONE);
            }
        }

        private void startLocationUpdates(Context context, String rideId) {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setMinUpdateDistanceMeters(10)
                    .build();

            LocationCallback callback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    for (Location location : locationResult.getLocations()) {
                        FirebaseFirestore.getInstance().collection("rides").document(rideId)
                                .update("currentLat", location.getLatitude(), "currentLng", location.getLongitude());
                    }
                }
            };

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper());
                activeLocationCallbacks.put(rideId, callback);
            }
        }

        private void stopLocationUpdates(String rideId) {
            LocationCallback callback = activeLocationCallbacks.get(rideId);
            if (callback != null) {
                activeLocationCallbacks.remove(rideId);
            }
        }

        private void handleOffer(BookingOffer offer, String status) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("offers").document(offer.offerId).update("status", status)
                    .addOnSuccessListener(aVoid -> {
                        if (status.equals("accepted")) {
                            db.collection("rides").document(offer.rideId)
                                    .update("seatsAvailable", FieldValue.increment(-offer.seatsRequested));
                            sendNotification(offer.passengerId, "Offer Accepted!", "Your offer has been accepted.");
                        } else {
                            sendNotification(offer.passengerId, "Offer Rejected", "Your offer was not accepted.");
                        }
                    });
        }

        @Override
        public int getItemCount() { return offers.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvEmail, tvDetails, tvStatus;
            LinearLayout layoutActions;
            View layoutAccepted;
            Button btnAccept, btnReject, btnChat, btnRideAction;
            MapView mapView;
            ImageView ivAnimatedCar;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvOfferPassengerName);
                tvEmail = itemView.findViewById(R.id.tvPassengerEmail);
                tvDetails = itemView.findViewById(R.id.tvOfferDetails);
                tvStatus = itemView.findViewById(R.id.tvOfferStatus);
                layoutActions = itemView.findViewById(R.id.layoutOfferActions);
                layoutAccepted = itemView.findViewById(R.id.layoutAccepted);
                btnAccept = itemView.findViewById(R.id.btnAcceptOffer);
                btnReject = itemView.findViewById(R.id.btnRejectOffer);
                btnChat = itemView.findViewById(R.id.btnChatPassenger);
                btnRideAction = itemView.findViewById(R.id.btnOfferRideAction);
                mapView = itemView.findViewById(R.id.offerMapView);
                ivAnimatedCar = itemView.findViewById(R.id.ivAnimatedCar);
            }
        }
    }
}
