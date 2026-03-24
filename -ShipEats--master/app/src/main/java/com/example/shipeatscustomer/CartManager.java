package com.example.shipeatscustomer;

import java.util.ArrayList;
import java.util.List;

public class CartManager {
    private static CartManager instance;
    private List<CartItem> cartItems;

    private CartManager() {
        cartItems = new ArrayList<>();
    }

    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public void addToCart(FoodItem foodItem, int quantity, String specialInstructions, boolean wantCutlery) {
        for (CartItem item : cartItems) {
            if (item.getFoodItem().getId().equals(foodItem.getId())) {
                int newQuantity = item.getQuantity() + quantity;
                // Cap at stock availability if needed, or 10 as per UI logic
                if (newQuantity > 10) newQuantity = 10;
                if (newQuantity > foodItem.getQuantity()) newQuantity = foodItem.getQuantity();
                
                item.setQuantity(newQuantity);
                if (specialInstructions != null && !specialInstructions.isEmpty()) {
                    item.setSpecialInstructions(specialInstructions);
                }
                item.setWantCutlery(wantCutlery);
                return;
            }
        }
        cartItems.add(new CartItem(foodItem, quantity, specialInstructions, wantCutlery));
    }

    public void addToCart(FoodItem foodItem, int quantity) {
        addToCart(foodItem, quantity, "", false);
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void clearCart() {
        cartItems.clear();
    }

    public int getCartCount() {
        int count = 0;
        for (CartItem item : cartItems) {
            count += item.getQuantity();
        }
        return count;
    }
}
