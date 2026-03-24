package com.example.shipeatscustomer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class C10_Notifications extends AppCompatActivity {

    private SwitchCompat swPush, swLockScreen, swMenuUpdates, swPaymentUpdate, swOrderUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_c10_notifications);

        // 1. Initialize Views
        swPush = findViewById(R.id.swPush);
        swLockScreen = findViewById(R.id.swLockScreen);
        swMenuUpdates = findViewById(R.id.swMenuUpdates);
        swPaymentUpdate = findViewById(R.id.swPaymentUpdate);
        swOrderUpdate = findViewById(R.id.swOrderUpdate);
        Button btnSave = findViewById(R.id.btnSaveNotifications);

        // 2. Load existing settings from SharedPreferences
        loadNotificationSettings();

        // 3. Back Button logic
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        // 4. Save Button logic
        btnSave.setOnClickListener(v -> {
            saveNotificationSettings();
            Toast.makeText(this, "Notification preferences saved!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void loadNotificationSettings() {
        SharedPreferences prefs = getSharedPreferences("NotifPrefs", MODE_PRIVATE);

        // Defaulting to 'true' (on) if no setting is found
        swPush.setChecked(prefs.getBoolean("push", true));
        swLockScreen.setChecked(prefs.getBoolean("lockscreen", true));
        swMenuUpdates.setChecked(prefs.getBoolean("menu", true));
        swPaymentUpdate.setChecked(prefs.getBoolean("payment", true));
        swOrderUpdate.setChecked(prefs.getBoolean("order", true));
    }

    private void saveNotificationSettings() {
        SharedPreferences prefs = getSharedPreferences("NotifPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean("push", swPush.isChecked());
        editor.putBoolean("lockscreen", swLockScreen.isChecked());
        editor.putBoolean("menu", swMenuUpdates.isChecked());
        editor.putBoolean("payment", swPaymentUpdate.isChecked());
        editor.putBoolean("order", swOrderUpdate.isChecked());

        editor.apply(); // Saves data permanently to the phone
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