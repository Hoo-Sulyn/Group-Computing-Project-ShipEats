package com.example.shipeatscustomer;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class C9_About extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        // Ensure this matches your XML file name
        setContentView(R.layout.activity_c9_about);

        // Initialize the back button (the anchor logo)
        ImageView ivBack = findViewById(R.id.ivBackAbout);

        ivBack.setOnClickListener(v -> {
            // Closes the activity and returns to CustomerSettingsActivity
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