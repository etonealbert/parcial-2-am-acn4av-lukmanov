package com.example.albertup.domain;

import androidx.annotation.Nullable;

import com.example.albertup.data.Exercise;

/** Validation rules for the number of repetitions in a set. */
public final class RepsPolicy {

    public static final int MIN_REPS = 1;
    public static final int MAX_REPS = 200;
    public static final int DEFAULT_REPS = 12;

    private RepsPolicy() {
    }

    /** Parses user input; returns null when it is empty or not a whole number. */
    @Nullable
    public static Integer parse(@Nullable CharSequence text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.toString().trim();
        if (trimmed.isEmpty() || trimmed.length() > 6) {
            return null;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (!Character.isDigit(trimmed.charAt(i))) {
                return null;
            }
        }
        return Integer.parseInt(trimmed);
    }

    public static boolean isValid(int reps) {
        return reps >= MIN_REPS && reps <= MAX_REPS;
    }

    public static int clamp(int reps) {
        return Math.max(MIN_REPS, Math.min(MAX_REPS, reps));
    }

    /** Valid but unusually high for one set: worth a confirmation to catch typos (150 vs 15). */
    public static boolean needsConfirmation(Exercise exercise, int reps) {
        return reps > exercise.getUnusualRepsThreshold();
    }
}
