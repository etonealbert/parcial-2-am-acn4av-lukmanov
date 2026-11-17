package com.example.albertup;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import com.example.albertup.data.Exercise;
import com.example.albertup.data.WorkoutRepository;
import com.example.albertup.network.ExerciseArticle;
import com.example.albertup.network.ExerciseContentRepository;
import com.example.albertup.network.NetworkModule;
import com.example.albertup.network.WikiQueryResponse;
import com.example.albertup.ui.ExerciseGuideContent;
import com.example.albertup.ui.Formats;
import com.example.albertup.ui.LoggedSetResult;
import com.example.albertup.ui.SetFeedback;
import com.example.albertup.ui.SystemBars;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import retrofit2.Call;

/**
 * Technique guide for one exercise, the user's own numbers for it and a Wikipedia summary with
 * a Wikimedia Commons photo loaded from the internet (with attribution and offline fallback).
 */
public class ExerciseGuideActivity extends AppCompatActivity {

    /** Required: stable id of the exercise to show. */
    public static final String EXTRA_EXERCISE_ID = "com.example.albertup.extra.GUIDE_EXERCISE_ID";

    private static final String TAG = "ExerciseGuideActivity";

    private Exercise exercise;
    private ExerciseGuideContent content;
    private WorkoutRepository repository;
    private ExerciseContentRepository contentRepository;
    @Nullable
    private Call<WikiQueryResponse> onlineCall;

    private View root;
    private View bottomBar;
    private TextView tvStatRecord;
    private TextView tvStatTotal;
    private TextView tvStatSets;
    private View onlineLoading;
    private View onlineContent;
    private View onlineError;
    private ImageView ivOnlinePhoto;
    private TextView tvOnlineStatus;
    private TextView tvOnlineExtract;
    private TextView tvTextCredit;
    private TextView tvPhotoCredit;
    private TextView tvOnlineError;
    private Button btnOpenArticle;
    private Button btnOpenPhotoLicense;

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

        content = ExerciseGuideContent.forExercise(exercise);
        bindContent(content);

        findViewById(R.id.btnLog).setOnClickListener(v ->
                logLauncher.launch(LogWorkoutActivity.newLogIntent(this, exercise)));
        findViewById(R.id.btnHistory).setOnClickListener(v ->
                startActivity(HistoryActivity.newIntent(this, exercise)));

