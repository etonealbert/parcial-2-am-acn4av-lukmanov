package com.example.albertup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.albertup.data.Exercise;
import com.example.albertup.data.WorkoutRepository;
import com.example.albertup.ui.ExerciseGuideContent;
import com.example.albertup.ui.Formats;
import com.example.albertup.ui.LoggedSetResult;
import com.example.albertup.ui.SetFeedback;
import com.example.albertup.ui.SystemBars;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

/** Technique guide for one exercise plus the user's own numbers for it. */
public class ExerciseGuideActivity extends AppCompatActivity {

    /** Required: stable id of the exercise to show. */
    public static final String EXTRA_EXERCISE_ID = "com.example.albertup.extra.GUIDE_EXERCISE_ID";

    private static final String TAG = "ExerciseGuideActivity";

    private Exercise exercise;
    private WorkoutRepository repository;

    private View root;
    private View bottomBar;
    private TextView tvStatRecord;
    private TextView tvStatTotal;
    private TextView tvStatSets;

    private final ActivityResultLauncher<Intent> logLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                LoggedSetResult logged = result.getResultCode() == RESULT_OK
                        ? LoggedSetResult.fromIntent(result.getData())
                        : null;
                if (logged != null) {
                    renderStats();
                    SetFeedback.show(root, bottomBar, logged, repository, this::renderStats);
                }
            });

    @NonNull
    public static Intent newIntent(@NonNull Context context, @NonNull Exercise exercise) {
        return new Intent(context, ExerciseGuideActivity.class)
                .putExtra(EXTRA_EXERCISE_ID, exercise.getId());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String exerciseId = getIntent().getStringExtra(EXTRA_EXERCISE_ID);
        exercise = Exercise.fromId(exerciseId);
        if (exercise == null) {
            Log.w(TAG, "Missing or unknown exercise id: " + exerciseId);
            Toast.makeText(this, R.string.error_exercise_not_found, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_exercise_guide);
        root = findViewById(R.id.root);
        bottomBar = findViewById(R.id.bottomBar);
        SystemBars.applyPadding(root, false);
        repository = WorkoutRepository.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(exercise.getNameRes());
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        tvStatRecord = findViewById(R.id.tvStatRecord);
        tvStatTotal = findViewById(R.id.tvStatTotal);
        tvStatSets = findViewById(R.id.tvStatSets);

        ImageView ivHero = findViewById(R.id.ivHero);
        ivHero.setImageResource(exercise.getIllustrationRes());

        bindContent(ExerciseGuideContent.forExercise(exercise));

        findViewById(R.id.btnLog).setOnClickListener(v ->
                logLauncher.launch(LogWorkoutActivity.newLogIntent(this, exercise)));
        findViewById(R.id.btnHistory).setOnClickListener(v ->
                startActivity(HistoryActivity.newIntent(this, exercise)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderStats();
    }

    private void bindContent(@NonNull ExerciseGuideContent content) {
        TextView tvSummary = findViewById(R.id.tvSummary);
        tvSummary.setText(content.summaryRes);

        ChipGroup chipGroupMuscles = findViewById(R.id.chipGroupMuscles);
        for (String muscle : getResources().getStringArray(content.musclesRes)) {
            Chip chip = new Chip(this);
            chip.setText(muscle);
            chip.setClickable(false);
            chip.setFocusable(false);
            chip.setCheckable(false);
            chipGroupMuscles.addView(chip);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout stepsContainer = findViewById(R.id.stepsContainer);
        String[] steps = getResources().getStringArray(content.stepsRes);
        for (int i = 0; i < steps.length; i++) {
            View row = inflater.inflate(R.layout.item_guide_step, stepsContainer, false);
            ((TextView) row.findViewById(R.id.tvStepNumber)).setText(Formats.number(this, i + 1));
            ((TextView) row.findViewById(R.id.tvStepText)).setText(steps[i]);
            stepsContainer.addView(row);
        }

        addBullets(inflater, findViewById(R.id.mistakesContainer),
                getResources().getStringArray(content.mistakesRes), R.drawable.ic_warning);
        addBullets(inflater, findViewById(R.id.progressionsContainer),
                getResources().getStringArray(content.progressionsRes), R.drawable.ic_chevron_right);
    }

    private static void addBullets(LayoutInflater inflater, ViewGroup container, String[] items,
                                   @DrawableRes int iconRes) {
        for (String item : items) {
            View row = inflater.inflate(R.layout.item_guide_bullet, container, false);
            ((ImageView) row.findViewById(R.id.ivBullet)).setImageResource(iconRes);
            ((TextView) row.findViewById(R.id.tvBulletText)).setText(item);
            container.addView(row);
        }
    }

    private void renderStats() {
        int best = repository.getBestReps(exercise);
        tvStatRecord.setText(best > 0 ? Formats.number(this, best) : getString(R.string.pr_empty));
        tvStatTotal.setText(Formats.number(this, repository.getTotalReps(exercise)));
        tvStatSets.setText(Formats.number(this, repository.getSetCount(exercise)));
    }
}
