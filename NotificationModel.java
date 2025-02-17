package com.onecodeman.driply.models;

import androidx.annotation.NonNull;

public class NotificationModel {
    private @NonNull String title;
    private @NonNull String message;
    private long timestamp;

    // No-argument constructor (required for Firebase)
    public NotificationModel() {
        this.title = "";
        this.message = "";
        this.timestamp = System.currentTimeMillis();
    }

    // Parameterized constructor
    public NotificationModel(@NonNull String title, @NonNull String message, long timestamp) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public @NonNull String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        this.title = title;
    }

    public @NonNull String getMessage() {
        return message;
    }

    public void setMessage(@NonNull String message) {
        if (message.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "NotificationModel{" +
                "title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
