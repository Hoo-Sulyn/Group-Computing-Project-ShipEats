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

public class A1_Login_Page extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_a1_login_page);

        TextView login_option = findViewById(R.id.login_option);
        MaterialButton login_button = findViewById(R.id.login_button);

        login_option.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        login_button.setOnClickListener(v -> startActivity(new Intent(this, A2_Dashboard.class)));
    }
}