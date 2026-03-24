package com.example.shipeatscustomer;

import android.os.Bundle;
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

public class Customer_Reviews_Activity extends AppCompatActivity {

    private String foodId, foodName;
    private RecyclerView recyclerView;
    private AdminReviewAdapter adapter; // We reuse the adapter we made for Admin
    private List<Review> reviewList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_reviews);

        foodId = getIntent().getStringExtra("FOOD_ID");
        foodName = getIntent().getStringExtra("FOOD_NAME");

        TextView tvTitle = findViewById(R.id.tv_customer_reviews_title);
        if (foodName != null) tvTitle.setText("Reviews: " + foodName);

        ImageView btnBack = findViewById(R.id.btn_back_customer_reviews);
        btnBack.setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.rv_customer_reviews_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewList = new ArrayList<>();
        adapter = new AdminReviewAdapter(reviewList);
        recyclerView.setAdapter(adapter);

        if (foodId != null) {
            loadAllReviews();
        }
    }

    private void loadAllReviews() {
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Customer_Reviews_Activity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}