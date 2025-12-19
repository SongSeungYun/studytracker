package com.example.studytrackerviewer.ui.history;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studytrackerviewer.R;
import com.example.studytrackerviewer.data.model.StudySession;
import com.example.studytrackerviewer.data.network.ApiClient;
import com.example.studytrackerviewer.data.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";

    private ApiService apiService;
    private RecyclerView recyclerView;
    private HistoryAdapter historyAdapter;
    private final List<StudySession> sessionList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        // ApiService 초기화
        apiService = ApiClient.getApiService(getContext());

        // RecyclerView 설정
        recyclerView = view.findViewById(R.id.historyRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 어댑터 설정 (초기에는 빈 리스트로 설정)
        historyAdapter = new HistoryAdapter(sessionList, session -> {
            Intent intent = new Intent(getContext(), HistoryDetailActivity.class);
            // session.getId()를 "sessionId"라는 키로 전달
            intent.putExtra("sessionId", session.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(historyAdapter);

        // 서버에서 히스토리 데이터 로드
        fetchHistorySessions();

        return view;
    }

    private void fetchHistorySessions() {
        // date 쿼리를 null로 전달하여 모든 세션 목록을 가져옴
        apiService.getHistorySessions(null).enqueue(new Callback<List<StudySession>>() {
            @Override
            public void onResponse(@NonNull Call<List<StudySession>> call, @NonNull Response<List<StudySession>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessionList.clear();
                    sessionList.addAll(response.body());
                    historyAdapter.notifyDataSetChanged(); // 어댑터에 데이터 변경 알림
                    Log.d(TAG, "Sessions loaded: " + response.body().size());
                } else {
                    showErrorToast("Failed to load history sessions. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<StudySession>> call, @NonNull Throwable t) {
                showErrorToast(t.getMessage());
            }
        });
    }

    private void showErrorToast(String message) {
        Log.e(TAG, "API Error: " + message);
        if (getContext() != null) {
            Toast.makeText(getContext(), "Failed to load data: " + message, Toast.LENGTH_SHORT).show();
        }
    }
}
