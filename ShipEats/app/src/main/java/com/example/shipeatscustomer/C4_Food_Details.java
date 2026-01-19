package com.example.shipeatscustomer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class C4_Food_Details extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_c4_food_details);

        TextView close_food_details = findViewById(R.id.close_food_details);
        MaterialButton add_to_cart_button = findViewById(R.id.add_to_cart_button);

        close_food_details.setOnClickListener(v -> startActivity(new Intent(this, C3_Menu_Page.class)));

    }
}