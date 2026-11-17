package com.example.albertup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.albertup.data.Exercise;
import com.example.albertup.data.WorkoutRepository;
import com.example.albertup.data.WorkoutSet;
import com.example.albertup.domain.WorkoutStats;
import com.example.albertup.ui.Formats;
import com.example.albertup.ui.HistoryAdapter;
import com.example.albertup.ui.HistoryItem;
import com.example.albertup.ui.LoggedSetResult;
import com.example.albertup.ui.SetFeedback;
import com.example.albertup.ui.SystemBars;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;

/** All logged sets grouped by local day, with filter, edit and delete (with undo). */
public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.Listener {

    /** Optional: stable id of the exercise to filter by when the screen opens. */
    public static final String EXTRA_EXERCISE_FILTER = "com.example.albertup.extra.EXERCISE_FILTER";

    private static final String TAG = "HistoryActivity";

    private WorkoutRepository repository;
    private HistoryAdapter adapter;

    private View root;
    private ChipGroup chipGroupFilter;
    private TextView tvSummary;
    private RecyclerView recycler;
    private View emptyState;
    private TextView tvEmptyTitle;
    private TextView tvEmptyMessage;
    private ExtendedFloatingActionButton fabLog;

    private final ActivityResultLauncher<Intent> logLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                LoggedSetResult logged = result.getResultCode() == RESULT_OK
                        ? LoggedSetResult.fromIntent(result.getData())
                        : null;
                if (logged != null) {
                    reload();
                    SetFeedback.show(root, fabLog, logged, repository, this::reload);
                }
            });

    @NonNull
    public static Intent newIntent(@NonNull Context context, @Nullable Exercise filter) {
        Intent intent = new Intent(context, HistoryActivity.class);
        if (filter != null) {
            intent.putExtra(EXTRA_EXERCISE_FILTER, filter.getId());
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);
        root = findViewById(R.id.root);
        SystemBars.applyPadding(root, false);
        repository = WorkoutRepository.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        tvSummary = findViewById(R.id.tvSummary);
        recycler = findViewById(R.id.recycler);
        emptyState = findViewById(R.id.emptyState);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        fabLog = findViewById(R.id.fabLog);

        adapter = new HistoryAdapter(this);
        recycler.setAdapter(adapter);
        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView view, int dx, int dy) {
                if (dy > 0) {
                    fabLog.shrink();
                } else if (dy < 0) {
                    fabLog.extend();
                }
            }
        });

        if (savedInstanceState == null) {
            chipGroupFilter.check(chipIdFor(readFilterExtra()));
        }
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> reload());

        View.OnClickListener logNew = v ->
                logLauncher.launch(LogWorkoutActivity.newLogIntent(this, currentFilter()));
        fabLog.setOnClickListener(logNew);
        findViewById(R.id.btnEmptyLog).setOnClickListener(logNew);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Data may have changed on other screens, and "Hoy"/"Ayer" depend on the current date.
        reload();
    }

    @Nullable
    private Exercise readFilterExtra() {
        String filterId = getIntent().getStringExtra(EXTRA_EXERCISE_FILTER);
        Exercise filter = Exercise.fromId(filterId);
        if (filterId != null && filter == null) {
            Log.w(TAG, "Unknown exercise filter, showing all sets: " + filterId);
        }
        return filter;
    }

    private static int chipIdFor(@Nullable Exercise exercise) {
        if (exercise == Exercise.PUSH_UP) {
            return R.id.chipPushUps;
        }
        if (exercise == Exercise.PULL_UP) {
            return R.id.chipPullUps;
        }
        return R.id.chipAll;
    }

    @Nullable
    private Exercise currentFilter() {
        int checkedId = chipGroupFilter.getCheckedChipId();
        if (checkedId == R.id.chipPushUps) {
            return Exercise.PUSH_UP;
        }
        if (checkedId == R.id.chipPullUps) {
            return Exercise.PULL_UP;
        }
        return null;
    }

    private void reload() {
        Exercise filter = currentFilter();
        List<WorkoutSet> sets = repository.getAllSets(filter);
        adapter.submitList(HistoryItem.group(this, sets, System.currentTimeMillis()));

        boolean empty = sets.isEmpty();
        recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        tvSummary.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            tvEmptyTitle.setText(filter == null
                    ? R.string.history_empty_title : R.string.history_empty_filtered_title);
            tvEmptyMessage.setText(filter == null
                    ? R.string.history_empty_message : R.string.history_empty_filtered_message);
            fabLog.extend();
        } else {
            tvSummary.setText(getString(R.string.history_total_summary,
                    Formats.sets(this, sets.size()),
                    Formats.reps(this, WorkoutStats.totalReps(sets))));
        }
    }

    @Override
    public void onEdit(@NonNull WorkoutSet set) {
        logLauncher.launch(LogWorkoutActivity.newEditIntent(this, set.getId()));
    }

    @Override
    public void onDelete(@NonNull WorkoutSet set) {
        if (!repository.deleteSet(set.getId())) {
            reload();
            return;
        }
        reload();
        SetFeedback.show(root, fabLog,
                new LoggedSetResult(LoggedSetResult.Action.DELETED, set, false),
                repository, this::reload);
    }
}
