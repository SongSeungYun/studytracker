package com.example.studytrackerviewer.data.model;

import com.google.gson.annotations.SerializedName;

public class StudyEvent {

    @SerializedName("id")
    private int id;

    @SerializedName("session")
    private int sessionId;

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("state")
    private String state;

    @SerializedName("confidence")
    private float confidence;

    // Getters
    public int getId() { return id; }
    public int getSessionId() { return sessionId; }
    public String getTimestamp() { return timestamp; }
    public String getState() { return state; }
    public float getConfidence() { return confidence; }
}