        contentRepository = new ExerciseContentRepository(this);
        bindOnlineViews();
        findViewById(R.id.btnRetry).setOnClickListener(v -> loadOnlineContent());
        loadOnlineContent();
    }

    @Override
    protected void onDestroy() {
        if (onlineCall != null) {
            onlineCall.cancel();
        }
        super.onDestroy();
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

    private void bindOnlineViews() {
        onlineLoading = findViewById(R.id.onlineLoading);
        onlineContent = findViewById(R.id.onlineContent);
        onlineError = findViewById(R.id.onlineError);
        ivOnlinePhoto = findViewById(R.id.ivOnlinePhoto);
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus);
        tvOnlineExtract = findViewById(R.id.tvOnlineExtract);
        tvTextCredit = findViewById(R.id.tvTextCredit);
        tvPhotoCredit = findViewById(R.id.tvPhotoCredit);
        tvOnlineError = findViewById(R.id.tvOnlineError);
        btnOpenArticle = findViewById(R.id.btnOpenArticle);
        btnOpenPhotoLicense = findViewById(R.id.btnOpenPhotoLicense);
    }

    /**
     * Shows the saved copy right away (if any) and refreshes it from Wikipedia. Without a saved
     * copy the user sees loading, then the content, an empty message or an error with retry.
     */
    private void loadOnlineContent() {
        String language = getString(R.string.wiki_language);
        String title = getString(content.wikiTitleRes);
        String imageFile = getString(content.wikiImageFileRes);
        ExerciseArticle cached = contentRepository.getCached(language, title);
        if (cached != null) {
            showArticle(cached, false);
        } else {
            showOnlineLoading();
        }
        if (onlineCall != null) {
            onlineCall.cancel();
        }
        onlineCall = contentRepository.fetch(language, title, imageFile,
                new ExerciseContentRepository.Listener() {
                    @Override
                    public void onLoaded(@NonNull ExerciseArticle article) {
                        showArticle(article, false);
                    }

                    @Override
                    public void onEmpty() {
                        if (cached == null) {
                            showOnlineError(R.string.online_empty);
                        }
                    }

                    @Override
                    public void onFailed(@NonNull ExerciseContentRepository.Failure failure) {
                        if (cached != null) {
                            showArticle(cached, true);
                        } else {
                            showOnlineError(errorMessage(failure));
                        }
                    }
                });
    }

    @StringRes
    private static int errorMessage(@NonNull ExerciseContentRepository.Failure failure) {
        switch (failure) {
            case OFFLINE:
                return R.string.online_error_offline;
            case RATE_LIMITED:
                return R.string.online_error_rate_limited;
            case SERVER:
            default:
                return R.string.online_error_server;
        }
    }

    private void showOnlineLoading() {
        onlineLoading.setVisibility(View.VISIBLE);
        onlineContent.setVisibility(View.GONE);
        onlineError.setVisibility(View.GONE);
        ivOnlinePhoto.setVisibility(View.GONE);
    }

    private void showOnlineError(@StringRes int message) {
        onlineLoading.setVisibility(View.GONE);
        onlineContent.setVisibility(View.GONE);
        onlineError.setVisibility(View.VISIBLE);
        ivOnlinePhoto.setVisibility(View.GONE);
        tvOnlineError.setText(message);
    }

    private void showArticle(@NonNull ExerciseArticle article, boolean offlineCopy) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        onlineLoading.setVisibility(View.GONE);
        onlineError.setVisibility(View.GONE);
        onlineContent.setVisibility(View.VISIBLE);
        tvOnlineExtract.setText(article.getExtract());
        tvTextCredit.setText(getString(R.string.online_text_credit, article.getTitle()));
        if (offlineCopy) {
            tvOnlineStatus.setText(getString(R.string.online_cached_note,
                    Formats.dateTime(this, article.getFetchedAt())));
            tvOnlineStatus.setVisibility(View.VISIBLE);
        } else {
            tvOnlineStatus.setVisibility(View.GONE);
        }
        String articleUrl = article.getArticleUrl();
        btnOpenArticle.setVisibility(articleUrl != null ? View.VISIBLE : View.GONE);
        btnOpenArticle.setOnClickListener(v -> openLink(articleUrl));
        showPhoto(article);
    }

    /** Downloads the photo with Glide; the bundled illustration is the placeholder and fallback. */
    private void showPhoto(@NonNull ExerciseArticle article) {
        tvPhotoCredit.setVisibility(View.GONE);
        btnOpenPhotoLicense.setVisibility(View.GONE);
        String imageUrl = article.getImageUrl();
        if (imageUrl == null) {
            Glide.with(this).clear(ivOnlinePhoto);
            ivOnlinePhoto.setVisibility(View.GONE);
            return;
        }
        ivOnlinePhoto.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(NetworkModule.imageUrl(imageUrl))
                .placeholder(exercise.getIllustrationRes())
                .error(exercise.getIllustrationRes())
                .transition(DrawableTransitionOptions.withCrossFade())
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                @NonNull Target<Drawable> target,
                                                boolean isFirstResource) {
                        // The fallback is our own illustration, so no photo credit is shown.
                        ivOnlinePhoto.setContentDescription(
                                getString(R.string.guide_illustration_desc));
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(@NonNull Drawable resource,
                                                   @NonNull Object model,
                                                   Target<Drawable> target,
                                                   @NonNull DataSource dataSource,
                                                   boolean isFirstResource) {
                        ivOnlinePhoto.setContentDescription(
                                getString(R.string.online_photo_desc));
                        showPhotoCredit(article);
                        return false;
                    }
                })
                .into(ivOnlinePhoto);
    }

    private void showPhotoCredit(@NonNull ExerciseArticle article) {
        String author = article.getImageAuthor();
        String license = article.getImageLicense();
        tvPhotoCredit.setText(author != null && license != null
                ? getString(R.string.online_photo_credit, author, license)
                : getString(R.string.online_photo_credit_unknown));
        tvPhotoCredit.setVisibility(View.VISIBLE);
        String pageUrl = article.getImagePageUrl();
        if (pageUrl != null) {
            btnOpenPhotoLicense.setVisibility(View.VISIBLE);
            btnOpenPhotoLicense.setOnClickListener(v -> openLink(pageUrl));
        }
    }

    private void openLink(@Nullable String url) {
        if (url == null) {
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.error_no_browser, Toast.LENGTH_SHORT).show();
        }
    }

    private void renderStats() {
        int best = repository.getBestReps(exercise);
        tvStatRecord.setText(best > 0 ? Formats.number(this, best) : getString(R.string.pr_empty));
        tvStatTotal.setText(Formats.number(this, repository.getTotalReps(exercise)));
        tvStatSets.setText(Formats.number(this, repository.getSetCount(exercise)));
    }
}
