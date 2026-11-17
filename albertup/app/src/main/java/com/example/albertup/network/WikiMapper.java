package com.example.albertup.network;

import androidx.annotation.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Converts the raw API response into an {@link ExerciseArticle}. Pure Java, unit-tested. */
public final class WikiMapper {

    private static final int NAMESPACE_ARTICLE = 0;
    private static final int NAMESPACE_FILE = 6;

    private WikiMapper() {
    }

    /** Returns null when the response has no usable article text (the "empty" state). */
    @Nullable
    public static ExerciseArticle toArticle(@Nullable WikiQueryResponse response, long fetchedAt) {
        if (response == null || response.query == null || response.query.pages == null) {
            return null;
        }
        WikiQueryResponse.Page article = null;
        WikiQueryResponse.Page file = null;
        // Pages are matched by namespace, never by position in the array.
        for (WikiQueryResponse.Page page : response.query.pages) {
            if (page == null) {
                continue;
            }
            if (page.namespace == NAMESPACE_ARTICLE && !page.missing && article == null) {
                article = page;
            } else if (page.namespace == NAMESPACE_FILE && file == null
                    && page.imageInfo != null && !page.imageInfo.isEmpty()) {
                // A shared Commons file can be "missing" locally and still carry image info.
                file = page;
            }
        }
        if (article == null || isBlank(article.title) || isBlank(article.extract)) {
            return null;
        }
        String articleUrl = trustedHttpsUrl(
                article.canonicalUrl != null ? article.canonicalUrl : article.fullUrl);

        String imageUrl = null;
        String imageAuthor = null;
        String imageLicense = null;
        String imagePageUrl = null;
        WikiQueryResponse.ImageInfo info = file != null ? first(file.imageInfo) : null;
        if (info != null) {
            imageUrl = trustedHttpsUrl(info.thumbUrl != null ? info.thumbUrl : info.url);
            imagePageUrl = trustedHttpsUrl(info.descriptionUrl);
            imageAuthor = plainText(metadata(info, "Artist"));
            imageLicense = plainText(metadata(info, "LicenseShortName"));
        }
        return new ExerciseArticle(article.title, article.extract.trim(), articleUrl,
                imageUrl, imageAuthor, imageLicense, imagePageUrl, fetchedAt);
    }

    /** Accepts only https links to Wikipedia/Wikimedia hosts coming from remote data. */
    @Nullable
    static String trustedHttpsUrl(@Nullable String url) {
        if (url == null) {
            return null;
        }
        try {
            URI uri = new URI(url.trim());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null) {
                return null;
            }
            host = host.toLowerCase(Locale.ROOT);
            boolean trusted = host.equals("wikipedia.org") || host.endsWith(".wikipedia.org")
                    || host.equals("wikimedia.org") || host.endsWith(".wikimedia.org");
            return trusted ? uri.toString() : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /** Commons metadata may contain HTML (e.g. links to the author's page). */
    @Nullable
    static String plainText(@Nullable String html) {
        if (html == null) {
            return null;
        }
        String text = html.replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
        return text.isEmpty() ? null : text;
    }

    @Nullable
    private static String metadata(WikiQueryResponse.ImageInfo info, String key) {
        Map<String, WikiQueryResponse.MetadataValue> metadata = info.extMetadata;
        WikiQueryResponse.MetadataValue value = metadata != null ? metadata.get(key) : null;
        return value != null ? value.value : null;
    }

    @Nullable
    private static <T> T first(@Nullable List<T> items) {
        return items == null || items.isEmpty() ? null : items.get(0);
    }

    private static boolean isBlank(@Nullable String text) {
        return text == null || text.trim().isEmpty();
    }
}
