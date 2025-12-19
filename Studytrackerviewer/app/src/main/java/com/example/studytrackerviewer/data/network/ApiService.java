package com.example.studytrackerviewer.data.network;

import com.example.studytrackerviewer.data.model.AuthToken;
import com.example.studytrackerviewer.data.model.DateStat;
import com.example.studytrackerviewer.data.model.SessionDetail;
import com.example.studytrackerviewer.data.model.StudyEvent;
import com.example.studytrackerviewer.data.model.StudyImage;
import com.example.studytrackerviewer.data.model.StudySession;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // --- Authentication ---
    @POST("api/token/")
    Call<AuthToken> login(@Body RequestBody body);

    // --- Session Management ---
    @GET("api/study/sessions/active/")
    Call<StudySession> getActiveSession();

    @POST("api/study/sessions/")
    Call<StudySession> startSession(@Body RequestBody body);

    @POST("api/study/sessions/{id}/end/")
    Call<StudySession> endSession(@Path("id") int sessionId);

    @PATCH("api/study/sessions/{id}/objects/")
    Call<StudySession> updateAllowedObjects(
            @Path("id") int sessionId,
            @Body RequestBody body
    );

    // --- Edge Data (YOLO) ---
    @POST("api/edge/events/")
    Call<StudyEvent> createStudyEvent(@Body RequestBody body);

    @Multipart
    @POST("api/edge/images/")
    Call<StudyImage> createStudyImage(
            @Part("session") RequestBody session,
            @Part("event") RequestBody event,
            @Part("captured_at") RequestBody capturedAt,
            @Part MultipartBody.Part image
    );

    // --- Stats / History ---
    @GET("api/stats/today/")
    Call<DateStat> getTodayStats();

    @GET("api/stats/")
    Call<List<DateStat>> getRangeStats(@Query("range") String range);

    @GET("api/history/sessions/")
    Call<List<StudySession>> getHistorySessions(@Query("date") String date);

    @GET("api/history/session/{id}/timeline/")
    Call<SessionDetail> getSessionTimeline(@Path("id") int sessionId);
}
