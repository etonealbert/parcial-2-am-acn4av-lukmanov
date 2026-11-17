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
    /** Wikipedia article title in the current app language. */
    @StringRes
    public final int wikiTitleRes;
    /** Wikimedia Commons file chosen for this exercise (licensed for reuse). */
    @StringRes
    public final int wikiImageFileRes;

    private ExerciseGuideContent(@StringRes int summaryRes, @ArrayRes int musclesRes,
                                 @ArrayRes int stepsRes, @ArrayRes int mistakesRes,
                                 @ArrayRes int progressionsRes, @StringRes int wikiTitleRes,
                                 @StringRes int wikiImageFileRes) {
        this.summaryRes = summaryRes;
        this.musclesRes = musclesRes;
        this.stepsRes = stepsRes;
        this.mistakesRes = mistakesRes;
        this.progressionsRes = progressionsRes;
        this.wikiTitleRes = wikiTitleRes;
        this.wikiImageFileRes = wikiImageFileRes;
    }

    @NonNull
    public static ExerciseGuideContent forExercise(@NonNull Exercise exercise) {
        if (exercise == Exercise.PULL_UP) {
            return new ExerciseGuideContent(R.string.guide_pull_up_summary,
                    R.array.guide_pull_up_muscles, R.array.guide_pull_up_steps,
                    R.array.guide_pull_up_mistakes, R.array.guide_pull_up_progressions,
                    R.string.wiki_title_pull_up, R.string.wiki_file_pull_up);
        }
        return new ExerciseGuideContent(R.string.guide_push_up_summary,
                R.array.guide_push_up_muscles, R.array.guide_push_up_steps,
                R.array.guide_push_up_mistakes, R.array.guide_push_up_progressions,
                R.string.wiki_title_push_up, R.string.wiki_file_push_up);
    }
}
