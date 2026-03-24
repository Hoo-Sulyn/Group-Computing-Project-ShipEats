package com.example.shipeatscustomer;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class C12_Profile extends AppCompatActivity {

    private String currentImageUri = "";
    private ActivityResultLauncher<String> galleryLauncher;
    private ImageView ivProfileLarge;
    private TextView tvProfileName;
    private EditText etPhone, etDOB, etEmail;
    private AutoCompleteTextView genderBox;

    private DatabaseReference userRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_c12_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        // Standardized path for user data
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());

        // --- 1. INITIALIZE VIEWS ---
        ivProfileLarge = findViewById(R.id.ivProfileLarge);
        tvProfileName = findViewById(R.id.tvProfileName);
        genderBox = findViewById(R.id.genderAutoComplete);
        etDOB = findViewById(R.id.etDOB);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        Button btnSaveProfile = findViewById(R.id.btnSaveProfile);
        ImageView ivBack = findViewById(R.id.ivBack);
        FloatingActionButton btnCamera = findViewById(R.id.btnCamera);

        // --- 2. BACK BUTTON LOGIC ---
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        // --- 3. LOAD DATA FROM FIREBASE ---
        loadUserDataFromFirebase();

        // --- 4. GALLERY PICKER (LOCAL STORAGE LOGIC) ---
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        String localPath = saveImageToInternalStorage(uri);
                        if (localPath != null) {
                            currentImageUri = localPath;

                            // Load the local file into the circular preview
                            Glide.with(this)
                                    .load(new File(localPath))
                                    .circleCrop()
                                    .into(ivProfileLarge);

                            // Save the local path string to Firebase Database
                            userRef.child("profileImage").setValue(localPath);
                            Toast.makeText(this, "Profile Photo!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        btnCamera.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        // --- 5. GENDER SELECTION ---
        String[] genderOptions = {"Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, genderOptions);
        genderBox.setAdapter(adapter);

        // --- 6. DATE PICKER ---
        etDOB.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                String date = String.format("%02d/%02d/%d", day, (month + 1), year);
                etDOB.setText(date);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // --- 7. SAVE BUTTON ---
        btnSaveProfile.setOnClickListener(v -> saveProfileData());
    }

    private String saveImageToInternalStorage(Uri uri) {
        try {
            // Create private app folder "profile_pics"
            File directory = getDir("profile_pics", MODE_PRIVATE);

            String timeStamp = String.valueOf(System.currentTimeMillis());
            String fileName = mAuth.getCurrentUser().getUid() + "_" + timeStamp + ".jpg";

            File myPath = new File(directory, fileName);

            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(myPath);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            return myPath.getAbsolutePath();
        } catch (Exception e) {
            Log.e("LOCAL_STORAGE", "Error: " + e.getMessage());
            return null;
        }
    }

    private void loadUserDataFromFirebase() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String dob = snapshot.child("dob").getValue(String.class);
                    String gender = snapshot.child("gender").getValue(String.class);
                    String imgPath = snapshot.child("profileImage").getValue(String.class);

                    if (name != null) tvProfileName.setText(name);
                    if (email != null) {
                        etEmail.setText(email);
                        etEmail.setEnabled(false); // Security: Don't allow email editing here
                    }
                    if (phone != null) etPhone.setText(phone);
                    if (dob != null) etDOB.setText(dob);
                    if (gender != null) genderBox.setText(gender, false);

                    if (imgPath != null && !imgPath.isEmpty()) {
                        currentImageUri = imgPath;
                        File imgFile = new File(imgPath);
                        if (imgFile.exists()) {
                            Glide.with(C12_Profile.this)
                                    .load(imgFile)
                                    .circleCrop()
                                    .into(ivProfileLarge);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveProfileData() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("gender", genderBox.getText().toString());
        updates.put("dob", etDOB.getText().toString());
        updates.put("phone", etPhone.getText().toString());

        if (!currentImageUri.isEmpty()) {
            updates.put("profileImage", currentImageUri);
        }

        userRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Profile Saved", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
            }
        });
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