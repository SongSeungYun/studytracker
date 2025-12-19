package com.example.studytrackerviewer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.studytrackerviewer.data.model.AuthToken;
import com.example.studytrackerviewer.data.network.ApiClient;
import com.example.studytrackerviewer.data.network.ApiService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthManager {

    private static final String TAG = "AuthManager";
    private static final String PREF_NAME = "StudyTrackerPrefs";
    private static final String KEY_AUTH_TOKEN = "authToken";
    private static final String ADMIN_USERNAME = "admin"; // Hardcoded admin username
    private static final String ADMIN_PASSWORD = "1234"; // Hardcoded admin password (MUST BE SECURED IN PRODUCTION)

    private static AuthManager instance;
    private SharedPreferences prefs;
    private String currentToken;
    private ApiService apiService; // For making the login API call

    private AuthManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        currentToken = prefs.getString(KEY_AUTH_TOKEN, null);
        apiService = ApiClient.getApiService(context);
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context.getApplicationContext());
        }
        return instance;
    }

    public String getToken() {
        return currentToken;
    }

    public boolean isLoggedIn() {
        return currentToken != null;
    }

    // This method will perform the login and store the token
    public void login(AuthCallback callback) {
        if (isLoggedIn()) {
            // Already logged in, just report success
            if (callback != null) {
                callback.onSuccess(currentToken);
            }
            return;
        }

        Log.d(TAG, "Attempting to log in as admin...");
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("username", ADMIN_USERNAME);
        jsonBody.addProperty("password", ADMIN_PASSWORD);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonBody.toString());

        apiService.login(body).enqueue(new Callback<AuthToken>() {
            @Override
            public void onResponse(@NonNull Call<AuthToken> call, @NonNull Response<AuthToken> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentToken = response.body().getToken();
                    prefs.edit().putString(KEY_AUTH_TOKEN, currentToken).apply();
                    Log.d(TAG, "Admin login successful. Token: " + currentToken);
                    if (callback != null) {
                        callback.onSuccess(currentToken);
                    }
                } else {
                    Log.e(TAG, "Admin login failed: " + response.code());
                    String errorMessage = "Login failed: ";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage += response.errorBody().string();
                        } else {
                            errorMessage += response.message();
                        }
                    } catch (IOException e) {
                        errorMessage += "Could not read error body: " + e.getMessage();
                    }
                    
                    if (callback != null) {
                        callback.onFailure(errorMessage);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthToken> call, @NonNull Throwable t) {
                Log.e(TAG, "Admin login API call failed: " + t.getMessage());
                if (callback != null) {
                    callback.onFailure("Login failed: " + t.getMessage());
                }
            }
        });
    }

    public void logout() {
        currentToken = null;
        prefs.edit().remove(KEY_AUTH_TOKEN).apply();
        Log.d(TAG, "Logged out.");
    }

    public interface AuthCallback {
        void onSuccess(String token);
        void onFailure(String errorMessage);
    }
}
