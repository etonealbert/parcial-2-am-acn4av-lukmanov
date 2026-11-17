package com.example.albertup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;

import com.example.albertup.data.AppSettings;
import com.example.albertup.data.Exercise;
import com.example.albertup.data.WorkoutRepository;
import com.example.albertup.data.WorkoutSet;
import com.example.albertup.domain.LocalDay;
import com.example.albertup.domain.WorkoutStats;
import com.example.albertup.ui.Formats;
import com.example.albertup.ui.LoggedSetResult;
import com.example.albertup.ui.SetFeedback;
import com.example.albertup.ui.SystemBars;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;
import java.util.TimeZone;

/**
 * Dashboard: today's progress, streak, exercises, today's sets and records. Every value is
 * derived from the database on each refresh, so it is always consistent with the other screens.
 */
public class MainActivity extends AppCompatActivity {

    private WorkoutRepository repository;
    private AppSettings settings;

    private View root;
    private TextView tvDate;
    private TextView tvStreak;
    private TextView tvTodayRepsValue;
    private TextView tvTodaySetsValue;
    private TextView tvGoalPercent;
    private LinearProgressIndicator progressDaily;
    private TextView tvDailyGoal;
    private TextView tvMotivation;
    private TextView tvPushUpStats;
    private TextView tvPullUpStats;
    private TextView tvTodaysSetsTitle;
    private View emptyToday;
    private LinearLayout setsList;
    private TextView tvPrPushValue;
    private TextView tvPrPullValue;
    private TextView tvPrTotalValue;
    private TextView tvPrTotalSets;
    private ExtendedFloatingActionButton fabLog;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable dayChangeRefresh = this::onDayChanged;

