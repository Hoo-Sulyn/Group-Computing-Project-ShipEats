package com.example.shipeatscustomer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class C11_Payment_Method extends AppCompatActivity {

    private ImageView ivBack;
    private TextView tvPaymentInfo;
    private MaterialCardView btnCardInfo, btnGrabPayInfo;
    private MaterialButton btnPlaceOrder;

    // Data passed from C7
    private String pickupTime;
    private double subtotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_c11_payment_method);

        // 1. Capture data from Shopping Cart (C7)
        pickupTime = getIntent().getStringExtra("PICKUP_TIME");
        subtotal = getIntent().getDoubleExtra("SUBTOTAL", 0.0);

        // 2. Initialize Views
        ivBack = findViewById(R.id.ivBack);
        tvPaymentInfo = findViewById(R.id.tvPaymentInfo);
        btnCardInfo = findViewById(R.id.btnCardInfo);
        btnGrabPayInfo = findViewById(R.id.btnGrabPayInfo);

        // 3. Set initial information text
        tvPaymentInfo.setText("Select a method above to see security and usage details.");

        // 4. Button Listeners for info switching
        btnCardInfo.setOnClickListener(v -> showCardInfo());
        btnGrabPayInfo.setOnClickListener(v -> showGrabPayInfo());

        // 5. Navigation
        ivBack.setOnClickListener(v -> finish());
    }

    private void showCardInfo() {
        String cardText = "Credit / Debit Card via Stripe:\n\n" +
                "• Secured by Stripe (PCI-DSS Level 1 compliant).\n" +
                "• Your sensitive card data never touches our servers.\n" +
                "• Supports Visa, Mastercard, and AMEX.\n" +
                "• Instant payment verification.";
        tvPaymentInfo.setText(cardText);
    }

    private void showGrabPayInfo() {
        String grabText = "GrabPay E-Wallet:\n\n" +
                "• Fast and secure checkout via your Grab App.\n" +
                "• Earn GrabRewards points for your campus meals.\n" +
                "• Requires a mobile device with the Grab app installed.\n" +
                "• Payment is processed via Stripe's GrabPay integration.";
        tvPaymentInfo.setText(grabText);
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