package com.example.shipeatscustomer;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

public class FoodRatingHelper {

    public static void submitReview(Context context, String foodId, float rating, String comment) {
        // Get Current User ID
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // Reference to the User's name in Firebase
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("userName");
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Get real name or default to "Anonymous" if not found
                String realName = snapshot.exists() ? snapshot.getValue(String.class) : "Anonymous";

                // Proceed to save with the real name
                processRating(context, foodId, rating, comment, realName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                processRating(context, foodId, rating, comment, "Customer");
            }
        });
    }

    private static void processRating(Context context, String foodId, float rating, String comment, String customerName) {
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("food_reviews").child(foodId);
        DatabaseReference menuRef = FirebaseDatabase.getInstance().getReference("menu_items").child(foodId);

        // 1. Save individual review for Admin using the fetched name
        String reviewId = reviewsRef.push().getKey();
        Review review = new Review(customerName, rating, comment, System.currentTimeMillis());
        if (reviewId != null) reviewsRef.child(reviewId).setValue(review);

        // 2. Update Average Rating on Menu using Transaction
        menuRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                FoodItem food = mutableData.getValue(FoodItem.class);
                if (food == null) return Transaction.success(mutableData);

                // Now using the actual fields we added to FoodItem.java
                int currentCount = food.getReviewCount();
                double currentAvg = food.getRating();

                int newCount = currentCount + 1;
                double newAvg = ((currentAvg * currentCount) + rating) / newCount;

                // Update the food object with the new math
                food.setReviewCount(newCount);
                food.setRating(newAvg);

                mutableData.setValue(food);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (committed) {
                    Toast.makeText(context, "Review submitted by " + customerName + "!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}