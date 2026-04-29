package com.example.carpool_project;

import java.io.Serializable;

public class Notification implements Serializable {
    public String id;
    public String recipientId;
    public String title;
    public String message;
    public long timestamp;
    public boolean read;

    public Notification() {}

    public Notification(String id, String recipientId, String title, String message, long timestamp) {
        this.id = id;
        this.recipientId = recipientId;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.read = false;
    }
}
