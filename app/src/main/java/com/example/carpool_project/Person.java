package com.example.carpool_project;

public class Person {
    public String userId;
    public String password;
    public String phoneNumber;

    public String name;
    public String email;
    public String role; // e.g., "Student", "Office Worker"
    public String profileImageUrl; 
    
    // New fields for security and info
    public String workplace; // University or Office Name
    public String workplaceId;
    public String workplaceEmail;
    public String workplaceAddress;

    public Person() {}

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
