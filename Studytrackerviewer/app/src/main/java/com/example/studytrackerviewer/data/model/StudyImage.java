package com.example.studytrackerviewer.data.model;

import com.google.gson.annotations.SerializedName;

public class StudyImage {

    @SerializedName("id")
    private int id;

    @SerializedName("session")
    private int sessionId;

    @SerializedName("event")
    private Integer eventId; // Can be null

    @SerializedName("image")
    private String imageUrl; // URL to the uploaded image

    @SerializedName("captured_at")
    private String capturedAt;

    // Getters
    public int getId() { return id; }
    public int getSessionId() { return sessionId; }
    public Integer getEventId() { return eventId; }
    public String getImageUrl() { return imageUrl; }
    public String getCapturedAt() { return capturedAt; }
}
