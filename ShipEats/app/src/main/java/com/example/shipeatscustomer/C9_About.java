package com.example.shipeatscustomer;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class C9_About extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure this matches your XML file name
        setContentView(R.layout.activity_c9_about);

        // Initialize the back button (the anchor logo)
        ImageView ivBack = findViewById(R.id.ivBackAbout);

        ivBack.setOnClickListener(v -> {
            // Closes the activity and returns to CustomerSettingsActivity
            finish();
        });
    }
}