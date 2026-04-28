package com.example.carpool_project;

public class ChatMessage {
    public String senderId;
    public String message;
    public long timestamp;

    public ChatMessage() {}

    public ChatMessage(String senderId, String message, long timestamp) {
        this.senderId = senderId;
        this.message = message;
        this.timestamp = timestamp;
    }
}
