package com.example.albertup.network;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Shared HTTP setup. Wikimedia rejects generic user agents, so every request identifies the app. */
public final class NetworkModule {

    public static final String USER_AGENT = "AlberTUP/1.0"
            + " (https://github.com/etonealbert/parcial-2-am-acn4av-lukmanov; student project)";

    private static final String LANGUAGE_SPANISH = "es";
    private static final String LANGUAGE_ENGLISH = "en";

    private static final Map<String, WikipediaApi> APIS = new ConcurrentHashMap<>();
    private static volatile OkHttpClient httpClient;

    private NetworkModule() {
    }

    @NonNull
    static OkHttpClient httpClient() {
        if (httpClient == null) {
            synchronized (NetworkModule.class) {
                if (httpClient == null) {
                    httpClient = new OkHttpClient.Builder()
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(15, TimeUnit.SECONDS)
                            .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                                    .header("User-Agent", USER_AGENT)
                                    .build()))
                            .build();
                }
            }
        }
        return httpClient;
    }

    /** Wikipedia API for a supported language; anything else falls back to Spanish. */
    @NonNull
    public static WikipediaApi wikipedia(@NonNull String language) {
        String host = LANGUAGE_ENGLISH.equals(language) ? LANGUAGE_ENGLISH : LANGUAGE_SPANISH;
        return APIS.computeIfAbsent(host, key -> new Retrofit.Builder()
                .baseUrl("https://" + key + ".wikipedia.org/")
                .client(httpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WikipediaApi.class));
    }

    /** Glide model that sends the same identifying User-Agent as the API client. */
    @NonNull
    public static GlideUrl imageUrl(@NonNull String url) {
        return new GlideUrl(url, new LazyHeaders.Builder()
                .setHeader("User-Agent", USER_AGENT)
                .build());
    }
}
