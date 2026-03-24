package com.example.shipeatscustomer;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class Admin_Reviews_Activity extends AppCompatActivity {

    private String foodId, foodName;
    private RecyclerView recyclerView;
    private AdminReviewAdapter adapter;
    private List<Review> reviewList;
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_admin_reviews);

        // 1. Get data passed from the Menu List
        foodId = getIntent().getStringExtra("FOOD_ID");
        foodName = getIntent().getStringExtra("FOOD_NAME");

        // 2. Initialize Views
        tvTitle = findViewById(R.id.tv_admin_reviews_title);
        if (foodName != null) tvTitle.setText("Reviews for " + foodName);

        ImageView btnBack = findViewById(R.id.btn_back_reviews);
        btnBack.setOnClickListener(v -> finish());

        // 3. Setup RecyclerView
        recyclerView = findViewById(R.id.rv_admin_reviews_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewList = new ArrayList<>();
        adapter = new AdminReviewAdapter(reviewList);
        recyclerView.setAdapter(adapter);

        // 4. Load from Firebase
        if (foodId != null) {
            loadReviews();
        } else {
            Toast.makeText(this, "Food ID not found!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadReviews() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("food_reviews").child(foodId);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reviewList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Review review = data.getValue(Review.class);
                    if (review != null) {
                        reviewList.add(review);
                    }
                }
                adapter.notifyDataSetChanged();

                if (reviewList.isEmpty()) {
                    Toast.makeText(Admin_Reviews_Activity.this, "No reviews yet!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Admin_Reviews_Activity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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