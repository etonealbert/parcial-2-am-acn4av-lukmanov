package com.example.albertup.data;

import androidx.annotation.NonNull;

import java.util.Objects;

/** One logged set. Immutable; {@link #getPerformedAt()} is epoch milliseconds (UTC). */
public final class WorkoutSet {

    private final long id;
    private final String uuid;
    private final Exercise exercise;
    private final int reps;
    private final long performedAt;
    private final long updatedAt;

    public WorkoutSet(long id, @NonNull String uuid, @NonNull Exercise exercise, int reps,
                      long performedAt, long updatedAt) {
        this.id = id;
        this.uuid = uuid;
        this.exercise = exercise;
        this.reps = reps;
        this.performedAt = performedAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    /** Globally unique id, stable across devices (used as the cloud document id). */
    @NonNull
    public String getUuid() {
        return uuid;
    }

    @NonNull
    public Exercise getExercise() {
        return exercise;
    }

    public int getReps() {
        return reps;
    }

    public long getPerformedAt() {
        return performedAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorkoutSet)) {
            return false;
        }
        WorkoutSet other = (WorkoutSet) o;
        return id == other.id
                && reps == other.reps
                && performedAt == other.performedAt
                && updatedAt == other.updatedAt
                && uuid.equals(other.uuid)
                && exercise == other.exercise;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, uuid, exercise, reps, performedAt, updatedAt);
    }
}
