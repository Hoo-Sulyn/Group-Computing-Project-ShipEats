package com.example.shipeatscustomer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class AdminOrdersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final List<AdminOrderModel> orderList;

    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_COMPLETED = 1;

    public AdminOrdersAdapter(Context context, List<AdminOrderModel> orderList) {
        this.context = context;
        this.orderList = orderList;
    }

    @Override
    public int getItemViewType(int position) {
        AdminOrderModel order = orderList.get(position);
        if (order != null && ("Completed".equalsIgnoreCase(order.status) || "Done".equalsIgnoreCase(order.status))) {
            return TYPE_COMPLETED;
        }
        return TYPE_NORMAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_COMPLETED) {
            View v = LayoutInflater.from(context).inflate(R.layout.admin_it_order_completed_card, parent, false);
            return new CompletedViewHolder(v);
        } else {
            View v = LayoutInflater.from(context).inflate(R.layout.admin_it_order_card, parent, false);
            return new OrderViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AdminOrderModel order = orderList.get(position);
        if (order == null) return;

        String orderId = order.orderId != null ? order.orderId : "unknown";
        String displayId = orderId.length() > 8 ? orderId.substring(orderId.length() - 8).toUpperCase() : orderId;
        String status = order.status != null ? order.status : "Pending";
        String customerName = order.customerName != null ? order.customerName : "Guest";

        if (holder instanceof CompletedViewHolder) {
            CompletedViewHolder cvh = (CompletedViewHolder) holder;
            cvh.tvOrderNo.setText("#" + displayId);
            cvh.tvCustomerName.setText(customerName);
            cvh.tvDate.setText(order.date != null ? order.date : "29/10/2025");
            cvh.tvTime.setText("10:30 AM");

            loadCustomerProfile(order.customerId, cvh.ivCustomerProfile);

            // Open completed orders ONLY when clicked View Details
            cvh.btnViewDetails.setOnClickListener(v -> openDetails(orderId, status));
            cvh.itemView.setOnClickListener(null); // Disable card-level click for completed orders

        } else if (holder instanceof OrderViewHolder) {
            OrderViewHolder ovh = (OrderViewHolder) holder;

            // --- UPDATED LOGIC: Display PIN if Ready ---
            if ("Ready".equalsIgnoreCase(status) && order.getPickupPin() != null) {
                ovh.tvOrderNo.setText("#" + displayId + " [PIN: " + order.getPickupPin() + "]");
                ovh.tvStatusBadge.setBackgroundColor(Color.parseColor("#2196F3"));
                ovh.tvStatusBadge.setTextColor(Color.WHITE);
            } else {
                ovh.tvOrderNo.setText("#" + displayId);

                if ("Pending".equalsIgnoreCase(status)) {
                    ovh.tvStatusBadge.setBackgroundColor(Color.parseColor("#FFA500"));
                    ovh.tvStatusBadge.setTextColor(Color.WHITE);
                } else if ("Preparing".equalsIgnoreCase(status)) {
                    ovh.tvStatusBadge.setBackgroundColor(Color.parseColor("#4CAF50"));
                    ovh.tvStatusBadge.setTextColor(Color.WHITE);
                }
            }

            ovh.tvStatusBadge.setText(status.toLowerCase());

            if (order.pickupTime != null && !order.pickupTime.isEmpty()) {
                ovh.tvPickupTime.setText("Pickup: " + order.pickupTime);
                ovh.tvPickupTime.setVisibility(View.VISIBLE);
            } else {
                ovh.tvPickupTime.setVisibility(View.GONE);
            }

            ovh.tvCustomerName.setText("⚪ " + customerName);
            String itemsText = order.items != null ? order.items.replace("\n", ", ") : "No items";
            ovh.tvItems.setText("Items: " + itemsText);
            long count = order.itemCount != null ? order.itemCount : 0;
            ovh.tvItemCount.setText(count + (count > 1 ? " items" : " item"));
            ovh.tvTotal.setText("Total " + (order.totalPrice != null ? order.totalPrice : "RM 0.00"));

            ovh.itemView.setOnClickListener(v -> openDetails(orderId, status));
        }
    }

    private void loadCustomerProfile(String customerId, ImageView imageView) {
        if (customerId == null || customerId.isEmpty()) return;

        FirebaseDatabase.getInstance().getReference("Users").child(customerId)
                .child("profileImage").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String imgUrl = snapshot.getValue(String.class);
                        if (imgUrl != null && !imgUrl.isEmpty()) {
                            Glide.with(context).load(imgUrl).circleCrop()
                                    .placeholder(R.drawable.placeholder_user).into(imageView);
                        } else {
                            imageView.setImageResource(R.drawable.placeholder_user);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void openDetails(String orderId, String status) {
        Intent intent;
        if ("Pending".equalsIgnoreCase(status)) {
            intent = new Intent(context, A4_OrderDetailPendingActivity.class);
        } else if ("Preparing".equalsIgnoreCase(status)) {
            intent = new Intent(context, A4_OrderDetailPreparingActivity.class);
        } else if ("Ready".equalsIgnoreCase(status)) {
            intent = new Intent(context, A4_OrderDetailReadyActivity.class);
        } else {
            intent = new Intent(context, A4_OrderDetailCompletedActivity.class);
        }
        intent.putExtra("orderId", orderId);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderNo, tvStatusBadge, tvCustomerName, tvItems, tvItemCount, tvTotal, tvPickupTime;
        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderNo = itemView.findViewById(R.id.tvOrderNo);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvPickupTime = itemView.findViewById(R.id.tvPickupTime);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvItems = itemView.findViewById(R.id.tvItems);
            tvItemCount = itemView.findViewById(R.id.tvItemCount);
            tvTotal = itemView.findViewById(R.id.tvTotal);
        }
    }

    static class CompletedViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderNo, tvCustomerName, tvDate, tvTime;
        ImageView ivCustomerProfile;
        View btnViewDetails;
        CompletedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderNo = itemView.findViewById(R.id.tvOrderNo);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivCustomerProfile = itemView.findViewById(R.id.ivCustomerProfile);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }
    }
}