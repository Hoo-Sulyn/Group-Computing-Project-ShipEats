package com.example.shipeatscustomer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText; // NEW IMPORT
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder; // NEW IMPORT
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class A4_OrderDetailReadyActivity extends AppCompatActivity {

    private TextView tvOrderNo, tvStatus, tvCustName, tvCustPhone, tvItems, tvTotal, tvPaymet, tvPaystat, tvDate, tvOrderType, tvSpecialInstructions;
    private String orderId;
    private DatabaseReference orderRef;
    private AdminOrderModel currentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_a4_order_det_ready);

        orderId = getIntent().getStringExtra("orderId");
        if (orderId == null) {
            Toast.makeText(this, "Order ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId);

        tvOrderNo = findViewById(R.id.tvOrderNoValue);
        tvStatus = findViewById(R.id.tvStatusBadge);
        tvCustName = findViewById(R.id.tvCustName);
        tvCustPhone = findViewById(R.id.tvCustPhone);
        tvItems = findViewById(R.id.tvItemName);
        tvTotal = findViewById(R.id.tvTotalValue);
        tvPaymet = findViewById(R.id.tvPaymet);
        tvPaystat = findViewById(R.id.tvPaystat);
        tvDate = findViewById(R.id.tvDateValue);
        tvOrderType = findViewById(R.id.tvOrderTypeValue);
        tvSpecialInstructions = findViewById(R.id.tvSpecialInstructions);

        loadOrderDetails();

        // MARK AS COMPLETED (UPDATED WITH SECURE PIN LOGIC)
        View btnComplete = findViewById(R.id.btnComplete);
        if (btnComplete != null) {
            btnComplete.setOnClickListener(v -> {
                if (currentOrder == null) return;

                // Instead of immediately completing, show the PIN verification dialog
                showVerifyPinDialog();
            });
        }

        View btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }
    }

    // --- NEW METHOD: Show PIN Dialog ---
    private void showVerifyPinDialog() {
        final EditText etPinInput = new EditText(this);
        etPinInput.setHint("Enter 4-digit PIN");
        etPinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etPinInput.setPadding(50, 40, 50, 40);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Verify Pickup")
                .setMessage("Ask the student for their Secure PIN to release the food.")
                .setView(etPinInput)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String inputPin = etPinInput.getText().toString().trim();

                    // Check if the input matches the PIN stored in the order
                    if (currentOrder.getPickupPin() != null && inputPin.equals(currentOrder.getPickupPin())) {
                        completeOrder(); // Correct PIN! Move to complete
                    } else {
                        Toast.makeText(this, "Wrong PIN! Please check with the student again.", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- NEW METHOD: Execute the completion after verification ---
    private void completeOrder() {
        orderRef.child("status").setValue("Completed")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Order Verified & Completed!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, OrderCompletedPPActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
                    if (tvStatus != null) tvStatus.setText("Ready");
                    if (tvCustName != null) tvCustName.setText(currentOrder.customerName != null ? currentOrder.customerName : "Guest");
                    if (tvItems != null) tvItems.setText(currentOrder.items != null ? currentOrder.items : "No items");
                    if (tvTotal != null) tvTotal.setText(currentOrder.totalPrice != null ? currentOrder.totalPrice : "RM 0.00");

                    if (tvPaymet != null) tvPaymet.setText(currentOrder.paymentMethod != null ? currentOrder.paymentMethod : "ONLINE");
                    if (tvPaystat != null) tvPaystat.setText("PAID");

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
                    Toast.makeText(A4_OrderDetailReadyActivity.this, "Order details not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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