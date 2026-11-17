package com.example.miniproject_reviewhub;

import com.google.firebase.Timestamp;

public class Review {
    private String id;
    private String userId;
    private String reviewerName;
    private String itemTitle;
    private String itemDetail;
    private String reviewText;
    private int rating;
    private Timestamp createdAt;

    public Review() {
        // Empty constructor required by Firestore
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public String getItemDetail() {
        return itemDetail;
    }

    public String getReviewText() {
        return reviewText;
    }

    public int getRating() {
        return rating;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }
}