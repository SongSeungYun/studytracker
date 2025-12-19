package com.example.studytrackerviewer.ui.history;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;

public class NonScrollableLinearLayoutManager extends LinearLayoutManager {
    public NonScrollableLinearLayoutManager(Context context) {
        super(context);
    }

    @Override
    public boolean canScrollVertically() {
        // RecyclerView의 수직 스크롤 기능을 비활성화합니다.
        return false;
    }
}
