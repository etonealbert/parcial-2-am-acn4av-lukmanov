package com.example.albertup.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * MediaWiki Action API. One request returns the article intro (plain text) and the metadata of
 * an explicitly chosen Commons photo (URL, author and license) for attribution.
 */
public interface WikipediaApi {

    @GET("w/api.php?action=query&format=json&formatversion=2&redirects=1"
            + "&prop=extracts%7Cinfo%7Cimageinfo&exintro=1&explaintext=1&exsentences=3"
            + "&inprop=url&iiprop=url%7Cextmetadata&iiurlwidth=330"
            + "&iiextmetadatafilter=Artist%7CLicenseShortName")
    Call<WikiQueryResponse> getArticleWithImage(@Query("titles") String titles);
}
