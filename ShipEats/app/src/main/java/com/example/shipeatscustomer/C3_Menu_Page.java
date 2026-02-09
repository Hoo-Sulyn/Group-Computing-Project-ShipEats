package com.example.shipeatscustomer;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

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
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        LinearLayout filterBtn = findViewById(R.id.filter_icon);
        TextView close_tab_button = findViewById(R.id.close_tab_button);
        com.google.android.material.search.SearchBar searchBar = findViewById(R.id.search_bar);

        filterBtn.setOnClickListener(v -> {
            drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);
        });

        close_tab_button.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));

        history_nav.setOnClickListener(v -> startActivity(new Intent(this, C5_History_Page.class)));
        settings_nav.setOnClickListener(v -> startActivity(new Intent(this, C13_Settings_Page.class)));
        shopping_cart.setOnClickListener(v -> startActivity(new Intent(this, C7_Shopping_Cart.class)));


        // This listens for when the user presses the "Search" icon on their keyboard
        searchBar.setOnKeyListener((v, keyCode, event) -> {
            // If the event is a key-down event on the "enter" button
            if ((event.getAction() == android.view.KeyEvent.ACTION_DOWN) &&
                    (keyCode == android.view.KeyEvent.KEYCODE_ENTER)) {

                String query = searchBar.getText().toString();
                if (!query.isEmpty()) {
                    performSearch(query);
                }
                return true;
            }
            return false;
        });


        //CHANGE FOOTER BUTTON COLOR
        int activeColor = Color.parseColor("#FFD700");
        menu_icon.setColorFilter(activeColor);
        menu_text.setTextColor(activeColor);
        menu_text.setTypeface(null, Typeface.BOLD);
    }

    private void performSearch(String query) {
        LinearLayout container = findViewById(R.id.food_card_container);
        TextView noMenuText = findViewById(R.id.no_menu_text);

        container.removeAllViews(); // Clear previous results

        // Logic to count results
        int resultsFound = 0;

        // Simulate filtering
        // for (Item item : itemList) {
        //    if(item.matches(query)) {
        //        addCardToContainer(item);
        //        resultsFound++;
        //    }
        // }

        if (resultsFound == 0) {
            noMenuText.setVisibility(android.view.View.VISIBLE);
        } else {
            noMenuText.setVisibility(android.view.View.GONE);
        }
    }
}