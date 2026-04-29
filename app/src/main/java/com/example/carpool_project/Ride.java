package com.example.carpool_project;

import java.io.Serializable;

public class Ride implements Serializable {
    private static final long serialVersionUID = 1L;

    public String rideId;
    public String driverId;
    public String driverName;
    public String driverImageUrl;
    public String driverPhone;
    public double driverRating;
    public String origin;
    public String destination;
    public double startLat, startLng;
    public double endLat, endLng;
    public String time;
    public String recurringDays;
    public int seatsAvailable;
    public String status;

    public Ride() {}

    public Ride(String rideId, String driverId, String driverName, String driverImageUrl, String driverPhone, 
                double driverRating, String origin, String destination, double startLat, double startLng,
                double endLat, double endLng, String time, String recurringDays, 
                int seatsAvailable, String status) {
        this.rideId = rideId;
        this.driverId = driverId;
        this.driverName = driverName;
        this.driverImageUrl = driverImageUrl;
        this.driverPhone = driverPhone;
        this.driverRating = driverRating;
        this.origin = origin;
        this.destination = destination;
        this.startLat = startLat;
        this.startLng = startLng;
        this.endLat = endLat;
        this.endLng = endLng;
        this.time = time;
        this.recurringDays = recurringDays;
        this.seatsAvailable = seatsAvailable;
        this.status = status;
    }

    // Constructor for PostRideFragment which doesn't provide lat/lng
    public Ride(String rideId, String driverId, String driverName, String driverImageUrl, String driverPhone, 
                double driverRating, String origin, String destination, String time, String recurringDays, 
                int seatsAvailable, String status) {
        this.rideId = rideId;
        this.driverId = driverId;
        this.driverName = driverName;
        this.driverImageUrl = driverImageUrl;
        this.driverPhone = driverPhone;
        this.driverRating = driverRating;
        this.origin = origin;
        this.destination = destination;
        this.time = time;
        this.recurringDays = recurringDays;
        this.seatsAvailable = seatsAvailable;
        this.status = status;
        this.startLat = 0.0;
        this.startLng = 0.0;
        this.endLat = 0.0;
        this.endLng = 0.0;
    }
}
