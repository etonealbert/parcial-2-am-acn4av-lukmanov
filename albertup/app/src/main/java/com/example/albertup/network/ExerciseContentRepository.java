package com.example.albertup.network;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Loads an exercise article from Wikipedia and keeps an offline copy of the last result. */
public final class ExerciseContentRepository {

    public enum Failure { OFFLINE, RATE_LIMITED, SERVER }

    /** Called on the main thread. */
    public interface Listener {
        void onLoaded(@NonNull ExerciseArticle article);

        void onEmpty();

        void onFailed(@NonNull Failure failure);
    }

    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private final ArticleCache cache;

    public ExerciseContentRepository(@NonNull Context context) {
        cache = new ArticleCache(context);
    }

    @Nullable
    public ExerciseArticle getCached(@NonNull String language, @NonNull String title) {
        return cache.get(cacheKey(language, title));
    }

    /** Starts the request; cancel the returned call when the screen goes away. */
    @NonNull
    public Call<WikiQueryResponse> fetch(@NonNull String language, @NonNull String title,
                                         @NonNull String imageFile,
                                         @NonNull Listener listener) {
        Call<WikiQueryResponse> request = NetworkModule.wikipedia(language)
                .getArticleWithImage(title + "|" + imageFile);
        request.enqueue(new Callback<WikiQueryResponse>() {
            @Override
            public void onResponse(@NonNull Call<WikiQueryResponse> call,
                                   @NonNull Response<WikiQueryResponse> response) {
                if (call.isCanceled()) {
                    return;
                }
                if (response.code() == HTTP_TOO_MANY_REQUESTS) {
                    listener.onFailed(Failure.RATE_LIMITED);
                    return;
                }
                if (!response.isSuccessful()) {
                    listener.onFailed(Failure.SERVER);
                    return;
                }
                ExerciseArticle article = WikiMapper.toArticle(response.body(),
                        System.currentTimeMillis());
                if (article == null) {
                    listener.onEmpty();
                    return;
                }
                cache.put(cacheKey(language, title), article);
                listener.onLoaded(article);
            }

            @Override
            public void onFailure(@NonNull Call<WikiQueryResponse> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                listener.onFailed(t instanceof IOException ? Failure.OFFLINE : Failure.SERVER);
            }
        });
        return request;
    }

    private static String cacheKey(String language, String title) {
        return language + ":" + title;
    }
}
