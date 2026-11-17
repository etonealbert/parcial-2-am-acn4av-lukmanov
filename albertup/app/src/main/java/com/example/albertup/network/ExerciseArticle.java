package com.example.albertup.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Online summary of an exercise plus its photo attribution. Plain Java, immutable. */
public final class ExerciseArticle {

    private final String title;
    private final String extract;
    @Nullable
    private final String articleUrl;
    @Nullable
    private final String imageUrl;
    @Nullable
    private final String imageAuthor;
    @Nullable
    private final String imageLicense;
    @Nullable
    private final String imagePageUrl;
    private final long fetchedAt;

    public ExerciseArticle(@NonNull String title, @NonNull String extract,
                           @Nullable String articleUrl, @Nullable String imageUrl,
                           @Nullable String imageAuthor, @Nullable String imageLicense,
                           @Nullable String imagePageUrl, long fetchedAt) {
        this.title = title;
        this.extract = extract;
        this.articleUrl = articleUrl;
        this.imageUrl = imageUrl;
        this.imageAuthor = imageAuthor;
        this.imageLicense = imageLicense;
        this.imagePageUrl = imagePageUrl;
        this.fetchedAt = fetchedAt;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getExtract() {
        return extract;
    }

    @Nullable
    public String getArticleUrl() {
        return articleUrl;
    }

    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    @Nullable
    public String getImageAuthor() {
        return imageAuthor;
    }

    @Nullable
    public String getImageLicense() {
        return imageLicense;
    }

    /** Wikimedia Commons page with the full author and license information. */
    @Nullable
    public String getImagePageUrl() {
        return imagePageUrl;
    }

    public long getFetchedAt() {
        return fetchedAt;
    }
}
