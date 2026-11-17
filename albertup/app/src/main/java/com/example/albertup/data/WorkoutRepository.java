package com.example.albertup.data;

import static com.example.albertup.data.WorkoutDbHelper.COL_EXERCISE;
import static com.example.albertup.data.WorkoutDbHelper.COL_ID;
import static com.example.albertup.data.WorkoutDbHelper.COL_PERFORMED_AT;
import static com.example.albertup.data.WorkoutDbHelper.COL_REPS;
import static com.example.albertup.data.WorkoutDbHelper.COL_UPDATED_AT;
import static com.example.albertup.data.WorkoutDbHelper.COL_UUID;
import static com.example.albertup.data.WorkoutDbHelper.TABLE_SETS;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Single source of truth for logged sets. Totals and records are always derived from the
 * stored rows, so edits and deletes can never leave stale values behind.
 *
 * <p>Queries are small and indexed, so they run synchronously on the caller's thread.</p>
 */
public final class WorkoutRepository {

    private static final String[] SET_COLUMNS = {
            COL_ID, COL_UUID, COL_EXERCISE, COL_REPS, COL_PERFORMED_AT, COL_UPDATED_AT
    };

    private static volatile WorkoutRepository instance;

    private final WorkoutDbHelper dbHelper;

    private WorkoutRepository(Context context) {
        dbHelper = new WorkoutDbHelper(context);
    }

