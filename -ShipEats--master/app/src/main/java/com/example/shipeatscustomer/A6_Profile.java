package com.example.shipeatscustomer;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class A6_Profile extends AppCompatActivity {

    private LinearLayout layoutViewMode, layoutEditMode;
    private FloatingActionButton btnEditAvatar;
    private ImageView ivProfileImage;
    private TextView tvName, tvEmail, tvPhone, tvLocation, tvCurrentHours, tvHeaderName, tvHeaderEmail;
    private EditText etName, etEmail, etPhone, etLocation;

    private DatabaseReference adminRef;
    private Uri selectedImageUri = null;
    private String currentImageUrl = "";
    private String sHour = "14", sMin = "30", eHour = "16", eMin = "30";

    // Launcher for image selection from gallery
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    // Preview the image immediately
                    Glide.with(this).load(selectedImageUri).circleCrop().into(ivProfileImage);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_a6_profile);

        adminRef = FirebaseDatabase.getInstance().getReference("admin_profile");

        initViews();
        loadProfileData();

        btnEditAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        findViewById(R.id.btn_change_hours).setOnClickListener(v -> showChangeHoursDialog());
        findViewById(R.id.card_logout).setOnClickListener(v -> showLogoutDialog());
        findViewById(R.id.btn_edit_profile).setOnClickListener(v -> switchToEditMode());
        findViewById(R.id.btn_cancel).setOnClickListener(v -> switchToViewMode());
        findViewById(R.id.btn_save).setOnClickListener(v -> saveProfileDirectly());

        setupBottomNav();
        setupHeader();

        // Highlight the Menu tab
        ImageView menuIcon = findViewById(R.id.profile_icon);
        TextView menuText = findViewById(R.id.profile_text);
        highlightCurrentTab(menuIcon, menuText);
    }

    private void setupHeader() {
        View notifBtn = findViewById(R.id.admin_notification_btn);
        if (notifBtn != null) {
            notifBtn.setOnClickListener(v -> AdminDialogHelper.showNotificationsDialog(this));
        }
    }

    private void initViews() {
        layoutViewMode = findViewById(R.id.layout_view_mode);
        layoutEditMode = findViewById(R.id.layout_edit_mode);
        btnEditAvatar = findViewById(R.id.btn_edit_avatar);
        ivProfileImage = findViewById(R.id.iv_profile_image);

        tvName = findViewById(R.id.tv_name);
        tvEmail = findViewById(R.id.tv_email);
        tvPhone = findViewById(R.id.tv_phone);
        tvLocation = findViewById(R.id.tv_location);
        tvCurrentHours = findViewById(R.id.tv_current_hours);
        tvHeaderName = findViewById(R.id.tv_header_name);
        tvHeaderEmail = findViewById(R.id.tv_header_email);

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etLocation = findViewById(R.id.et_location);

        btnEditAvatar.setVisibility(View.GONE);
    }

    private void loadProfileData() {
        adminRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    AdminProfile profile = snapshot.getValue(AdminProfile.class);
                    if (profile != null) {
                        updateUI(profile);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(A6_Profile.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(AdminProfile profile) {
        tvName.setText(profile.name);
        tvEmail.setText(profile.email);
        tvPhone.setText(profile.phone);
        tvLocation.setText(profile.location);
        tvHeaderName.setText(profile.name);
        tvHeaderEmail.setText(profile.email);

        etName.setText(profile.name);
        etEmail.setText(profile.email);
        etPhone.setText(profile.phone);
        etLocation.setText(profile.location);

        sHour = profile.startHour; sMin = profile.startMinute;
        eHour = profile.endHour; eMin = profile.endMinute;
        tvCurrentHours.setText("Orders accepted from " + sHour + ":" + sMin + " to " + eHour + ":" + eMin);

        currentImageUrl = profile.imageUrl;
        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
            Glide.with(this).load(currentImageUrl).circleCrop().into(ivProfileImage);
        }
    }

    private void saveProfileDirectly() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        // If user picked a new image, use that URI string. Otherwise, keep the old one.
        String imageUrlToSave = (selectedImageUri != null) ? selectedImageUri.toString() : currentImageUrl;

        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        AdminProfile updatedProfile = new AdminProfile(
                name, email, phone, location, imageUrlToSave,
                sHour, sMin, eHour, eMin
        );

        adminRef.setValue(updatedProfile).addOnSuccessListener(aVoid -> {
            switchToViewMode();
            showStatusPopup("Profile saved successfully");
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Permission Denied: Check Firebase Rules", Toast.LENGTH_LONG).show();
        });
    }

    private void showChangeHoursDialog() {
        Dialog dialog = new Dialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.admin_dialog_change_hours, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = android.view.Gravity.CENTER;
            dialog.getWindow().setAttributes(lp);
        }

        EditText etStartTime = view.findViewById(R.id.et_start_time);
        EditText etEndTime = view.findViewById(R.id.et_end_time);
        etStartTime.setText(sHour + ":" + sMin);
        etEndTime.setText(eHour + ":" + eMin);

        etStartTime.setOnClickListener(v -> new TimePickerDialog(this, (view1, h, m) -> {
            sHour = String.format("%02d", h);
            sMin = String.format("%02d", m);
            etStartTime.setText(sHour + ":" + sMin);
        }, Integer.parseInt(sHour), Integer.parseInt(sMin), true).show());

        etEndTime.setOnClickListener(v -> new TimePickerDialog(this, (view12, h, m) -> {
            eHour = String.format("%02d", h);
            eMin = String.format("%02d", m);
            etEndTime.setText(eHour + ":" + eMin);
        }, Integer.parseInt(eHour), Integer.parseInt(eMin), true).show());

        view.findViewById(R.id.btn_cancel_hours).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_save_hours).setOnClickListener(v -> {
            adminRef.child("startHour").setValue(sHour);
            adminRef.child("startMinute").setValue(sMin);
            adminRef.child("endHour").setValue(eHour);
            adminRef.child("endMinute").setValue(eMin).addOnSuccessListener(aVoid -> {
                dialog.dismiss();
                showStatusPopup("Operating hours updated successfully");
            });
        });
        dialog.show();
    }

    private void showStatusPopup(String message) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.admin_dialog_success);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Set custom message
        TextView tvMsg = dialog.findViewById(R.id.tv_success_message);
        if (tvMsg != null) tvMsg.setText(message);

        dialog.show();
        new Handler(Looper.getMainLooper()).postDelayed(dialog::dismiss, 1500);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Intent intent = new Intent(this, A1_Login_Page.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void switchToEditMode() {
        layoutViewMode.setVisibility(View.GONE);
        layoutEditMode.setVisibility(View.VISIBLE);
        btnEditAvatar.setVisibility(View.VISIBLE);
    }

    private void switchToViewMode() {
        layoutViewMode.setVisibility(View.VISIBLE);
        layoutEditMode.setVisibility(View.GONE);
        btnEditAvatar.setVisibility(View.GONE);
    }

    private void highlightCurrentTab(ImageView icon, TextView text) {
        if (icon != null && text != null) {
            int activeColor = Color.parseColor("#FFD700");
            icon.setColorFilter(activeColor);
            text.setTextColor(activeColor);
            text.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

    private void setupBottomNav() {
        View footer = findViewById(R.id.footer_section);
        if (footer != null) {
            footer.findViewById(R.id.dashboard_nav).setOnClickListener(v -> startActivity(new Intent(this, A2_Dashboard.class)));
            footer.findViewById(R.id.inventory_nav).setOnClickListener(v -> startActivity(new Intent(this, A3_Inventory_Management.class)));
            footer.findViewById(R.id.orders_nav).setOnClickListener(v -> startActivity(new Intent(this, A4_CustomerOrderActivity.class)));
            footer.findViewById(R.id.menu_nav).setOnClickListener(v -> startActivity(new Intent(this, A5_MenuManagementActivity.class)));
            footer.findViewById(R.id.profile_nav).setOnClickListener(v -> Toast.makeText(this, "Already on Profile", Toast.LENGTH_SHORT).show());
        }
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