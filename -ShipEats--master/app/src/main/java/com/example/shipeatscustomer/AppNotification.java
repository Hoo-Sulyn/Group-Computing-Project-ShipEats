package com.example.shipeatscustomer;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppNotification implements Serializable {
    private String id;
    private String title;
    private String message;
    private long timestamp;
    private String type;       // e.g., "order", "inventory", "promo"
    private String relatedId;   // e.g., orderId, itemId
    private boolean read;

    // Required empty constructor for Firebase
    public AppNotification() {}

    public AppNotification(String title, String message, String type, String relatedId) {
        this.title = title;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.relatedId = relatedId;
        this.read = false;
    }

    // Getters and setters (required for Firebase)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRelatedId() { return relatedId; }
    public void setRelatedId(String relatedId) { this.relatedId = relatedId; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}