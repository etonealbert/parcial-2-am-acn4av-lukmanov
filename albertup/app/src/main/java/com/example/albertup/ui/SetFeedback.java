package com.example.albertup.ui;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.albertup.R;
import com.example.albertup.data.WorkoutRepository;
import com.example.albertup.data.WorkoutSet;
import com.google.android.material.snackbar.Snackbar;

/** Snackbar confirmation (with undo where possible) after a set is created, edited or deleted. */
public final class SetFeedback {

    private SetFeedback() {
    }

    public static void show(@NonNull View root, @Nullable View anchor,
                            @NonNull LoggedSetResult result,
                            @NonNull WorkoutRepository repository,
                            @NonNull Runnable onDataChanged) {
        Context context = root.getContext();
        WorkoutSet set = result.getSet();
        String what = Formats.exerciseCount(context, set.getExercise(), set.getReps());
        Snackbar snackbar;
        switch (result.getAction()) {
            case CREATED:
                snackbar = Snackbar.make(root, context.getString(result.isNewRecord()
                                ? R.string.feedback_new_record
                                : R.string.feedback_set_created, what), Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_undo, v -> {
                            repository.deleteSet(set.getId());
                            onDataChanged.run();
                        });
                break;
            case UPDATED:
                snackbar = Snackbar.make(root,
                        context.getString(R.string.feedback_set_updated, what),
                        Snackbar.LENGTH_SHORT);
                break;
            case DELETED:
            default:
                snackbar = Snackbar.make(root,
                                context.getString(R.string.feedback_set_deleted, what),
                                Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_undo, v -> {
                            repository.restoreSet(set);
                            onDataChanged.run();
                        });
                break;
        }
        if (anchor != null && anchor.getVisibility() == View.VISIBLE) {
            snackbar.setAnchorView(anchor);
        }
        snackbar.show();
    }
}
