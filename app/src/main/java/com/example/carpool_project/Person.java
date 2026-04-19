package com.example.carpool_project;

public class Person {
    public String userId;
    public String password;
    public String phoneNumber;

    public String name;
    public String email;
    public String role; // e.g., "Student", "Office Worker"
    public String profileImageUrl; // URL to the image stored in Firebase Storage

    public Person() {} // Required for Firebase

    public Person(String userId, String name, String email, String role, String profileImageUrl, String password, String phoneNumber) {
        this.userId = userId;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.email = email;
        this.role = role;
        this.profileImageUrl = profileImageUrl;
    }
}
