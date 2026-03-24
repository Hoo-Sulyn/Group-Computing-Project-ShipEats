package com.example.shipeatscustomer;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class C13_Settings_Page extends AppCompatActivity {

    // UI Elements for the Student ID Card
    private TextView tvCardName, tvCardID, tvCardValidity;
    private ImageView ivCardProfilePicture;

    // Layout and Navigation
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_c13_settings_page);

        // 1. Initialize Card Views
        tvCardName = findViewById(R.id.tvStudentName);
        tvCardID = findViewById(R.id.tvStudentID);
        tvCardValidity = findViewById(R.id.tvValidity);
        ivCardProfilePicture = findViewById(R.id.ivProfilePicture);

        // 2. Initialize Layout and Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        setupNavigationDrawer(); // Handles the close button inside the header

        // 3. Initialize Footer Navigation
        setupFooterNavigation();

        // 4. Load Real-time Profile Data
        loadUserAccountData();

        // 5. Setup Settings Menu List
        setupMenuNavigation();
    }

    private void setupNavigationDrawer() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            // Access the header to find the close button
            View headerView = navigationView.getHeaderView(0);
            ImageView closeTabBtn = headerView.findViewById(R.id.close_tab_button);

            if (closeTabBtn != null) {
                closeTabBtn.setOnClickListener(v -> {
                    if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                });
            }
        }
    }

    private void setupFooterNavigation() {
        LinearLayout history_nav = findViewById(R.id.history_nav);
        LinearLayout settings_nav = findViewById(R.id.settings_nav);
        LinearLayout menu_nav = findViewById(R.id.menu_nav);
        ImageView settings_icon = findViewById(R.id.settings_icon);
        TextView settings_text = findViewById(R.id.settings_text);

        if (history_nav != null) history_nav.setOnClickListener(v -> startActivity(new Intent(this, C5_History_Page.class)));
        if (menu_nav != null) menu_nav.setOnClickListener(v -> startActivity(new Intent(this, C3_Menu_Page.class)));

        if (settings_icon != null && settings_text != null) {
            int activeColor = Color.parseColor("#FFD700");
            settings_icon.setColorFilter(activeColor);
            settings_text.setTextColor(activeColor);
            settings_text.setTypeface(null, Typeface.BOLD);
        }

        // Already on settings, just close drawer if open
        if (settings_nav != null) {
            settings_nav.setOnClickListener(v -> {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
            });
        }
    }

    private void loadUserAccountData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Link directly to the specific logged-in user
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String imgPath = snapshot.child("profileImage").getValue(String.class);

                    // 1. Set Name
                    if (tvCardName != null) tvCardName.setText(name != null ? name : "Student Name");

                    // 2. Extract Student ID and Detect Program
                    if (email != null && email.contains("@")) {
                        String studentID = email.split("@")[0].toUpperCase();

                        // Set the ID text (e.g., BSSE123456)
                        if (tvCardID != null) tvCardID.setText(studentID);

                        // Set the Validity/Program text based on the prefix
                        if (tvCardValidity != null) {
                            if (studentID.startsWith("BSSE")) {
                                tvCardValidity.setText("BSc (HONS) Computer Science (Software Engineering)");
                            } else if (studentID.startsWith("BSCS")) {
                                tvCardValidity.setText("BSc (HONS) Computer Science (Cybersecurity)");
                            } else {
                                tvCardValidity.setText("BSc (HONS) Computer Science");
                            }
                        }
                    }

                    // --- UPDATED IMAGE LOADING LOGIC ---
                    if (imgPath != null && !imgPath.isEmpty() && !isDestroyed()) {
                        java.io.File imgFile = new java.io.File(imgPath);

                        // Check if the path is a local file (internal storage)
                        if (imgFile.exists()) {
                            Glide.with(C13_Settings_Page.this)
                                    .load(imgFile) // Load from Local Storage
                                    .circleCrop()
                                    .placeholder(R.drawable.placeholder_user)
                                    .into(ivCardProfilePicture);
                        } else {
                            // If it's not a local file, try to load it as a URL (for backwards compatibility)
                            Glide.with(C13_Settings_Page.this)
                                    .load(imgPath)
                                    .circleCrop()
                                    .placeholder(R.drawable.placeholder_user)
                                    .into(ivCardProfilePicture);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Settings", "Database error: " + error.getMessage());
            }
        });
    }

    private void setupMenuNavigation() {
        // Edit Profile
        View btnEditProfile = findViewById(R.id.btnEditProfile);
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> startActivity(new Intent(this, C12_Profile.class)));
        }

        // Order History
        View btnOrderHistory = findViewById(R.id.btnOrderHistory);
        if (btnOrderHistory != null) {
            btnOrderHistory.setOnClickListener(v -> startActivity(new Intent(this, C5_History_Page.class)));
        }

        // Payment Methods
        View btnPaymentMethods = findViewById(R.id.btnPaymentMethods);
        if (btnPaymentMethods != null) {
            btnPaymentMethods.setOnClickListener(v -> startActivity(new Intent(this, C11_Payment_Method.class)));
        }

        // Notifications
        View btnNotifications = findViewById(R.id.btnNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> startActivity(new Intent(this, C10_Notifications.class)));
        }

        // About Ship Eats
        View btnAbout = findViewById(R.id.btnAboutShipEats);
        if (btnAbout != null) {
            btnAbout.setOnClickListener(v -> startActivity(new Intent(this, C9_About.class)));
        }

        // Terms and Conditions
        View btnTerms = findViewById(R.id.btnTerms);
        if (btnTerms != null) {
            btnTerms.setOnClickListener(v -> startActivity(new Intent(this, C14_Terms_And_Condition.class)));
        }

        // Logout
        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutDialog());
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut(); // End session
                    Intent intent = new Intent(C13_Settings_Page.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
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