package com.example.shipeatscustomer;

import android.content.Intent;
import android.os.Bundle;
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

    // Stripe Variables
    private PaymentSheet paymentSheet;
    private String paymentIntentClientSecret = "";
    private String selectedMethod = "Card";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_c15_payment_process);

        // 1. Initialize Stripe (REPLACE WITH YOUR PK_TEST KEY)
        PaymentConfiguration.init(getApplicationContext(), "pk_test_51T3SmJ0cHohjJvNL1rNu02WISLkeE44QjMd6rWhkhdqSQAcPlQgvskQquMp3y0mVMf2Yp4IuYjYDKMOXDNhI8HQx00cLeVIZ2G");
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);

        // 2. Initialize Firebase & Views
        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");
        ivBack = findViewById(R.id.ivBack);
        stripeButton = findViewById(R.id.stripeButton);
        tvTotalPrice = findViewById(R.id.total_price_display);

        // 3. Display Price from Cart
        double subtotal = getIntent().getDoubleExtra("SUBTOTAL", 0.0);
        double total = subtotal + (subtotal * 0.06) + 1.00;
        tvTotalPrice.setText(String.format(Locale.getDefault(), "RM %.2f", total));

        // 4. Back Button
        ivBack.setOnClickListener(v -> finish());

        // 5. Start Payment Flow
        stripeButton.setOnClickListener(v -> fetchPaymentIntent());
    }

    private void fetchPaymentIntent() {
        stripeButton.setEnabled(false);

        // Convert total to Cents (RM 10.50 -> 1050)
        double subtotal = getIntent().getDoubleExtra("SUBTOTAL", 0.0);
        double total = subtotal + (subtotal * 0.06) + 1.00;
        long amountInCents = Math.round(total * 100);

        Map<String, Object> data = new HashMap<>();
        data.put("amount", amountInCents);

        // CALL YOUR DEPLOYED FIREBASE FUNCTION
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
                    Toast.makeText(this, "Function Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
            // SUCCESS: Save order to database
            generateOrderIdAndSave();
        } else if (result instanceof PaymentSheetResult.Canceled) {
            stripeButton.setEnabled(true);
            Toast.makeText(this, "Payment Canceled", Toast.LENGTH_SHORT).show();
        } else if (result instanceof PaymentSheetResult.Failed) {
            stripeButton.setEnabled(true);
            Toast.makeText(this, "Payment Failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateOrderIdAndSave() {
        ordersRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String nextId = "A0001";
                if (snapshot.exists()) {
                    for (DataSnapshot lastOrder : snapshot.getChildren()) {
                        String lastId = lastOrder.getKey();
                        if (lastId != null && lastId.matches("^[A-Z]\\d{4}$")) {
                            char letter = lastId.charAt(0);
                            int number = Integer.parseInt(lastId.substring(1));
                            if (number < 9999) {
                                nextId = String.format(Locale.getDefault(), "%c%04d", letter, number + 1);
                            } else {
                                char nextLetter = (char) (letter + 1);
                                if (nextLetter > 'Z') nextLetter = 'A';
                                nextId = String.format(Locale.getDefault(), "%c0001", nextLetter);
                            }
                        }
                    }
                }
                saveOrderToFirebase(nextId);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveOrderToFirebase(String orderId) {
        List<CartItem> cartItems = CartManager.getInstance().getCartItems();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = user != null ? user.getUid() : "GuestUser";
        String userName = user != null && user.getEmail() != null ? user.getEmail().split("@")[0] : "Guest";
        String pickupTime = getIntent().getStringExtra("PICKUP_TIME");

        StringBuilder itemsSummary = new StringBuilder();
        double subtotal = 0;
        int totalQty = 0;

        for (CartItem item : cartItems) {
            itemsSummary.append(item.getFoodItem().getName()).append(" x").append(item.getQuantity()).append("\n\n");
            subtotal += item.getFoodItem().getPrice() * item.getQuantity();
            totalQty += item.getQuantity();
        }

        String finalPrice = String.format(Locale.getDefault(), "RM %.2f", (subtotal + (subtotal * 0.06) + 1.00));
        String date = new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(new Date());

        AdminOrderModel order = new AdminOrderModel(
                orderId, itemsSummary.toString().trim(), finalPrice, "Pending",
                (long) totalQty, userName, userId, date, selectedMethod, pickupTime
        );

        ordersRef.child(orderId).setValue(order).addOnSuccessListener(aVoid -> {
            CartManager.getInstance().clearCart();
            NotificationHelper.sendOrderNotification(userId, orderId, "Pending");

            // Go to Success Screen
            Intent intent = new Intent(this, C8_Order_Placed.class);
            intent.putExtra("ORDER_ID", orderId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
