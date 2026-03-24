package com.example.shipeatscustomer;

import java.io.Serializable;

public class CartItem implements Serializable {
    private FoodItem foodItem;
    private int quantity;
    private String specialInstructions;
    private boolean wantCutlery;

    public CartItem() {}

    public CartItem(FoodItem foodItem, int quantity, String specialInstructions, boolean wantCutlery) {
        this.foodItem = foodItem;
        this.quantity = quantity;
        this.specialInstructions = specialInstructions;
        this.wantCutlery = wantCutlery;
    }

    public FoodItem getFoodItem() { return foodItem; }
    public void setFoodItem(FoodItem foodItem) { this.foodItem = foodItem; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getSpecialInstructions() { return specialInstructions; }
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }

    public boolean isWantCutlery() { return wantCutlery; }
    public void setWantCutlery(boolean wantCutlery) { this.wantCutlery = wantCutlery; }
}