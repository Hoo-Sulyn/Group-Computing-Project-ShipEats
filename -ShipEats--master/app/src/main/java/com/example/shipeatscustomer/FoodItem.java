package com.example.shipeatscustomer;

import java.io.Serializable;

public class FoodItem implements Serializable {

    private String id;
    private String name;
    private String description;
    private String category;
    private double price;
    private int quantity;
    private String imageUrl;
    private String status;
    private boolean published;

    // AI FIELDS
    private int orderCount;
    private int clickCount;
    private String ingredients;

    // ADDED ONLY THESE TWO FOR RATING
    private double rating;
    private int reviewCount;

    // Required empty constructor for Firebase
    public FoodItem() {
        this.published = true;
        this.orderCount = 0;
        this.clickCount = 0;
        this.ingredients = "";
        this.rating = 0.0;
        this.reviewCount = 0;
    }

    public FoodItem(String id, String name, String description,
                    String category, double price,
                    int quantity, String imageUrl, int orderCount,
                    int clickCount, String ingredients, double rating, int reviewCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.status = calculateStatus(quantity);
        this.published = true;
        this.orderCount = orderCount;
        this.clickCount = clickCount;
        this.ingredients = ingredients;
        // ADDED THESE TWO
        this.rating = rating;
        this.reviewCount = reviewCount;
    }

    public static String calculateStatus(int quantity) {
        if (quantity <= 0) return "Sold Out";
        else if (quantity <= 3) return "Low Stock";
        else return "Available";
    }

    // ===== GETTERS =====
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public String getImageUrl() { return imageUrl; }
    public int getOrderCount() { return orderCount; }
    public int getClickCount() { return clickCount; }
    public String getIngredients() { return ingredients; }
    public boolean isPublished() { return published; }

    // ADDED GETTERS
    public double getRating() { return rating; }
    public int getReviewCount() { return reviewCount; }

    public String getStatus() {
        return calculateStatus(this.quantity);
    }

    // ===== SETTERS =====
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setPrice(double price) { this.price = price; }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.status = calculateStatus(quantity);
    }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setStatus(String status) { this.status = status; }
    public void setPublished(boolean published) { this.published = published; }
    public void setOrderCount(int orderCount) { this.orderCount = orderCount; }
    public void setClickCount(int clickCount) { this.clickCount = clickCount; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    // ADDED SETTERS
    public void setRating(double rating) { this.rating = rating; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
}