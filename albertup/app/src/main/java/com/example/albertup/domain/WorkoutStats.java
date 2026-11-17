package com.example.albertup.domain;

import com.example.albertup.data.Exercise;
import com.example.albertup.data.WorkoutSet;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

/** Pure calculations derived from logged sets. No Android dependencies, unit-testable. */
public final class WorkoutStats {

    private WorkoutStats() {
    }

    public static int totalReps(Collection<WorkoutSet> sets) {
        int total = 0;
        for (WorkoutSet set : sets) {
            total += set.getReps();
        }
        return total;
    }

    public static int totalReps(Collection<WorkoutSet> sets, Exercise exercise) {
        int total = 0;
        for (WorkoutSet set : sets) {
            if (set.getExercise() == exercise) {
                total += set.getReps();
            }
        }
        return total;
    }

    /**
     * Consecutive local days with at least one set, ending today. If nothing was logged today
     * yet, a streak that ended yesterday is still alive and is counted.
     */
    public static int currentStreak(Collection<Long> timestamps, long now, TimeZone timeZone) {
        if (timestamps.isEmpty()) {
            return 0;
        }
        Set<Integer> activeDays = new HashSet<>();
        for (long timestamp : timestamps) {
            activeDays.add(LocalDay.dayKey(timestamp, timeZone));
        }
        Calendar cursor = Calendar.getInstance(timeZone);
        cursor.setTimeInMillis(now);
        // Stepping from noon keeps every step on the intended date even across DST changes.
        cursor.set(Calendar.HOUR_OF_DAY, 12);
        if (!activeDays.contains(LocalDay.dayKey(cursor))) {
            cursor.add(Calendar.DAY_OF_MONTH, -1);
        }
        int streak = 0;
        while (activeDays.contains(LocalDay.dayKey(cursor))) {
            streak++;
            cursor.add(Calendar.DAY_OF_MONTH, -1);
        }
        return streak;
    }

    /** Progress towards the goal as 0..100. */
    public static int goalPercent(int reps, int goal) {
        if (goal <= 0) {
            return reps > 0 ? 100 : 0;
        }
        long percent = (long) Math.max(0, reps) * 100L / goal;
        return (int) Math.min(100L, percent);
    }

    public static int remainingToGoal(int reps, int goal) {
        return Math.max(0, goal - Math.max(0, reps));
    }

    /** A set is a new record when it beats the previous best (the first set always does). */
    public static boolean isNewRecord(int reps, int previousBest) {
        return reps > previousBest;
    }
}
