package com.example.shipeatscustomer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class OrderReadyPPActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_dialog_order_ready);
        
        // Auto-close after 2 seconds
        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
    }
}