package com.example.shipeatscustomer;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;

public class C12_Profile extends AppCompatActivity {

    private String currentImageUri = "";
    private ActivityResultLauncher<String> galleryLauncher;
    private ImageView ivProfileLarge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_c12_profile);

        // --- 1. INITIALIZE VIEWS ---
        ivProfileLarge = findViewById(R.id.ivProfileLarge);
        FloatingActionButton btnCamera = findViewById(R.id.btnCamera);
        AutoCompleteTextView genderBox = findViewById(R.id.genderAutoComplete);
        EditText etDOB = findViewById(R.id.etDOB);
        EditText etPhone = findViewById(R.id.etPhone);
        EditText etEmail = findViewById(R.id.etEmail);
        TextView tvProfileName = findViewById(R.id.tvProfileName);
        Button btnSaveProfile = findViewById(R.id.btnSaveProfile);

        // --- 2. LOAD SAVED DATA (If any exists) ---
        loadExistingData(etPhone, etDOB, genderBox);

        // --- 3. GALLERY PICKER LOGIC ---
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        currentImageUri = uri.toString();
                        // Use Glide to update the large profile circle immediately
                        Glide.with(this).load(uri).circleCrop().into(ivProfileLarge);
                    }
                });

        btnCamera.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        // --- 4. GENDER SELECTION LOGIC ---
        String[] genderOptions = {"Male", "Female", "Prefer Not to Say"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, genderOptions);
        genderBox.setAdapter(adapter);

        // --- 5. DATE OF BIRTH PICKER LOGIC ---
        etDOB.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, day) -> {
                        String date = String.format("%02d/%02d/%d", day, (month + 1), year);
                        etDOB.setText(date);
                    },
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        // --- 6. SAVE BUTTON LOGIC ---
        btnSaveProfile.setOnClickListener(v -> {
            String name = tvProfileName.getText().toString();
            String gender = genderBox.getText().toString();
            String dob = etDOB.getText().toString();
            String phone = etPhone.getText().toString();
            String email = etEmail.getText().toString();

            // Save to SharedPreferences for persistence
            saveToPreferences(name, gender, dob, phone, email, currentImageUri);

            // Return object to Settings screen
            Student updatedStudent = new Student(name, gender, dob, phone, email, currentImageUri);
            Intent resultIntent = new Intent();
            resultIntent.putExtra("STUDENT_DATA", updatedStudent);
            setResult(RESULT_OK, resultIntent);

            Toast.makeText(this, "Profile Saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void saveToPreferences(String name, String gender, String dob, String phone, String email, String uri) {
        SharedPreferences.Editor editor = getSharedPreferences("StudentPrefs", MODE_PRIVATE).edit();
        editor.putString("name", name);
        editor.putString("gender", gender);
        editor.putString("dob", dob);
        editor.putString("phone", phone);
        editor.putString("email", email);
        if(!uri.isEmpty()) editor.putString("imageUri", uri);
        editor.apply();
    }

    private void loadExistingData(EditText phone, EditText dob, AutoCompleteTextView gender) {
        SharedPreferences prefs = getSharedPreferences("StudentPrefs", MODE_PRIVATE);
        phone.setText(prefs.getString("phone", ""));
        dob.setText(prefs.getString("dob", ""));
        gender.setText(prefs.getString("gender", ""), false);

        String savedUri = prefs.getString("imageUri", "");
        if (!savedUri.isEmpty()) {
            currentImageUri = savedUri;
            Glide.with(this).load(Uri.parse(savedUri)).circleCrop().into(ivProfileLarge);
        }
    }
}