package com.example.shipeatscustomer;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.FirebaseDatabase;
import androidx.appcompat.widget.SwitchCompat;
import java.util.List;

public class A5_MenuAdapter extends RecyclerView.Adapter<A5_MenuAdapter.MenuViewHolder> {
    private Context context;
    private List<FoodItem> menuList;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public A5_MenuAdapter(Context context, List<FoodItem> menuList, ActivityResultLauncher<Intent> launcher) {
        this.context = context;
        this.menuList = menuList;
        this.imagePickerLauncher = launcher;
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.admin_it_menuitem, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        FoodItem item = menuList.get(position);

        // DATA BINDING
        holder.tvName.setText(item.getName());
        holder.tvPrice.setText("RM " + String.format("%.2f", item.getPrice()));
        holder.tvSoldQuantity.setText("Sold: " + item.getOrderCount());
        if (item.getCategory() != null) {
            holder.tvCategory.setText("Category: " + item.getCategory());
        } else {
            holder.tvCategory.setText("Category: N/A");
        }

        // UNPUBLISHED STATE LOGIC
        updateCardVisuals(holder, item.isPublished());

        // SWITCH LOGIC
        holder.swPublish.setOnCheckedChangeListener(null);
        holder.swPublish.setChecked(item.isPublished());

        String status = item.getStatus() != null ? item.getStatus() : "Available";
        holder.chipStatus.setText(status);

        if ("Sold Out".equalsIgnoreCase(status)) {
            holder.chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#DC2626")));
        } else if ("Low Stock".equalsIgnoreCase(status)) {
            holder.chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F59E0B")));
        } else {
            holder.chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#16A34A")));
        }

        Glide.with(context)
                .load(item.getImageUrl())
                .placeholder(R.drawable.no_image_available)
                .error(R.drawable.no_image_available)
                .into(holder.imgFood);

        holder.swPublish.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setPublished(isChecked);
            FirebaseDatabase.getInstance().getReference("menu_items")
                    .child(item.getId()).child("published").setValue(isChecked);

            if (isChecked) {
                Toast.makeText(context, "Item published to menu", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Item unpublished from menu", Toast.LENGTH_SHORT).show();
            }
        });

        // STEP 4: Opening the Reviews Page when the Admin clicks the item card
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, Admin_Reviews_Activity.class);
            // Pass the details so the next page knows what to load
            intent.putExtra("FOOD_ID", item.getId());
            intent.putExtra("FOOD_NAME", item.getName());
            context.startActivity(intent);
        });

        holder.btnRemove.setOnClickListener(v -> {
            AdminDialogHelper.showDeleteConfirmDialog(context, item.getId(), () -> {
                FirebaseDatabase.getInstance().getReference("menu_items")
                        .child(item.getId()).removeValue()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Removed from menu", Toast.LENGTH_SHORT).show();
                            AdminDialogHelper.showStatusDialog(context, R.layout.admin_dialog_menu_delete);
                        });
            });
        });
    }

    private void updateCardVisuals(MenuViewHolder holder, boolean isPublished) {
        if (isPublished) {
            // Restore Normal Appearance
            holder.cardMain.setAlpha(1.0f);
            holder.cardMain.setCardBackgroundColor(Color.WHITE);
            holder.imgFood.setColorFilter(null); // Remove Grayscale
        } else {
            // Apply "Disabled" Appearance
            holder.cardMain.setAlpha(0.6f); // Make it slightly transparent
            holder.cardMain.setCardBackgroundColor(Color.parseColor("#F3F4F6")); // Light Greyish background

            // Apply Grayscale Filter to the Image
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0); // 0 means fully black and white
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            holder.imgFood.setColorFilter(filter);
        }
    }

    @Override
    public int getItemCount() { return menuList.size(); }

    public static class MenuViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvCategory, tvSoldQuantity;
        Button btnRemove;
        ImageView imgFood;
        SwitchCompat swPublish;
        Chip chipStatus;
        MaterialCardView cardMain;
        public MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvFoodName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvSoldQuantity = itemView.findViewById(R.id.tvSoldQuantity);
            btnRemove = itemView.findViewById(R.id.btnRemove);
            imgFood = itemView.findViewById(R.id.imgFood);
            swPublish = itemView.findViewById(R.id.swPublish);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            cardMain = (MaterialCardView) itemView.findViewById(R.id.menu_card);
        }
    }
}