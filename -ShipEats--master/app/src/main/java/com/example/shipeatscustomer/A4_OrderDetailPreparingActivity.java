package com.example.shipeatscustomer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class A4_OrderDetailPreparingActivity extends AppCompatActivity {

    private TextView tvOrderNo, tvStatus, tvCustName, tvCustPhone, tvItems, tvTotal, tvDate, tvOrderType, tvSpecialInstructions;
    private String orderId;
    private DatabaseReference orderRef;
    private AdminOrderModel currentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_a4_order_det_preparing);

        orderId = getIntent().getStringExtra("orderId");
        if (orderId == null) {
            Toast.makeText(this, "Order ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId);

        // Initialize Views with null checks
        tvOrderNo = findViewById(R.id.tvOrderNoValue);
        tvStatus = findViewById(R.id.tvStatusBadge);
        tvCustName = findViewById(R.id.tvCustName);
        tvCustPhone = findViewById(R.id.tvCustPhone);
        tvItems = findViewById(R.id.tvItemName);
        tvTotal = findViewById(R.id.tvTotalValue);
        tvDate = findViewById(R.id.tvDateValue);
        tvOrderType = findViewById(R.id.tvOrderTypeValue);
        tvSpecialInstructions = findViewById(R.id.tvSpecialInstructions);

        loadOrderDetails();

        // MARK AS READY / COMPLETED
        View btnReady = findViewById(R.id.btnPrimaryAction);
        if (btnReady != null) {
            btnReady.setOnClickListener(v -> {
                if (currentOrder == null) return;

                // --- STEP 2: GENERATE SECURE PICKUP PIN ---
                int randomPin = new Random().nextInt(9000) + 1000; // Generates 1000-9999
                String securePin = String.valueOf(randomPin);

                // Use a Map to update multiple fields at once (Status and PIN)
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", "Ready");
                updates.put("pickupPin", securePin);

                orderRef.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            // Send notification to customer
                            NotificationHelper.sendOrderNotification(currentOrder.customerId, orderId, "Ready for Pickup");

                            // Go to the Success/Ready screen and pass the PIN so Admin can see it
                            Intent intent = new Intent(this, OrderReadyPPActivity.class);
                            intent.putExtra("orderId", orderId);
                            intent.putExtra("pickupPin", securePin);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
        }

        View btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }
    }

    private void loadOrderDetails() {
        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentOrder = snapshot.getValue(AdminOrderModel.class);
                if (currentOrder != null) {
                    currentOrder.orderId = orderId;
                    String displayId = orderId.length() > 8 ? orderId.substring(orderId.length() - 8).toUpperCase() : orderId;
                    if (tvOrderNo != null) tvOrderNo.setText("#" + displayId);

                    if (tvStatus != null && currentOrder.status != null) {
                        tvStatus.setText(currentOrder.status.toLowerCase());
                    }

                    if (tvCustName != null) {
                        tvCustName.setText(currentOrder.customerName != null ? currentOrder.customerName : "Guest");
                    }

                    if (tvItems != null) {
                        tvItems.setText(currentOrder.items != null ? currentOrder.items : "No items");
                    }

                    if (tvTotal != null) {
                        tvTotal.setText(currentOrder.totalPrice != null ? currentOrder.totalPrice : "RM 0.00");
                    }

                    if (tvDate != null) {
                        tvDate.setText(currentOrder.date != null ? currentOrder.date : "N/A");
                    }

                    if (tvOrderType != null) {
                        if (currentOrder.pickupTime != null && !currentOrder.pickupTime.isEmpty()) {
                            tvOrderType.setText("Pre-order (" + currentOrder.pickupTime + ")");
                        } else {
                            tvOrderType.setText("Pickup on spot");
                        }
                    }

                    if (currentOrder.orderItems != null && !currentOrder.orderItems.isEmpty()) {
                        CartItem firstItem = currentOrder.orderItems.get(0);
                        String note = firstItem.getSpecialInstructions();

                        if (tvSpecialInstructions != null) {
                            if (note != null && !note.isEmpty()) {
                                tvSpecialInstructions.setVisibility(View.VISIBLE);
                                tvSpecialInstructions.setText("Note: " + note);
                            } else {
                                tvSpecialInstructions.setVisibility(View.GONE);
                            }
                        }
                    }

                } else {
                    Toast.makeText(A4_OrderDetailPreparingActivity.this, "Order details not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(A4_OrderDetailPreparingActivity.this, "Error loading data", Toast.LENGTH_SHORT).show();
            }
        });
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