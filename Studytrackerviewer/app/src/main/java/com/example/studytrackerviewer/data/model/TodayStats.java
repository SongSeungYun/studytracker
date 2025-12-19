package com.example.studytrackerviewer.data.model;

import com.google.gson.annotations.SerializedName;

public class TodayStats {

    @SerializedName("date")
    private String date;

    @SerializedName("study_minutes")
    private int studyMinutes;

    @SerializedName("distracted_minutes")
    private int distractedMinutes;

    @SerializedName("away_minutes")
    private int awayMinutes;

    @SerializedName("focus_rate")
    private float focusRate;

    // Getters
    public String getDate() { return date; }
    public int getStudyMinutes() { return studyMinutes; }
    public int getDistractedMinutes() { return distractedMinutes; }
    public int getAwayMinutes() { return awayMinutes; }
    public float getFocusRate() { return focusRate; }
}
