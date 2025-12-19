package com.example.studytrackerviewer.ui.history;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studytrackerviewer.R;
import com.example.studytrackerviewer.data.model.SessionDetail;
import com.example.studytrackerviewer.data.model.TimelineItem;
import com.example.studytrackerviewer.data.network.ApiClient;
import com.example.studytrackerviewer.data.network.ApiService;
import com.example.studytrackerviewer.ui.main.MainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryDetailActivity extends AppCompatActivity {

    private static final String TAG = "HistoryDetailActivity";

    private ApiService apiService;
    private TextView detailDate, detailSummary;
    private RecyclerView timelineRecyclerView;
    private TimelineAdapter timelineAdapter;
    private final List<TimelineItem> timelineItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);

        // UI 요소 바인딩
        detailDate = findViewById(R.id.detailDate);
        detailSummary = findViewById(R.id.detailSummary);
        timelineRecyclerView = findViewById(R.id.timelineRecyclerView);

        // ApiService 초기화
        apiService = ApiClient.getApiService(getApplicationContext());

        // RecyclerView 및 어댑터 설정
        timelineRecyclerView.setNestedScrollingEnabled(false); // Add this line for smooth scrolling
        timelineRecyclerView.setLayoutManager(new NonScrollableLinearLayoutManager(this));
        timelineAdapter = new TimelineAdapter(this, timelineItems);
        timelineRecyclerView.setAdapter(timelineAdapter);

        // 인텐트로부터 sessionId 가져오기
        int sessionId = getIntent().getIntExtra("sessionId", -1);

        if (sessionId != -1) {
            fetchSessionDetails(sessionId);
        } else {
            showErrorToast("Invalid Session ID.");
            finish(); // 세션 ID가 없으면 액티비티 종료
        }

        // 하단 네비게이션 설정
        setupBottomNavigation();
    }

    private void fetchSessionDetails(int sessionId) {
        apiService.getSessionTimeline(sessionId).enqueue(new Callback<SessionDetail>() {
            @Override
            public void onResponse(@NonNull Call<SessionDetail> call, @NonNull Response<SessionDetail> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SessionDetail sessionDetail = response.body();
                    updateUI(sessionDetail);
                } else {
                    showErrorToast("Failed to load session details. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<SessionDetail> call, @NonNull Throwable t) {
                showErrorToast(t.getMessage());
            }
        });
    }

    private void updateUI(SessionDetail sessionDetail) {
        // 상단 정보 업데이트
        try {
            OffsetDateTime odt = OffsetDateTime.parse(sessionDetail.getStartTime());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault());
            detailDate.setText(odt.format(formatter));
        } catch (Exception e) {
            detailDate.setText(sessionDetail.getStartTime());
        }

        int totalSeconds = sessionDetail.getTotalDurationSec();
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        String summary;
        if (hours > 0) {
            summary = String.format(Locale.getDefault(), "총 공부시간 %d시간 %d분", hours, minutes);
        } else {
            summary = String.format(Locale.getDefault(), "총 공부시간 %d분", minutes);
        }
        detailSummary.setText(summary);

        // 타임라인 업데이트
        timelineItems.clear();
        if (sessionDetail.getTimeline() != null) {
            timelineItems.addAll(sessionDetail.getTimeline());
        }
        timelineAdapter.notifyDataSetChanged();
    }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setSelectedItemId(R.id.nav_history); // 현재 화면이 히스토리임을 표시

        nav.setOnItemSelectedListener(item -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                intent.putExtra("tab", "home");
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_stats) {
                intent.putExtra("tab", "stats");
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_history) {
                // 이미 히스토리 탭에 속하므로, 메인 액티비티의 히스토리 프래그먼트로 돌아감
                intent.putExtra("tab", "history");
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private void showErrorToast(String message) {
        Log.e(TAG, "API Error: " + message);
        Toast.makeText(this, "Failed to load data: " + message, Toast.LENGTH_SHORT).show();
    }
}
