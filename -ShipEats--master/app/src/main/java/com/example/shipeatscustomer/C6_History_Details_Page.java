package com.example.shipeatscustomer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class C6_History_Details_Page extends AppCompatActivity {

    private String orderId;
    private DatabaseReference orderRef;
    private ValueEventListener orderListener;

    private TextView tvOrderNum, tvOrderDate, tvSubtotal, tvTax, tvFee, tvTotal, tvPaymentStatus, tvTransactionType, tvPickupStatus;
    private TextView tvPickupPin;
    private View pinCardView;
    private LinearLayout itemListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_c6_history_details_page);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        orderId = getIntent().getStringExtra("ORDER_ID");

        // Initialize Views
        ImageView backButton = findViewById(R.id.back_button);
        tvOrderNum = findViewById(R.id.order_num);
        tvOrderDate = findViewById(R.id.order_date);
        tvSubtotal = findViewById(R.id.sub_total);
        tvTax = findViewById(R.id.service_tax);
        tvFee = findViewById(R.id.platform_fee);
        tvTotal = findViewById(R.id.total_cost);
        tvPaymentStatus = findViewById(R.id.payment_status);
        tvTransactionType = findViewById(R.id.transaction_type);
        tvPickupStatus = findViewById(R.id.pickup_status);
        itemListContainer = findViewById(R.id.order_item_list);

        tvPickupPin = findViewById(R.id.tv_pickup_pin_value);
        pinCardView = findViewById(R.id.pin_card_view);

        if (backButton != null) backButton.setOnClickListener(v -> finish());

        setupBottomNav();

        if (orderId != null) {
            orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId);
            loadOrderDetails();
        } else {
            Toast.makeText(this, "Order ID not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadOrderDetails() {
        orderListener = orderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                AdminOrderModel order = snapshot.getValue(AdminOrderModel.class);
                if (order != null) {
                    // 1. Set Order IDs and Basic Info
                    tvOrderNum.setText("#" + (order.orderId != null ? order.orderId : orderId));
                    tvOrderDate.setText(order.date != null ? order.date : "N/A");
                    tvTotal.setText(order.totalPrice);
                    tvTransactionType.setText(order.paymentMethod != null ? order.paymentMethod : "Card");

                    // 2. PIN Logic: Show PIN when the Chef marks order as 'Ready'
                    if ("Ready".equalsIgnoreCase(order.status) && order.getPickupPin() != null) {
                        pinCardView.setVisibility(View.VISIBLE);
                        tvPickupPin.setText(order.getPickupPin());
                    } else {
                        pinCardView.setVisibility(View.GONE);
                    }

                    // 3. Pricing Breakdown (Parsing "RM XX.XX" format)
                    try {
                        String priceStr = order.totalPrice.replace("RM", "").replace(" ", "").trim();
                        double total = Double.parseDouble(priceStr);
                        double subtotal = (total - 1.00) / 1.06;
                        double tax = subtotal * 0.06;
                        tvSubtotal.setText(String.format("RM %.2f", subtotal));
                        tvTax.setText(String.format("RM %.2f", tax));
                        tvFee.setText("RM 1.00");
                    } catch (Exception e) {
                        tvSubtotal.setText("N/A"); tvTax.setText("N/A"); tvFee.setText("N/A");
                    }

                    // 4. Status UI Color Logic
                    updateStatusUI(order.status);

                    // 5. Populate Item List
                    populateItems(order);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateStatusUI(String status) {
        boolean isCompleted = "Completed".equalsIgnoreCase(status) || "Success".equalsIgnoreCase(status);
        if (isCompleted) {
            tvPickupStatus.setText("Success");
            tvPickupStatus.setTextColor(Color.parseColor("#16A34A"));
            tvPaymentStatus.setText("PAID");
            tvPaymentStatus.setTextColor(Color.parseColor("#16A34A"));
        } else if ("Cancelled".equalsIgnoreCase(status)) {
            tvPickupStatus.setText("Cancelled");
            tvPickupStatus.setTextColor(Color.RED);
            tvPaymentStatus.setText("REFUNDED");
            tvPaymentStatus.setTextColor(Color.GRAY);
        } else {
            tvPickupStatus.setText(status);
            tvPickupStatus.setTextColor(Color.parseColor("#E69303"));
            tvPaymentStatus.setText("PAID");
            tvPaymentStatus.setTextColor(Color.parseColor("#16A34A"));
        }
    }

    private void populateItems(AdminOrderModel order) {
        itemListContainer.removeAllViews();
        if (order.items != null) {
            String[] itemsArray = order.items.split("\n");
            for (String itemLine : itemsArray) {
                if (itemLine.trim().isEmpty()) continue;

                View row = LayoutInflater.from(this).inflate(R.layout.history_item_row, itemListContainer, false);
                TextView nameTv = row.findViewById(R.id.tvItemName);
                TextView qtyTv = row.findViewById(R.id.tvItemQty);
                MaterialButton btnRate = row.findViewById(R.id.btnRateItem);

                // Assuming format: "2x Nasi Lemak" or "Nasi Lemak x2"
                int xIndex = itemLine.toLowerCase().indexOf("x");
                if (xIndex != -1) {
                    nameTv.setText(itemLine.substring(xIndex + 1).trim());
                    qtyTv.setText(itemLine.substring(0, xIndex + 1).trim());
                } else {
                    nameTv.setText(itemLine.trim());
                }

                if (("Completed".equalsIgnoreCase(order.status)) && btnRate != null) {
                    btnRate.setVisibility(View.VISIBLE);
                    btnRate.setOnClickListener(v -> showRatingPopup(nameTv.getText().toString()));
                }
                itemListContainer.addView(row);
            }
        }
    }

    private void showRatingPopup(String foodName) {
        ContextThemeWrapper themeWrapper = new ContextThemeWrapper(this, com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog);
        View dialogView = LayoutInflater.from(themeWrapper).inflate(R.layout.dialog_rate_food, null);
        RatingBar ratingBar = dialogView.findViewById(R.id.food_rating_bar);
        EditText etComment = dialogView.findViewById(R.id.rating_comment);

        new MaterialAlertDialogBuilder(themeWrapper)
                .setTitle("Rate " + foodName)
                .setView(dialogView)
                .setPositiveButton("Submit", (dialog, which) -> {
                    if (ratingBar.getRating() > 0) {
                        submitReview(foodName, ratingBar.getRating(), etComment.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitReview(String foodName, float rating, String comment) {
        // Logic to find foodId and call FoodRatingHelper.submitReview
        DatabaseReference menuRef = FirebaseDatabase.getInstance().getReference("menu_items");
        menuRef.orderByChild("name").equalTo(foodName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    FoodRatingHelper.submitReview(C6_History_Details_Page.this, ds.getKey(), rating, comment);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupBottomNav() {
        findViewById(R.id.menu_nav).setOnClickListener(v -> {
            Intent intent = new Intent(this, C3_Menu_Page.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
        findViewById(R.id.history_nav).setOnClickListener(v -> finish());
        findViewById(R.id.settings_nav).setOnClickListener(v -> {
            Intent intent = new Intent(this, C13_Settings_Page.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderRef != null && orderListener != null) {
            orderRef.removeEventListener(orderListener);
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