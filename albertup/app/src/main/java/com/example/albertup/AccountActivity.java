package com.example.albertup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.example.albertup.cloud.CloudBackup;
import com.example.albertup.data.AppSettings;
import com.example.albertup.data.WorkoutRepository;
import com.example.albertup.data.WorkoutSet;
import com.example.albertup.ui.Formats;
import com.example.albertup.ui.SystemBars;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.List;

/**
 * Optional account (Firebase Auth, email and password) and manual backup to Cloud Firestore.
 * Without Firebase configuration the screen explains how to enable it and nothing else breaks.
 */
public class AccountActivity extends AppCompatActivity {

    private static final int MIN_PASSWORD_LENGTH = 6;
    /** Firestore queues writes while offline; stop the spinner instead of waiting forever. */
    private static final long BACKUP_TIMEOUT_MS = 20_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable backupTimeout = this::onBackupTimeout;

    private AppSettings settings;
    private boolean configured;
    private boolean busy;

    private View cardNotConfigured;
    private View signedOutGroup;
    private View signedInGroup;
    private View progressGroup;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private Button btnSignIn;
    private Button btnCreateAccount;
    private Button btnBackup;
    private Button btnSignOut;
    private TextView tvSignedInAs;
    private TextView tvLastBackup;
    private TextView tvProgress;
    private TextView tvAccountMessage;

