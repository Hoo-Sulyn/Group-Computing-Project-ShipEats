package com.example.shipeatscustomer;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminDialogHelper {
    private static DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("food_items");
    private static Uri selectedImageUri = null;
    private static ImageView dialogImageView = null;

    public static void showEditMenuDialog(AppCompatActivity activity, ActivityResultLauncher<Intent> launcher, FoodItem item, boolean isNew) {
        Dialog dialog = new Dialog(activity);

        // Reset static state for the new dialog instance
        selectedImageUri = null;
        dialogImageView = null;

        View view = LayoutInflater.from(activity).inflate(R.layout.admin_dialog_add_item, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
            lp.width = (int) (screenWidth * 0.90);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = android.view.Gravity.CENTER;

            dialog.getWindow().setAttributes(lp);
        }

        // Initialize UI Elements
        EditText etName = view.findViewById(R.id.et_name);
        EditText etDesc = view.findViewById(R.id.et_description);
        EditText etPrice = view.findViewById(R.id.et_price);
        EditText etQuantity = view.findViewById(R.id.et_quantity);
        MaterialButton btnPlus = view.findViewById(R.id.btn_plus);
        MaterialButton btnMinus = view.findViewById(R.id.btn_minus);
        Spinner spinnerCat = view.findViewById(R.id.spinner_category);
        MaterialButton btnConfirm = view.findViewById(R.id.btn_add_confirm);
        ImageView btnClose = view.findViewById(R.id.btn_close);
        dialogImageView = view.findViewById(R.id.iv_food_preview);

        // Quantity Button Logic
        btnPlus.setOnClickListener(v -> {
            int current = etQuantity.getText().toString().isEmpty() ? 0 : Integer.parseInt(etQuantity.getText().toString());
            etQuantity.setText(String.valueOf(current + 1));
        });

        btnMinus.setOnClickListener(v -> {
            int current = etQuantity.getText().toString().isEmpty() ? 0 : Integer.parseInt(etQuantity.getText().toString());
            if (current > 0) etQuantity.setText(String.valueOf(current - 1));
        });

        // Category Spinner Setup
        ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(activity,
                R.array.category_array, android.R.layout.simple_spinner_item);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCat.setAdapter(catAdapter);

        // Populate Data if in Edit Mode
        if (!isNew && item != null) {
            etName.setText(item.getName());
            etDesc.setText(item.getDescription());
            etPrice.setText(String.valueOf(item.getPrice()));
            etQuantity.setText(String.valueOf(item.getQuantity()));

            int catPosition = catAdapter.getPosition(item.getCategory());
            spinnerCat.setSelection(catPosition);

            Glide.with(activity)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.no_image_available)
                    .error(R.drawable.no_image_available)
                    .into(dialogImageView);
        } else {
            etQuantity.setText("1");
            Glide.with(activity)
                    .load(R.drawable.no_image_available)
                    .into(dialogImageView);
        }

        // Image Picker Trigger
        dialogImageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            launcher.launch(intent);
        });

        btnConfirm.setOnClickListener(v -> {
            try {
                String name = etName.getText().toString().trim();
                String desc = etDesc.getText().toString().trim();
                String priceTxt = etPrice.getText().toString().trim();
                String qtyTxt = etQuantity.getText().toString().trim();

                if (name.isEmpty() || priceTxt.isEmpty()) {
                    Toast.makeText(activity, "Fill required fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                String id = isNew ? databaseRef.push().getKey() : item.getId();
                double price = Double.parseDouble(priceTxt);
                int quantity = Integer.parseInt(qtyTxt);
                String category = spinnerCat.getSelectedItem().toString();

                String imageUrl = (selectedImageUri != null) ? selectedImageUri.toString() :
                                 (item != null && item.getImageUrl() != null ? item.getImageUrl() : "");

                // AI Integration: Carry over existing AI stats if editing, otherwise default to 0
                int orderCount = (!isNew && item != null) ? item.getOrderCount() : 0;
                int clickCount = (!isNew && item != null) ? item.getClickCount() : 0;
                String ingredients = (!isNew && item != null && item.getIngredients() != null) ? item.getIngredients() : "";

                FoodItem updatedItem = new FoodItem(id, name, desc, category, price, quantity, imageUrl, orderCount, clickCount, ingredients, 0.0, 0);

                // MULTI-PATH UPDATE
                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                java.util.HashMap<String, Object> updateMap = new java.util.HashMap<>();

                // ONLY write to food_items (Inventory).
                // menu_items is managed separately by the admin
                // inside Menu Management → "Add from Inventory".
                updateMap.put("/food_items/" + id, updatedItem);

                // If editing an item that already exists in menu_items,
                // keep it in sync (name, price, quantity etc.) but do NOT
                // auto-add new items to menu_items.
                if (!isNew) {
                    // Check and sync to menu_items only if it already exists there
                    rootRef.child("menu_items").child(id)
                            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                                @Override
                                public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        // Item already in menu - sync the updated fields
                                        updateMap.put("/menu_items/" + id, updatedItem);
                                    }
                                    // Perform the actual write
                                    rootRef.updateChildren(updateMap).addOnSuccessListener(aVoid -> {
                                        dialog.dismiss();
                                        showStatusDialog(activity, R.layout.admin_dialog_menu_complete);
                                    }).addOnFailureListener(e ->
                                            Toast.makeText(activity, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                }
                                @Override
                                public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                                    // Still save to food_items even if check fails
                                    rootRef.updateChildren(updateMap).addOnSuccessListener(aVoid -> {
                                        dialog.dismiss();
                                        showStatusDialog(activity, R.layout.admin_dialog_menu_complete);
                                    });
                                }
                            });
                } else {
                    // New item - only write to food_items, never auto-add to menu_items
                    rootRef.updateChildren(updateMap).addOnSuccessListener(aVoid -> {
                        dialog.dismiss();
                        // No broadcastNewMenuAdded — item is not in menu yet
                        showStatusDialog(activity, R.layout.admin_dialog_menu_add);
                    }).addOnFailureListener(e ->
                            Toast.makeText(activity, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

            } catch (NumberFormatException e) {
                Toast.makeText(activity, "Invalid number format", Toast.LENGTH_SHORT).show();
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        // Ensure static references are cleared when dialog is hidden to avoid leakage
        dialog.setOnDismissListener(d -> {
            selectedImageUri = null;
            dialogImageView = null;
        });

        dialog.show();
    }

    public static void handleImageResult(Uri uri) {
        if (dialogImageView != null && uri != null) {
            selectedImageUri = uri;
            Glide.with(dialogImageView.getContext()).load(uri).into(dialogImageView);
        }
    }

    public static void showDeleteConfirmDialog(Context context, String itemId, Runnable onDeleteConfirmed) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.admin_dialog_item_confirm_delete);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lp);
        }

        Button btnDelete = dialog.findViewById(R.id.btnDelete);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        View btnClose = dialog.findViewById(R.id.btnClose);

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                if (onDeleteConfirmed != null) {
                    onDeleteConfirmed.run();
                }
                dialog.dismiss();
            });
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    public static void showStatusDialog(Context context, int layoutId) {
        // 1. Safety check: If the context is an activity, is it finishing?
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            if (activity.isFinishing() || activity.isDestroyed()) return;
        }

        Dialog dialog = new Dialog(context);
        dialog.setContentView(layoutId);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lp);
        }

        dialog.show();

        // 2. The most important part: The safety check inside the delay
        new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                // Only dismiss if the activity is still valid and the dialog is showing
                if (context instanceof AppCompatActivity) {
                    AppCompatActivity activity = (AppCompatActivity) context;
                    if (!activity.isFinishing() && !activity.isDestroyed() && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                } else if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            } catch (Exception e) {
                // Catch-all to prevent the "Not attached to window manager" crash
                android.util.Log.e("DialogHelper", "Failed to dismiss dialog: " + e.getMessage());
            }
        }, 1500);
    }

    /**
     * Shows the notifications as a sidebar from the right.
     * Works for both Admin and Customer based on the current user type.
     */
    public static void showNotificationsDialog(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = user.getUid();

        Dialog dialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setContentView(R.layout.admin_notifications_popup);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());

            float density = context.getResources().getDisplayMetrics().density;
            lp.width = (int) (320 * density);
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            lp.gravity = android.view.Gravity.END;
            lp.windowAnimations = android.R.style.Animation_InputMethod; // Side animation

            dialog.getWindow().setAttributes(lp);
        }

        ImageView btnClose = dialog.findViewById(R.id.btnClose);
        TextView tvUnreadCount = dialog.findViewById(R.id.tvUnreadCount);
        TextView btnMarkAllRead = dialog.findViewById(R.id.btnMarkAllRead);
        TextView btnDeleteAll = dialog.findViewById(R.id.btnDeleteAll);
        RecyclerView rvNotifications = dialog.findViewById(R.id.rvAdminNotifications);

        rvNotifications.setLayoutManager(new LinearLayoutManager(context));
        List<AppNotification> notificationList = new ArrayList<>();
        NotificationsAdapter adapter = new NotificationsAdapter(notificationList);
        
        // Determine if admin mode based on node check (simplified for this context)
        FirebaseDatabase.getInstance().getReference("Admins").child(userId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    adapter.setAdminMode(snapshot.exists());
                    rvNotifications.setAdapter(adapter);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("notifications").child(userId);

        notifRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationList.clear();
                int unreadCount = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    AppNotification notif = ds.getValue(AppNotification.class);
                    if (notif != null) {
                        notif.setId(ds.getKey());
                        notificationList.add(0, notif); // newest first
                        if (!notif.isRead()) unreadCount++;
                    }
                }
                adapter.notifyDataSetChanged();
                tvUnreadCount.setText("You have " + unreadCount + " unread notifications");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        btnMarkAllRead.setOnClickListener(v -> {
            notifRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        ds.getRef().child("read").setValue(true);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        });

        if (btnDeleteAll != null) {
            btnDeleteAll.setOnClickListener(v -> {
                notifRef.removeValue().addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "All notifications cleared", Toast.LENGTH_SHORT).show();
                });
            });
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    public static void showOrderCancelConfirmDialog(Context context, Runnable onConfirm) {
        Dialog dialog = new Dialog(context);
        // Use your new copy here
        dialog.setContentView(R.layout.admin_dialog_order_cancel_confirm);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lp);
        }

        // Connect your buttons (Make sure these IDs match your new XML copy)
        Button btnConfirm = dialog.findViewById(R.id.btnConfirm);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        View btnClose = dialog.findViewById(R.id.btnClose);

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                // DISMISS FIRST to clear the window from the manager
                dialog.dismiss();

                // THEN run the Firebase/Finish logic
                onConfirm.run();
            });
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
