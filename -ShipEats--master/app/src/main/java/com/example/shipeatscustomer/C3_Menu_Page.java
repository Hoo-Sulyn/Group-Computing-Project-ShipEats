package com.example.shipeatscustomer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class C3_Menu_Page extends AppCompatActivity {

    private LinearLayout foodCardContainer;
    private TextView noMenuText;
    private DatabaseReference menuRef = FirebaseDatabase.getInstance().getReference("menu_items");
    private List<FoodItem> fullMenuList = new ArrayList<>();
    private DrawerLayout drawerLayout;

    // AI Views & Preference Data
    private RecyclerView rvTrending;
    private TrendingAdapter trendingAdapter;
    private List<FoodItem> trendingList = new ArrayList<>();
    private String favoriteCategory = "";
    private boolean preferencesLoaded = false;

    // AI Insight Views
    private MaterialCardView aiInsightCard;
    private TextView aiInsightText;

    // Filter Views
    private CheckBox cbQty, cbPricing, cbMain, cbSides, cbDrinks, cbDessert;
    private EditText etMinPrice, etMaxPrice;
    private String currentQuery = "";

    private View notificationBadge;
    private boolean isInitialLoadFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_c3_menu_page);

        // 1. View Binding
        foodCardContainer = findViewById(R.id.food_card_container);
        noMenuText = findViewById(R.id.no_menu_text);
        drawerLayout = findViewById(R.id.drawer_layout);
        rvTrending = findViewById(R.id.rv_trending);
        aiInsightCard = findViewById(R.id.ai_insight_card);
        aiInsightText = findViewById(R.id.ai_insight_text);
        notificationBadge = findViewById(R.id.notification_badge);

        // 2. Setup RecyclerView (Horizontal for Trending)
        rvTrending.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        trendingAdapter = new TrendingAdapter(trendingList);
        rvTrending.setAdapter(trendingAdapter);

        // 3. Setup Navigation & Search
        setupNavigation();
        setupSearchAndFilter();

        // 4. Load user preferences first, then menu
        loadUserPreferencesAndMenu();

        setupCustomerNotificationListener();
    }

    private void setupNavigation() {
        LinearLayout history_nav = findViewById(R.id.history_nav);
        LinearLayout settings_nav = findViewById(R.id.settings_nav);
        ImageView cart_icon = findViewById(R.id.shopping_cart_icon);
        LinearLayout filter_icon = findViewById(R.id.filter_icon);
        ImageView notification_icon = findViewById(R.id.notification_icon);
        ImageView menu_icon = findViewById(R.id.menu_icon);
        TextView menu_text = findViewById(R.id.menu_text);

        if (history_nav != null) history_nav.setOnClickListener(v -> startActivity(new Intent(this, C5_History_Page.class)));
        if (settings_nav != null) settings_nav.setOnClickListener(v -> startActivity(new Intent(this, C13_Settings_Page.class)));
        if (cart_icon != null) cart_icon.setOnClickListener(v -> startActivity(new Intent(this, C7_Shopping_Cart.class)));
        if (filter_icon != null) filter_icon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        if (notification_icon != null) {
            notification_icon.setOnClickListener(v -> {
                // Hide the red dot because the user is viewing notifications
                notificationBadge.setVisibility(View.GONE);
                AdminDialogHelper.showNotificationsDialog(this);
            });
        }

        if (menu_icon != null && menu_text != null) {
            int activeColor = Color.parseColor("#FFD700");
            menu_icon.setColorFilter(activeColor);
            menu_text.setTextColor(activeColor);
            menu_text.setTypeface(null, Typeface.BOLD);
        }
    }

    private void setupSearchAndFilter() {
        final EditText etSearch = findViewById(R.id.et_search_input);

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentQuery = s.toString();
                    applyFiltersAndDisplay();
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            etSearch.setOnEditorActionListener((v, actionId, event) -> {
                hideKeyboard(v);
                return true;
            });
        }
        setupFilterDrawer();
    }

    private void hideKeyboard(View view) {
        view.clearFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void loadUserPreferencesAndMenu() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            loadMenuItems();
            return;
        }

        DatabaseReference prefRef = FirebaseDatabase.getInstance().getReference("user_preferences").child(userId);
        prefRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long maxClicks = -1;
                favoriteCategory = "";
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Long clicks = ds.getValue(Long.class);
                    if (clicks != null && clicks > maxClicks) {
                        maxClicks = clicks;
                        favoriteCategory = ds.getKey();
                    }
                }
                preferencesLoaded = true;
                loadMenuItems();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                preferencesLoaded = true;
                loadMenuItems();
            }
        });
    }

    private void loadMenuItems() {
        menuRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<FoodItem> availableItems = new ArrayList<>();
                List<FoodItem> soldOutItems = new ArrayList<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    FoodItem item = ds.getValue(FoodItem.class);
                    if (item != null && item.isPublished()) {
                        item.setId(ds.getKey());

                        // Separate them immediately
                        if (item.getQuantity() > 0) {
                            availableItems.add(item);
                        } else {
                            soldOutItems.add(item);
                        }
                    }
                }

                // Combine: Available first, then Sold Out
                fullMenuList.clear();
                fullMenuList.addAll(availableItems);
                fullMenuList.addAll(soldOutItems);

                applyFiltersAndDisplay();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(C3_Menu_Page.this, "Failed to load menu", Toast.LENGTH_SHORT).show();
            }
        });

        loadTrendingItems();
    }

    private void loadTrendingItems() {
        Query trendingQuery = menuRef.orderByChild("orderCount").limitToLast(5);
        trendingQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                trendingList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    FoodItem item = ds.getValue(FoodItem.class);
                    if (item != null && item.isPublished()) {
                        item.setId(ds.getKey());
                        trendingList.add(0, item);
                    }
                }
                trendingAdapter.notifyDataSetChanged();

                if (!trendingList.isEmpty()) {
                    String topDish = trendingList.get(0).getName();
                    if (aiInsightCard != null && aiInsightText != null) {
                        aiInsightCard.setVisibility(View.VISIBLE);
                        aiInsightText.setText("✨ Everyone is loving " + topDish + " today! It's our #1 top-selling dish.");
                    }
                } else {
                    if (aiInsightCard != null) aiInsightCard.setVisibility(View.GONE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void applyFiltersAndDisplay() {
        List<FoodItem> filtered = new ArrayList<>();
        String lowerQuery = currentQuery.toLowerCase().trim();

        double minPrice = 0;
        double maxPrice = Double.MAX_VALUE;
        try {
            if (etMinPrice != null && !etMinPrice.getText().toString().isEmpty()) {
                minPrice = Double.parseDouble(etMinPrice.getText().toString());
            }
            if (etMaxPrice != null && !etMaxPrice.getText().toString().isEmpty()) {
                maxPrice = Double.parseDouble(etMaxPrice.getText().toString());
            }
        } catch (NumberFormatException e) {
            etMaxPrice.setError("Please enter numerical digits only");
            return;
        }

        for (FoodItem item : fullMenuList) {
            boolean matchesSearch = TextUtils.isEmpty(lowerQuery) ||
                    (item.getName() != null && item.getName().toLowerCase().contains(lowerQuery)) ||
                    (item.getIngredients() != null && item.getIngredients().toLowerCase().contains(lowerQuery));

            boolean matchesQty = true;
            if (cbQty != null && cbQty.isChecked()) {
                matchesQty = item.getQuantity() > 0;
            }

            boolean matchesPrice = true;
            if (cbPricing != null && cbPricing.isChecked()) {
                matchesPrice = item.getPrice() >= minPrice && item.getPrice() <= maxPrice;
            }

            if (matchesSearch && matchesQty && matchesPrice) {
                filtered.add(item);
            }
        }

        boolean anyCategoryChecked = cbMain != null && (cbMain.isChecked() || cbSides.isChecked() || cbDrinks.isChecked() || cbDessert.isChecked());
        if (anyCategoryChecked) {
            List<FoodItem> categoryFiltered = new ArrayList<>();
            Map<CheckBox, String> categoryMap = new HashMap<>();
            if (cbMain != null) categoryMap.put(cbMain, "Main Dish");
            if (cbSides != null) categoryMap.put(cbSides, "Side Dish");
            if (cbDrinks != null) categoryMap.put(cbDrinks, "Drink");
            if (cbDessert != null) categoryMap.put(cbDessert, "Dessert");

            for (FoodItem item : filtered) {
                for (Map.Entry<CheckBox, String> entry : categoryMap.entrySet()) {
                    if (entry.getKey().isChecked() && entry.getValue().equalsIgnoreCase(item.getCategory())) {
                        categoryFiltered.add(item);
                        break;
                    }
                }
            }
            filtered = categoryFiltered;
        }

        if (preferencesLoaded && !favoriteCategory.isEmpty()) {
            List<FoodItem> favorites = new ArrayList<>();
            List<FoodItem> others = new ArrayList<>();
            for (FoodItem item : filtered) {
                if (item.getCategory() != null && item.getCategory().equalsIgnoreCase(favoriteCategory)) {
                    favorites.add(item);
                } else {
                    others.add(item);
                }
            }
            favorites.addAll(others);
            filtered = favorites;
        }

        displayMenu(filtered);
    }

    private void displayMenu(List<FoodItem> itemsToDisplay) {
        runOnUiThread(() -> {
            foodCardContainer.removeAllViews();
            for (FoodItem item : itemsToDisplay) {
                addVerticalFoodCard(item);
            }
            updateNoMenuText();
        });
    }

    private void addVerticalFoodCard(FoodItem item) {
        View view = LayoutInflater.from(this).inflate(R.layout.customer_item_card_layout, foodCardContainer, false);
        fillCardData(view, item, false);
        TextView badge = view.findViewById(R.id.trending_badge);
        if (badge != null) badge.setVisibility(View.GONE);
        foodCardContainer.addView(view);
    }

    private void fillCardData(View view, FoodItem item, boolean isTrending) {
        TextView name = view.findViewById(R.id.item_name);
        TextView price = view.findViewById(R.id.item_price);
        ImageView image = view.findViewById(R.id.item_image);
        TextView quantity = view.findViewById(R.id.item_quantity);
        MaterialButton btnAdd = view.findViewById(R.id.btn_add_to_cart);
        TextView soldOutOverlay = view.findViewById(R.id.sold_out_overlay);

        // --- NEW: TOTAL SOLD BINDING ---
        TextView tvTotalSold = view.findViewById(R.id.tv_total_sold);
        if (tvTotalSold != null) {
            if (item.getOrderCount() > 0) {
                tvTotalSold.setVisibility(View.VISIBLE);
                tvTotalSold.setText("🔥 " + item.getOrderCount() + " Sold");
            } else {
                tvTotalSold.setVisibility(View.GONE);
            }
        }

        if (name != null) name.setText(item.getName());
        if (price != null) price.setText("RM " + String.format("%.2f", item.getPrice()));

        if (quantity != null) {
            if (item.getQuantity() <= 0) {
                quantity.setText("Quantity: 0");
                quantity.setTextColor(android.graphics.Color.parseColor("#C4C4C4"));
                if (btnAdd != null) {
                    btnAdd.setText("Sold Out");
                    btnAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#B2EBF2")));
                    btnAdd.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
                    btnAdd.setEnabled(false);
                }
                if (soldOutOverlay != null) soldOutOverlay.setVisibility(View.VISIBLE);
            } else {
                if (soldOutOverlay != null) soldOutOverlay.setVisibility(View.GONE);
                if (item.getQuantity() <= 3) {
                    quantity.setText("Low Stock: " + item.getQuantity());
                    quantity.setTextColor(android.graphics.Color.parseColor("#F59E0B"));
                } else {
                    quantity.setText("Available: " + item.getQuantity());
                    quantity.setTextColor(android.graphics.Color.parseColor("#16A34A"));
                }

                if (btnAdd != null) {
                    btnAdd.setText(isTrending ? "Add" : "Add to Cart");
                    btnAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#B2EBF2")));
                    btnAdd.setTextColor(android.graphics.Color.parseColor("#032565"));
                    btnAdd.setEnabled(true);
                }
            }
        }

        if (image != null) {
            // Check if the activity is still alive before Glide tries to load
            if (!isFinishing() && !isDestroyed()) {
                Glide.with(this)
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.no_image_available)
                        .error(R.drawable.no_image_available)
                        .into(image);
            }
        }

        view.setOnClickListener(v -> {
            String userId = FirebaseAuth.getInstance().getUid();
            if (userId != null && item.getCategory() != null) {
                FirebaseDatabase.getInstance().getReference("user_preferences")
                        .child(userId).child(item.getCategory())
                        .setValue(ServerValue.increment(1));
            }
            Intent intent = new Intent(this, C4_Food_Details.class);
            intent.putExtra("FOOD_ID", item.getId());
            startActivity(intent);
        });

        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                if (item.getQuantity() > 0) {
                    CartManager.getInstance().addToCart(item, 1);
                    Toast.makeText(this, item.getName() + " added to cart", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Sold out", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupFilterDrawer() {
        View filterView = findViewById(R.id.filter_drawer_container);
        if (filterView == null) return;
        cbQty = filterView.findViewById(R.id.quantity_checkbox);
        cbPricing = filterView.findViewById(R.id.pricing_checkbox);
        etMinPrice = filterView.findViewById(R.id.min_price);
        etMaxPrice = filterView.findViewById(R.id.max_price);
        cbMain = filterView.findViewById(R.id.main_checkbox);
        cbSides = filterView.findViewById(R.id.sides_checkbox);
        cbDrinks = filterView.findViewById(R.id.drinks_checkbox);
        cbDessert = filterView.findViewById(R.id.dessert_checkbox);
        MaterialButton btnApply = filterView.findViewById(R.id.apply_button);
        MaterialButton btnClear = filterView.findViewById(R.id.clear_button);
        TextView closeTab = filterView.findViewById(R.id.close_tab_button);

        if (closeTab != null) closeTab.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));

        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                // 1. Check if Pricing Filter is even active
                if (cbPricing != null && cbPricing.isChecked()) {
                    String minStr = etMinPrice.getText().toString().trim();
                    String maxStr = etMaxPrice.getText().toString().trim();

                    // 2. Check if both fields are filled
                    if (minStr.isEmpty() || maxStr.isEmpty()) {
                        if (minStr.isEmpty()) etMinPrice.setError("Please enter a minimum price");
                        if (maxStr.isEmpty()) etMaxPrice.setError("Please enter a maximum price");
                        Toast.makeText(this, "Both price fields are required for filtering", Toast.LENGTH_SHORT).show();
                        return; // Stop right here
                    }

                    try {
                        double minVal = Double.parseDouble(minStr);
                        double maxVal = Double.parseDouble(maxStr);

                        // 3. Check if Max is greater than Min
                        if (maxVal < minVal) {
                            etMaxPrice.setError("Max price must be higher than min price");
                            Toast.makeText(this, "Invalid price range!", Toast.LENGTH_SHORT).show();
                            return; // Stop right here
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // 4. If all checks pass (or pricing isn't checked), apply filters
                applyFiltersAndDisplay();
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                if (cbQty != null) cbQty.setChecked(false);
                if (cbPricing != null) cbPricing.setChecked(false);
                if (etMinPrice != null) etMinPrice.setText("");
                if (etMaxPrice != null) etMaxPrice.setText("");
                if (cbMain != null) cbMain.setChecked(false);
                if (cbSides != null) cbSides.setChecked(false);
                if (cbDrinks != null) cbDrinks.setChecked(false);
                if (cbDessert != null) cbDessert.setChecked(false);
                applyFiltersAndDisplay();
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }
    }

    private void updateNoMenuText() {
        if (foodCardContainer.getChildCount() == 0) noMenuText.setVisibility(View.VISIBLE);
        else noMenuText.setVisibility(View.GONE);
    }

    private class TrendingAdapter extends RecyclerView.Adapter<TrendingAdapter.ViewHolder> {
        private List<FoodItem> list;
        public TrendingAdapter(List<FoodItem> list) { this.list = list; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.trending_item_layout, parent, false);
            return new ViewHolder(view);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FoodItem item = list.get(position);
            fillCardData(holder.itemView, item, true);
            TextView badge = holder.itemView.findViewById(R.id.trending_badge);
            if (badge != null) {
                badge.setVisibility(View.VISIBLE);
                badge.setText("🔥 #" + (position + 1) + " Popular");
            }
        }
        @Override public int getItemCount() { return list.size(); }
        class ViewHolder extends RecyclerView.ViewHolder { public ViewHolder(@NonNull View itemView) { super(itemView); } }
    }

    private void setupCustomerNotificationListener() {
        String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("notifications").child(userId);

        // Skip old notifications on startup
        notifRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // If there are existing notifications, show the dot
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    notificationBadge.setVisibility(View.VISIBLE);
                }
                isInitialLoadFinished = true;
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        notifRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String prev) {
                // Show dot for any new incoming notification
                notificationBadge.setVisibility(View.VISIBLE);

                if (!isInitialLoadFinished) return;

                AppNotification notif = snapshot.getValue(AppNotification.class);
                if (notif != null) {
                    NotificationHelper.triggerSystemNotification(
                            getApplicationContext(),
                            notif.getTitle(),
                            notif.getMessage(),
                            notif.getType()
                    );
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot s, String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {
                if (!s.getRef().getParent().equals(null)) {
                    checkIfNotificationsExist(notifRef);
                }
            }
            @Override public void onChildMoved(@NonNull DataSnapshot s, String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void checkIfNotificationsExist(DatabaseReference ref) {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) notificationBadge.setVisibility(View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
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