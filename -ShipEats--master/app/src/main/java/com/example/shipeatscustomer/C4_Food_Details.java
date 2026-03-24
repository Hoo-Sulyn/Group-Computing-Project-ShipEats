package com.example.shipeatscustomer;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater; // Added
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout; // Added
import android.widget.RatingBar; // Added
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class C4_Food_Details extends AppCompatActivity {

    private String foodId;
    private FoodItem currentFood;
    private int quantity = 1;

    private ImageView foodImage;
    private TextView tvName, tvPrice, tvQuantity, tvDescription, tvCounter;
    private EditText etSpecialInstructions;
    private SwitchMaterial swCutlery;

    private MaterialButton btnAddToCart;

    // NEW VARIABLES FOR REVIEWS
    private LinearLayout reviewsPreviewContainer;
    private TextView tvAvgRating, tvNoReviews;
    private RatingBar detailRatingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_c4_food_details);

        foodId = getIntent().getStringExtra("FOOD_ID");

        // Initialize Views
        foodImage = findViewById(R.id.detail_food_image);
        tvName = findViewById(R.id.food_name);
        tvPrice = findViewById(R.id.food_price);
        tvQuantity = findViewById(R.id.food_quantity);
        tvDescription = findViewById(R.id.food_description);
        tvCounter = findViewById(R.id.food_counter);
        etSpecialInstructions = findViewById(R.id.special_instructions_input);
        swCutlery = findViewById(R.id.want_cutlery);

        // INITIALIZE NEW REVIEW VIEWS
        reviewsPreviewContainer = findViewById(R.id.reviews_preview_container);
        tvAvgRating = findViewById(R.id.tv_detail_avg_rating);
        detailRatingBar = findViewById(R.id.detail_rating_bar);
        tvNoReviews = findViewById(R.id.tv_no_reviews_yet);
        TextView btnSeeAll = findViewById(R.id.btn_see_all_reviews);

        ImageView btnPlus = findViewById(R.id.plus_button);
        ImageView btnMinus = findViewById(R.id.minus_button);
        btnAddToCart = findViewById(R.id.add_to_cart_button);
        ImageView btnClose = findViewById(R.id.close_food_details);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

        // Counter Logic
        if (btnPlus != null) {
            btnPlus.setOnClickListener(v -> {
                if (currentFood != null && quantity < currentFood.getQuantity()) {
                    quantity++;
                    tvCounter.setText(String.valueOf(quantity));
                    // ADD THIS LINE: Tells the buttons to check if they should grey out
                    updateCounterButtons(btnPlus, btnMinus);
                } else if (currentFood != null) {
                    Toast.makeText(this, "Only " + currentFood.getQuantity() + " items available", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnMinus != null) {
            btnMinus.setOnClickListener(v -> {
                if (quantity > 1) {
                    quantity--;
                    tvCounter.setText(String.valueOf(quantity));
                    // ADD THIS LINE: Tells the buttons to check if they should grey out
                    updateCounterButtons(btnPlus, btnMinus);
                }
            });
        }

        updateCounterButtons(btnPlus, btnMinus);

        if (foodId != null) {
            loadFoodDetails();
            loadReviewPreview(); // CALL NEW FUNCTION
        }

        if (swCutlery != null) {
            swCutlery.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    Toast.makeText(this, "Cutlery added", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Cutlery removed", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // SEE ALL REVIEWS BUTTON
        if (btnSeeAll != null) {
            btnSeeAll.setOnClickListener(v -> {
                Intent intent = new Intent(this, Customer_Reviews_Activity.class);
                intent.putExtra("FOOD_ID", foodId);
                intent.putExtra("FOOD_NAME", tvName.getText().toString());
                startActivity(intent);
            });
        }

        if (btnAddToCart != null) {
            btnAddToCart.setOnClickListener(v -> {
                if (currentFood != null) {
                    if (currentFood.getQuantity() <= 0) {
                        Toast.makeText(this, "Sorry, this item is sold out", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String instructions = etSpecialInstructions.getText().toString().trim();
                    boolean wantCutlery = swCutlery.isChecked();

                    CartManager.getInstance().addToCart(currentFood, quantity, instructions, wantCutlery);
                    Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    }

    private void updateCounterButtons(ImageView btnPlus, ImageView btnMinus) {
        if (currentFood == null) return;

        // Minus Button Logic: Disable if quantity is 1
        if (quantity <= 1) {
            btnMinus.setEnabled(false);
            btnMinus.setAlpha(0.3f);
        } else {
            btnMinus.setEnabled(true);
            btnMinus.setAlpha(1.0f);
        }

        // Plus Button Logic: Disable if quantity hits the stock limit
        if (quantity >= currentFood.getQuantity()) {
            btnPlus.setEnabled(false);
            btnPlus.setAlpha(0.3f);
        } else {
            btnPlus.setEnabled(true);
            btnPlus.setAlpha(1.0f);
        }
    }

    private void loadFoodDetails() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("menu_items").child(foodId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentFood = snapshot.getValue(FoodItem.class);
                if (currentFood != null) {
                    currentFood.setId(snapshot.getKey());
                    tvName.setText(currentFood.getName());
                    tvPrice.setText("RM " + String.format("%.2f", currentFood.getPrice()));

                    // UPDATE CALCULATED STARS ON THIS PAGE
                    if (tvAvgRating != null) tvAvgRating.setText(String.format("%.1f", currentFood.getRating()));
                    if (detailRatingBar != null) detailRatingBar.setRating((float) currentFood.getRating());

                    // Show quantity and availability
                    if (tvQuantity != null) {
                        if (currentFood.getQuantity() <= 0) {
                            tvQuantity.setText("Sold Out");
                            tvQuantity.setTextColor(Color.parseColor("#DC2626")); // Red
                            if (btnAddToCart != null) {
                                btnAddToCart.setEnabled(true);
                                btnAddToCart.setText("Out of Stock");
                                btnAddToCart.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                            }
                        } else if (currentFood.getQuantity() <= 3) {
                            tvQuantity.setText("Low Stock: " + currentFood.getQuantity());
                            tvQuantity.setTextColor(Color.parseColor("#F59E0B")); // Orange
                            if (btnAddToCart != null) {
                                btnAddToCart.setEnabled(true);
                                btnAddToCart.setText("Add to Cart");
                                btnAddToCart.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E69303")));
                            }
                        } else {
                            tvQuantity.setText("Quantity: " + currentFood.getQuantity());
                            tvQuantity.setTextColor(Color.parseColor("#16A34A")); // Green
                            if (btnAddToCart != null) {
                                btnAddToCart.setEnabled(true);
                                btnAddToCart.setText("Add to Cart");
                                btnAddToCart.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E69303")));
                            }
                        }
                    }

                    tvDescription.setText(currentFood.getDescription() != null && !currentFood.getDescription().isEmpty() ?
                            currentFood.getDescription() : "No description available.");

                    Glide.with(C4_Food_Details.this)
                            .load(currentFood.getImageUrl())
                            .placeholder(R.drawable.no_image_available)
                            .into(foodImage);

                    ImageView btnPlus = findViewById(R.id.plus_button);
                    ImageView btnMinus = findViewById(R.id.minus_button);
                    updateCounterButtons(btnPlus, btnMinus);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(C4_Food_Details.this, "Error loading details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // NEW FUNCTION TO LOAD RECENT 2 REVIEWS
    private void loadReviewPreview() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("food_reviews").child(foodId);

        // Pull only the last 2 reviews for preview
        ref.limitToLast(2).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (reviewsPreviewContainer != null) {
                    reviewsPreviewContainer.removeAllViews();

                    if (!snapshot.exists()) {
                        if (tvNoReviews != null) tvNoReviews.setVisibility(View.VISIBLE);
                        return;
                    }

                    if (tvNoReviews != null) tvNoReviews.setVisibility(View.GONE);

                    for (DataSnapshot data : snapshot.getChildren()) {
                        Review review = data.getValue(Review.class);
                        if (review != null) {
                            addReviewToPreview(review);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // HELPER TO INJECT REVIEW ROW
    private void addReviewToPreview(Review review) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_review_admin, reviewsPreviewContainer, false);

        TextView name = view.findViewById(R.id.tv_admin_customer_name);
        TextView comment = view.findViewById(R.id.tv_admin_review_comment);
        RatingBar rb = view.findViewById(R.id.admin_review_rating_bar);

        if (name != null) name.setText(review.getCustomerName());
        if (comment != null) comment.setText(review.getComment());
        if (rb != null) rb.setRating(review.getRating());

        reviewsPreviewContainer.addView(view);
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