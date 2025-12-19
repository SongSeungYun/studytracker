package com.example.studytrackerviewer.ui.home;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.studytrackerviewer.R;
import com.example.studytrackerviewer.data.model.StudySession;
import com.example.studytrackerviewer.data.network.ApiClient;
import com.example.studytrackerviewer.data.network.ApiService;
import com.example.studytrackerviewer.util.AuthManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final long REFRESH_INTERVAL = 5000; // 5 seconds

    private ApiService apiService;
    private AuthManager authManager;
    private StudySession currentActiveSession;

    // UI elements - Correctly mapped to fragment_home.xml IDs
    private TextView currentStatusLabelTextView; // "현재 상태" 라벨 (e.g., "현재 상태" / "세션 없음")
    private TextView currentStatusMessageTextView; // 동적 메시지 (e.g., "집중 중" / "새로운 공부 세션을 시작하세요.")
    private TextView studyTimeTextView; // "순수 공부" 시간
    private TextView distractedTimeTextView; // "산만" 시간
    private LinearLayout allowedObjectsContainer; // Checkboxes will be dynamically added/managed here
    private Button startStopStudyButton;

    // Map to hold references to dynamically created/bound checkboxes
    private final Map<String, CheckBox> objectCheckboxes = new HashMap<>();

    // List of all possible objects the app knows about, to create checkboxes
    private final List<String> allInternalObjects = Arrays.asList(
            "book",
            "laptop",
            "cell phone"
    ); // Internal (English) names

    // Mapping for display names (Korean) to internal names (English)
    private final Map<String, String> internalNameToDisplayName = new HashMap<>();

    private Handler handler;
    private Runnable refreshRunnable;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize ApiService and AuthManager
        apiService = ApiClient.getApiService(getContext());
        authManager = AuthManager.getInstance(getContext());

        // Initialize the mapping
        initDisplayNameMap();

        // Bind UI elements
        bindViews(v);

        // Setup Checkboxes - Dynamic setup based on allInternalObjects
        setupAllowedObjectsCheckboxes();

        // Set listeners
        startStopStudyButton.setOnClickListener(view -> toggleStudySession());

        // Setup periodic refresh
        handler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchActiveSession(); // Fetch data
                handler.postDelayed(this, REFRESH_INTERVAL); // Schedule next run
            }
        };

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.post(refreshRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
    }

    private void initDisplayNameMap() {
        internalNameToDisplayName.put("book", "책");
        internalNameToDisplayName.put("cell phone", "스마트폰");
        internalNameToDisplayName.put("laptop", "노트북/태블릿");
    }

    private String getDisplayName(String internalName) {
        return internalNameToDisplayName.getOrDefault(
                internalName,
                internalName
        );
    }

    private void bindViews(View v) {
        currentStatusLabelTextView =
                v.findViewById(R.id.currentStatusLabelTextView);
        currentStatusMessageTextView =
                v.findViewById(R.id.currentStatusMessageTextView);
        studyTimeTextView =
                v.findViewById(R.id.studyTimeTextView);
        distractedTimeTextView =
                v.findViewById(R.id.distractedTimeTextView);
        allowedObjectsContainer =
                v.findViewById(R.id.allowedObjectsContainer);
        startStopStudyButton =
                v.findViewById(R.id.startStopStudyButton);
    }

    private void setupAllowedObjectsCheckboxes() {
        if (allowedObjectsContainer != null) {
            allowedObjectsContainer.removeAllViews();
            objectCheckboxes.clear();
        }

        for (String objName : allInternalObjects) {
            CheckBox cb = new CheckBox(getContext());
            cb.setText(getDisplayName(objName));
            cb.setId(View.generateViewId());
            cb.setTextColor(Color.parseColor("#111111"));
            cb.setButtonTintList(
                    getResources().getColorStateList(
                            R.color.checkbox_selector,
                            null
                    )
            );
            cb.setOnCheckedChangeListener(this::onObjectCheckboxChanged);

            if (allowedObjectsContainer != null) {
                allowedObjectsContainer.addView(cb);
            }

            objectCheckboxes.put(objName, cb);
        }
    }

    // Listener for when an allowed object checkbox changes state
    private void onObjectCheckboxChanged(
            CompoundButton buttonView,
            boolean isChecked
    ) {
        if (currentActiveSession != null && isResumed()) {
            updateAllowedObjectsOnServer();
        } else if (currentActiveSession == null) {
            showErrorToast("세션 시작 후 설정이 적용됩니다.");
        }
    }

    private void fetchActiveSession() {
        apiService.getActiveSession().enqueue(new Callback<StudySession>() {
            @Override
            public void onResponse(
                    @NonNull Call<StudySession> call,
                    @NonNull Response<StudySession> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    currentActiveSession = response.body();
                    updateUIWithSession(currentActiveSession);
                    startStopStudyButton.setText("공부 종료");
                } else if (response.code() == 404) {
                    currentActiveSession = null;
                    updateUIForNoActiveSession();
                    startStopStudyButton.setText("공부 시작");
                } else {
                    showErrorToast("활성화된 세션 로드 실패: " + response.code());
                    try {
                        Log.e(TAG, "Response: " + response.errorBody().string());
                    } catch (IOException e) {
                        Log.e(TAG, "Response: Could not read error body: " + e.getMessage());
                    }
                    updateUIForNoActiveSession();
                    startStopStudyButton.setText("공부 시작");
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<StudySession> call,
                    @NonNull Throwable t
            ) {
                showErrorToast("API 요청 실패: " + t.getMessage());
                Log.e(TAG, "Error fetching active session: " + t.getMessage());
                updateUIForNoActiveSession();
                startStopStudyButton.setText("공부 시작");
            }
        });
    }

    private void updateUIWithSession(StudySession session) {
        currentStatusLabelTextView.setText("현재 상태");
        currentStatusMessageTextView.setText(session.getCurrentStatus());

        studyTimeTextView.setText(
                formatTime(session.getCurrentStudyDurationSec())
        );
        distractedTimeTextView.setText(
                formatTime(session.getCurrentDistractedDurationSec())
        );

        for (Map.Entry<String, CheckBox> entry : objectCheckboxes.entrySet()) {
            CheckBox cb = entry.getValue();
            if (cb != null) {
                cb.setOnCheckedChangeListener(null);
                cb.setChecked(
                        session.getAllowedObjects().contains(entry.getKey())
                );
                cb.setOnCheckedChangeListener(this::onObjectCheckboxChanged);
            }
        }
    }

    private void updateUIForNoActiveSession() {
        currentStatusLabelTextView.setText("세션 없음");
        currentStatusMessageTextView.setText("공부를 시작해주세요");
        studyTimeTextView.setText(formatTime(0));
        distractedTimeTextView.setText(formatTime(0));

        for (Map.Entry<String, CheckBox> entry : objectCheckboxes.entrySet()) {
            CheckBox cb = entry.getValue();
            if (cb != null) {
                cb.setOnCheckedChangeListener(null);
                cb.setChecked(false);
                cb.setOnCheckedChangeListener(this::onObjectCheckboxChanged);
            }
        }
    }

    private String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;

        if (hours > 0) {
            return String.format(
                    Locale.getDefault(),
                    "%d시간 %d분",
                    hours,
                    minutes
            );
        } else {
            return String.format(
                    Locale.getDefault(),
                    "%d분",
                    minutes
            );
        }
    }

    private void toggleStudySession() {
        String token = authManager.getToken();

        if (token == null) {
            showErrorToast("인증 토큰이 없습니다. 앱을 재시작해주세요.");
            return;
        }

        if (currentActiveSession != null) {
            showErrorToast("세션 종료 중...");
            apiService.endSession(
                    currentActiveSession.getId()
            ).enqueue(new Callback<StudySession>() {
                @Override
                public void onResponse(
                        @NonNull Call<StudySession> call,
                        @NonNull Response<StudySession> response
                ) {
                    if (response.isSuccessful()) {
                        showErrorToast("세션 종료 성공!");
                        currentActiveSession = null;
                        updateUIForNoActiveSession();
                        startStopStudyButton.setText("공부 시작");
                    }
                }

                @Override
                public void onFailure(
                        @NonNull Call<StudySession> call,
                        @NonNull Throwable t
                ) {
                    showErrorToast("세션 종료 API 요청 실패: " + t.getMessage());
                }
            });
        } else {
            List<String> selectedObjects = new ArrayList<>();

            for (Map.Entry<String, CheckBox> entry : objectCheckboxes.entrySet()) {
                if (entry.getValue() != null && entry.getValue().isChecked()) {
                    selectedObjects.add(entry.getKey());
                }
            }

            if (selectedObjects.isEmpty()) {
                showErrorToast("공부 오브젝트를 하나 이상 선택해주세요.");
                return;
            }

            showErrorToast("세션 시작 중...");
            JsonObject jsonBody = new JsonObject();
            jsonBody.add(
                    "allowed_objects",
                    new Gson().toJsonTree(selectedObjects)
            );
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    jsonBody.toString()
            );

            apiService.startSession(body)
                    .enqueue(new Callback<StudySession>() {
                        @Override
                        public void onResponse(
                                @NonNull Call<StudySession> call,
                                @NonNull Response<StudySession> response
                        ) {
                            if (response.isSuccessful() && response.body() != null) {
                                currentActiveSession = response.body();
                                updateUIWithSession(currentActiveSession);
                                startStopStudyButton.setText("공부 종료");
                                showErrorToast("새 세션 시작 성공!");
                            }
                        }

                        @Override
                        public void onFailure(
                                @NonNull Call<StudySession> call,
                                @NonNull Throwable t
                        ) {
                            showErrorToast("세션 시작 API 요청 실패: " + t.getMessage());
                        }
                    });
        }
    }

    private void updateAllowedObjectsOnServer() {
        List<String> selectedObjects = new ArrayList<>();

        for (Map.Entry<String, CheckBox> entry : objectCheckboxes.entrySet()) {
            if (entry.getValue() != null && entry.getValue().isChecked()) {
                selectedObjects.add(entry.getKey());
            }
        }

        if (currentActiveSession != null) {
            String token = authManager.getToken();
            if (token == null) {
                showErrorToast("인증 토큰이 없어 오브젝트 설정 업데이트 불가.");
                return;
            }

            JsonObject jsonBody = new JsonObject();
            jsonBody.add(
                    "allowed_objects",
                    new Gson().toJsonTree(selectedObjects)
            );
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    jsonBody.toString()
            );

            apiService.updateAllowedObjects(
                    currentActiveSession.getId(),
                    body
            ).enqueue(new Callback<StudySession>() {
                @Override
                public void onResponse(
                        @NonNull Call<StudySession> call,
                        @NonNull Response<StudySession> response
                ) {
                    if (response.isSuccessful()) {
                        showErrorToast("공부 오브젝트 설정 업데이트 성공!");
                    } else {
                        showErrorToast("설정 업데이트 실패: " + response.code());
                    }
                }

                @Override
                public void onFailure(
                        @NonNull Call<StudySession> call,
                        @NonNull Throwable t
                ) {
                    showErrorToast("설정 업데이트 실패: " + t.getMessage());
                }
            });
        }
    }

    private void showErrorToast(String message) {
        Log.e(TAG, "App Error: " + message);
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
