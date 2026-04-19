package com.example.carpool_project;

public class Ride {
    public String rideId;
    public String driverId;
    public String origin;
    public String destination;
    public String time;
    public String recurringDays; // e.g., "Mon, Wed, Fri"
    public int seatsAvailable;


    public Ride() {} // Required for Firebase

    public Ride(String rideId, String driverId, String origin, String destination, String time, String recurringDays, int seatsAvailable) {
        this.rideId = rideId;
        this.driverId = driverId;
        this.seatsAvailable = seatsAvailable;
        this.origin = origin;
        this.destination = destination;
        this.time = time;
        this.recurringDays = recurringDays;
    }
}