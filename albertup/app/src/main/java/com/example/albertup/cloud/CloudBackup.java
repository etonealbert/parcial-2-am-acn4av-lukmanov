package com.example.albertup.cloud;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.albertup.data.WorkoutSet;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Saves local sets to users/{uid}/sets in Firestore. */
public final class CloudBackup {

    /** Stay below the documented 500 writes per batch (one extra write for the summary). */
    private static final int MAX_WRITES_PER_BATCH = 450;

    public interface Listener {
        void onSuccess(int backedUpSets);

        void onFailure(@NonNull Exception error);
    }

    private CloudBackup() {
    }

    /**
     * True only when the default Firebase app was initialized, i.e. the build included a
     * google-services.json. Firebase.getInstance() calls would throw otherwise.
     */
    public static boolean isConfigured(@NonNull Context context) {
        for (FirebaseApp app : FirebaseApp.getApps(context)) {
            if (FirebaseApp.DEFAULT_APP_NAME.equals(app.getName())) {
                return true;
            }
        }
        return false;
    }

    public static void backUp(@NonNull FirebaseUser user, @NonNull List<WorkoutSet> sets,
                              @NonNull Listener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDoc = db.collection("users").document(user.getUid());
        CollectionReference setsCollection = userDoc.collection("sets");

        List<Task<Void>> commits = new ArrayList<>();
        WriteBatch batch = db.batch();
        int writesInBatch = 0;
        for (WorkoutSet set : sets) {
            batch.set(setsCollection.document(set.getUuid()), toDocument(set));
            writesInBatch++;
            if (writesInBatch == MAX_WRITES_PER_BATCH) {
                commits.add(batch.commit());
                batch = db.batch();
                writesInBatch = 0;
            }
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("lastBackupAt", FieldValue.serverTimestamp());
        summary.put("setCount", sets.size());
        batch.set(userDoc, summary, SetOptions.merge());
        commits.add(batch.commit());

        Tasks.whenAll(commits)
                .addOnSuccessListener(unused -> listener.onSuccess(sets.size()))
                .addOnFailureListener(listener::onFailure);
    }

    @NonNull
    static Map<String, Object> toDocument(@NonNull WorkoutSet set) {
        Map<String, Object> document = new HashMap<>();
        document.put("exerciseId", set.getExercise().getId());
        document.put("reps", set.getReps());
        document.put("performedAt", new Timestamp(new Date(set.getPerformedAt())));
        document.put("updatedAt", new Timestamp(new Date(set.getUpdatedAt())));
        document.put("backedUpAt", FieldValue.serverTimestamp());
        return document;
    }
}
