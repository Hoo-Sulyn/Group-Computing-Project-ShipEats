package com.example.shipeatscustomer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.atomic.AtomicInteger;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "shipeats_notifications";

    private static final AtomicInteger notificationIdCounter = new AtomicInteger(0);

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "ShipEats Alerts";
            String description = "Channel for orders and menu updates";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void triggerSystemNotification(Context context, String title, String message, String type) {
        String pkg = context.getPackageName();
        boolean isCustomerApp = pkg.contains("customer");

        Log.d(TAG, "Testing on phone - Package: " + pkg + " | IsCustomer: " + isCustomerApp);

        // 1. The Logic Gate
        if (isCustomerApp) {
            android.content.SharedPreferences prefs = context.getSharedPreferences("NotifPrefs", Context.MODE_PRIVATE);
            if (!prefs.getBoolean("push", true) || !prefs.getBoolean(type, true)) {
                Log.d(TAG, "Customer Muted.");
                return;
            }
        } else {
            Log.d(TAG, "Admin Bypass Active.");
        }

        // 2. The "Universal" Intent
        // Instead of naming a specific class (C3 or A2),
        // we just tell Android to "Open this app's main screen"
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkg);

        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        // 3. The Build (Standard)
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationIdCounter.incrementAndGet(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // This forces the "Ding"
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notificationIdCounter.get(), builder.build());
        }
    }

    private static DatabaseReference getNotificationsRef(String userId) {
        return FirebaseDatabase.getInstance().getReference("notifications").child(userId);
    }

    public static void sendNotification(String userId, String title, String message,
                                        String type, String relatedId) {
        DatabaseReference ref = getNotificationsRef(userId);
        String notifId = ref.push().getKey();
        if (notifId == null) return;

        AppNotification notification = new AppNotification(title, message, type, relatedId);
        notification.setId(notifId);

        ref.child(notifId).setValue(notification)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to write notification", e));
    }

    public static void sendOrderNotification(String userId, String orderId, String status) {
        String title = "Order Update";
        String message = "Your order #" + orderId + " is now " + status + ".";
        sendNotification(userId, title, message, "order", orderId);
    }

    public static void sendPaymentNotification(String userId, String orderId, String amount) {
        String title = "Payment Successful";
        String message = "Payment of " + amount + " for Order #" + orderId + " was successful.";
        sendNotification(userId, title, message, "payment", orderId);
    }

    public static void broadcastToAdmins(String title, String message, String type, String relatedId) {
        DatabaseReference adminsRef = FirebaseDatabase.getInstance().getReference("Admins");
        adminsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot adminSnap : snapshot.getChildren()) {
                    String adminId = adminSnap.getKey();
                    if (adminId != null) {
                        sendNotification(adminId, title, message, type, relatedId);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void broadcastToAllUsers(String title, String message, String type, String relatedId) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String userId = userSnap.getKey();
                    if (userId != null) {
                        sendNotification(userId, title, message, type, relatedId);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void broadcastNewMenuAdded(String itemName) {
        String title = "\uD83C\uDF74 New Menu Item!";
        String message = itemName + " has been added to the menu. Check it out!";
        broadcastToAllUsers(title, message, "menu", "");
    }

    public static void broadcastLowStockAlert(String itemName, int remaining) {
        String title = "Low Stock Alerts";
        String message = itemName + " is running low (" + remaining + " remaining)";
        broadcastToAdmins(title, message, "inventory", itemName);
    }

    public static void broadcastNewOrderAlert(String orderId, String customerName) {
        String title = "New Order Received";
        String displayId = orderId.length() > 8 ? orderId.substring(orderId.length() - 8).toUpperCase() : orderId;
        String message = "Order #" + displayId + " from " + customerName;
        broadcastToAdmins(title, message, "order", orderId);
    }

    public static void sendAdminNotification(String adminId, String title, String message,
                                             String type, String relatedId) {
        sendNotification(adminId, title, message, type, relatedId);
    }
}
