package com.example.studytrackerviewer.ui.stats;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.studytrackerviewer.R;
import com.example.studytrackerviewer.data.model.DateStat;
import com.example.studytrackerviewer.data.network.ApiClient;
import com.example.studytrackerviewer.data.network.ApiService;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatsFragment extends Fragment {

    private static final String TAG = "StatsFragment";

    private ApiService apiService;

    private TextView tabDaily, tabWeekly, tabMonthly;
    private View layoutDaily, layoutWeekly, layoutMonthly, layoutInsights;
    private BarChart dailyChart, weeklyChart;
    private LineChart monthlyChart;

    private TextView summaryStudyTime, summaryDistractedTime, summaryAwayTime;
    private TextView insightTotalTime, insightAverageTime, insightBestDay;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_stats, container, false);

        apiService = ApiClient.getApiService(requireContext());
        bindViews(v);
        setupTabs();
        selectTab("daily");

        return v;
    }

    private void bindViews(View v) {
        tabDaily = v.findViewById(R.id.tabDaily);
        tabWeekly = v.findViewById(R.id.tabWeekly);
        tabMonthly = v.findViewById(R.id.tabMonthly);

        layoutDaily = v.findViewById(R.id.layoutDaily);
        layoutWeekly = v.findViewById(R.id.layoutWeekly);
        layoutMonthly = v.findViewById(R.id.layoutMonthly);
        layoutInsights = v.findViewById(R.id.layoutInsights);

        dailyChart = v.findViewById(R.id.dailyChart);
        weeklyChart = v.findViewById(R.id.weeklyChart);
        monthlyChart = v.findViewById(R.id.monthlyChart);

        summaryStudyTime = v.findViewById(R.id.summaryStudyTime);
        summaryDistractedTime = v.findViewById(R.id.summaryDistractedTime);
        summaryAwayTime = v.findViewById(R.id.summaryAwayTime);

        insightTotalTime = v.findViewById(R.id.insightTotalTime);
        insightAverageTime = v.findViewById(R.id.insightAverageTime);
        insightBestDay = v.findViewById(R.id.insightBestDay);
    }

    private void setupTabs() {
        tabDaily.setOnClickListener(v -> selectTab("daily"));
        tabWeekly.setOnClickListener(v -> selectTab("weekly"));
        tabMonthly.setOnClickListener(v -> selectTab("monthly"));
    }

    private void selectTab(String type) {
        layoutDaily.setVisibility(type.equals("daily") ? View.VISIBLE : View.GONE);
        layoutWeekly.setVisibility(type.equals("weekly") ? View.VISIBLE : View.GONE);
        layoutMonthly.setVisibility(type.equals("monthly") ? View.VISIBLE : View.GONE);
        layoutInsights.setVisibility(type.equals("daily") ? View.GONE : View.VISIBLE);

        tabDaily.setTextColor(type.equals("daily") ? Color.parseColor("#4CAF50") : Color.GRAY);
        tabWeekly.setTextColor(type.equals("weekly") ? Color.parseColor("#4CAF50") : Color.GRAY);
        tabMonthly.setTextColor(type.equals("monthly") ? Color.parseColor("#4CAF50") : Color.GRAY);

        resetSummary();

        if (type.equals("daily")) fetchDaily();
        if (type.equals("weekly")) fetchWeekly();
        if (type.equals("monthly")) fetchMonthly();
    }

    private void fetchDaily() {
        apiService.getTodayStats().enqueue(new Callback<DateStat>() {
            @Override
            public void onResponse(@NonNull Call<DateStat> call,
                                   @NonNull Response<DateStat> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                DateStat s = response.body();
                updateSummary(s);

                List<BarEntry> entries = new ArrayList<>();
                entries.add(new BarEntry(0, s.getStudyMinutes()));
                entries.add(new BarEntry(1, s.getDistractedMinutes()));
                entries.add(new BarEntry(2, s.getAwayMinutes()));

                setupBarChart(
                        dailyChart,
                        entries,
                        new String[]{"Study", "Distracted", "Away"},
                        "#4CAF50"
                );
            }

            @Override
            public void onFailure(@NonNull Call<DateStat> call, @NonNull Throwable t) {
                toast(t.getMessage());
            }
        });
    }

    private void fetchWeekly() {
        apiService.getRangeStats("week").enqueue(new Callback<List<DateStat>>() {
            @Override
            public void onResponse(@NonNull Call<List<DateStat>> call,
                                   @NonNull Response<List<DateStat>> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                List<DateStat> list = response.body();
                updateSummary(list);

                List<BarEntry> entries = new ArrayList<>();
                List<String> labels = new ArrayList<>();

                for (int i = 0; i < list.size(); i++) {
                    entries.add(new BarEntry(i, list.get(i).getStudyMinutes()));
                    labels.add(formatDayOfWeek(list.get(i).getDate()));
                }

                setupBarChart(
                        weeklyChart,
                        entries,
                        labels.toArray(new String[0]),
                        "#2196F3"
                );

                showInsights(list);
            }

            @Override
            public void onFailure(@NonNull Call<List<DateStat>> call, @NonNull Throwable t) {
                toast(t.getMessage());
            }
        });
    }

    private void fetchMonthly() {
        apiService.getRangeStats("month").enqueue(new Callback<List<DateStat>>() {
            @Override
            public void onResponse(@NonNull Call<List<DateStat>> call,
                                   @NonNull Response<List<DateStat>> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                List<Entry> entries = new ArrayList<>();

                for (DateStat s : response.body()) {
                    entries.add(new Entry(
                            extractDayOfMonth(s.getDate()),
                            s.getStudyMinutes()
                    ));
                }

                setupLineChart(monthlyChart, entries, "#9C27B0");
            }

            @Override
            public void onFailure(@NonNull Call<List<DateStat>> call, @NonNull Throwable t) {
                toast(t.getMessage());
            }
        });
    }

    private void resetSummary() {
        summaryStudyTime.setText("-");
        summaryDistractedTime.setText("-");
        summaryAwayTime.setText("-");
    }

    private void updateSummary(DateStat s) {
        summaryStudyTime.setText(formatMinutes(s.getStudyMinutes()));
        summaryDistractedTime.setText(formatMinutes(s.getDistractedMinutes()));
        summaryAwayTime.setText(formatMinutes(s.getAwayMinutes()));
    }

    private void updateSummary(List<DateStat> list) {
        int s = 0, d = 0, a = 0;
        for (DateStat e : list) {
            s += e.getStudyMinutes();
            d += e.getDistractedMinutes();
            a += e.getAwayMinutes();
        }
        summaryStudyTime.setText(formatMinutes(s));
        summaryDistractedTime.setText(formatMinutes(d));
        summaryAwayTime.setText(formatMinutes(a));
    }

    private String formatMinutes(int m) {
        return m >= 60
                ? (m / 60 + "시간 " + m % 60 + "분")
                : (m + "분");
    }

    private void showInsights(List<DateStat> list) {
        int total = 0;
        int max = 0;
        String bestDay = "";

        for (DateStat s : list) {
            total += s.getStudyMinutes();
            if (s.getStudyMinutes() > max) {
                max = s.getStudyMinutes();
                bestDay = formatFullDayName(s.getDate());
            }
        }

        insightTotalTime.setText("• 주간 총 공부 시간: " + formatMinutes(total));
        insightAverageTime.setText("• 일 평균 공부 시간: " + formatMinutes(total / list.size()));
        insightBestDay.setText("• 최고의 공부 요일: " + bestDay);
    }

    private void setupBarChart(BarChart chart,
                               List<BarEntry> entries,
                               String[] labels,
                               String color) {

        BarDataSet set = new BarDataSet(entries, "");
        set.setColor(Color.parseColor(color));

        BarData data = new BarData(set);
        chart.setData(data);

        XAxis x = chart.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setGranularity(1f);

        chart.getDescription().setEnabled(false);
        chart.invalidate();
    }

    private void setupLineChart(LineChart chart,
                                List<Entry> entries,
                                String color) {

        LineDataSet set = new LineDataSet(entries, "");
        set.setColor(Color.parseColor(color));
        set.setCircleColor(Color.parseColor(color));

        chart.setData(new LineData(set));
        chart.getDescription().setEnabled(false);
        chart.invalidate();
    }

    private String formatDayOfWeek(String dateString) {
        try {
            SimpleDateFormat parser =
                    new SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN);
            SimpleDateFormat formatter =
                    new SimpleDateFormat("E", Locale.KOREAN);

            Date date = parser.parse(dateString);
            return formatter.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    private float extractDayOfMonth(String dateString) {
        try {
            SimpleDateFormat parser =
                    new SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN);
            Date date = parser.parse(dateString);

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal.get(Calendar.DAY_OF_MONTH);
        } catch (Exception e) {
            return 0f;
        }
    }

    private String formatFullDayName(String dateString) {
        try {
            SimpleDateFormat parser =
                    new SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN);
            SimpleDateFormat formatter =
                    new SimpleDateFormat("EEEE", Locale.KOREAN);

            Date date = parser.parse(dateString);
            return formatter.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    private void toast(String msg) {
        Log.e(TAG, msg);
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
