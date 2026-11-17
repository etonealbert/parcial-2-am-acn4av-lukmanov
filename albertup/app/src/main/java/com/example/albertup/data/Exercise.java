package com.example.albertup.data;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;

import com.example.albertup.R;

/**
 * Exercises tracked by the app. {@link #getId()} is the stable, language-independent value
 * stored in the database and passed between screens; display names come from resources.
 */
public enum Exercise {
    PUSH_UP("push_up", R.string.pushups, R.plurals.exercise_push_up_count,
            R.drawable.ic_pushup, R.drawable.illus_push_up, 100),
    PULL_UP("pull_up", R.string.pullups, R.plurals.exercise_pull_up_count,
            R.drawable.ic_pullup, R.drawable.illus_pull_up, 40);

    private final String id;
    @StringRes
    private final int nameRes;
    @PluralsRes
    private final int countPluralsRes;
    @DrawableRes
    private final int iconRes;
    @DrawableRes
    private final int illustrationRes;
    private final int unusualRepsThreshold;

    Exercise(String id, @StringRes int nameRes, @PluralsRes int countPluralsRes,
             @DrawableRes int iconRes, @DrawableRes int illustrationRes,
             int unusualRepsThreshold) {
        this.id = id;
        this.nameRes = nameRes;
        this.countPluralsRes = countPluralsRes;
        this.iconRes = iconRes;
        this.illustrationRes = illustrationRes;
        this.unusualRepsThreshold = unusualRepsThreshold;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @StringRes
    public int getNameRes() {
        return nameRes;
    }

    @PluralsRes
    public int getCountPluralsRes() {
        return countPluralsRes;
    }

    @DrawableRes
    public int getIconRes() {
        return iconRes;
    }

    @DrawableRes
    public int getIllustrationRes() {
        return illustrationRes;
    }

    public int getUnusualRepsThreshold() {
        return unusualRepsThreshold;
    }

    @Nullable
    public static Exercise fromId(@Nullable String id) {
        if (id == null) {
            return null;
        }
        for (Exercise exercise : values()) {
            if (exercise.id.equals(id)) {
                return exercise;
            }
        }
        return null;
    }
}
