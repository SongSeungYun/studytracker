package com.example.studytrackerviewer.data.model;

import com.google.gson.annotations.SerializedName;

public class TimelineItem {

    @SerializedName("time")
    private String time;

    @SerializedName("type")
    private String type; // "event" 또는 "image"

    @SerializedName("state")
    private String state; // "STUDY", "DISTRACTED", "AWAY", 또는 null

    @SerializedName("confidence")
    private Float confidence;

    @SerializedName("image_url")
    private String imageUrl;

    // Getters
    public String getTime() { return time; }
    public String getType() { return type; }
    public String getState() { return state; }
    public Float getConfidence() { return confidence; }
    public String getImageUrl() { return imageUrl; }
}
