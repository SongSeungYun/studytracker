package com.example.studytrackerviewer.ui.main;

import android.os.Bundle;
import android.util.Log; // Added import
import android.widget.Toast; // Added import

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.studytrackerviewer.R;
import com.example.studytrackerviewer.ui.history.HistoryFragment;
import com.example.studytrackerviewer.ui.home.HomeFragment;
import com.example.studytrackerviewer.ui.stats.StatsFragment;
import com.example.studytrackerviewer.util.AuthManager; // Added import
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Authentication Login ---
        AuthManager.getInstance(this).login(new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(String token) {
                Log.d(TAG, "Admin login successful in MainActivity.");
                // Now proceed with UI setup
                setupUI();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Admin login failed in MainActivity: " + errorMessage);
                Toast.makeText(MainActivity.this, "인증 실패: " + errorMessage, Toast.LENGTH_LONG).show();
                // Optionally disable UI or show a retry button if critical
            }
        });
    }

    private void setupUI() {
        BottomNavigationView nav = findViewById(R.id.bottomNav);

        // ⭐ 1️⃣ 네비 클릭 시 Fragment 교체
        nav.setOnItemSelectedListener(item -> {
            Fragment f = null;

            int itemId = item.getItemId(); // Cache item.getItemId()
            if (itemId == R.id.nav_home) {
                f = new HomeFragment();
            } else if (itemId == R.id.nav_stats) {
                f = new StatsFragment();
            } else if (itemId == R.id.nav_history) {
                f = new HistoryFragment();
            }

            if (f != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, f)
                        .commit();
            }
            return true;
        });

        // ⭐ 2️⃣ HistoryDetailActivity에서 넘어온 탭 처리
        String tab = getIntent().getStringExtra("tab");
        if ("stats".equals(tab)) {
            nav.setSelectedItemId(R.id.nav_stats);
        } else if ("history".equals(tab)) {
            nav.setSelectedItemId(R.id.nav_history);
        } else {
            nav.setSelectedItemId(R.id.nav_home);
        }
    }
}
