package com.example.albertup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.albertup.data.Exercise;
import com.example.albertup.data.WorkoutRepository;
import com.example.albertup.data.WorkoutSet;
import com.example.albertup.domain.RepsPolicy;
import com.example.albertup.domain.WorkoutStats;
import com.example.albertup.ui.Formats;
import com.example.albertup.ui.LoggedSetResult;
import com.example.albertup.ui.SystemBars;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.EnumMap;
import java.util.Map;

/**
 * Logs a new set or, when started with {@link #EXTRA_SET_ID}, edits or deletes an existing one.
 * The outcome is returned to the caller as a {@link LoggedSetResult} Bundle.
 */
public class LogWorkoutActivity extends AppCompatActivity {

    /** Optional: stable id of the exercise to preselect. */
    public static final String EXTRA_EXERCISE_ID = "com.example.albertup.extra.EXERCISE_ID";
    /** Optional: id of the set to edit. Must exist, otherwise the screen closes. */
    public static final String EXTRA_SET_ID = "com.example.albertup.extra.SET_ID";

    private static final String TAG = "LogWorkoutActivity";
    private static final String STATE_EXERCISE_ID = "state_exercise_id";
    private static final int[] QUICK_PICKS = {5, 10, 15, 20};

    private WorkoutRepository repository;
    /** Set being edited, or null when logging a new one. */
    @Nullable
    private WorkoutSet editingSet;
    private Exercise selectedExercise = Exercise.PUSH_UP;
    private final Map<Exercise, Integer> bestByExercise = new EnumMap<>(Exercise.class);
    private boolean saving;

    private MaterialCardView cardPushUp;
    private MaterialCardView cardPullUp;
    private TextView tvPushUpRecord;
    private TextView tvPullUpRecord;
    private TextView tvRecordHint;
    private TextInputLayout tilReps;
    private TextInputEditText etReps;
    private Button btnMinus;
    private Button btnPlus;
    private Button btnSave;
    private OnBackPressedCallback discardChangesCallback;

    @NonNull
    public static Intent newLogIntent(@NonNull Context context, @Nullable Exercise preselected) {
        Intent intent = new Intent(context, LogWorkoutActivity.class);
        if (preselected != null) {
            intent.putExtra(EXTRA_EXERCISE_ID, preselected.getId());
        }
        return intent;
    }

    @NonNull
    public static Intent newEditIntent(@NonNull Context context, long setId) {
        return new Intent(context, LogWorkoutActivity.class).putExtra(EXTRA_SET_ID, setId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = WorkoutRepository.getInstance(this);
        if (!readIntentExtras()) {
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_log_workout);
        SystemBars.applyPadding(findViewById(R.id.root), true);
        bindViews();

        if (savedInstanceState != null) {
            Exercise restored = Exercise.fromId(savedInstanceState.getString(STATE_EXERCISE_ID));
            if (restored != null) {
                selectedExercise = restored;
            }
        } else {
            setReps(editingSet != null ? editingSet.getReps() : RepsPolicy.DEFAULT_REPS);
        }

        setUpToolbar();
        setUpBackHandling();
        setUpListeners();
        loadRecords();
        renderSelection();
        renderRepsState();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_EXERCISE_ID, selectedExercise.getId());
    }

