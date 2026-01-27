package com.example.shipeatscustomer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class C10_Notifications extends AppCompatActivity {

    private SwitchCompat swPush, swBadge, swLockScreen, swMenuUpdates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_c10_notifications);

        // 1. Initialize Views
        swPush = findViewById(R.id.swPush);
        swBadge = findViewById(R.id.swBadge);
        swLockScreen = findViewById(R.id.swLockScreen);
        swMenuUpdates = findViewById(R.id.swMenuUpdates);
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
        swBadge.setChecked(prefs.getBoolean("badge", true));
        swLockScreen.setChecked(prefs.getBoolean("lockscreen", true));
        swMenuUpdates.setChecked(prefs.getBoolean("menu", true));
    }

    private void saveNotificationSettings() {
        SharedPreferences prefs = getSharedPreferences("NotifPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean("push", swPush.isChecked());
        editor.putBoolean("badge", swBadge.isChecked());
        editor.putBoolean("lockscreen", swLockScreen.isChecked());
        editor.putBoolean("menu", swMenuUpdates.isChecked());

        editor.apply(); // Saves data permanently to the phone
    }
}