    @NonNull
    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, AccountActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account);
        SystemBars.applyPadding(findViewById(R.id.root), true);
        settings = new AppSettings(this);
        configured = CloudBackup.isConfigured(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        cardNotConfigured = findViewById(R.id.cardNotConfigured);
        signedOutGroup = findViewById(R.id.signedOutGroup);
        signedInGroup = findViewById(R.id.signedInGroup);
        progressGroup = findViewById(R.id.progressGroup);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        btnBackup = findViewById(R.id.btnBackup);
        btnSignOut = findViewById(R.id.btnSignOut);
        tvSignedInAs = findViewById(R.id.tvSignedInAs);
        tvLastBackup = findViewById(R.id.tvLastBackup);
        tvProgress = findViewById(R.id.tvProgress);
        tvAccountMessage = findViewById(R.id.tvAccountMessage);

        btnSignIn.setOnClickListener(v -> submit(false));
        btnCreateAccount.setOnClickListener(v -> submit(true));
        btnBackup.setOnClickListener(v -> backUp());
        btnSignOut.setOnClickListener(v -> signOut());
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit(false);
                return true;
            }
            return false;
        });
        render();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(backupTimeout);
        super.onDestroy();
    }

    @Nullable
    private FirebaseUser currentUser() {
        return configured ? FirebaseAuth.getInstance().getCurrentUser() : null;
    }

    private void render() {
        FirebaseUser user = currentUser();
        cardNotConfigured.setVisibility(configured ? View.GONE : View.VISIBLE);
        signedOutGroup.setVisibility(configured && user == null ? View.VISIBLE : View.GONE);
        signedInGroup.setVisibility(user != null ? View.VISIBLE : View.GONE);
        if (user == null) {
            return;
        }
        String email = user.getEmail();
        tvSignedInAs.setText(getString(R.string.account_signed_in_as,
                email != null ? email : user.getUid()));
        long lastBackupAt = settings.getLastBackupAt(user.getUid());
        tvLastBackup.setText(lastBackupAt > 0
                ? getString(R.string.account_last_backup, Formats.dateTime(this, lastBackupAt),
                Formats.sets(this, settings.getLastBackupCount(user.getUid())))
                : getString(R.string.account_no_backup));
    }

    private void submit(boolean createAccount) {
        if (busy || !configured) {
            return;
        }
        String email = textOf(etEmail).trim();
        String password = textOf(etPassword);
        tilEmail.setError(null);
        tilPassword.setError(null);
        boolean valid = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.account_error_email));
            valid = false;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            tilPassword.setError(getString(R.string.account_error_password_length,
                    MIN_PASSWORD_LENGTH));
            valid = false;
        }
        if (!valid) {
            return;
        }

        showMessage(null);
        setBusy(true, createAccount
                ? R.string.account_progress_creating : R.string.account_progress_signing_in);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        Task<AuthResult> task = createAccount
                ? auth.createUserWithEmailAndPassword(email, password)
                : auth.signInWithEmailAndPassword(email, password);
        task.addOnCompleteListener(result -> {
            if (isDestroyed()) {
                return;
            }
            setBusy(false, 0);
            if (result.isSuccessful()) {
                etPassword.setText(null);
                showMessage(getString(createAccount
                        ? R.string.account_created : R.string.account_signed_in));
            } else {
                showMessage(getString(authErrorMessage(result.getException())));
            }
            render();
        });
    }

    @StringRes
    private static int authErrorMessage(@Nullable Exception error) {
        // Weak-password extends invalid-credentials, so it must be checked first.
        if (error instanceof FirebaseAuthWeakPasswordException) {
            return R.string.account_error_weak_password;
        }
        if (error instanceof FirebaseAuthUserCollisionException) {
            return R.string.account_error_email_in_use;
        }
        if (error instanceof FirebaseAuthInvalidCredentialsException
                || error instanceof FirebaseAuthInvalidUserException) {
            return R.string.account_error_credentials;
        }
        if (error instanceof FirebaseNetworkException) {
            return R.string.account_error_network;
        }
        if (error instanceof FirebaseTooManyRequestsException) {
            return R.string.account_error_too_many;
        }
        return R.string.account_error_generic;
    }

    private void backUp() {
        FirebaseUser user = currentUser();
        if (busy || user == null) {
            render();
            return;
        }
        List<WorkoutSet> sets = WorkoutRepository.getInstance(this).getAllSets(null);
        if (sets.isEmpty()) {
            showMessage(getString(R.string.account_backup_empty));
            return;
        }
        showMessage(null);
        setBusy(true, R.string.account_progress_backup);
        handler.postDelayed(backupTimeout, BACKUP_TIMEOUT_MS);
        String uid = user.getUid();
        CloudBackup.backUp(user, sets, new CloudBackup.Listener() {
            @Override
            public void onSuccess(int backedUpSets) {
                handler.removeCallbacks(backupTimeout);
                settings.setLastBackup(uid, System.currentTimeMillis(), backedUpSets);
                if (isDestroyed()) {
                    return;
                }
                setBusy(false, 0);
                showMessage(getString(R.string.account_backup_done,
                        Formats.sets(AccountActivity.this, backedUpSets)));
                render();
            }

            @Override
            public void onFailure(@NonNull Exception error) {
                handler.removeCallbacks(backupTimeout);
                if (isDestroyed()) {
                    return;
                }
                setBusy(false, 0);
                boolean denied = error instanceof FirebaseFirestoreException
                        && ((FirebaseFirestoreException) error).getCode()
                        == FirebaseFirestoreException.Code.PERMISSION_DENIED;
                showMessage(getString(denied
                        ? R.string.account_backup_denied : R.string.account_backup_failed));
                render();
            }
        });
    }

    private void onBackupTimeout() {
        setBusy(false, 0);
        showMessage(getString(R.string.account_backup_pending));
    }

    private void signOut() {
        if (busy || !configured) {
            return;
        }
        FirebaseAuth.getInstance().signOut();
        showMessage(getString(R.string.account_signed_out));
        render();
    }

    private void setBusy(boolean value, @StringRes int progressText) {
        busy = value;
        progressGroup.setVisibility(value ? View.VISIBLE : View.GONE);
        if (value) {
            tvProgress.setText(progressText);
        }
        etEmail.setEnabled(!value);
        etPassword.setEnabled(!value);
        btnSignIn.setEnabled(!value);
        btnCreateAccount.setEnabled(!value);
        btnBackup.setEnabled(!value);
        btnSignOut.setEnabled(!value);
    }

    private void showMessage(@Nullable String message) {
        tvAccountMessage.setText(message);
        tvAccountMessage.setVisibility(message == null ? View.GONE : View.VISIBLE);
    }

    @NonNull
    private static String textOf(@NonNull TextInputEditText field) {
        return field.getText() != null ? field.getText().toString() : "";
    }
}
