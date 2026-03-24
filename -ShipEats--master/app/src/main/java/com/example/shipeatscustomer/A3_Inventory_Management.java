package com.example.shipeatscustomer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class A3_Inventory_Management extends AppCompatActivity {

    RecyclerView recyclerView;
    MaterialButton addItemBtn;
    EditText etSearch;
    CardView btnSort;
    ChipGroup chipGroupCategory;
    TextView tvItemCountLabel;
    TextView tvStatTotal, tvStatLow, tvStatOut;

    DatabaseReference databaseRef;
    List<FoodItem> foodList    = new ArrayList<>(); // master list from Firebase
    List<FoodItem> displayList = new ArrayList<>(); // filtered + sorted for adapter
    A3_InventoryAdapter adapter;

    // Filter state — matches string-array category_array exactly
    private String selectedCategory = "All";

    // Sort state
    private String sortMode = "name_az";

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    AdminDialogHelper.handleImageResult(selectedImageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_a3_inventory_management);

        recyclerView      = findViewById(R.id.inventory_recycler);
        addItemBtn        = findViewById(R.id.btn_add_item);
        etSearch          = findViewById(R.id.et_search);
        btnSort           = findViewById(R.id.btn_sort);
        chipGroupCategory = findViewById(R.id.chipGroupCategory);
        tvItemCountLabel  = findViewById(R.id.tv_item_count_label);
        tvStatTotal       = findViewById(R.id.tv_stat_total);
        tvStatLow         = findViewById(R.id.tv_stat_low);
        tvStatOut         = findViewById(R.id.tv_stat_out);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        databaseRef = FirebaseDatabase.getInstance().getReference("food_items");

        adapter = new A3_InventoryAdapter(this, displayList,
                new A3_InventoryAdapter.OnItemActionListener() {
                    @Override
                    public void onDelete(FoodItem item) {
                        AdminDialogHelper.showDeleteConfirmDialog(
                                A3_Inventory_Management.this, item.getId(), () -> {
                                    DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                                    java.util.HashMap<String, Object> deleteMap = new java.util.HashMap<>();
                                    deleteMap.put("/food_items/" + item.getId(), null);
                                    deleteMap.put("/menu_items/" + item.getId(), null);
                                    rootRef.updateChildren(deleteMap).addOnSuccessListener(aVoid -> {
                                        Toast.makeText(A3_Inventory_Management.this,
                                                "Item deleted", Toast.LENGTH_SHORT).show();
                                        AdminDialogHelper.showStatusDialog(
                                                A3_Inventory_Management.this,
                                                R.layout.admin_dialog_menu_delete);
                                    });
                                });
                    }

                    @Override
                    public void onEdit(FoodItem item) {
                        AdminDialogHelper.showEditMenuDialog(
                                A3_Inventory_Management.this, imagePickerLauncher, item, false);
                    }
                });
        recyclerView.setAdapter(adapter);

        // Firebase real-time listener
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                foodList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    FoodItem item = data.getValue(FoodItem.class);
                    if (item != null) {
                        item.setId(data.getKey());
                        foodList.add(item);
                    }
                }
                updateStats();
                applyFiltersAndSort();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(A3_Inventory_Management.this,
                        "Database Error", Toast.LENGTH_SHORT).show();
            }
        });

        // Search
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                applyFiltersAndSort();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Category chips
        chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip chip = findViewById(checkedIds.get(0));
            if (chip != null) {
                selectedCategory = chip.getText().toString();
                applyFiltersAndSort();
            }
        });

        // Sort — opens bottom sheet
        btnSort.setOnClickListener(v -> showSortBottomSheet());

        // Add item
        addItemBtn.setOnClickListener(v ->
                AdminDialogHelper.showEditMenuDialog(this, imagePickerLauncher, null, true));

        setupBottomNav();
        setupHeader();
        highlightCurrentTab(
                findViewById(R.id.inventory_icon),
                findViewById(R.id.inventory_text));
    }

    // ══════════════════════════════════════════════════════════════
    //  Stats bar — always reflects full foodList, not filtered
    // ══════════════════════════════════════════════════════════════
    private void updateStats() {
        int total = foodList.size();
        int low = 0, out = 0;
        for (FoodItem item : foodList) {
            int qty = item.getQuantity();
            if      (qty <= 0) out++;
            else if (qty <= 5) low++;
        }
        if (tvStatTotal != null) tvStatTotal.setText(String.valueOf(total));
        if (tvStatLow   != null) tvStatLow  .setText(String.valueOf(low));
        if (tvStatOut   != null) tvStatOut  .setText(String.valueOf(out));
    }

    // ══════════════════════════════════════════════════════════════
    //  Sort bottom sheet
    // ══════════════════════════════════════════════════════════════
    private void showSortBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.admin_bottom_sheet_sort, null);
        dialog.setContentView(view);
        dialog.getBehavior().setPeekHeight(900);

        // Show checkmark on current sort option
        updateSortChecks(view, sortMode);

        // Row IDs mapped to sort modes
        int[]    rowIds = {
                R.id.sort_name_az,  R.id.sort_name_za,
                R.id.sort_price_asc, R.id.sort_price_desc,
                R.id.sort_stock_asc, R.id.sort_stock_desc
        };
        String[] modes = {
                "name_az",  "name_za",
                "price_asc", "price_desc",
                "stock_asc", "stock_desc"
        };

        for (int i = 0; i < rowIds.length; i++) {
            final String mode = modes[i];
            View row = view.findViewById(rowIds[i]);
            if (row != null) {
                row.setOnClickListener(v -> {
                    sortMode = mode;
                    updateSortChecks(view, sortMode);
                    applyFiltersAndSort();
                    dialog.dismiss();
                });
            }
        }

        dialog.show();
    }

    private void updateSortChecks(View view, String mode) {
        int[]    checkIds = {
                R.id.check_name_az,   R.id.check_name_za,
                R.id.check_price_asc, R.id.check_price_desc,
                R.id.check_stock_asc, R.id.check_stock_desc
        };
        String[] modes = {
                "name_az",  "name_za",
                "price_asc", "price_desc",
                "stock_asc", "stock_desc"
        };
        for (int i = 0; i < checkIds.length; i++) {
            View check = view.findViewById(checkIds[i]);
            if (check != null)
                check.setVisibility(modes[i].equals(mode) ? View.VISIBLE : View.GONE);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Filter + Sort
    //  Categories match string-array exactly:
    //  "All" | "Main Dish" | "Side Dish" | "Drink" | "Dessert"
    // ══════════════════════════════════════════════════════════════
    private void applyFiltersAndSort() {
        String query = etSearch.getText().toString().toLowerCase().trim();

        List<FoodItem> filtered = new ArrayList<>();
        for (FoodItem item : foodList) {
            if (item.getName() == null) continue;

            boolean matchCat = "All".equals(selectedCategory)
                    || (item.getCategory() != null
                    && item.getCategory().equalsIgnoreCase(selectedCategory));

            boolean matchSearch = item.getName().toLowerCase().contains(query);

            if (matchCat && matchSearch) filtered.add(item);
        }

        // Sort
        switch (sortMode) {
            case "name_az":
                Collections.sort(filtered, (a, b) ->
                        a.getName().compareToIgnoreCase(b.getName()));
                break;
            case "name_za":
                Collections.sort(filtered, (a, b) ->
                        b.getName().compareToIgnoreCase(a.getName()));
                break;
            case "price_asc":
                Collections.sort(filtered, (a, b) ->
                        Double.compare(a.getPrice(), b.getPrice()));
                break;
            case "price_desc":
                Collections.sort(filtered, (a, b) ->
                        Double.compare(b.getPrice(), a.getPrice()));
                break;
            case "stock_asc":
                Collections.sort(filtered, (a, b) ->
                        Integer.compare(a.getQuantity(), b.getQuantity()));
                break;
            case "stock_desc":
                Collections.sort(filtered, (a, b) ->
                        Integer.compare(b.getQuantity(), a.getQuantity()));
                break;
        }

        // Update item count label
        if (tvItemCountLabel != null) {
            String cat = "All".equals(selectedCategory) ? "All Items" : selectedCategory;
            tvItemCountLabel.setText(cat + " (" + filtered.size() + ")");
        }

        displayList.clear();
        displayList.addAll(filtered);
        adapter.updateList(displayList);
    }

    // ══════════════════════════════════════════════════════════════
    //  Navigation
    // ══════════════════════════════════════════════════════════════
    private void setupHeader() {
        View notifBtn = findViewById(R.id.admin_notification_btn);
        if (notifBtn != null)
            notifBtn.setOnClickListener(v ->
                    AdminDialogHelper.showNotificationsDialog(this));
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
            footer.findViewById(R.id.dashboard_nav).setOnClickListener(v ->
                    startActivity(new Intent(this, A2_Dashboard.class)));
            footer.findViewById(R.id.inventory_nav).setOnClickListener(v ->
                    Toast.makeText(this,
                            "You are already on Inventory", Toast.LENGTH_SHORT).show());
            footer.findViewById(R.id.orders_nav).setOnClickListener(v ->
                    startActivity(new Intent(this, A4_CustomerOrderActivity.class)));
            footer.findViewById(R.id.menu_nav).setOnClickListener(v ->
                    startActivity(new Intent(this, A5_MenuManagementActivity.class)));
            footer.findViewById(R.id.profile_nav).setOnClickListener(v ->
                    startActivity(new Intent(this, A6_Profile.class)));
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