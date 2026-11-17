package com.example.albertup.network;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/** Gson model of the parts of the MediaWiki "query" response that the app uses. */
public final class WikiQueryResponse {

    @Nullable
    @SerializedName("query")
    public Query query;

    public static final class Query {
        @Nullable
        @SerializedName("pages")
        public List<Page> pages;
    }

    public static final class Page {
        /** 0 = article, 6 = file. */
        @SerializedName("ns")
        public int namespace;
        @Nullable
        @SerializedName("title")
        public String title;
        @SerializedName("missing")
        public boolean missing;
        @Nullable
        @SerializedName("extract")
        public String extract;
        @Nullable
        @SerializedName("canonicalurl")
        public String canonicalUrl;
        @Nullable
        @SerializedName("fullurl")
        public String fullUrl;
        @Nullable
        @SerializedName("imageinfo")
        public List<ImageInfo> imageInfo;
    }

    public static final class ImageInfo {
        @Nullable
        @SerializedName("thumburl")
        public String thumbUrl;
        @Nullable
        @SerializedName("url")
        public String url;
        @Nullable
        @SerializedName("descriptionurl")
        public String descriptionUrl;
        @Nullable
        @SerializedName("extmetadata")
        public Map<String, MetadataValue> extMetadata;
    }

    public static final class MetadataValue {
        @Nullable
        @SerializedName("value")
        public String value;
    }
}
