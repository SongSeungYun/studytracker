package com.example.studytrackerviewer.ui.history;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studytrackerviewer.R;
import com.example.studytrackerviewer.data.model.TimelineItem;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {

    private final List<TimelineItem> timelineItems;
    private final Context context;

    public TimelineAdapter(Context context, List<TimelineItem> timelineItems) {
        this.context = context;
        this.timelineItems = timelineItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeline, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimelineItem item = timelineItems.get(position);
        holder.bind(item, context);
    }

    @Override
    public int getItemCount() {
        return timelineItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timelineTime;
        TextView timelineState;
        ImageView timelineImage;

        ViewHolder(View itemView) {
            super(itemView);
            timelineTime = itemView.findViewById(R.id.timelineTime);
            timelineState = itemView.findViewById(R.id.timelineState);
            timelineImage = itemView.findViewById(R.id.timelineImage);
        }

        public void bind(final TimelineItem item, final Context context) {
            // Format time
            try {
                OffsetDateTime odt = OffsetDateTime.parse(item.getTime());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault());
                timelineTime.setText(odt.format(formatter));
            } catch (Exception e) {
                timelineTime.setText(item.getTime());
            }

            // Always display state information
            timelineState.setVisibility(View.VISIBLE);
            timelineState.setText(item.getState());

            // Set color based on state
            switch (item.getState() != null ? item.getState() : "") {
                case "STUDY":
                    timelineState.setTextColor(Color.parseColor("#4CAF50")); // Green
                    break;
                case "DISTRACTED":
                    timelineState.setTextColor(Color.parseColor("#FFC107")); // Amber
                    break;
                case "AWAY":
                    timelineState.setTextColor(Color.parseColor("#F44336")); // Red
                    break;
                default:
                    timelineState.setTextColor(Color.GRAY);
                    break;
            }

            // Display image if URL exists
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                timelineImage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(item.getImageUrl())
                        .centerCrop()
                        .placeholder(R.drawable.bg_card_light) // Optional: add a placeholder
                        .into(timelineImage);
            } else {
                timelineImage.setVisibility(View.GONE);
            }
        }
    }
}
