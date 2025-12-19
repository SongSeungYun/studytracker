package com.example.studytrackerviewer.data.model;

import com.google.gson.annotations.SerializedName;

public class AuthToken {
    @SerializedName("token")
    private String token;

    public String getToken() {
        return token;
    }
}
