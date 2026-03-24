package com.example.shipeatscustomer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class C15_Payment_Process extends AppCompatActivity {

    private ImageView ivBack;
    private Button stripeButton;
    private TextView tvTotalPrice;
    private DatabaseReference ordersRef;

    private boolean isSavingOrder = false;

    // Stripe Variables
    private PaymentSheet paymentSheet;
    private String paymentIntentClientSecret = "";
    private String selectedMethod = "Card";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_c15_payment_process);

        // 1. Initialize Stripe
        PaymentConfiguration.init(getApplicationContext(), "pk_test_51T3SmJ0cHohjJvNL1rNu02WISLkeE44QjMd6rWhkhdqSQAcPlQgvskQquMp3y0mVMf2Yp4IuYjYDKMOXDNhI8HQx00cLeVIZ2G");
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);

        // 2. Initialize Firebase & Views
        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");
        ivBack = findViewById(R.id.ivBack);
        stripeButton = findViewById(R.id.stripeButton);
        tvTotalPrice = findViewById(R.id.total_price_display);

        // 3. Display Price (Subtotal + 6% Tax + RM1.00 Fee)
        double subtotal = getIntent().getDoubleExtra("SUBTOTAL", 0.0);
        double total = subtotal + (subtotal * 0.06) + 1.00;
        tvTotalPrice.setText(String.format(Locale.getDefault(), "RM %.2f", total));

        ivBack.setOnClickListener(v -> finish());
        stripeButton.setOnClickListener(v -> fetchPaymentIntent());
    }

    private void fetchPaymentIntent() {
        stripeButton.setEnabled(false);

        double subtotal = getIntent().getDoubleExtra("SUBTOTAL", 0.0);
        double total = subtotal + (subtotal * 0.06) + 1.00;
        long amountInCents = Math.round(total * 100);

        Map<String, Object> data = new HashMap<>();
        data.put("amount", amountInCents);

        FirebaseFunctions.getInstance()
                .getHttpsCallable("createStripePaymentIntent")
                .call(data)
                .addOnSuccessListener(result -> {
                    Map<String, Object> res = (Map<String, Object>) result.getData();
                    if (res != null) {
                        paymentIntentClientSecret = (String) res.get("clientSecret");
                        presentPaymentSheet();
                    }
                })
                .addOnFailureListener(e -> {
                    stripeButton.setEnabled(true);
                    Toast.makeText(this, "Stripe Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void presentPaymentSheet() {
        final PaymentSheet.Configuration configuration = new PaymentSheet.Configuration.Builder("Ship Eats")
                .merchantDisplayName("Ship Eats Malaysia")
                .build();
        paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret, configuration);
    }

    private void onPaymentSheetResult(final PaymentSheetResult result) {
        if (result instanceof PaymentSheetResult.Completed) {
            this.selectedMethod = "Card";

            generateOrderIdAndSave();
        } else {
            stripeButton.setEnabled(true);
            if (result instanceof PaymentSheetResult.Canceled) {
                Log.d("STRIPE_LOG", "Payment Canceled");
            } else if (result instanceof PaymentSheetResult.Failed) {
                Log.e("STRIPE_LOG", "Payment Failed", ((PaymentSheetResult.Failed) result).getError());
            }
        }
    }

    private void generateOrderIdAndSave() {
        if (isSavingOrder) return;
        isSavingOrder = true;

        ordersRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String nextId = "A0001";
                if (snapshot.exists()) {
                    DataSnapshot lastChild = snapshot.getChildren().iterator().next();
                    String lastId = lastChild.getKey();

                    if (lastId != null && lastId.matches("^[A-Z]\\d{4}$")) {
                        char letter = lastId.charAt(0);
                        int number = Integer.parseInt(lastId.substring(1));
                        if (number < 9999) {
                            nextId = String.format(Locale.getDefault(), "%c%04d", letter, number + 1);
                        } else {
                            char nextLetter = (char) (letter + 1);
                            nextId = String.format(Locale.getDefault(), "%c0001", nextLetter);
                        }
                    }
                }
                saveOrderToFirebase(nextId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isSavingOrder = false;
                stripeButton.setEnabled(true);
            }
        });
    }

    private void saveOrderToFirebase(String orderId) {
        List<CartItem> cartItems = CartManager.getInstance().getCartItems();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = user != null ? user.getUid() : "Guest";

        // Extracting Student ID from email if available
        String studentId = (user != null && user.getEmail() != null)
                ? user.getEmail().split("@")[0].toUpperCase() : "GUEST";

        String pickupTime = getIntent().getStringExtra("PICKUP_TIME");

        DatabaseReference menuRef = FirebaseDatabase.getInstance().getReference("menu_items");
        DatabaseReference inventoryRef = FirebaseDatabase.getInstance().getReference("food_items");

        StringBuilder itemsSummary = new StringBuilder();
        double subtotal = 0;
        int totalQty = 0;

        // Atomic Inventory Update & Summary Building
        for (CartItem item : cartItems) {
            int qtyBought = item.getQuantity();
            String foodId = item.getFoodItem().getId();

            itemsSummary.append(item.getQuantity()).append("x ").append(item.getFoodItem().getName()).append("\n");
            subtotal += item.getFoodItem().getPrice() * item.getQuantity();
            totalQty += item.getQuantity();

            if (foodId != null) {
                // 1. UPDATE MENU_ITEMS
                // Deduct stock by actual quantity
                menuRef.child(foodId).child("quantity").setValue(ServerValue.increment(-qtyBought));
                // Increment sold count by actual quantity
                menuRef.child(foodId).child("orderCount").setValue(ServerValue.increment(qtyBought));

                // 2. UPDATE FOOD_ITEMS (Inventory Sync)
                inventoryRef.child(foodId).child("quantity").setValue(ServerValue.increment(-qtyBought));
                inventoryRef.child(foodId).child("orderCount").setValue(ServerValue.increment(qtyBought));
            }
        }

        // Final Calculations
        String finalPriceStr = String.format(Locale.getDefault(), "RM %.2f", (subtotal + (subtotal * 0.06) + 1.00));
        String dateStr = new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());

        // Generate Secure 4-Digit PIN
        String pickupPin = String.valueOf((int)(Math.random() * 9000) + 1000);

        // Create Model Instance
        AdminOrderModel order = new AdminOrderModel(
                orderId,
                itemsSummary.toString().trim(),
                cartItems,
                finalPriceStr,
                "Pending",
                (long) totalQty,
                studentId, // Using Student ID as customer name/identifier
                userId,
                dateStr,
                selectedMethod,
                pickupTime,
                pickupPin
        );

        // Save to Database
        ordersRef.child(orderId).setValue(order).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Also save to User's private history node
                FirebaseDatabase.getInstance().getReference("UserOrders")
                        .child(userId).child(orderId).setValue(order);

                CartManager.getInstance().clearCart();

                Intent intent = new Intent(C15_Payment_Process.this, C8_Order_Placed.class);
                intent.putExtra("ORDER_ID", orderId);
                intent.putExtra("PICKUP_PIN", pickupPin);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                isSavingOrder = false;
                stripeButton.setEnabled(true);
                Toast.makeText(this, "Failed to save order", Toast.LENGTH_SHORT).show();
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