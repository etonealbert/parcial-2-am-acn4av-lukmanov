package com.example.albertup.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.albertup.R;
import com.example.albertup.data.WorkoutSet;
import com.example.albertup.domain.LocalDay;
import com.example.albertup.domain.WorkoutStats;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

/** Row of the history list: either a day header or a logged set. */
public final class HistoryItem {

    private final int dayKey;
    @Nullable
    private final String title;
    @Nullable
    private final String summary;
    @Nullable
    private final WorkoutSet set;

    private HistoryItem(int dayKey, @Nullable String title, @Nullable String summary,
                        @Nullable WorkoutSet set) {
        this.dayKey = dayKey;
        this.title = title;
        this.summary = summary;
        this.set = set;
    }

    /** Groups sets (newest first) by local day, adding a header with that day's totals. */
    @NonNull
    public static List<HistoryItem> group(@NonNull Context context,
                                          @NonNull List<WorkoutSet> newestFirst, long now) {
        TimeZone timeZone = TimeZone.getDefault();
        Map<Integer, List<WorkoutSet>> byDay = new LinkedHashMap<>();
        for (WorkoutSet set : newestFirst) {
            int key = LocalDay.dayKey(set.getPerformedAt(), timeZone);
            List<WorkoutSet> day = byDay.get(key);
            if (day == null) {
                day = new ArrayList<>();
                byDay.put(key, day);
            }
            day.add(set);
        }
        List<HistoryItem> items = new ArrayList<>(newestFirst.size() + byDay.size());
        for (Map.Entry<Integer, List<WorkoutSet>> entry : byDay.entrySet()) {
            List<WorkoutSet> day = entry.getValue();
            String title = Formats.dayLabel(context, day.get(0).getPerformedAt(), now);
            String summary = context.getString(R.string.format_pair,
                    Formats.sets(context, day.size()),
                    Formats.reps(context, WorkoutStats.totalReps(day)));
            items.add(new HistoryItem(entry.getKey(), title, summary, null));
            for (WorkoutSet set : day) {
                items.add(new HistoryItem(entry.getKey(), null, null, set));
            }
        }
        return items;
    }

    public boolean isHeader() {
        return set == null;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getSummary() {
        return summary;
    }

    @Nullable
    public WorkoutSet getSet() {
        return set;
    }

    boolean isSameItemAs(@NonNull HistoryItem other) {
        if (isHeader() != other.isHeader()) {
            return false;
        }
        return isHeader()
                ? dayKey == other.dayKey
                : Objects.requireNonNull(set).getId() == Objects.requireNonNull(other.set).getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HistoryItem)) {
            return false;
        }
        HistoryItem other = (HistoryItem) o;
        return dayKey == other.dayKey
                && Objects.equals(title, other.title)
                && Objects.equals(summary, other.summary)
                && Objects.equals(set, other.set);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dayKey, title, summary, set);
    }
}
