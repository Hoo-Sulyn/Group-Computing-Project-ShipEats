package com.example.shipeatscustomer;

public class Review {
    private String customerName;
    private float rating;
    private String comment;
    private long timestamp;

    // 1. MUST have an empty constructor for Firebase to work
    public Review() {}

    // 2. Full constructor
    public Review(String customerName, float rating, String comment, long timestamp) {
        this.customerName = customerName;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    // 3. Getters (Firebase uses these to read the data)
    public String getCustomerName() { return customerName; }
    public float getRating() { return rating; }
    public String getComment() { return comment; }
    public long getTimestamp() { return timestamp; }
}