    /** Validates the launch extras. Returns false (and closes the screen) when invalid. */
    private boolean readIntentExtras() {
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_SET_ID)) {
            long setId = intent.getLongExtra(EXTRA_SET_ID, -1L);
            editingSet = setId > 0 ? repository.getSet(setId) : null;
            if (editingSet == null) {
                Toast.makeText(this, R.string.error_set_not_found, Toast.LENGTH_LONG).show();
                setResult(RESULT_CANCELED);
                finish();
                return false;
            }
            selectedExercise = editingSet.getExercise();
            return true;
        }
        String exerciseId = intent.getStringExtra(EXTRA_EXERCISE_ID);
        Exercise preselected = Exercise.fromId(exerciseId);
        if (preselected != null) {
            selectedExercise = preselected;
        } else if (exerciseId != null) {
            Log.w(TAG, "Ignoring unknown exercise id: " + exerciseId);
        }
        return true;
    }

    private void bindViews() {
        cardPushUp = findViewById(R.id.cardPushUp);
        cardPullUp = findViewById(R.id.cardPullUp);
        tvPushUpRecord = findViewById(R.id.tvPushUpRecord);
        tvPullUpRecord = findViewById(R.id.tvPullUpRecord);
        tvRecordHint = findViewById(R.id.tvRecordHint);
        tilReps = findViewById(R.id.tilReps);
        etReps = findViewById(R.id.etReps);
        btnMinus = findViewById(R.id.btnMinus);
        btnPlus = findViewById(R.id.btnPlus);
        btnSave = findViewById(R.id.btnSave);
        tilReps.setHelperText(getString(R.string.log_reps_helper,
                RepsPolicy.MIN_REPS, RepsPolicy.MAX_REPS));
    }

    private void setUpToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(editingSet == null ? R.string.log_title_new : R.string.log_title_edit);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        if (editingSet != null) {
            setUpEditMode(editingSet);
        }
    }

    private void setUpEditMode(@NonNull WorkoutSet set) {
        TextView tvEditInfo = findViewById(R.id.tvEditInfo);
        tvEditInfo.setText(getString(R.string.log_edit_info,
                Formats.dateTime(this, set.getPerformedAt())));
        tvEditInfo.setVisibility(View.VISIBLE);
        Button btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setVisibility(View.VISIBLE);
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    /** In edit mode, leaving with unsaved changes asks for confirmation first. */
    private void setUpBackHandling() {
        discardChangesCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                new MaterialAlertDialogBuilder(LogWorkoutActivity.this)
                        .setTitle(R.string.discard_title)
                        .setMessage(R.string.discard_message)
                        .setPositiveButton(R.string.action_discard, (dialog, which) -> {
                            setEnabled(false);
                            setResult(RESULT_CANCELED);
                            finish();
                        })
                        .setNegativeButton(R.string.action_keep_editing, null)
                        .show();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, discardChangesCallback);
    }

    private void setUpListeners() {
        cardPushUp.setOnClickListener(v -> selectExercise(Exercise.PUSH_UP));
        cardPullUp.setOnClickListener(v -> selectExercise(Exercise.PULL_UP));
        btnMinus.setOnClickListener(v -> stepReps(-1));
        btnPlus.setOnClickListener(v -> stepReps(1));
        btnSave.setOnClickListener(v -> attemptSave());

        int[] quickPickIds = {R.id.btnQuick1, R.id.btnQuick2, R.id.btnQuick3, R.id.btnQuick4};
        for (int i = 0; i < quickPickIds.length; i++) {
            int value = QUICK_PICKS[i];
            Button button = findViewById(quickPickIds[i]);
            button.setText(Formats.number(this, value));
            button.setContentDescription(getResources().getQuantityString(
                    R.plurals.quick_pick_desc, value, value));
            button.setOnClickListener(v -> setReps(value));
        }

        etReps.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                tilReps.setError(null);
                renderRepsState();
            }
        });
        etReps.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptSave();
                return true;
            }
            return false;
        });
    }

    private void loadRecords() {
        long excludedId = editingSet != null ? editingSet.getId() : -1L;
        for (Exercise exercise : Exercise.values()) {
            bestByExercise.put(exercise, repository.getBestReps(exercise, excludedId));
        }
        tvPushUpRecord.setText(recordLabel(Exercise.PUSH_UP));
        tvPullUpRecord.setText(recordLabel(Exercise.PULL_UP));
    }

    private String recordLabel(Exercise exercise) {
        int best = bestReps(exercise);
        return best > 0
                ? getString(R.string.log_card_record, Formats.number(this, best))
                : getString(R.string.log_card_record_none);
    }

    private int bestReps(Exercise exercise) {
        Integer best = bestByExercise.get(exercise);
        return best != null ? best : 0;
    }

    private void selectExercise(Exercise exercise) {
        selectedExercise = exercise;
        renderSelection();
        renderRepsState();
    }

    /** Selection is shown with a check icon and a thicker outline, not by color alone. */
    private void renderSelection() {
        applySelection(cardPushUp, selectedExercise == Exercise.PUSH_UP);
        applySelection(cardPullUp, selectedExercise == Exercise.PULL_UP);
    }

    private void applySelection(MaterialCardView card, boolean selected) {
        card.setChecked(selected);
        card.setStrokeWidth(getResources().getDimensionPixelSize(
                selected ? R.dimen.card_stroke_selected : R.dimen.card_stroke));
    }

    private void stepReps(int delta) {
        Integer current = RepsPolicy.parse(etReps.getText());
        setReps(current == null ? RepsPolicy.DEFAULT_REPS : RepsPolicy.clamp(current + delta));
    }

    private void setReps(int reps) {
        String text = String.valueOf(reps);
        etReps.setText(text);
        etReps.setSelection(text.length());
    }

    private void renderRepsState() {
        Integer reps = RepsPolicy.parse(etReps.getText());
        boolean valid = reps != null && RepsPolicy.isValid(reps);
        btnMinus.setEnabled(reps != null && reps > RepsPolicy.MIN_REPS);
        btnPlus.setEnabled(reps == null || reps < RepsPolicy.MAX_REPS);

        if (editingSet != null) {
            btnSave.setText(R.string.log_save_changes);
        } else if (valid) {
            btnSave.setText(getString(R.string.log_save_format,
                    Formats.exerciseCount(this, selectedExercise, reps)));
        } else {
            btnSave.setText(R.string.log_save_generic);
        }

        int best = bestReps(selectedExercise);
        if (best == 0) {
            tvRecordHint.setText(R.string.log_record_first);
        } else if (valid && reps > best) {
            tvRecordHint.setText(getString(R.string.log_record_beaten,
                    Formats.exerciseCount(this, selectedExercise, best)));
        } else {
            tvRecordHint.setText(getString(R.string.log_record_current,
                    Formats.exerciseCount(this, selectedExercise, best)));
        }

        updateDiscardGuard(reps);
    }

    private void updateDiscardGuard(@Nullable Integer reps) {
        if (editingSet == null || discardChangesCallback == null) {
            return;
        }
        boolean changed = selectedExercise != editingSet.getExercise()
                || reps == null
                || reps != editingSet.getReps();
        discardChangesCallback.setEnabled(changed);
    }

    private void attemptSave() {
        if (saving) {
            return;
        }
        Integer reps = RepsPolicy.parse(etReps.getText());
        if (reps == null) {
            tilReps.setError(getString(R.string.error_reps_empty));
            etReps.requestFocus();
            return;
        }
        if (!RepsPolicy.isValid(reps)) {
            tilReps.setError(getString(R.string.error_reps_range,
                    RepsPolicy.MIN_REPS, RepsPolicy.MAX_REPS));
            etReps.requestFocus();
            return;
        }
        if (RepsPolicy.needsConfirmation(selectedExercise, reps)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.confirm_unusual_title)
                    .setMessage(getString(R.string.confirm_unusual_message,
                            Formats.exerciseCount(this, selectedExercise, reps)))
                    .setPositiveButton(R.string.action_save_anyway, (dialog, which) -> save(reps))
                    .setNegativeButton(R.string.action_fix, (dialog, which) -> etReps.requestFocus())
                    .show();
            return;
        }
        save(reps);
    }

    private void save(int reps) {
        if (saving) {
            return;
        }
        saving = true;
        btnSave.setEnabled(false);

        LoggedSetResult result;
        if (editingSet == null) {
            int previousBest = repository.getBestReps(selectedExercise);
            WorkoutSet created = repository.addSet(selectedExercise, reps,
                    System.currentTimeMillis());
            result = new LoggedSetResult(LoggedSetResult.Action.CREATED, created,
                    WorkoutStats.isNewRecord(reps, previousBest));
        } else {
            boolean updated = repository.updateSet(editingSet.getId(), selectedExercise, reps);
            WorkoutSet saved = updated ? repository.getSet(editingSet.getId()) : null;
            if (saved == null) {
                Toast.makeText(this, R.string.error_set_not_found, Toast.LENGTH_LONG).show();
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
            result = new LoggedSetResult(LoggedSetResult.Action.UPDATED, saved, false);
        }
        discardChangesCallback.setEnabled(false);
        setResult(RESULT_OK, result.toIntent());
        finish();
    }

    private void confirmDelete() {
        if (editingSet == null) {
            return;
        }
        WorkoutSet set = editingSet;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(getString(R.string.confirm_delete_message,
                        Formats.exerciseCount(this, set.getExercise(), set.getReps()),
                        Formats.dateTime(this, set.getPerformedAt())))
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    repository.deleteSet(set.getId());
                    discardChangesCallback.setEnabled(false);
                    setResult(RESULT_OK, new LoggedSetResult(
                            LoggedSetResult.Action.DELETED, set, false).toIntent());
                    finish();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
