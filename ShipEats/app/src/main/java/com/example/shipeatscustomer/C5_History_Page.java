package com.example.shipeatscustomer;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class C5_History_Page extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_c5_history_page);

        ImageView history_icon = findViewById(R.id.history_icon);
        TextView history_text = findViewById(R.id.history_text);
        ImageView back_button = findViewById(R.id.back_button);

        int activeColor = Color.parseColor("#FFD700");
        history_icon.setColorFilter(activeColor);
        history_text.setTextColor(activeColor);
        history_text.setTypeface(null, Typeface.BOLD);
    }
}