    @NonNull
    public static WorkoutRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (WorkoutRepository.class) {
                if (instance == null) {
                    instance = new WorkoutRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    @NonNull
    public WorkoutSet addSet(@NonNull Exercise exercise, int reps, long performedAt) {
        String uuid = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(COL_UUID, uuid);
        values.put(COL_EXERCISE, exercise.getId());
        values.put(COL_REPS, reps);
        values.put(COL_PERFORMED_AT, performedAt);
        values.put(COL_UPDATED_AT, now);
        long id = writable().insertOrThrow(TABLE_SETS, null, values);
        return new WorkoutSet(id, uuid, exercise, reps, performedAt, now);
    }

    /** Re-inserts a previously deleted set with its original id and uuid (used by "undo"). */
    public void restoreSet(@NonNull WorkoutSet set) {
        ContentValues values = new ContentValues();
        values.put(COL_ID, set.getId());
        values.put(COL_UUID, set.getUuid());
        values.put(COL_EXERCISE, set.getExercise().getId());
        values.put(COL_REPS, set.getReps());
        values.put(COL_PERFORMED_AT, set.getPerformedAt());
        values.put(COL_UPDATED_AT, set.getUpdatedAt());
        writable().insertWithOnConflict(TABLE_SETS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Corrects exercise and reps of an existing set; the original date is kept. */
    public boolean updateSet(long id, @NonNull Exercise exercise, int reps) {
        ContentValues values = new ContentValues();
        values.put(COL_EXERCISE, exercise.getId());
        values.put(COL_REPS, reps);
        values.put(COL_UPDATED_AT, System.currentTimeMillis());
        return writable().update(TABLE_SETS, values, COL_ID + " = ?",
                new String[]{String.valueOf(id)}) > 0;
    }

    public boolean deleteSet(long id) {
        return writable().delete(TABLE_SETS, COL_ID + " = ?",
                new String[]{String.valueOf(id)}) > 0;
    }

    @Nullable
    public WorkoutSet getSet(long id) {
        try (Cursor cursor = readable().query(TABLE_SETS, SET_COLUMNS, COL_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            List<WorkoutSet> sets = readSets(cursor);
            return sets.isEmpty() ? null : sets.get(0);
        }
    }

    /** Sets performed in [startInclusive, endExclusive), oldest first. */
    @NonNull
    public List<WorkoutSet> getSetsBetween(long startInclusive, long endExclusive) {
        try (Cursor cursor = readable().query(TABLE_SETS, SET_COLUMNS,
                COL_PERFORMED_AT + " >= ? AND " + COL_PERFORMED_AT + " < ?",
                new String[]{String.valueOf(startInclusive), String.valueOf(endExclusive)},
                null, null, COL_PERFORMED_AT + " ASC, " + COL_ID + " ASC")) {
            return readSets(cursor);
        }
    }

    /** Every set, newest first, optionally restricted to one exercise. */
    @NonNull
    public List<WorkoutSet> getAllSets(@Nullable Exercise exercise) {
        String selection = exercise == null ? null : COL_EXERCISE + " = ?";
        String[] args = exercise == null ? null : new String[]{exercise.getId()};
        try (Cursor cursor = readable().query(TABLE_SETS, SET_COLUMNS, selection, args,
                null, null, COL_PERFORMED_AT + " DESC, " + COL_ID + " DESC")) {
            return readSets(cursor);
        }
    }

    /** Timestamps of every set, used to compute streaks. */
    @NonNull
    public List<Long> getAllPerformedAt() {
        List<Long> timestamps = new ArrayList<>();
        try (Cursor cursor = readable().query(TABLE_SETS, new String[]{COL_PERFORMED_AT},
                null, null, null, null, COL_PERFORMED_AT + " DESC")) {
            while (cursor.moveToNext()) {
                timestamps.add(cursor.getLong(0));
            }
        }
        return timestamps;
    }

    public int getBestReps(@NonNull Exercise exercise) {
        return getBestReps(exercise, -1L);
    }

    /** Best single set for the exercise, ignoring {@code excludedSetId} (pass -1 for none). */
    public int getBestReps(@NonNull Exercise exercise, long excludedSetId) {
        return (int) DatabaseUtils.longForQuery(readable(),
                "SELECT COALESCE(MAX(" + COL_REPS + "), 0) FROM " + TABLE_SETS
                        + " WHERE " + COL_EXERCISE + " = ? AND " + COL_ID + " != ?",
                new String[]{exercise.getId(), String.valueOf(excludedSetId)});
    }

    public int getTotalReps(@Nullable Exercise exercise) {
        return (int) aggregate("COALESCE(SUM(" + COL_REPS + "), 0)", exercise);
    }

    public int getSetCount(@Nullable Exercise exercise) {
        return (int) aggregate("COUNT(*)", exercise);
    }

    private long aggregate(String expression, @Nullable Exercise exercise) {
        String sql = "SELECT " + expression + " FROM " + TABLE_SETS;
        if (exercise == null) {
            return DatabaseUtils.longForQuery(readable(), sql, null);
        }
        return DatabaseUtils.longForQuery(readable(), sql + " WHERE " + COL_EXERCISE + " = ?",
                new String[]{exercise.getId()});
    }

    @NonNull
    private static List<WorkoutSet> readSets(Cursor cursor) {
        List<WorkoutSet> sets = new ArrayList<>(cursor.getCount());
        int idIndex = cursor.getColumnIndexOrThrow(COL_ID);
        int uuidIndex = cursor.getColumnIndexOrThrow(COL_UUID);
        int exerciseIndex = cursor.getColumnIndexOrThrow(COL_EXERCISE);
        int repsIndex = cursor.getColumnIndexOrThrow(COL_REPS);
        int performedIndex = cursor.getColumnIndexOrThrow(COL_PERFORMED_AT);
        int updatedIndex = cursor.getColumnIndexOrThrow(COL_UPDATED_AT);
        while (cursor.moveToNext()) {
            Exercise exercise = Exercise.fromId(cursor.getString(exerciseIndex));
            if (exercise == null) {
                // Unknown id (e.g. written by a newer app version): skip instead of crashing.
                continue;
            }
            sets.add(new WorkoutSet(
                    cursor.getLong(idIndex),
                    cursor.getString(uuidIndex),
                    exercise,
                    cursor.getInt(repsIndex),
                    cursor.getLong(performedIndex),
                    cursor.getLong(updatedIndex)));
        }
        return sets;
    }

    private SQLiteDatabase readable() {
        return dbHelper.getReadableDatabase();
    }

    private SQLiteDatabase writable() {
        return dbHelper.getWritableDatabase();
    }
}
