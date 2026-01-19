package com.example.shipeatscustomer;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
        LinearLayout history_nav = findViewById(R.id.history_nav);
        LinearLayout settings_nav = findViewById(R.id.settings_nav);
        ImageView shopping_cart = findViewById(R.id.shopping_cart_icon);
        ImageView notification = findViewById(R.id.notification_icon);
        LinearLayout filter_icon = findViewById(R.id.filter_icon);

        history_nav.setOnClickListener(v -> startActivity(new Intent(this, C5_History_Page.class)));
//        settings_nav.setOnClickListener(v -> startActivity(new Intent(this, )));
        shopping_cart.setOnClickListener(v -> startActivity(new Intent(this, C7_Shopping_Cart.class)));


        //CHANGE FOOTER BUTTON COLOR
        int activeColor = Color.parseColor("#FFD700");
        menu_icon.setColorFilter(activeColor);
        menu_text.setTextColor(activeColor);
        menu_text.setTypeface(null, Typeface.BOLD);
    }


}