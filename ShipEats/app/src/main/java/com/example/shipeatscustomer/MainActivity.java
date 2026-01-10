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

public class MainActivity extends AppCompatActivity {

    private MaterialButton admin_option, login_button;
    private TextView create_account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        admin_option = findViewById(R.id.admin_option);
        login_button = findViewById(R.id.login_button);
        create_account = findViewById(R.id.create_account);

        admin_option.setOnClickListener(v -> startActivity(new Intent(this, A1_Login_Page.class)));
        login_button.setOnClickListener(v -> startActivity(new Intent(this, C3_Menu_Page.class)));
        create_account.setOnClickListener(v -> startActivity(new Intent(this, C2_Create_Account.class)));
    }
}