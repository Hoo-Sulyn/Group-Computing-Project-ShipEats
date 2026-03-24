package com.example.shipeatscustomer;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class C2_Create_Account extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private TextInputEditText fullNameEditText, emailEditText, dobEditText, phoneEditText, passwordEditText, reenterPasswordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_c2_create_account);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        fullNameEditText = findViewById(R.id.full_name);
        emailEditText = findViewById(R.id.email_address);
        dobEditText = findViewById(R.id.date_of_birth);
        phoneEditText = findViewById(R.id.phone);
        passwordEditText = findViewById(R.id.new_password);
        reenterPasswordEditText = findViewById(R.id.reenter_password);

        dobEditText.setOnClickListener(v -> showDatePicker());
        findViewById(R.id.create_account_button).setOnClickListener(v -> createAccount());
        findViewById(R.id.login_option).setOnClickListener(v -> finish());
    }

    private void createAccount() {
        String fullName = fullNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String dob = dobEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String reenter = reenterPasswordEditText.getText().toString().trim();

        // 1. Basic Validation
        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. University Regex Check (STILL ACTIVE)
        if (!isValidUniversityEmail(email)) {
            emailEditText.setError("Email must be your provided student E-mail!");
            Toast.makeText(this, "Registration restricted to Peninsula College students.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!password.equals(reenter)) {
            reenterPasswordEditText.setError("Passwords do not match");
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return;
        }

        if (!isOldEnough(dob)) {
            dobEditText.setError("You must be at least 16 years old to register.");
            Toast.makeText(this, "Registration restricted to 16+ years old.", Toast.LENGTH_LONG).show();
            return;
        }

        // 3. Create User in Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    String userId = user.getUid();
                    DatabaseReference userRef = mDatabase.child("Users").child(userId);

                    // Prepare User Profile Data
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("fullName", fullName);
                    userData.put("email", email);
                    userData.put("dob", dob);
                    userData.put("phone", phone);
                    userData.put("role", "Guest");

                    // 4. Save to Realtime Database
                    userRef.setValue(userData).addOnCompleteListener(dbTask -> {
                        if (dbTask.isSuccessful()) {
                            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                            mAuth.signOut(); // Logs them out so they have to sign in properly
                            finish(); // Returns to Login screen
                        } else {
                            Toast.makeText(this, "Database Error: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Toast.makeText(this, "Registration Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isValidUniversityEmail(String email) {
        String universityPattern = "^(?i)(BSSE|BSCS).*@peninsulamalaysia\\.edu\\.my$";
        return email != null && email.matches(universityPattern);
    }

    private void showDatePicker() {
        // 1. Calculate the constraint (today - 16 years)
        Calendar constraintsCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        constraintsCalendar.add(Calendar.YEAR, -16); // Move back 16 years
        long sixteenYearsAgo = constraintsCalendar.getTimeInMillis();

        com.google.android.material.datepicker.CalendarConstraints constraints =
                new com.google.android.material.datepicker.CalendarConstraints.Builder()
                        .setEnd(sixteenYearsAgo) // User cannot select dates after this
                        .setValidator(com.google.android.material.datepicker.DateValidatorPointBackward.before(sixteenYearsAgo))
                        .build();

        // 2. Build the Picker with Constraints
        com.google.android.material.datepicker.MaterialDatePicker<Long> datePicker =
                com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select Date of Birth")
                        .setCalendarConstraints(constraints) // Apply the 16+ limit
                        .setTheme(R.style.CustomDatePickerTheme)
                        .setSelection(sixteenYearsAgo) // Start the calendar at the limit
                        .build();

        datePicker.show(getSupportFragmentManager(), "DOB_PICKER");

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(selection);
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            dobEditText.setText(format.format(calendar.getTime()));
        });
    }

    private boolean isOldEnough(String dob) {
        if (TextUtils.isEmpty(dob)) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar birthDate = Calendar.getInstance();
            birthDate.setTime(sdf.parse(dob));

            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR);

            // Adjust if birthday hasn't happened yet this year
            if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }

            return age >= 16;
        } catch (Exception e) {
            return false;
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