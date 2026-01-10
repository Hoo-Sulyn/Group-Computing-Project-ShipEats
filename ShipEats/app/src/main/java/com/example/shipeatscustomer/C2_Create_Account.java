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

public class C2_Create_Account extends AppCompatActivity {

    private MaterialButton create_account_button;
    private TextView login_option;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_c2_create_account);

        create_account_button = findViewById(R.id.create_account_button);
        login_option = findViewById(R.id.login_option);

        create_account_button.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        login_option.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
    }
}