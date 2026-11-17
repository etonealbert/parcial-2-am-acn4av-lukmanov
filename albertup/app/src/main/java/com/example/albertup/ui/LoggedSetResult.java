package com.example.albertup.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.albertup.data.Exercise;
import com.example.albertup.data.WorkoutSet;
import com.example.albertup.domain.RepsPolicy;

/**
 * Result returned by the log/edit screen. The whole set travels as a {@link Bundle} inside the
 * result Intent, so the caller can show feedback and undo without querying the database.
 */
public final class LoggedSetResult {

    public static final String EXTRA_RESULT = "com.example.albertup.extra.LOGGED_SET_RESULT";

    private static final String KEY_ACTION = "action";
    private static final String KEY_SET_ID = "set_id";
    private static final String KEY_UUID = "uuid";
    private static final String KEY_EXERCISE_ID = "exercise_id";
    private static final String KEY_REPS = "reps";
    private static final String KEY_PERFORMED_AT = "performed_at";
    private static final String KEY_UPDATED_AT = "updated_at";
    private static final String KEY_NEW_RECORD = "new_record";

    public enum Action { CREATED, UPDATED, DELETED }

    private final Action action;
    private final WorkoutSet set;
    private final boolean newRecord;

    public LoggedSetResult(@NonNull Action action, @NonNull WorkoutSet set, boolean newRecord) {
        this.action = action;
        this.set = set;
        this.newRecord = newRecord;
    }

    @NonNull
    public Action getAction() {
        return action;
    }

    @NonNull
    public WorkoutSet getSet() {
        return set;
    }

    public boolean isNewRecord() {
        return newRecord;
    }

    @NonNull
    public Intent toIntent() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ACTION, action.name());
        bundle.putLong(KEY_SET_ID, set.getId());
        bundle.putString(KEY_UUID, set.getUuid());
        bundle.putString(KEY_EXERCISE_ID, set.getExercise().getId());
        bundle.putInt(KEY_REPS, set.getReps());
        bundle.putLong(KEY_PERFORMED_AT, set.getPerformedAt());
        bundle.putLong(KEY_UPDATED_AT, set.getUpdatedAt());
        bundle.putBoolean(KEY_NEW_RECORD, newRecord);
        return new Intent().putExtra(EXTRA_RESULT, bundle);
    }

    /** Parses and validates a result; returns null if it is missing or malformed. */
    @Nullable
    public static LoggedSetResult fromIntent(@Nullable Intent data) {
        Bundle bundle = data != null ? data.getBundleExtra(EXTRA_RESULT) : null;
        if (bundle == null) {
            return null;
        }
        Action action = parseAction(bundle.getString(KEY_ACTION));
        Exercise exercise = Exercise.fromId(bundle.getString(KEY_EXERCISE_ID));
        String uuid = bundle.getString(KEY_UUID);
        long id = bundle.getLong(KEY_SET_ID, -1L);
        int reps = bundle.getInt(KEY_REPS, 0);
        if (action == null || exercise == null || uuid == null || id <= 0
                || !RepsPolicy.isValid(reps)) {
            return null;
        }
        WorkoutSet set = new WorkoutSet(id, uuid, exercise, reps,
                bundle.getLong(KEY_PERFORMED_AT), bundle.getLong(KEY_UPDATED_AT));
        return new LoggedSetResult(action, set, bundle.getBoolean(KEY_NEW_RECORD, false));
    }

    @Nullable
    private static Action parseAction(@Nullable String name) {
        if (name == null) {
            return null;
        }
        for (Action action : Action.values()) {
            if (action.name().equals(name)) {
                return action;
            }
        }
        return null;
    }
}
