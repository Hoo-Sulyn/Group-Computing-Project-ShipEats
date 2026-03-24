package com.example.shipeatscustomer;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class C7_Shopping_Cart extends AppCompatActivity {

    private LinearLayout cartItemsContainer;
    private TextView subtotalTv, taxTv, feeTv, totalTv, closingTimeTv;
    private MaterialButton continueToPaymentButton;
    private SwitchMaterial preOrderSwitch;
    private EditText pickupTimeInput;

    private double platformFee = 1.00;
    private DatabaseReference adminRef, menuRef;

    // Store hours variables with defaults
    private String sHour = "00", sMin = "00", eHour = "23", eMin = "59";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_c7_shopping_cart);

        // Firebase References
        adminRef = FirebaseDatabase.getInstance().getReference("admin_profile");
        menuRef = FirebaseDatabase.getInstance().getReference("menu_items");

        initViews();
        loadAdminHours();
        loadCartItems();
    }

    private void initViews() {
        cartItemsContainer = findViewById(R.id.shopping_cart_items);
        subtotalTv = findViewById(R.id.subtotal);
        taxTv = findViewById(R.id.service_tax);
        feeTv = findViewById(R.id.platform_fee);
        totalTv = findViewById(R.id.total_price);
        closingTimeTv = findViewById(R.id.closing_time);
        preOrderSwitch = findViewById(R.id.pre_order);
        pickupTimeInput = findViewById(R.id.pickup_time_input);
        continueToPaymentButton = findViewById(R.id.continue_to_payment_button);

        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        // Time Picker Setup
        pickupTimeInput.setFocusable(false);
        pickupTimeInput.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (view, h, m) -> {
                pickupTimeInput.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        });

        // Toggle Pre-Order Section
        preOrderSwitch.setOnCheckedChangeListener((v, isChecked) -> {
            findViewById(R.id.enable_pre_order).setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Checkout Button
        continueToPaymentButton.setOnClickListener(v -> validateOrderAndProceed());
    }

    private void loadAdminHours() {
        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Assuming AdminProfile class has these fields
                    sHour = snapshot.child("startHour").getValue(String.class);
                    sMin = snapshot.child("startMinute").getValue(String.class);
                    eHour = snapshot.child("endHour").getValue(String.class);
                    eMin = snapshot.child("endMinute").getValue(String.class);

                    if (closingTimeTv != null && eHour != null && eMin != null) {
                        closingTimeTv.setText(eHour + ":" + eMin);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void validateOrderAndProceed() {
        List<CartItem> cartItems = CartManager.getInstance().getCartItems();
        if (cartItems.isEmpty()) return;

        // 1. Business Hours Validation
        String pickupTime = "Immediate";
        if (preOrderSwitch.isChecked()) {
            pickupTime = pickupTimeInput.getText().toString().trim();
            if (pickupTime.isEmpty()) {
                Toast.makeText(this, "Please select a pickup time", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isWithinBusinessHours(pickupTime)) {
                Toast.makeText(this, "Store is closed at this time. Hours: " + sHour + ":" + sMin + " - " + eHour + ":" + eMin, Toast.LENGTH_LONG).show();
                return;
            }
        }

        // 2. Real-time Stock Verification
        final String finalPickupTime = pickupTime;
        menuRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (CartItem item : cartItems) {
                    DataSnapshot foodSnap = snapshot.child(item.getFoodItem().getId());
                    Integer currentStock = foodSnap.child("quantity").getValue(Integer.class);

                    if (currentStock != null && currentStock < item.getQuantity()) {
                        Toast.makeText(C7_Shopping_Cart.this, item.getFoodItem().getName() + " is now out of stock!", Toast.LENGTH_LONG).show();
                        loadCartItems(); // Refresh cart UI
                        return;
                    }
                }

                // 3. Success -> Proceed to C15
                Intent intent = new Intent(C7_Shopping_Cart.this, C15_Payment_Process.class);
                intent.putExtra("SUBTOTAL", calculateSubtotal());
                intent.putExtra("TOTAL_AMOUNT", calculateTotalWithExtras());
                intent.putExtra("PICKUP_TIME", finalPickupTime);
                startActivity(intent);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private boolean isWithinBusinessHours(String pickedTime) {
        try {
            String[] parts = pickedTime.split(":");
            int ph = Integer.parseInt(parts[0]);
            int pm = Integer.parseInt(parts[1]);

            int start = Integer.parseInt(sHour) * 60 + Integer.parseInt(sMin);
            int end = Integer.parseInt(eHour) * 60 + Integer.parseInt(eMin);
            int picked = ph * 60 + pm;

            return picked >= start && picked <= end;
        } catch (Exception e) { return false; }
    }

    private void loadCartItems() {
        cartItemsContainer.removeAllViews();
        List<CartItem> items = CartManager.getInstance().getCartItems();

        if (items.isEmpty()) {
            updateTotalsUI();
            toggleEmptyState(true);
            return;
        }

        toggleEmptyState(false);

        for (CartItem item : items) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.shopping_cart_items_layout, cartItemsContainer, false);

            TextView name = itemView.findViewById(R.id.item_name);
            TextView price = itemView.findViewById(R.id.item_price);
            TextView counter = itemView.findViewById(R.id.food_counter);
            TextView stockTv = itemView.findViewById(R.id.stock_quantity);
            ImageView img = itemView.findViewById(R.id.item_image);

            FoodItem food = item.getFoodItem();
            name.setText(food.getName());
            price.setText(String.format(Locale.getDefault(), "RM %.2f", food.getPrice() * item.getQuantity()));
            counter.setText(String.valueOf(item.getQuantity()));
            if (stockTv != null) stockTv.setText("Stock: " + food.getQuantity());

            Glide.with(this).load(food.getImageUrl()).placeholder(R.drawable.no_image_available).into(img);

            // Button Listeners
            itemView.findViewById(R.id.plus_button).setOnClickListener(v -> {
                if (item.getQuantity() < food.getQuantity()) {
                    item.setQuantity(item.getQuantity() + 1);
                    refreshItemUI(item, counter, price);
                } else {
                    Toast.makeText(this, "Only " + food.getQuantity() + " units available in stock", Toast.LENGTH_SHORT).show();
                }
            });

            itemView.findViewById(R.id.minus_button).setOnClickListener(v -> {
                if (item.getQuantity() > 1) {
                    item.setQuantity(item.getQuantity() - 1);
                    refreshItemUI(item, counter, price);
                } else {
                    removeItem(item, itemView);
                }
            });

            itemView.findViewById(R.id.btn_remove).setOnClickListener(v -> removeItem(item, itemView));

            cartItemsContainer.addView(itemView);
        }
        updateTotalsUI();
    }

    private void refreshItemUI(CartItem item, TextView counter, TextView price) {
        counter.setText(String.valueOf(item.getQuantity()));
        price.setText(String.format(Locale.getDefault(), "RM %.2f", item.getFoodItem().getPrice() * item.getQuantity()));
        updateTotalsUI();
    }

    private void removeItem(CartItem item, View itemView) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Remove Item")
                .setMessage("Are you sure you want to remove " + item.getFoodItem().getName() + " from your cart?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    // Actual removal logic
                    CartManager.getInstance().getCartItems().remove(item);
                    cartItemsContainer.removeView(itemView);

                    if (CartManager.getInstance().getCartItems().isEmpty()) {
                        toggleEmptyState(true);
                    }
                    updateTotalsUI();
                    Toast.makeText(this, "Item removed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private double calculateSubtotal() {
        double sub = 0;
        for (CartItem item : CartManager.getInstance().getCartItems()) {
            sub += item.getFoodItem().getPrice() * item.getQuantity();
        }
        return sub;
    }

    private double calculateTotalWithExtras() {
        double sub = calculateSubtotal();
        if (sub == 0) return 0;
        return sub + (sub * 0.06) + platformFee;
    }

    private void updateTotalsUI() {
        double sub = calculateSubtotal();
        double tax = sub * 0.06;
        double total = calculateTotalWithExtras();

        subtotalTv.setText(String.format(Locale.getDefault(), "RM %.2f", sub));
        taxTv.setText(String.format(Locale.getDefault(), "RM %.2f", tax));
        feeTv.setText(String.format(Locale.getDefault(), "RM %.2f", (sub == 0 ? 0 : platformFee)));
        totalTv.setText(String.format(Locale.getDefault(), "RM %.2f", total));
    }

    private void toggleEmptyState(boolean isEmpty) {
        findViewById(R.id.empty_cart_message).setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        findViewById(R.id.pre_order_container).setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        findViewById(R.id.checkout_summary_container).setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        continueToPaymentButton.setEnabled(!isEmpty);
        continueToPaymentButton.setBackgroundTintList(ColorStateList.valueOf(
                isEmpty ? Color.parseColor("#c7c7c7") : Color.parseColor("#E69303")));
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