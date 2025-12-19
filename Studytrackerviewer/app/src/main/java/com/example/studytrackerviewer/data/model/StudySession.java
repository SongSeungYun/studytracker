package com.example.studytrackerviewer.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StudySession {

    @SerializedName("id")
    private int id;

    @SerializedName("user")
    private int userId;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("allowed_objects")
    private List<String> allowedObjects;

    @SerializedName("total_duration_sec")
    private int totalDurationSec;

    @SerializedName("study_duration_sec")
    private int studyDurationSec;

    @SerializedName("distracted_duration_sec")
    private int distractedDurationSec;

    @SerializedName("away_duration_sec")
    private int awayDurationSec;

    @SerializedName("created_at")
    private String createdAt;

    // --- New fields for active session status and dynamic durations ---
    @SerializedName("current_status")
    private String currentStatus;

    @SerializedName("current_total_duration_sec")
    private int currentTotalDurationSec;

    @SerializedName("current_study_duration_sec")
    private int currentStudyDurationSec;

    @SerializedName("current_distracted_duration_sec")
    private int currentDistractedDurationSec;

    @SerializedName("current_away_duration_sec")
    private int currentAwayDurationSec;

    // Getters
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public List<String> getAllowedObjects() { return allowedObjects; }
    public int getTotalDurationSec() { return totalDurationSec; }
    public int getStudyDurationSec() { return studyDurationSec; }
    public int getDistractedDurationSec() { return distractedDurationSec; }
    public int getAwayDurationSec() { return awayDurationSec; }
    public String getCreatedAt() { return createdAt; }

    // --- New Getters for active session status and dynamic durations ---
    public String getCurrentStatus() { return currentStatus; }
    public int getCurrentTotalDurationSec() { return currentTotalDurationSec; }
    public int getCurrentStudyDurationSec() { return currentStudyDurationSec; }
    public int getCurrentDistractedDurationSec() { return currentDistractedDurationSec; }
    public int getCurrentAwayDurationSec() { return currentAwayDurationSec; }
}

