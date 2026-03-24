package com.example.shipeatscustomer;

public class AdminProfile {
    public String name, email, phone, location, imageUrl;
    public String startHour, startMinute, endHour, endMinute;

    public AdminProfile() { } // Required for Firebase

    public AdminProfile(String name, String email, String phone, String location, String imageUrl,
                        String startHour, String startMinute, String endHour, String endMinute) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.location = location;
        this.imageUrl = imageUrl;
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.endHour = endHour;
        this.endMinute = endMinute;
    }
}