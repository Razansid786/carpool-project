package com.example.carpool_project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<Notification> notifications;

    public NotificationAdapter(List<Notification> notifications) {
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.tvTitle.setText(notification.title);
        holder.tvMessage.setText(notification.message);
        
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault());
        String dateStr = sdf.format(new Date(notification.timestamp));
        holder.tvTime.setText(dateStr);

        holder.viewUnreadStrip.setVisibility(notification.read ? View.GONE : View.VISIBLE);

        if (notification.title != null) {
            String title = notification.title.toLowerCase();
            if (title.contains("accepted") || title.contains("confirmed") || title.contains("started")) {
                holder.ivIcon.setImageResource(android.R.drawable.checkbox_on_background);
                holder.ivIcon.setColorFilter(0xFF10B981); 
            } else if (title.contains("rejected") || title.contains("cancelled") || title.contains("deleted")) {
                holder.ivIcon.setImageResource(android.R.drawable.ic_delete);
                holder.ivIcon.setColorFilter(0xFFEF4444); 
            } else if (title.contains("chat") || title.contains("message")) {
                holder.ivIcon.setImageResource(android.R.drawable.stat_notify_chat);
                holder.ivIcon.setColorFilter(0xFF6366F1); 
            } else {
                holder.ivIcon.setImageResource(android.R.drawable.ic_popup_reminder);
                holder.ivIcon.setColorFilter(0xFFF59E0B); 
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (!notification.read) {
                notification.read = true;
                notifyItemChanged(position);
                FirebaseFirestore.getInstance().collection("notifications").document(notification.id)
                        .update("read", true);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications != null ? notifications.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;
        ImageView ivIcon;
        View viewUnreadStrip;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
            viewUnreadStrip = itemView.findViewById(R.id.viewUnreadStrip);
        }
    }
}
