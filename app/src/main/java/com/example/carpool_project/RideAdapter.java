package com.example.carpool_project;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RideAdapter extends RecyclerView.Adapter<RideAdapter.RideViewHolder> {

    private List<Ride> rideList;
    private boolean isMyPosts = false;
    private OnDeleteClickListener deleteClickListener;

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

        if (isMyPosts) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteClickListener != null) {
                    deleteClickListener.onDeleteClick(ride);
                }
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }

        // Entire card click listener
        holder.itemView.setOnClickListener(v -> {
            Log.d("RideAdapter", "Clicked on ride: " + ride.rideId);
            AppCompatActivity activity = getAppCompatActivity(v.getContext());
            if (activity != null) {
                try {
                    RideDetailsBottomSheet bottomSheet = RideDetailsBottomSheet.newInstance(ride);
                    bottomSheet.show(activity.getSupportFragmentManager(), "RideDetails");
                } catch (Exception e) {
                    Log.e("RideAdapter", "Error opening BottomSheet", e);
                    Toast.makeText(v.getContext(), "Could not open details", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(v.getContext(), "Context error: " + v.getContext().getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
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

    @Override
    public int getItemCount() {
        return rideList != null ? rideList.size() : 0;
    }

    static class RideViewHolder extends RecyclerView.ViewHolder {
        TextView tvDriverName, tvRating, tvRoute, tvTimeDays, tvSeatsBadge;
        ImageView ivDriver;
        ImageButton btnDelete;

        public RideViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDriver = itemView.findViewById(R.id.ivDriver);
            tvDriverName = itemView.findViewById(R.id.tvDriverName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvTimeDays = itemView.findViewById(R.id.tvTimeDays);
            tvSeatsBadge = itemView.findViewById(R.id.tvSeatsBadge);
            btnDelete = itemView.findViewById(R.id.btnDeleteRide);
        }
    }
}
