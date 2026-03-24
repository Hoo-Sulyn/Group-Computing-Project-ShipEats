package com.example.shipeatscustomer;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class C5_History_Page extends AppCompatActivity {

    private LinearLayout historyContainer;
    private DatabaseReference ordersRef;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_c5_history_page);

        // 1. Init Views & Firebase
        historyContainer = findViewById(R.id.history_lists_container);
        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");
        currentUserId = FirebaseAuth.getInstance().getUid();

        TextView btnClearHistory = findViewById(R.id.clear_history);
        if (btnClearHistory != null) {
            btnClearHistory.setOnClickListener(v -> showClearHistoryConfirmation());
        }

        setupNavigation();
        loadOrderHistory();
    }

    private void loadOrderHistory() {
        TextView tvEmptyMessage = findViewById(R.id.empty_history_text);
        TextView btnClearHistory = findViewById(R.id.clear_history);

        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                historyContainer.removeAllViews();
                List<AdminOrderModel> userOrders = new ArrayList<>();

                // 1. Collect all orders for this user
                for (DataSnapshot ds : snapshot.getChildren()) {
                    AdminOrderModel order = ds.getValue(AdminOrderModel.class);
                    if (order != null && currentUserId != null && currentUserId.equals(order.customerId)) {

                        // --- THE FILTER ---
                        if (order.hiddenByCustomer) {
                            continue; // Skip this order, don't add it to the list
                        }

                        userOrders.add(order);
                    }
                }

                // 2. Decide what to show
                if (userOrders.isEmpty()) {
                    // SHOW EMPTY STATE
                    if (tvEmptyMessage != null) {
                        tvEmptyMessage.setVisibility(View.VISIBLE);
                        tvEmptyMessage.setText("No order history.\nTry ordering something!");
                        // Optional: make the text take them back to the menu
                        tvEmptyMessage.setOnClickListener(v -> finish());
                    }
                    if (btnClearHistory != null) btnClearHistory.setVisibility(View.GONE);
                } else {
                    // SHOW HISTORY LIST
                    if (tvEmptyMessage != null) tvEmptyMessage.setVisibility(View.GONE);
                    if (btnClearHistory != null) btnClearHistory.setVisibility(View.VISIBLE);

                    // Sort newest first
                    Collections.reverse(userOrders);

                    // Add the rows ONLY when we have data
                    for (AdminOrderModel order : userOrders) {
                        addHistoryRow(order);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(C5_History_Page.this, "Error loading history", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addHistoryRow(AdminOrderModel order) {
        View row = LayoutInflater.from(this).inflate(R.layout.order_history_card_layout, historyContainer, false);

        TextView tvDate = row.findViewById(R.id.date_history);
        TextView tvPayment = row.findViewById(R.id.payment_history);
        TextView tvAmount = row.findViewById(R.id.amount_history);
        TextView btnDetails = row.findViewById(R.id.view_details_button);

        if (tvDate != null) tvDate.setText(order.date);
        if (tvAmount != null) tvAmount.setText(order.totalPrice != null ? order.totalPrice.replace("RM ", "") : "0.00");

        // --- DYNAMIC PAYMENT METHOD LOGIC ---
        if (tvPayment != null) {
            String method = order.paymentMethod; // Field from your AdminOrderModel
            if ("GrabPay".equalsIgnoreCase(method)) {
                tvPayment.setText("GrabPay");
                tvPayment.setTextColor(Color.parseColor("#00B14F"));
            } else if ("Card".equalsIgnoreCase(method)){
                tvPayment.setText("Card");
                tvPayment.setTextColor(Color.parseColor("#1E5D8B"));
            }
            else{
                tvPayment.setText("Paid");
                tvPayment.setTextColor(Color.parseColor("#000000"));
            }
        }

        if (btnDetails != null) {
            btnDetails.setOnClickListener(v -> {
                Intent intent = new Intent(this, C6_History_Details_Page.class);
                intent.putExtra("ORDER_ID", order.orderId);
                startActivity(intent);
            });
        }

        historyContainer.addView(row);
    }

    private void setupNavigation() {
        ImageView history_icon = findViewById(R.id.history_icon);
        TextView history_text = findViewById(R.id.history_text);

        // Highlight current tab
        if (history_icon != null && history_text != null) {
            int activeColor = Color.parseColor("#FFD700");
            history_icon.setColorFilter(activeColor);
            history_text.setTextColor(activeColor);
            history_text.setTypeface(null, Typeface.BOLD);
        }

        findViewById(R.id.menu_nav).setOnClickListener(v -> startActivity(new Intent(this, C3_Menu_Page.class)));
        findViewById(R.id.settings_nav).setOnClickListener(v -> startActivity(new Intent(this, C13_Settings_Page.class)));
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
    }

    private void showClearHistoryConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear Order History")
                .setMessage("Are you sure? This will permanently removed all your past orders that's older than 1 week. This action cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    performClearHistory();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void performClearHistory() {
        if (currentUserId == null) return;

        ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int updateCount = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    AdminOrderModel order = ds.getValue(AdminOrderModel.class);

                    if (order != null && currentUserId.equals(order.customerId)) {
                        // 1. Check if the order is Finished (Completed or Cancelled)
                        boolean isFinished = "Completed".equalsIgnoreCase(order.status) ||
                                "Cancelled".equalsIgnoreCase(order.status);

                        // 2. Check if the order is older than a week using our helper method
                        boolean isOldEnough = isOlderThanOneWeek(order.date);

                        // 3. ONLY hide if BOTH conditions are true
                        if (isFinished && isOldEnough) {
                            // This updates the field in Firebase without deleting the whole order
                            ds.getRef().child("hiddenByCustomer").setValue(true);
                            updateCount++;
                        }
                    }
                }

                if (updateCount > 0) {
                    Toast.makeText(C5_History_Page.this, updateCount + " old orders removed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(C5_History_Page.this, "No eligible orders to clear (must be 1 week old)", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(C5_History_Page.this, "Database error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isOlderThanOneWeek(String dateString) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            java.util.Date orderDate = sdf.parse(dateString);
            if (orderDate == null) return false;

            long diff = System.currentTimeMillis() - orderDate.getTime();
            return diff > (7L * 24 * 60 * 60 * 1000);
        } catch (Exception e) {
            return false;
        }
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // Hides the bottom buttons
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);    // Hides the top status bar
    }
}