package com.onecodeman.driply.models;

public class User {
    private String username;
    private int points;
    private String profilePicUrl;
    private int dailyPosts;
    private String lastReset;

    // Default constructor for Firebase
    public User() {}

    public User(String username, int points, String profilePicUrl, int dailyPosts, String lastReset) {
        this.username = username;
        this.points = points;
        this.profilePicUrl = profilePicUrl;
        this.dailyPosts = dailyPosts;
        this.lastReset = lastReset;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public void setProfilePicUrl(String profilePicUrl) {
        this.profilePicUrl = profilePicUrl;
    }

    public int getDailyPosts() {
        return dailyPosts;
    }

    public void setDailyPosts(int dailyPosts) {
        this.dailyPosts = dailyPosts;
    }

    public String getLastReset() {
        return lastReset;
    }

    public void setLastReset(String lastReset) {
        this.lastReset = lastReset;
    }
}
