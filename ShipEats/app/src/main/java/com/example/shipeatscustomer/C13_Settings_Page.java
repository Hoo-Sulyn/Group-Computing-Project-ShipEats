package com.example.shipeatscustomer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;

public class C13_Settings_Page extends AppCompatActivity {

    // UI Elements for the Student ID Card
    private TextView tvCardName, tvCardID, tvCardValidity;
    private ImageView ivCardProfilePicture;

    // Layout and Navigation
    private DrawerLayout drawerLayout;
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

        // 2. Initialize Layout and Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        ImageView closeTabBtn = findViewById(R.id.close_tab_button); // Inside drawer

        // 3. Initialize Footer Navigation (Inside include tag)
        LinearLayout history_nav = findViewById(R.id.history_nav);
        LinearLayout settings_nav = findViewById(R.id.settings_nav);
        LinearLayout menu_nav = findViewById(R.id.menu_nav);

        // 4. Setup Activity Result Launcher (Refreshes ID card after editing profile)
        profileResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Receiving the serializable object from C12_Profile
                        Student updatedStudent = (Student) result.getData().getSerializableExtra("STUDENT_DATA");
                        if (updatedStudent != null) {
                            updateCardUI(updatedStudent);
                        }
                    }
                }
        );

        // 5. Drawer & Footer Click Listeners
        if (closeTabBtn != null) {
            closeTabBtn.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        }

        history_nav.setOnClickListener(v -> startActivity(new Intent(this, C5_History_Page.class)));
        menu_nav.setOnClickListener(v -> startActivity(new Intent(this, C3_Menu_Page.class)));

        // We are already on Settings, so just close the drawer
        settings_nav.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START);
        });

        // 6. Load Initial Data and Setup Menu
        loadSavedProfile();
        setupMenuNavigation();
    }

    //Sets up click listeners for the individual settings rows in the ScrollView
    private void setupMenuNavigation() {
        // General Section
        findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            Intent intent = new Intent(this, C12_Profile.class);
            profileResultLauncher.launch(intent);
        });

        findViewById(R.id.btnOrderHistory).setOnClickListener(v ->
                startActivity(new Intent(this, C5_History_Page.class)));

        findViewById(R.id.btnPaymentMethods).setOnClickListener(v ->
                startActivity(new Intent(this, C11_Payment_Method.class)));

        findViewById(R.id.btnNotifications).setOnClickListener(v ->
                startActivity(new Intent(this, C10_Notifications.class)));

        // About Section
        findViewById(R.id.btnAboutShipEats).setOnClickListener(v ->
                startActivity(new Intent(this, C9_About.class)));

        findViewById(R.id.btnTerms).setOnClickListener(v ->
                startActivity(new Intent(this, C14_Terms_And_Condition.class)));

        // Logout
        findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());
    }

    
      // Loads the profile from SharedPreferences when the activity starts
    private void loadSavedProfile() {
        SharedPreferences sharedPreferences = getSharedPreferences("StudentPrefs", MODE_PRIVATE);

        String name = sharedPreferences.getString("name", "Hoo Suline");
        String id = sharedPreferences.getString("studentID", "050505070505");
        String imageUriString = sharedPreferences.getString("imageUri", "");

        tvCardName.setText(name);
        tvCardID.setText(id);

        if (!imageUriString.isEmpty()) {
            Glide.with(this)
                    .load(Uri.parse(imageUriString))
                    .placeholder(R.drawable.placeholder_user)
                    .circleCrop()
                    .into(ivCardProfilePicture);
        }
    }

    //Updates the UI immediately when data is returned from the Profile screen
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

    //Shows a confirmation dialog before logging out and clearing the task stack
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Intent intent = new Intent(C13_Settings_Page.this, MainActivity.class);
                    // Clear the backstack so the user cannot press back to return to settings
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
