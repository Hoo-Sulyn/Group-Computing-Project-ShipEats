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
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

public class A4_OrderDetailPendingActivity extends AppCompatActivity {
    
    private TextView tvOrderNo, tvStatus, tvCustName, tvCustPhone, tvItems, tvTotal, tvDate, tvOrderType, tvSpecialInstructions;
    private String orderId;
    private DatabaseReference orderRef;
    private AdminOrderModel currentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_a4_order_det_pending);

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

        // ACCEPT ORDER: Change status to 'Preparing'
        View btnAccept = findViewById(R.id.btnPrimaryAction);
        if (btnAccept != null) {
            btnAccept.setOnClickListener(v -> {
                if (currentOrder == null) return;
                
                orderRef.child("status").setValue("Preparing")
                        .addOnSuccessListener(aVoid -> {
                            // Send notification to customer
                            NotificationHelper.sendOrderNotification(currentOrder.customerId, orderId, "Preparing");
                            
                            Intent intent = new Intent(this, OrderAcceptedPPActivity.class);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
        }

        // CANCEL ORDER
        // CANCEL ORDER
        View btnCancel = findViewById(R.id.btnSecondaryAction);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                if (currentOrder == null) return;

                AdminDialogHelper.showOrderCancelConfirmDialog(this, () -> {
                    orderRef.child("status").setValue("Cancelled")
                            .addOnSuccessListener(aVoid -> {
                                // Restock inventory
                                if (currentOrder.orderItems != null) {
                                    for (CartItem item : currentOrder.orderItems) {
                                        addBackToInventory(item.getFoodItem().getId(), item.getQuantity());
                                    }
                                }

                                NotificationHelper.sendOrderNotification(currentOrder.customerId, orderId, "Cancelled");

                                Toast.makeText(this, "Order Cancelled", Toast.LENGTH_SHORT).show();

                                // Use finishAfterTransition() for a cleaner exit if using newer Android versions
                                // or just finish()
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to cancel", Toast.LENGTH_SHORT).show();
                            });
                });
            });
        }

        View btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }
    }

    private void addBackToInventory(String foodId, int quantityToAdd) {
        if (foodId == null) return;
        DatabaseReference inventoryRef = FirebaseDatabase.getInstance().getReference("food_items").child(foodId);
        DatabaseReference menuRef = FirebaseDatabase.getInstance().getReference("menu_items").child(foodId);

        Transaction.Handler transactionHandler = new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                FoodItem item = currentData.getValue(FoodItem.class);
                if (item == null) return Transaction.success(currentData);

                int currentQty = item.getQuantity();
                item.setQuantity(currentQty + quantityToAdd);
                
                currentData.setValue(item);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {}
        };

        inventoryRef.runTransaction(transactionHandler);
        menuRef.runTransaction(transactionHandler);
    }

    private void loadOrderDetails() {
        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentOrder = snapshot.getValue(AdminOrderModel.class);
                if (currentOrder != null) {
                    currentOrder.orderId = orderId; // Ensure ID is set
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

                        if (note != null && !note.isEmpty()) {
                            tvSpecialInstructions.setVisibility(View.VISIBLE);
                            tvSpecialInstructions.setText("Note: " + note);
                        } else {
                            // Hide it if there are no instructions to keep the UI clean
                            tvSpecialInstructions.setVisibility(View.GONE);
                        }
                    }
                } else {
                    Toast.makeText(A4_OrderDetailPendingActivity.this, "Order details not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(A4_OrderDetailPendingActivity.this, "Error loading data", Toast.LENGTH_SHORT).show();
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
