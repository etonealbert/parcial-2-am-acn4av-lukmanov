package com.example.albertup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.example.albertup.cloud.CloudBackup;
import com.example.albertup.data.AppSettings;
import com.example.albertup.ui.Formats;
import com.example.albertup.ui.SystemBars;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

/**
 * App language (independent from the device language), theme and daily goal.
 * Changes apply immediately; AppCompat recreates the screens with the new configuration.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String LANGUAGE_SYSTEM = "";
    private static final String LANGUAGE_SPANISH = "es";
    private static final String LANGUAGE_ENGLISH = "en";

    private AppSettings settings;
    private TextView tvGoalValue;
    private Button btnGoalMinus;
    private Button btnGoalPlus;
    private TextView tvAccountStatus;

    @NonNull
    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        SystemBars.applyPadding(findViewById(R.id.root), false);
        settings = new AppSettings(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        RadioGroup rgLanguage = findViewById(R.id.rgLanguage);
        rgLanguage.check(radioForLanguage(currentLanguage()));
        rgLanguage.setOnCheckedChangeListener((group, checkedId) ->
                applyLanguage(languageForRadio(checkedId)));

        RadioGroup rgTheme = findViewById(R.id.rgTheme);
        rgTheme.check(radioForNightMode(settings.getNightMode()));
        rgTheme.setOnCheckedChangeListener((group, checkedId) ->
                applyNightMode(nightModeForRadio(checkedId)));

        tvGoalValue = findViewById(R.id.tvGoalValue);
        btnGoalMinus = findViewById(R.id.btnGoalMinus);
        btnGoalPlus = findViewById(R.id.btnGoalPlus);
        btnGoalMinus.setOnClickListener(v -> changeGoal(-AppSettings.DAILY_GOAL_STEP));
        btnGoalPlus.setOnClickListener(v -> changeGoal(AppSettings.DAILY_GOAL_STEP));
        renderGoal(settings.getDailyGoal());

        tvAccountStatus = findViewById(R.id.tvAccountStatus);
        findViewById(R.id.btnOpenAccount).setOnClickListener(v ->
                startActivity(AccountActivity.newIntent(this)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderAccountStatus();
    }

    private void renderAccountStatus() {
        if (!CloudBackup.isConfigured(this)) {
            tvAccountStatus.setText(R.string.settings_account_not_configured);
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            tvAccountStatus.setText(R.string.settings_account_signed_out);
        } else {
            String email = user.getEmail();
            tvAccountStatus.setText(getString(R.string.settings_account_signed_in,
                    email != null ? email : user.getUid()));
        }
    }

    /** Language code chosen in the app, or "" when it follows the device. */
    @NonNull
    private static String currentLanguage() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        Locale first = locales.isEmpty() ? null : locales.get(0);
        return first != null ? first.getLanguage() : LANGUAGE_SYSTEM;
    }

    private static void applyLanguage(@NonNull String language) {
        if (language.equals(currentLanguage())) {
            return;
        }
        // Persisted by AppCompat (autoStoreLocales below API 33, the system on API 33+).
        AppCompatDelegate.setApplicationLocales(language.isEmpty()
                ? LocaleListCompat.getEmptyLocaleList()
                : LocaleListCompat.forLanguageTags(language));
    }

    @IdRes
    private static int radioForLanguage(@NonNull String language) {
        switch (language) {
            case LANGUAGE_SPANISH:
                return R.id.rbLangEs;
            case LANGUAGE_ENGLISH:
                return R.id.rbLangEn;
            default:
                return R.id.rbLangSystem;
        }
    }

    @NonNull
    private static String languageForRadio(@IdRes int radioId) {
        if (radioId == R.id.rbLangEs) {
            return LANGUAGE_SPANISH;
        }
        if (radioId == R.id.rbLangEn) {
            return LANGUAGE_ENGLISH;
        }
        return LANGUAGE_SYSTEM;
    }

    private void applyNightMode(int mode) {
        if (mode == settings.getNightMode()) {
            return;
        }
        settings.setNightMode(mode);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    @IdRes
    private static int radioForNightMode(int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            return R.id.rbThemeLight;
        }
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            return R.id.rbThemeDark;
        }
        return R.id.rbThemeSystem;
    }

    private static int nightModeForRadio(@IdRes int radioId) {
        if (radioId == R.id.rbThemeLight) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if (radioId == R.id.rbThemeDark) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    private void changeGoal(int delta) {
        int goal = AppSettings.clampGoal(settings.getDailyGoal() + delta);
        settings.setDailyGoal(goal);
        renderGoal(goal);
    }

    private void renderGoal(int goal) {
        tvGoalValue.setText(Formats.reps(this, goal));
        btnGoalMinus.setEnabled(goal > AppSettings.MIN_DAILY_GOAL);
        btnGoalPlus.setEnabled(goal < AppSettings.MAX_DAILY_GOAL);
    }
}
