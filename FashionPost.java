package com.onecodeman.driply.models;

public class FashionPost {
    private String postId; // New field to track the unique ID of the post
    private String imageUrl;
    private String description;
    private String posterId;
    private int likes;
    private long timestamp;

    // Empty constructor for Firebase
    public FashionPost() {}

    // Constructor
    public FashionPost(String postId, String imageUrl, String description, String posterId, int likes, long timestamp) {
        this.postId = postId;
        this.imageUrl = imageUrl;
        this.description = description;
        this.posterId = posterId;
        this.likes = likes;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPosterId() {
        return posterId;
    }

    public void setPosterId(String posterId) {
        this.posterId = posterId;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
