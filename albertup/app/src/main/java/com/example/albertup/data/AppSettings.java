package com.example.albertup.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/** Small user preferences stored in SharedPreferences. */
public final class AppSettings {

    public static final int DEFAULT_DAILY_GOAL = 100;
    public static final int MIN_DAILY_GOAL = 10;
    public static final int MAX_DAILY_GOAL = 1000;
    public static final int DAILY_GOAL_STEP = 10;

    private static final String PREFS_NAME = "albertup_settings";
    private static final String KEY_DAILY_GOAL = "daily_goal";

    private final SharedPreferences prefs;

    public AppSettings(@NonNull Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getDailyGoal() {
        return clampGoal(prefs.getInt(KEY_DAILY_GOAL, DEFAULT_DAILY_GOAL));
    }

    public void setDailyGoal(int goal) {
        prefs.edit().putInt(KEY_DAILY_GOAL, clampGoal(goal)).apply();
    }

    public static int clampGoal(int goal) {
        return Math.max(MIN_DAILY_GOAL, Math.min(MAX_DAILY_GOAL, goal));
    }
}
