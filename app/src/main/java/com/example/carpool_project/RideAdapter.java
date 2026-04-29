package com.example.carpool_project;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
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
import com.google.android.gms.location.LocationServices;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RideAdapter extends RecyclerView.Adapter<RideAdapter.RideViewHolder> {

    private List<Ride> rideList;
    private boolean isMyPosts = false;
    private boolean isMyRidesTab = false;
    private OnDeleteClickListener deleteClickListener;
    private int expandedPosition = -1;

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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ride_card, parent, false);
        return new RideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
        final Ride ride = rideList.get(position);
        if (ride == null) return;
        
        holder.tvDriverName.setText(ride.driverName != null ? ride.driverName : "Unknown Driver");
        holder.tvRating.setText(ride.driverRating + " ★");
        holder.tvRoute.setText(ride.origin + " ➔ " + ride.destination);
        holder.tvTimeDays.setText(ride.time + ", " + ride.recurringDays);
        holder.tvSeatsBadge.setText(ride.seatsAvailable + " Seats Available");

        if (isMyRidesTab) {
            updateMyRideStatus(ride, holder);
        } else {
            holder.layoutPendingStatus.setVisibility(View.GONE);
            holder.layoutConfirmedStatus.setVisibility(View.GONE);
            holder.layoutSafetyFeatures.setVisibility(View.GONE);
            holder.btnRideAction.setVisibility(View.GONE);
        }

        if (isMyPosts) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteClickListener != null) {
                    deleteClickListener.onDeleteClick(ride);
                }
            });
            
            boolean isExpanded = position == expandedPosition;
            holder.layoutUsers.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            
            if (isExpanded) {
                loadRideOffers(ride, holder.rvUsers);
            }

            holder.itemView.setOnClickListener(v -> {
                int previousExpandedPosition = expandedPosition;
                expandedPosition = isExpanded ? -1 : holder.getAdapterPosition();
                notifyItemChanged(previousExpandedPosition);
                notifyItemChanged(expandedPosition);
            });

        } else {
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

    private void updateMyRideStatus(Ride ride, RideViewHolder holder) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("offers")
                .whereEqualTo("rideId", ride.rideId)
                .whereEqualTo("passengerId", uid)
                .addSnapshotListener((value, error) -> {
                    if (value != null && !value.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) value.getDocuments().get(0);
                        String status = doc.getString("status");
                        String rideFlowStatus = doc.getString("rideFlowStatus"); // ongoing, completed

                        if ("pending".equals(status)) {
                            holder.layoutPendingStatus.setVisibility(View.VISIBLE);
                            holder.layoutConfirmedStatus.setVisibility(View.GONE);
                            holder.btnRideAction.setVisibility(View.GONE);
                        } else if ("accepted".equals(status)) {
                            holder.layoutPendingStatus.setVisibility(View.GONE);
                            holder.layoutConfirmedStatus.setVisibility(View.VISIBLE);
                            
                            if ("ongoing".equals(rideFlowStatus)) {
                                holder.layoutSafetyFeatures.setVisibility(View.VISIBLE);
                                holder.btnRideAction.setVisibility(View.GONE);
                                holder.btnCallPolice.setOnClickListener(v -> {
                                    Intent intent = new Intent(Intent.ACTION_DIAL);
                                    intent.setData(Uri.parse("tel:15"));
                                    v.getContext().startActivity(intent);
                                });
                                holder.btnShareRide.setOnClickListener(v -> {
                                    shareLiveLocation(v.getContext(), ride);
                                });
                            } else if ("completed".equals(rideFlowStatus)) {
                                holder.layoutSafetyFeatures.setVisibility(View.GONE);
                                holder.btnRideAction.setVisibility(View.VISIBLE);
                                holder.btnRideAction.setText("Ride Completed - Please pay fare");
                                holder.btnRideAction.setEnabled(false);
                            } else {
                                holder.btnRideAction.setVisibility(View.VISIBLE);
                                holder.btnRideAction.setText("Confirm Start Ride");
                                holder.btnRideAction.setOnClickListener(v -> {
                                    doc.getReference().update("rideFlowStatus", "ongoing");
                                    sendNotification(ride.driverId, "Ride Started", "Passenger has confirmed the ride start.");
                                });
                            }
                        }
                    } else {
                        holder.layoutPendingStatus.setVisibility(View.GONE);
                        holder.layoutConfirmedStatus.setVisibility(View.GONE);
                    }
                });
    }

    private void shareLiveLocation(Context context, Ride ride) {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Location permission required to share live location", Toast.LENGTH_SHORT).show();
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            String locationText = "I'm on my way using Carpool! Route: " + ride.origin + " to " + ride.destination;
            if (location != null) {
                locationText += "\nMy live location: https://www.google.com/maps/search/?api=1&query=" + location.getLatitude() + "," + location.getLongitude();
            }
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, locationText);
            sendIntent.setType("text/plain");
            context.startActivity(Intent.createChooser(sendIntent, "Share Ride Info"));
        });
    }

    private void loadRideOffers(Ride ride, RecyclerView rvUsers) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("offers")
                .whereEqualTo("rideId", ride.rideId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        List<BookingOffer> offers = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            offers.add(doc.toObject(BookingOffer.class));
                        }
                        OfferAdapter adapter = new OfferAdapter(offers, ride);
                        rvUsers.setLayoutManager(new LinearLayoutManager(rvUsers.getContext()));
                        rvUsers.setAdapter(adapter);
                    }
                });
    }

    private AppCompatActivity getAppCompatActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof AppCompatActivity) {
                return (AppCompatActivity) context;
            }
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
    public int getItemCount() {
        return rideList != null ? rideList.size() : 0;
    }

    static class RideViewHolder extends RecyclerView.ViewHolder {
        TextView tvDriverName, tvRating, tvRoute, tvTimeDays, tvSeatsBadge;
        ImageView ivDriver, ivBottomCar;
        ImageButton btnDelete;
        LinearLayout layoutUsers, layoutPendingStatus, layoutConfirmedStatus, layoutSafetyFeatures;
        Button btnRideAction, btnCallPolice, btnShareRide;
        RecyclerView rvUsers;

        public RideViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDriver = itemView.findViewById(R.id.ivDriver);
            ivBottomCar = itemView.findViewById(R.id.ivBottomCar);
            tvDriverName = itemView.findViewById(R.id.tvDriverName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvTimeDays = itemView.findViewById(R.id.tvTimeDays);
            tvSeatsBadge = itemView.findViewById(R.id.tvSeatsBadge);
            btnDelete = itemView.findViewById(R.id.btnDeleteRide);
            layoutUsers = itemView.findViewById(R.id.layoutUsers);
            layoutPendingStatus = itemView.findViewById(R.id.layoutPendingStatus);
            layoutConfirmedStatus = itemView.findViewById(R.id.layoutConfirmedStatus);
            layoutSafetyFeatures = itemView.findViewById(R.id.layoutSafetyFeatures);
            btnRideAction = itemView.findViewById(R.id.btnRideAction);
            btnCallPolice = itemView.findViewById(R.id.btnCallPolice);
            btnShareRide = itemView.findViewById(R.id.btnShareRide);
            rvUsers = itemView.findViewById(R.id.rvUsers);
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

            holder.btnChat.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ChatActivity.class);
                intent.putExtra("rideId", ride.rideId);
                intent.putExtra("otherUserId", offer.passengerId);
                v.getContext().startActivity(intent);
            });

            holder.mapView.onCreate(null);
            holder.mapView.getMapAsync(googleMap -> setupMap(googleMap, offer, holder.itemView.getContext()));
            holder.itemView.findViewById(R.id.cardMap).setOnClickListener(v -> openFullscreenMap(v.getContext(), offer));

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
                
                // Fetch rideFlowStatus from offer status or separate field
                // For simplicity, we use rideFlowStatus logic from Firebase
                FirebaseFirestore.getInstance().collection("offers").document(offer.offerId)
                    .addSnapshotListener((snapshot, e) -> {
                        if (snapshot != null && snapshot.exists()) {
                            String flowStatus = snapshot.getString("rideFlowStatus");
                            if ("ongoing".equals(flowStatus)) {
                                holder.btnRideAction.setText("End Ride");
                            } else {
                                holder.btnRideAction.setText("Start Ride");
                            }
                        }
                    });
                
                holder.btnRideAction.setOnClickListener(v -> {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    String currentText = holder.btnRideAction.getText().toString();
                    if ("Start Ride".equals(currentText)) {
                        db.collection("offers").document(offer.offerId).update("rideFlowStatus", "starting");
                        sendNotification(offer.passengerId, "Driver waiting", "Driver has pressed Start Ride. Please confirm.");
                    } else {
                        db.collection("offers").document(offer.offerId).update("rideFlowStatus", "completed");
                        sendNotification(offer.passengerId, "Ride Completed", "Driver has ended the ride. Hope you had a great trip!");
                    }
                });

                holder.ivAnimatedCar.clearAnimation();
                TranslateAnimation animation = new TranslateAnimation(
                        Animation.RELATIVE_TO_PARENT, -0.2f,
                        Animation.RELATIVE_TO_PARENT, 1.2f,
                        Animation.RELATIVE_TO_PARENT, 0,
                        Animation.RELATIVE_TO_PARENT, 0);
                animation.setDuration(4000);
                animation.setRepeatCount(Animation.INFINITE);
                holder.ivAnimatedCar.startAnimation(animation);
            } else {
                holder.layoutActions.setVisibility(View.GONE);
                holder.layoutAccepted.setVisibility(View.GONE);
                holder.btnRideAction.setVisibility(View.GONE);
            }
        }

        private void setupMap(GoogleMap googleMap, BookingOffer offer, Context context) {
            googleMap.clear();
            MapsInitializer.initialize(context);
            LatLng pickup = new LatLng(offer.pickupLat, offer.pickupLng);
            LatLng dropoff = new LatLng(offer.dropoffLat, offer.dropoffLng);
            LatLng start = new LatLng(ride.startLat, ride.startLng);
            LatLng end = new LatLng(ride.endLat, ride.endLng);

            googleMap.addMarker(new MarkerOptions().position(start).title("Ride Start").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            googleMap.addMarker(new MarkerOptions().position(end).title("Ride End").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            googleMap.addPolyline(new PolylineOptions().add(start, end).color(0x7FFF0000).width(5));

            googleMap.addMarker(new MarkerOptions().position(pickup).title("Passenger Pickup").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            googleMap.addPolyline(new PolylineOptions().add(pickup, dropoff).color(0x7F00FF00).width(8));

            LatLngBounds bounds = new LatLngBounds.Builder()
                .include(start).include(end).include(pickup).include(dropoff).build();
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
        }

        private void openFullscreenMap(Context context, BookingOffer offer) {
            Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setContentView(R.layout.layout_fullscreen_map);
            MapView fsMapView = dialog.findViewById(R.id.fullscreenMapView);
            ImageButton btnMinimize = dialog.findViewById(R.id.btnMinimizeMap);
            fsMapView.onCreate(null);
            fsMapView.getMapAsync(googleMap -> setupMap(googleMap, offer, context));
            fsMapView.onResume();
            btnMinimize.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
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
            TextView tvName, tvEmail, tvDetails, tvStatus, tvResultText;
            LinearLayout layoutActions;
            View layoutAccepted;
            ImageView ivAnimatedCar;
            Button btnAccept, btnReject, btnChat, btnRideAction;
            MapView mapView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvOfferPassengerName);
                tvEmail = itemView.findViewById(R.id.tvPassengerEmail);
                tvDetails = itemView.findViewById(R.id.tvOfferDetails);
                tvStatus = itemView.findViewById(R.id.tvOfferStatus);
                layoutActions = itemView.findViewById(R.id.layoutOfferActions);
                layoutAccepted = itemView.findViewById(R.id.layoutAccepted);
                ivAnimatedCar = itemView.findViewById(R.id.ivAnimatedCar);
                btnAccept = itemView.findViewById(R.id.btnAcceptOffer);
                btnReject = itemView.findViewById(R.id.btnRejectOffer);
                btnChat = itemView.findViewById(R.id.btnChatPassenger);
                btnRideAction = itemView.findViewById(R.id.btnOfferRideAction);
                mapView = itemView.findViewById(R.id.offerMapView);
            }
        }
    }
}

interface OfferActionAdapter {}
