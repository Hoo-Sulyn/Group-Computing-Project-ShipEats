package com.example.shipeatscustomer;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class A3_InventoryAdapter extends RecyclerView.Adapter<A3_InventoryAdapter.ViewHolder> {

    Context context;
    List<FoodItem> foodList;
    OnItemActionListener listener;
    private boolean isSelectionMode = false;
    private List<String> selectedItemIds = new ArrayList<>();

    public interface OnItemActionListener {
        void onDelete(FoodItem item);
        void onEdit(FoodItem item);
        default void onItemSelected(FoodItem item) {

        }
    }

    public A3_InventoryAdapter(Context context, List<FoodItem> foodList, OnItemActionListener listener) {
        this.context = context;
        this.foodList = foodList;
        this.listener = listener;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        notifyDataSetChanged();
    }

    public List<String> getSelectedItemIds() {
        return selectedItemIds;
    }

    public void updateList(List<FoodItem> newList) {
        this.foodList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.admin_inventory_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodItem item = foodList.get(position);

        holder.tvName.setText(item.getName());
        holder.tvPrice.setText("RM " + String.format("%.2f", item.getPrice()));
        holder.tvQuantity.setText("Quantity: " + item.getQuantity());

        String status = item.getStatus() != null ? item.getStatus() : "Available";
        holder.tvStatus.setText(status);

        // Status color
        switch (status) {
            case "Available":
                holder.tvStatus.setTextColor(Color.parseColor("#16A34A"));
                holder.badgeCard.setCardBackgroundColor(Color.parseColor("#DCFCE7"));
                break;

            case "Low Stock":
                holder.tvStatus.setTextColor(Color.parseColor("#D97706"));
                holder.badgeCard.setCardBackgroundColor(Color.parseColor("#FEF3C7"));
                break;

            case "Sold Out":
                holder.tvStatus.setTextColor(Color.parseColor("#DC2626"));
                holder.badgeCard.setCardBackgroundColor(Color.parseColor("#FEE2E2"));
                break;
        }

        // Load Image with placeholder and error to prevent leaking recycled images
        Glide.with(context)
                .load(item.getImageUrl())
                .placeholder(R.drawable.no_image_available)
                .error(R.drawable.no_image_available)
                .into(holder.ivFood);

        if (isSelectionMode) {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
            
            if (selectedItemIds.contains(item.getId())) {
                holder.cardView.setStrokeColor(Color.parseColor("#FFD700"));
                holder.cardView.setStrokeWidth(6);
            } else {
                holder.cardView.setStrokeColor(Color.parseColor("#D0D5DD"));
                holder.cardView.setStrokeWidth(2);
            }

            holder.itemView.setOnClickListener(v -> {
                if (selectedItemIds.contains(item.getId())) {
                    selectedItemIds.remove(item.getId());
                } else {
                    selectedItemIds.add(item.getId());
                }
                notifyItemChanged(position);
                listener.onItemSelected(item);
            });
        } else {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.cardView.setStrokeWidth(0);
            holder.btnEdit.setOnClickListener(v -> listener.onEdit(item));
            holder.btnDelete.setOnClickListener(v -> listener.onDelete(item));
        }
    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvQuantity, tvStatus;
        ImageView ivFood;
        MaterialButton btnEdit, btnDelete;
        MaterialCardView cardView;
        com.google.android.material.card.MaterialCardView badgeCard;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            tvName = itemView.findViewById(R.id.tv_food_name);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            tvStatus = itemView.findViewById(R.id.tv_status);
            badgeCard = itemView.findViewById(R.id.badge_card);
            ivFood = itemView.findViewById(R.id.iv_food);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