    /** Refreshes "today" when the date, time or time zone changes while the screen is visible. */
    private final BroadcastReceiver timeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onDayChanged();
        }
    };

    private final ActivityResultLauncher<Intent> logLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                LoggedSetResult logged = result.getResultCode() == RESULT_OK
                        ? LoggedSetResult.fromIntent(result.getData())
                        : null;
                if (logged != null) {
                    refresh();
                    SetFeedback.show(root, fabLog, logged, repository, this::refresh);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        root = findViewById(R.id.main);
        SystemBars.applyPadding(root, false);

        repository = WorkoutRepository.getInstance(this);
        settings = new AppSettings(this);
        bindViews();
        setUpNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        scheduleDayChangeRefresh();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        ContextCompat.registerReceiver(this, timeChangeReceiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(dayChangeRefresh);
        unregisterReceiver(timeChangeReceiver);
    }

    private void bindViews() {
        tvDate = findViewById(R.id.tvDate);
        tvStreak = findViewById(R.id.tvStreak);
        tvTodayRepsValue = findViewById(R.id.tvTodayRepsValue);
        tvTodaySetsValue = findViewById(R.id.tvTodaySetsValue);
        tvGoalPercent = findViewById(R.id.tvGoalPercent);
        progressDaily = findViewById(R.id.progressDaily);
        tvDailyGoal = findViewById(R.id.tvDailyGoal);
        tvMotivation = findViewById(R.id.tvMotivation);
        tvPushUpStats = findViewById(R.id.tvPushUpStats);
        tvPullUpStats = findViewById(R.id.tvPullUpStats);
        tvTodaysSetsTitle = findViewById(R.id.tvTodaysSetsTitle);
        emptyToday = findViewById(R.id.emptyToday);
        setsList = findViewById(R.id.setsList);
        tvPrPushValue = findViewById(R.id.tvPrPushValue);
        tvPrPullValue = findViewById(R.id.tvPrPullValue);
        tvPrTotalValue = findViewById(R.id.tvPrTotalValue);
        tvPrTotalSets = findViewById(R.id.tvPrTotalSets);
        fabLog = findViewById(R.id.fabLog);
    }

    private void setUpNavigation() {
        fabLog.setOnClickListener(v -> openLog(null));
        findViewById(R.id.btnLogPushUp).setOnClickListener(v -> openLog(Exercise.PUSH_UP));
        findViewById(R.id.btnLogPullUp).setOnClickListener(v -> openLog(Exercise.PULL_UP));
        findViewById(R.id.btnGuidePushUp).setOnClickListener(v -> openGuide(Exercise.PUSH_UP));
        findViewById(R.id.btnGuidePullUp).setOnClickListener(v -> openGuide(Exercise.PULL_UP));
        findViewById(R.id.cardPushUp).setOnClickListener(v -> openGuide(Exercise.PUSH_UP));
        findViewById(R.id.cardPullUp).setOnClickListener(v -> openGuide(Exercise.PULL_UP));
        View.OnClickListener openHistory = v ->
                startActivity(HistoryActivity.newIntent(this, null));
        findViewById(R.id.btnHistory).setOnClickListener(openHistory);
        findViewById(R.id.btnViewHistory).setOnClickListener(openHistory);
    }

    private void openLog(@Nullable Exercise preselected) {
        logLauncher.launch(LogWorkoutActivity.newLogIntent(this, preselected));
    }

    private void openGuide(@NonNull Exercise exercise) {
        startActivity(ExerciseGuideActivity.newIntent(this, exercise));
    }

    private void onDayChanged() {
        refresh();
        scheduleDayChangeRefresh();
    }

    private void scheduleDayChangeRefresh() {
        handler.removeCallbacks(dayChangeRefresh);
        long now = System.currentTimeMillis();
        long untilTomorrow = LocalDay.startOfNextDay(now, TimeZone.getDefault()) - now;
        handler.postDelayed(dayChangeRefresh, untilTomorrow + 1_000L);
    }

    private void refresh() {
        long now = System.currentTimeMillis();
        TimeZone timeZone = TimeZone.getDefault();
        List<WorkoutSet> today = repository.getSetsBetween(
                LocalDay.startOfDay(now, timeZone), LocalDay.startOfNextDay(now, timeZone));

        tvDate.setText(Formats.longDate(this, now, now));
        renderStreak(WorkoutStats.currentStreak(repository.getAllPerformedAt(), now, timeZone));
        renderToday(today);
        renderExerciseCards(today);
        renderTodaySets(today);
        renderRecords();
    }

    private void renderStreak(int streak) {
        tvStreak.setText(streak > 0
                ? getResources().getQuantityString(R.plurals.streak_days, streak,
                        Formats.number(this, streak))
                : getString(R.string.streak_none));
    }

    private void renderToday(@NonNull List<WorkoutSet> today) {
        int reps = WorkoutStats.totalReps(today);
        int goal = settings.getDailyGoal();
        int percent = WorkoutStats.goalPercent(reps, goal);
        int remaining = WorkoutStats.remainingToGoal(reps, goal);

        tvTodayRepsValue.setText(Formats.number(this, reps));
        tvTodaySetsValue.setText(Formats.number(this, today.size()));
        tvGoalPercent.setText(Formats.percent(this, percent));
        progressDaily.setProgressCompat(percent, true);
        String progressText = getString(R.string.daily_goal_progress,
                Formats.number(this, reps), Formats.number(this, goal));
        tvDailyGoal.setText(progressText);
        progressDaily.setContentDescription(getString(R.string.goal_progress_desc, progressText));

        if (reps == 0) {
            tvMotivation.setText(getString(R.string.motivation_start, Formats.reps(this, goal)));
        } else if (remaining > 0) {
            tvMotivation.setText(getResources().getQuantityString(R.plurals.motivation_remaining,
                    remaining, Formats.number(this, remaining)));
        } else {
            tvMotivation.setText(R.string.motivation_done);
        }
    }

    private void renderExerciseCards(@NonNull List<WorkoutSet> today) {
        tvPushUpStats.setText(exerciseCardStats(today, Exercise.PUSH_UP));
        tvPullUpStats.setText(exerciseCardStats(today, Exercise.PULL_UP));
    }

    private String exerciseCardStats(@NonNull List<WorkoutSet> today, @NonNull Exercise exercise) {
        int best = repository.getBestReps(exercise);
        return getString(R.string.exercise_card_stats,
                Formats.number(this, WorkoutStats.totalReps(today, exercise)),
                best > 0 ? Formats.number(this, best) : getString(R.string.pr_empty));
    }

    /** Rebuilds the list of today's sets: one tappable card per set, oldest first. */
    private void renderTodaySets(@NonNull List<WorkoutSet> today) {
        tvTodaysSetsTitle.setText(getString(R.string.todays_sets_format, today.size()));
        emptyToday.setVisibility(today.isEmpty() ? View.VISIBLE : View.GONE);
        setsList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < today.size(); i++) {
            WorkoutSet set = today.get(i);
            View row = inflater.inflate(R.layout.item_today_set, setsList, false);
            ((ImageView) row.findViewById(R.id.ivExercise))
                    .setImageResource(set.getExercise().getIconRes());
            ((TextView) row.findViewById(R.id.tvSetTitle)).setText(getString(
                    R.string.today_set_row, i + 1,
                    Formats.exerciseCount(this, set.getExercise(), set.getReps())));
            ((TextView) row.findViewById(R.id.tvSetTime))
                    .setText(Formats.time(this, set.getPerformedAt()));
            row.setOnClickListener(v ->
                    logLauncher.launch(LogWorkoutActivity.newEditIntent(this, set.getId())));
            ViewCompat.replaceAccessibilityAction(row, AccessibilityActionCompat.ACTION_CLICK,
                    getString(R.string.action_edit), null);
            setsList.addView(row);
        }
    }

    private void renderRecords() {
        tvPrPushValue.setText(recordText(repository.getBestReps(Exercise.PUSH_UP)));
        tvPrPullValue.setText(recordText(repository.getBestReps(Exercise.PULL_UP)));
        int totalReps = repository.getTotalReps(null);
        tvPrTotalValue.setText(Formats.reps(this, totalReps));
        tvPrTotalSets.setText(getString(R.string.pr_total_sets,
                Formats.sets(this, repository.getSetCount(null))));
    }

    private String recordText(int best) {
        return best > 0 ? Formats.reps(this, best) : getString(R.string.pr_empty);
    }
}
