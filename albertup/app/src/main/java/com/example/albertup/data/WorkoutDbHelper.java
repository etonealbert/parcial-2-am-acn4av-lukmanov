package com.example.albertup.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** SQLite schema for logged sets. */
final class WorkoutDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "albertup.db";
    private static final int DATABASE_VERSION = 1;

    static final String TABLE_SETS = "workout_sets";
    static final String COL_ID = "_id";
    static final String COL_UUID = "uuid";
    static final String COL_EXERCISE = "exercise_id";
    static final String COL_REPS = "reps";
    static final String COL_PERFORMED_AT = "performed_at";
    static final String COL_UPDATED_AT = "updated_at";

    WorkoutDbHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_SETS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_UUID + " TEXT NOT NULL UNIQUE, "
                + COL_EXERCISE + " TEXT NOT NULL, "
                + COL_REPS + " INTEGER NOT NULL CHECK (" + COL_REPS + " > 0), "
                + COL_PERFORMED_AT + " INTEGER NOT NULL, "
                + COL_UPDATED_AT + " INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_sets_performed_at ON " + TABLE_SETS
                + " (" + COL_PERFORMED_AT + ")");
        db.execSQL("CREATE INDEX idx_sets_exercise ON " + TABLE_SETS
                + " (" + COL_EXERCISE + ", " + COL_REPS + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 is the first schema. Future versions must migrate here without dropping data.
    }
}
