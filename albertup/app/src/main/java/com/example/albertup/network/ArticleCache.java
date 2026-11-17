package com.example.albertup.network;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/** Keeps the last successful article per language and title so the guide works offline. */
final class ArticleCache {

    private static final String PREFS_NAME = "albertup_online_cache";

    private final SharedPreferences prefs;

    ArticleCache(@NonNull Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    ExerciseArticle get(@NonNull String key) {
        String json = prefs.getString(key, null);
        if (json == null) {
            return null;
        }
        try {
            JSONObject object = new JSONObject(json);
            String title = object.optString("title", null);
            String extract = object.optString("extract", null);
            if (title == null || extract == null) {
                return null;
            }
            return new ExerciseArticle(title, extract,
                    object.optString("articleUrl", null),
                    object.optString("imageUrl", null),
                    object.optString("imageAuthor", null),
                    object.optString("imageLicense", null),
                    object.optString("imagePageUrl", null),
                    object.optLong("fetchedAt", 0L));
        } catch (JSONException e) {
            prefs.edit().remove(key).apply();
            return null;
        }
    }

    void put(@NonNull String key, @NonNull ExerciseArticle article) {
        try {
            // JSONObject.put ignores null values, so absent fields are simply not stored.
            JSONObject object = new JSONObject()
                    .put("title", article.getTitle())
                    .put("extract", article.getExtract())
                    .put("articleUrl", article.getArticleUrl())
                    .put("imageUrl", article.getImageUrl())
                    .put("imageAuthor", article.getImageAuthor())
                    .put("imageLicense", article.getImageLicense())
                    .put("imagePageUrl", article.getImagePageUrl())
                    .put("fetchedAt", article.getFetchedAt());
            prefs.edit().putString(key, object.toString()).apply();
        } catch (JSONException ignored) {
            // Nothing to cache; the next successful load will try again.
        }
    }
}
