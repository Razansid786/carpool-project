package com.example.carpool_project;

import java.io.Serializable;

public class BookingOffer implements Serializable {
    public String offerId;
    public String rideId;
    public String passengerId;
    public String passengerName;
    public String passengerEmail;
    public String driverId;
    public double pickupLat, pickupLng;
    public double dropoffLat, dropoffLng;
    public String pickupAddress, dropoffAddress;
    public double offeredPrice;
    public int seatsRequested;
    public String status; // pending, accepted, rejected
    public String rideFlowStatus; // starting, ongoing, completed
    public long timestamp;

    public BookingOffer() {}

    public BookingOffer(String offerId, String rideId, String passengerId, String passengerName, 
                        String passengerEmail, String driverId, double pickupLat, double pickupLng, 
                        double dropoffLat, double dropoffLng, String pickupAddress, String dropoffAddress, 
                        double offeredPrice, int seatsRequested, String status, long timestamp) {
        this.offerId = offerId;
        this.rideId = rideId;
        this.passengerId = passengerId;
        this.passengerName = passengerName;
        this.passengerEmail = passengerEmail;
        this.driverId = driverId;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.dropoffLat = dropoffLat;
        this.dropoffLng = dropoffLng;
        this.pickupAddress = pickupAddress;
        this.dropoffAddress = dropoffAddress;
        this.offeredPrice = offeredPrice;
        this.seatsRequested = seatsRequested;
        this.status = status;
        this.rideFlowStatus = "idle";
        this.timestamp = timestamp;
    }
}
