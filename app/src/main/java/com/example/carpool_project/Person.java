package com.example.carpool_project;

public class Person {
    public String userId;
    public String password;
    public String phoneNumber;

    public String name;
    public String email;
    public String role; 
    public String city;
    public String country;
    public String profileImageUrl; 
    
    public String workplace; 
    public String workplaceId;
    public String workplaceEmail;
    public String workplaceAddress;
    public double workplaceLat;
    public double workplaceLng;

    public String homeAddress;
    public double homeLat;
    public double homeLng;

    public Person() {}

    public Person(String userId, String name, String email, String role, String profileImageUrl, String password, String phoneNumber, String city, String country) {
        this.userId = userId;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.email = email;
        this.role = role;
        this.city = city;
        this.country = country;
        this.profileImageUrl = profileImageUrl;
    }

    public Person(String userId, String name, String email, String role, String profileImageUrl, String password, String phoneNumber, String city, String country, String workplace, String workplaceAddress, String homeAddress) {
        this(userId, name, email, role, profileImageUrl, password, phoneNumber, city, country);
        this.workplace = workplace;
        this.workplaceAddress = workplaceAddress;
        this.homeAddress = homeAddress;
    }
}
