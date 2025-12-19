package com.example.studytrackerviewer.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studytrackerviewer.R;
import com.example.studytrackerviewer.data.model.StudySession;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    // Click listener interface now works with StudySession objects
    public interface OnItemClickListener {
        void onClick(StudySession session);
    }

    private final List<StudySession> sessions;
    private final OnItemClickListener listener;

    public HistoryAdapter(List<StudySession> sessions, OnItemClickListener listener) {
        this.sessions = sessions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudySession session = sessions.get(position);
        holder.bind(session, listener);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textDate;
        TextView textSummary;

        ViewHolder(View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.textDate);
            textSummary = itemView.findViewById(R.id.textSummary);
        }

        public void bind(final StudySession session, final OnItemClickListener listener) {
            // Format the date
            try {
                // Assuming the date format from server is ISO_OFFSET_DATE_TIME (e.g., "2025-12-18T03:24:56Z")
                OffsetDateTime odt = OffsetDateTime.parse(session.getStartTime());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault());
                textDate.setText(odt.format(formatter));
            } catch (Exception e) {
                // Fallback if parsing fails
                textDate.setText(session.getStartTime());
            }

            // Format the duration
            int totalSeconds = session.getTotalDurationSec();
            int hours = totalSeconds / 3600;
            int minutes = (totalSeconds % 3600) / 60;
            String summary;
            if (hours > 0) {
                summary = String.format(Locale.getDefault(), "총 공부시간 %d시간 %d분", hours, minutes);
            } else {
                summary = String.format(Locale.getDefault(), "총 공부시간 %d분", minutes);
            }
            textSummary.setText(summary);

            // Set the click listener
            itemView.setOnClickListener(v -> listener.onClick(session));
        }
    }
}
