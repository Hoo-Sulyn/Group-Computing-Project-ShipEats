package com.example.shipeatscustomer;

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

public class A4_OrderDetailCompletedActivity extends AppCompatActivity {

    private TextView tvOrderNo, tvStatus, tvCustName, tvCustPhone, tvItems, tvTotal, tvPayMeth, tvPayStat, tvSpecialInstructions;
    private String orderId;
    private DatabaseReference orderRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_a4_order_det_history);

        orderId = getIntent().getStringExtra("orderId");
        if (orderId == null) {
            Toast.makeText(this, "Order ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId);

        // Initialize Views with robust null checks
        tvOrderNo = findViewById(R.id.tvOrderNoValue);
        tvStatus = findViewById(R.id.tvStatusBadge);
        tvCustName = findViewById(R.id.tvCustName);
        tvCustPhone = findViewById(R.id.tvCustPhone);
        tvItems = findViewById(R.id.tvItemName);
        tvTotal = findViewById(R.id.tvTotalValue);
        tvPayMeth = findViewById(R.id.tvPaymet);
        tvPayStat = findViewById(R.id.tvPaystat);
        tvSpecialInstructions = findViewById(R.id.tvSpecialInstructions);

        loadOrderDetails();

        View btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }
    }

    private void loadOrderDetails() {
        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                AdminOrderModel order = snapshot.getValue(AdminOrderModel.class);
                if (order != null) {
                    String displayId = orderId.length() > 8 ? orderId.substring(orderId.length() - 8).toUpperCase() : orderId;
                    if (tvOrderNo != null) tvOrderNo.setText("#" + displayId);
                    
                    if (tvStatus != null && order.status != null) {
                        tvStatus.setText(order.status.toLowerCase());
                    }
                    
                    if (tvCustName != null) {
                        tvCustName.setText(order.customerName != null ? order.customerName : "Guest");
                    }
                    
                    if (tvItems != null) {
                        tvItems.setText(order.items != null ? order.items : "No items");
                    }
                    
                    if (tvTotal != null) {
                        tvTotal.setText(order.totalPrice != null ? order.totalPrice : "RM 0.00");
                    }

                    if (order.orderItems != null && !order.orderItems.isEmpty()) {
                        CartItem firstItem = order.orderItems.get(0);
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

                    // Set default payment info for history
                    if (tvPayMeth != null) tvPayMeth.setText("ONLINE");
                    if (tvPayStat != null) tvPayStat.setText("PAID");

                } else {
                    Toast.makeText(A4_OrderDetailCompletedActivity.this, "Details not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(A4_OrderDetailCompletedActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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