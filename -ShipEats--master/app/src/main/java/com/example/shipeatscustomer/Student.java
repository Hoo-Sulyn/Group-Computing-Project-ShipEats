package com.example.shipeatscustomer;

import java.io.Serializable;

public class Student implements Serializable {
    private String name, gender, dob, phone, email, profileImageUri;

    public Student(String name, String gender, String dob, String phone, String email, String profileImageUri) {
        this.name = name;
        this.gender = gender;
        this.dob = dob;
        this.phone = phone;
        this.email = email;
        this.profileImageUri = profileImageUri;
    }

    public String getName() {
        return name; }
    public String getGender() {
        return gender; }
    public String getDob() {
        return dob; }
    public String getPhone() {
        return phone; }
    public String getEmail() {
        return email; }
    public String getProfileImageUri() {
        return profileImageUri; }
}