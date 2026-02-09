package com.example.shipeatscustomer;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class C11_Payment_Method extends AppCompatActivity {

    private MaterialCardView btnCard, btnFPX;
    private TextView tvPaymentInfo;
    private ImageView ivBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_c11_payment_method);

        // 1. Initialize Views
        btnCard = findViewById(R.id.btnCardInfo);
        btnFPX = findViewById(R.id.btnFPXInfo);
        tvPaymentInfo = findViewById(R.id.tvPaymentInfo);
        ivBack = findViewById(R.id.ivBack);

        // 2. Back Button Logic
        ivBack.setOnClickListener(v -> finish());

        // 3. Set Card Click Logic
        btnCard.setOnClickListener(v -> showCardInfo());

        // 4. Set FPX Click Logic
        btnFPX.setOnClickListener(v -> showFPXInfo());

        // Load default state (e.g., Card selected)
        showCardInfo();
    }

    private void showCardInfo() {
        // Update UI Colors
        btnCard.setCardBackgroundColor(Color.parseColor("#B2EBF2")); // Light Teal
        btnFPX.setCardBackgroundColor(Color.WHITE);

        // Update Text Content
        tvPaymentInfo.setText("1. Secure Transactions\nAll credit and debit card payments are processed through a secure and encrypted payment gateway to protect your personal and financial information.\n\n" +
                "2. No Card Data Stored\nWe do not store, process, or have access to your full card details. All card information is handled directly by our trusted payment service provider.\n\n" +
                "3. PCI-DSS Compliant\nOur payment system complies with PCI-DSS (Payment Card Industry Data Security Standards), ensuring industry-level protection for all card transactions.\n\n" +
                "4. Authentication & Fraud Prevention\nAdditional security measures such as OTP / 3D Secure (Verified by Visa, Mastercard SecureCode) may be required to prevent unauthorized transactions.\n\n" +
                "5. Privacy Protection\nYour payment information is used solely for transaction purposes and is protected in accordance with applicable data protection and privacy laws.\n\n" +
                "6. Trusted Payment Partners\nWe work only with reputable and certified payment gateways to ensure safe, reliable, and seamless payments.");
    }

    private void showFPXInfo() {
        // Update UI Colors
        btnFPX.setCardBackgroundColor(Color.parseColor("#B2EBF2")); // Light Teal
        btnCard.setCardBackgroundColor(Color.WHITE);

        // Update Text Content
        tvPaymentInfo.setText("1. Secure Bank Transfers\nFPX payments are processed through PayNet (Payments Network Malaysia), Malaysia's official national payments network.\n\n" +
                "2. Direct Bank Authentication\nAll FPX transactions require users to log in directly to their selected bank's secure online banking system, ensuring full authentication by the bank.\n\n" +
                "3. No Banking Credentials Stored\nWe do not collect, store, or have access to your bank login details. All authentication is handled entirely by your bank.\n\n" +
                "4. End-to-End Encryption\nFPX transactions are protected using industry-standard encryption to safeguard your payment information.\n\n" +
                "5. Real-Time Confirmation\nPayments are validated in real time, reducing errors, failed transactions, and unauthorized transfers.\n\n" +
                "6. Regulatory Compliance\nFPX operates under Bank Negara Malaysia (BNM) regulations and complies with applicable Malaysian financial security standards.");
    }
}