package com.example.shipeatscustomer;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class A5_MenuManagementActivity extends AppCompatActivity {
    private RecyclerView rvMenuItems;
    private A5_MenuAdapter adapter;
    private List<FoodItem> fullMenuList = new ArrayList<>();
    private List<FoodItem> filteredList = new ArrayList<>();
    
    private DatabaseReference inventoryRef = FirebaseDatabase.getInstance().getReference("food_items");
    private DatabaseReference menuRef = FirebaseDatabase.getInstance().getReference("menu_items");

    private ChipGroup chipGroup;
    private TextInputEditText etSearch;
    private String selectedCategory = "All";

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
        setContentView(R.layout.activity_a5_menu_management);

        rvMenuItems = findViewById(R.id.rvMenuItems);
        chipGroup = findViewById(R.id.chipGroupCategories);
        etSearch = findViewById(R.id.etSearchMenu);

        rvMenuItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new A5_MenuAdapter(this, filteredList, imagePickerLauncher);
        rvMenuItems.setAdapter(adapter);

        menuRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullMenuList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    FoodItem item = ds.getValue(FoodItem.class);
                    if (item != null) {
                        item.setId(ds.getKey());
                        fullMenuList.add(item);
                    }
                }
                applyFilters();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip chip = findViewById(checkedIds.get(0));
            selectedCategory = chip.getText().toString();
            applyFilters();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.add_menu_item_button).setOnClickListener(v -> showInventoryDialog());

        findViewById(R.id.btn_preview_menu).setOnClickListener(v -> {
            Intent intent = new Intent(this, MenuPreviewActivity.class);
            startActivity(intent);
        });

        setupBottomNav();
        setupHeader();

        ImageView menuIcon = findViewById(R.id.menu_icon);
        TextView menuText = findViewById(R.id.menu_text);
        highlightCurrentTab(menuIcon, menuText);
    }

    private void setupHeader() {
        View notifBtn = findViewById(R.id.admin_notification_btn);
        if (notifBtn != null) {
            notifBtn.setOnClickListener(v -> AdminDialogHelper.showNotificationsDialog(this));
        }
    }

    private void applyFilters() {
        String query = etSearch.getText().toString().toLowerCase().trim();
        filteredList.clear();

        for (FoodItem item : fullMenuList) {
            boolean matchesCategory = selectedCategory.equals("All") || item.getCategory().equalsIgnoreCase(selectedCategory);
            boolean matchesSearch = item.getName().toLowerCase().contains(query);

            if (matchesCategory && matchesSearch) {
                filteredList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showInventoryDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.activity_a5_menu_management_add_item);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Get existing IDs in the menu to prevent duplicates
        Set<String> existingMenuIds = new HashSet<>();
        for (FoodItem item : fullMenuList) {
            existingMenuIds.add(item.getId());
        }

        RecyclerView rvInv = dialog.findViewById(R.id.rvInventoryItems);
        rvInv.setLayoutManager(new LinearLayoutManager(this));
        
        List<FoodItem> inventoryList = new ArrayList<>();
        SelectionAdapter selectionAdapter = new SelectionAdapter(this, inventoryList);
        rvInv.setAdapter(selectionAdapter);

        inventoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                inventoryList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    FoodItem item = ds.getValue(FoodItem.class);
                    if (item != null) {
                        item.setId(ds.getKey());

                        // Filter out Sold Out and Already Added items
                        boolean isSoldOut = item.getQuantity() <= 0;
                        boolean alreadyInMenu = existingMenuIds.contains(item.getId());

                        if (!isSoldOut && !alreadyInMenu) {
                            inventoryList.add(item);
                        }
                    }
                }
                selectionAdapter.notifyDataSetChanged();
                
                if (inventoryList.isEmpty()) {
                    Toast.makeText(A5_MenuManagementActivity.this, "Inventory is empty. Add items to Inventory first.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        Button btnAdd = dialog.findViewById(R.id.btnAddSelection);
        btnAdd.setOnClickListener(v -> {
            List<FoodItem> selected = selectionAdapter.getSelectedItems();
            if (selected.isEmpty()) {
                Toast.makeText(this, "Select items first", Toast.LENGTH_SHORT).show();
                return;
            }

            for (FoodItem item : selected) {
                // Ensure the item is set to "published" when added to menu
                item.setPublished(true);
                menuRef.child(item.getId()).setValue(item);
            }
            dialog.dismiss();
            Toast.makeText(this, "Items added to Menu", Toast.LENGTH_SHORT).show();
        });

        dialog.findViewById(R.id.btnCloseDialog).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            int height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
            dialog.getWindow().setGravity(android.view.Gravity.CENTER);
        }
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
                    startActivity(new Intent(this, A3_Inventory_Management.class)));
            footer.findViewById(R.id.orders_nav).setOnClickListener(v ->
                    startActivity(new Intent(this, A4_CustomerOrderActivity.class)));
            footer.findViewById(R.id.menu_nav).setOnClickListener(v ->
                    Toast.makeText(this, "Already on Menu Management", Toast.LENGTH_SHORT).show());
            footer.findViewById(R.id.profile_nav).setOnClickListener(v ->
                    startActivity(new Intent(this, A6_Profile.class)));
        }
    }

    private static class SelectionAdapter extends RecyclerView.Adapter<SelectionAdapter.ViewHolder> {
        private Context context;
        private List<FoodItem> list;
        private List<FoodItem> selectedItems = new ArrayList<>();

        public SelectionAdapter(Context context, List<FoodItem> list) {
            this.context = context;
            this.list = list;
        }

        public List<FoodItem> getSelectedItems() { return selectedItems; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.activity_add_item_mm, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FoodItem item = list.get(position);
            holder.tvName.setText(item.getName());
            holder.tvPrice.setText("RM " + String.format("%.2f", item.getPrice()));
            holder.tvQty.setText("Quantity : " + item.getQuantity());
            
            String status = item.getStatus() != null ? item.getStatus() : "Available";
            holder.tvStatus.setText(status);

            if ("Sold Out".equalsIgnoreCase(status)) {
                holder.cardStatus.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#DC2626")));
            } else if ("Low Stock".equalsIgnoreCase(status)) {
                holder.cardStatus.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F59E0B")));
            } else {
                holder.cardStatus.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#16A34A")));
            }

            Glide.with(context).load(item.getImageUrl()).placeholder(R.drawable.no_image_available).into(holder.img);

            if (selectedItems.contains(item)) {
                holder.card.setStrokeColor(Color.parseColor("#04397B"));
                holder.card.setStrokeWidth(4);
            } else {
                holder.card.setStrokeColor(Color.parseColor("#D0D5DD"));
                holder.card.setStrokeWidth(1);
            }

            holder.itemView.setOnClickListener(v -> {
                if (selectedItems.contains(item)) {
                    selectedItems.remove(item);
                } else {
                    selectedItems.add(item);
                }
                notifyItemChanged(position);
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvPrice, tvQty, tvStatus;
            ImageView img;
            MaterialCardView card, cardStatus;
            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvInvName);
                tvPrice = itemView.findViewById(R.id.tvInvPrice);
                tvQty = itemView.findViewById(R.id.tvInvQuantity);
                tvStatus = itemView.findViewById(R.id.tvInvStatus);
                img = itemView.findViewById(R.id.imgInvFood);
                card = itemView.findViewById(R.id.inventoryCard);
                cardStatus = itemView.findViewById(R.id.cardInvStatus);
            }
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
