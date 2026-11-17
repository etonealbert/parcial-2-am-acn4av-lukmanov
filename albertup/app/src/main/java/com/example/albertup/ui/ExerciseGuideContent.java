package com.example.albertup.ui;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.example.albertup.R;
import com.example.albertup.data.Exercise;

/** Localized, bundled guide content (technique, muscles, mistakes, progressions). */
public final class ExerciseGuideContent {

    @StringRes
    public final int summaryRes;
    @ArrayRes
    public final int musclesRes;
    @ArrayRes
    public final int stepsRes;
    @ArrayRes
    public final int mistakesRes;
    @ArrayRes
    public final int progressionsRes;

    private ExerciseGuideContent(@StringRes int summaryRes, @ArrayRes int musclesRes,
                                 @ArrayRes int stepsRes, @ArrayRes int mistakesRes,
                                 @ArrayRes int progressionsRes) {
        this.summaryRes = summaryRes;
        this.musclesRes = musclesRes;
        this.stepsRes = stepsRes;
        this.mistakesRes = mistakesRes;
        this.progressionsRes = progressionsRes;
    }

    @NonNull
    public static ExerciseGuideContent forExercise(@NonNull Exercise exercise) {
        if (exercise == Exercise.PULL_UP) {
            return new ExerciseGuideContent(R.string.guide_pull_up_summary,
                    R.array.guide_pull_up_muscles, R.array.guide_pull_up_steps,
                    R.array.guide_pull_up_mistakes, R.array.guide_pull_up_progressions);
        }
        return new ExerciseGuideContent(R.string.guide_push_up_summary,
                R.array.guide_push_up_muscles, R.array.guide_push_up_steps,
                R.array.guide_push_up_mistakes, R.array.guide_push_up_progressions);
    }
}
