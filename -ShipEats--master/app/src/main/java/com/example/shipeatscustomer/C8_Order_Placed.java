package com.example.shipeatscustomer;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.OutputStream;

public class C8_Order_Placed extends AppCompatActivity {

    private LinearLayout receiptLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_c8_order_placed);

        // 1. Initialize Views
        receiptLayout = findViewById(R.id.main_receipt_container);
        TextView orderNumberTv = findViewById(R.id.order_number);
        MaterialButton doneButton = findViewById(R.id.done_button);

        // 2. Receive Data from Intent (Passed from C15)
        String orderId = getIntent().getStringExtra("ORDER_ID");

        // 3. Set Values
        if (orderId != null) orderNumberTv.setText(orderId);

        // 4. Done Button: Clear stack and return to Menu
        doneButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, C3_Menu_Page.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
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
