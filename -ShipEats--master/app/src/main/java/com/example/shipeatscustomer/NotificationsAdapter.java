package com.example.shipeatscustomer;

import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

    private List<AppNotification> notificationList;
    private boolean isAdmin;

    public NotificationsAdapter(List<AppNotification> notificationList) {
        this.notificationList = notificationList;
        this.isAdmin = false;
    }

    public void setAdminMode(boolean admin) {
        this.isAdmin = admin;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isAdmin ? R.layout.item_admin_notification : R.layout.item_notification;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppNotification notif = notificationList.get(position);
        
        holder.tvTitle.setText(notif.getTitle());
        holder.tvMessage.setText(notif.getMessage());
        
        // Show/Hide unread indicator for BOTH admin and customer
        if (holder.unreadIndicator != null) {
            holder.unreadIndicator.setVisibility(notif.isRead() ? View.GONE : View.VISIBLE);
        }

        if (isAdmin) {
            // Use relative time for admin view (e.g. "2 minutes ago")
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    notif.getTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);
            holder.tvTime.setText(timeAgo);
        } else {
            holder.tvTime.setText(notif.getFormattedTime());
        }

        holder.itemView.setOnClickListener(v -> {
            if (!notif.isRead()) {
                notif.setRead(true);
                // Update local UI immediately
                if (holder.unreadIndicator != null) {
                    holder.unreadIndicator.setVisibility(View.GONE);
                }
                
                // Update Firebase
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                if (notif.getId() != null) {
                    FirebaseDatabase.getInstance().getReference("notifications")
                            .child(userId).child(notif.getId()).child("read").setValue(true);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;
        View unreadIndicator;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }
    }
}