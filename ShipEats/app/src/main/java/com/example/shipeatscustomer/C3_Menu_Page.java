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

public class C3_Menu_Page extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_c3_menu_page);

        ImageView menu_icon = findViewById(R.id.menu_icon);
        TextView menu_text = findViewById(R.id.menu_text);


        //CHANGE FOOTER BUTTON COLOR
        int activeColor = Color.parseColor("#FFD700");
        menu_icon.setColorFilter(activeColor);
        menu_text.setTextColor(activeColor);
        menu_text.setTypeface(null, Typeface.BOLD);
    }
}