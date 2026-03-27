package com.example.shipeatscustomer;

public class A5_MenuItem {
    public String id, name, description, category, imageUrl;
    // Changed from String to double/int
    public double price;
    public int quantity;

    public A5_MenuItem() {} // Required for Firebase

    public A5_MenuItem(String id, String name, String description, double price, int quantity, String category, String imageUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.category = category;
        this.imageUrl = imageUrl;
    }
}
