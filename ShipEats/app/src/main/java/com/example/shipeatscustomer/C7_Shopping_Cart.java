package com.example.shipeatscustomer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class C7_Shopping_Cart extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_c7_shopping_cart);
        
        ImageView back_button = findViewById(R.id.back_button);
        SwitchMaterial preOrderSwitch = findViewById(R.id.pre_order);
        LinearLayout preOrderSection = findViewById(R.id.enable_pre_order);

        checkCartEmpty();

        back_button.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // IF THE PRE-ORDER TOGGLE IS ON, SHOW THE TIME SELECTION
        preOrderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Show the section when toggle is ON
                preOrderSection.setVisibility(View.VISIBLE);
            } else {
                // Hide the section when toggle is OFF
                preOrderSection.setVisibility(View.GONE);
            }
        });
    }

    private void checkCartEmpty() {
        LinearLayout cartItemsContainer = findViewById(R.id.shopping_cart_items);
        TextView emptyMessage = findViewById(R.id.empty_cart_message);
        LinearLayout preOrderContainer = findViewById(R.id.pre_order_container);
        com.google.android.material.button.MaterialButton paymentButton = findViewById(R.id.continue_to_payment_button);

        if (cartItemsContainer.getChildCount() == 0) {
            // Show empty message & Hide Pre-order
            emptyMessage.setVisibility(View.VISIBLE);
            preOrderContainer.setVisibility(View.GONE);

            // Disable and turn GREY
            paymentButton.setEnabled(false);
            paymentButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#c7c7c7")));
        } else {
            // Hide message & Show Pre-order
            emptyMessage.setVisibility(View.GONE);
            preOrderContainer.setVisibility(View.VISIBLE);

            // Enable and turn back to ORANGE
            paymentButton.setEnabled(true);
            paymentButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E69303")));
        }
    }
}