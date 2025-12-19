package com.example.studytrackerviewer.data.network;

import android.content.Context; // Needed for AuthManager.getInstance(context)
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient; // Needed for OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor; // Optional: For logging network requests
import okhttp3.Interceptor; // Needed for Interceptor
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException; // Needed for IOException in intercept method

import com.example.studytrackerviewer.util.AuthManager; // Needed to get the token

public class ApiClient {

    // private static final String BASE_URL = "http://10.0.2.2:8000/"; // For emulator to reach host PC
    private static final String BASE_URL = "https://seungyun3.pythonanywhere.com/";
    
    

    private static Retrofit retrofit = null;

    // Modified getClient to take Context
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            // Create an Interceptor to add the Authorization header
            Interceptor authInterceptor = chain -> {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder();

                // Get the token from AuthManager
                String token = AuthManager.getInstance(context).getToken();
                if (token != null) {
                    requestBuilder.header("Authorization", "Token " + token);
                }
                Request request = requestBuilder.build();
                return chain.proceed(request);
            };

            // Optional: Add logging interceptor for debugging network requests
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // Log request and response bodies

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(loggingInterceptor) // Add logging for debugging
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client) // Set the OkHttpClient with interceptors
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    // Modified getApiService to take Context
    public static ApiService getApiService(Context context) {
        return getClient(context).create(ApiService.class);
    }
}
