package com.example.shipeatscustomer;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class C13_Settings_Page extends AppCompatActivity {

    // ID Card UI Elements
    private TextView tvCardName, tvCardID, tvCardValidity;
    private ImageView ivCardProfilePicture;

    // Launcher to receive updated data back from the Profile Activity
    private ActivityResultLauncher<Intent> profileResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_c13_settings_page);

        // 1. Initialize Card Views
        tvCardName = findViewById(R.id.tvStudentName);
        tvCardID = findViewById(R.id.tvStudentID);
        tvCardValidity = findViewById(R.id.tvValidity);
        ivCardProfilePicture = findViewById(R.id.ivProfilePicture);

        // 2. Load saved data from disk immediately on startup
        loadSavedProfile();

        // 3. Initialize the Launcher to refresh the card if profile is edited
        profileResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Student updatedStudent = (Student) result.getData().getSerializableExtra("STUDENT_DATA");
                        if (updatedStudent != null) {
                            updateCardUI(updatedStudent);
                        }
                    }
                }
        );

        // 4. Set up Click Listeners for all menu rows
        setupMenuNavigation();
    }

    private void setupMenuNavigation() {
        // Edit Profile
        findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            Intent intent = new Intent(this, C12_Profile.class);
            profileResultLauncher.launch(intent);
        });

        // Notifications
        findViewById(R.id.btnNotifications).setOnClickListener(v -> {
            startActivity(new Intent(this, C10_Notifications.class));
        });

        // Payment Methods
        findViewById(R.id.btnPaymentMethods).setOnClickListener(v -> {
            startActivity(new Intent(this, C11_Payment_Method.class));
        });

        // About ShipEats
        findViewById(R.id.btnAboutShipEats).setOnClickListener(v -> {
            startActivity(new Intent(this, C9_About.class));
        });

        // Terms and Conditions
        findViewById(R.id.btnTerms).setOnClickListener(v -> {
            startActivity(new Intent(this, C14_Terms_And_Condition.class));
        });

        // Order History
        findViewById(R.id.btnOrderHistory).setOnClickListener(v -> {
            startActivity(new Intent(this, C5_History_Page.class));
        });

        // Log Out with Confirmation Dialog
        findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());
    }

    private void loadSavedProfile() {
        SharedPreferences sharedPreferences = getSharedPreferences("StudentPrefs", MODE_PRIVATE);

        // Retrieve values (using default "Hoo Suline" if none found)
        String name = sharedPreferences.getString("name", "Hoo Suline");
        String imageUriString = sharedPreferences.getString("imageUri", "");

        tvCardName.setText(name);

        if (!imageUriString.isEmpty()) {
            Glide.with(this)
                    .load(Uri.parse(imageUriString))
                    .placeholder(R.drawable.placeholder_user)
                    .circleCrop()
                    .into(ivCardProfilePicture);
        }
    }

    private void updateCardUI(Student student) {
        tvCardName.setText(student.getName());

        if (student.getProfileImageUri() != null && !student.getProfileImageUri().isEmpty()) {
            Glide.with(this)
                    .load(Uri.parse(student.getProfileImageUri()))
                    .placeholder(R.drawable.placeholder_user)
                    .circleCrop()
                    .into(ivCardProfilePicture);
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {

                    Intent intent = new Intent(C13_Settings_Page.this, MainActivity.class);

                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    startActivity(intent);

                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }
}