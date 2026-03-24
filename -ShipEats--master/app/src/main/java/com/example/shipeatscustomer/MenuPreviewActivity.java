package com.example.shipeatscustomer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MenuPreviewActivity extends AppCompatActivity {

    private RecyclerView rvMenuPreview;
    private PreviewAdapter adapter;
    private List<FoodItem> menuList = new ArrayList<>();
    private DatabaseReference menuRef = FirebaseDatabase.getInstance().getReference("menu_items");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_a5_menu_preview);

        rvMenuPreview = findViewById(R.id.rvMenuPreview);
        if (rvMenuPreview == null) {
            Toast.makeText(this, "Error: RecyclerView not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rvMenuPreview.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PreviewAdapter(menuList);
        rvMenuPreview.setAdapter(adapter);

        // Load items from Firebase "menu_items"
        menuRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                menuList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    FoodItem item = ds.getValue(FoodItem.class);
                    if (item != null) {
                        item.setId(ds.getKey());
                        // Only show published items in the preview
                        if (item.isPublished()) {
                            menuList.add(item);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MenuPreviewActivity.this, "Database Error", Toast.LENGTH_SHORT).show();
            }
        });

        // "Edit Mode" button returns to Management screen
        View btnEditMode = findViewById(R.id.btnEditMode);
        if (btnEditMode != null) {
            btnEditMode.setOnClickListener(v -> finish());
        }

        setupBottomNav();
        setupHeader();
    }

    private void setupHeader() {
        ImageView notifIcon = findViewById(R.id.admin_notification_icon);
        if (notifIcon != null) {
            notifIcon.setOnClickListener(v -> AdminDialogHelper.showNotificationsDialog(this));
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
                    finish()); // Returns to Menu Management

            footer.findViewById(R.id.profile_nav).setOnClickListener(v ->
                    startActivity(new Intent(this, A6_Profile.class)));
        }
    }

    private class PreviewAdapter extends RecyclerView.Adapter<PreviewAdapter.ViewHolder> {
        private List<FoodItem> items;

        public PreviewAdapter(List<FoodItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Using the specific preview item layout activity_item_menu_preview
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_item_menu_preview, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FoodItem item = items.get(position);
            holder.tvName.setText(item.getName());
            holder.tvPrice.setText("RM " + String.format("%.2f", item.getPrice()));
            holder.tvQty.setText("Quantity: " + item.getQuantity());


            Glide.with(MenuPreviewActivity.this)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.no_image_available)
                    .into(holder.img);

            // Navigate to customer details page on click
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(MenuPreviewActivity.this, C4_Food_Details.class);
                intent.putExtra("FOOD_ID", item.getId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvPrice, tvQty;
            ImageView img;

            public ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvFoodName);
                tvPrice = itemView.findViewById(R.id.tvPrice);
                tvQty = itemView.findViewById(R.id.tvQuantity);
                img = itemView.findViewById(R.id.imgFood);
             }
        }
    }
}
