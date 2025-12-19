package com.example.studytrackerviewer.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// 이 클래스는 StudySession을 상속받아 timeline 필드를 추가합니다.
public class SessionDetail extends StudySession {

    @SerializedName("timeline")
    private List<TimelineItem> timeline;

    public List<TimelineItem> getTimeline() {
        return timeline;
    }
}
