package com.example.shipeatscustomer;

import java.io.Serializable;
import java.util.List;

public class AdminOrderModel implements Serializable {
    public String orderId;
    public String items;
    public List<CartItem> orderItems;
    public String totalPrice;
    public String status;
    public String customerName;
    public String customerId;
    public Long itemCount;
    public String date;
    public String paymentMethod;
    public String pickupTime;
    public String pickupPin;
    public boolean hiddenByCustomer = false;

    public AdminOrderModel() {
        // Required for Firebase
    }

    // Use this ONE constructor for everything
    public AdminOrderModel(String orderId, String items, List<CartItem> orderItems, String totalPrice, String status, Long itemCount, String customerName, String customerId, String date, String paymentMethod, String pickupTime, String pickupPin) {
        this.orderId = orderId;
        this.items = items;
        this.orderItems = orderItems;
        this.totalPrice = totalPrice;
        this.status = status;
        this.itemCount = itemCount;
        this.customerName = customerName;
        this.customerId = customerId;
        this.date = date;
        this.paymentMethod = paymentMethod;
        this.pickupTime = pickupTime;
        this.pickupPin = pickupPin;
        this.hiddenByCustomer = false;
    }

    public String getPickupPin() { return pickupPin; }
    public void setPickupPin(String pickupPin) { this.pickupPin = pickupPin; }
}