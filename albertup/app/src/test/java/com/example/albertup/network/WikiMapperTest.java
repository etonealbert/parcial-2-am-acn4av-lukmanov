package com.example.albertup.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;

import org.junit.Test;

public class WikiMapperTest {

    private static final String FILE_PAGE = "{'ns':6,'title':'File:Marine Pull-ups.jpg',"
            + "'missing':true,'known':true,'imagerepository':'shared','imageinfo':[{"
            + "'thumburl':'https://upload.wikimedia.org/wikipedia/commons/thumb/6/67/"
            + "Marine_Pull-ups.jpg/330px-Marine_Pull-ups.jpg',"
            + "'url':'https://upload.wikimedia.org/wikipedia/commons/6/67/Marine_Pull-ups.jpg',"
            + "'descriptionurl':'https://commons.wikimedia.org/wiki/File:Marine_Pull-ups.jpg',"
            + "'extmetadata':{"
            + "'Artist':{'value':'<a href=//commons.wikimedia.org/wiki/User:X>Cpl. C.J. Yard</a>"
            + " &amp; USMC','source':'commons-desc-page'},"
            + "'LicenseShortName':{'value':'Public domain','hidden':''}}}]}";

    private static WikiQueryResponse parse(String singleQuotedJson) {
        return new Gson().fromJson(singleQuotedJson.replace('\'', '"'), WikiQueryResponse.class);
    }

    @Test
    public void mapsArticleAndPhotoByNamespaceWithPlainTextAttribution() {
        WikiQueryResponse response = parse("{'batchcomplete':true,'query':{'pages':["
                + FILE_PAGE + ","
                + "{'pageid':1,'ns':0,'title':'Dominada','extract':'Se denomina dominada. ',"
                + "'canonicalurl':'https://es.wikipedia.org/wiki/Dominada'}]}}");

        ExerciseArticle article = WikiMapper.toArticle(response, 42L);

        assertNotNull(article);
        assertEquals("Dominada", article.getTitle());
        assertEquals("Se denomina dominada.", article.getExtract());
        assertEquals("https://es.wikipedia.org/wiki/Dominada", article.getArticleUrl());
        assertEquals("https://upload.wikimedia.org/wikipedia/commons/thumb/6/67/"
                + "Marine_Pull-ups.jpg/330px-Marine_Pull-ups.jpg", article.getImageUrl());
        assertEquals("https://commons.wikimedia.org/wiki/File:Marine_Pull-ups.jpg",
                article.getImagePageUrl());
        assertEquals("Cpl. C.J. Yard & USMC", article.getImageAuthor());
        assertEquals("Public domain", article.getImageLicense());
        assertEquals(42L, article.getFetchedAt());
    }

    @Test
    public void missingArticleOrBlankExtractMeansNoContent() {
        assertNull(WikiMapper.toArticle(parse("{'query':{'pages':["
                + "{'ns':0,'title':'Dominada','missing':true}," + FILE_PAGE + "]}}"), 0L));
        assertNull(WikiMapper.toArticle(parse("{'query':{'pages':["
                + "{'ns':0,'title':'Dominada','extract':'   '}]}}"), 0L));
        assertNull(WikiMapper.toArticle(parse("{'error':{'code':'badvalue'}}"), 0L));
    }

    @Test
    public void articleWithoutPhotoKeepsTextOnly() {
        ExerciseArticle article = WikiMapper.toArticle(parse("{'query':{'pages':["
                + "{'ns':0,'title':'Push-up','extract':'The push-up is an exercise.',"
                + "'fullurl':'https://en.wikipedia.org/wiki/Push-up'}]}}"), 0L);

        assertNotNull(article);
        assertEquals("https://en.wikipedia.org/wiki/Push-up", article.getArticleUrl());
        assertNull(article.getImageUrl());
        assertNull(article.getImageAuthor());
    }

    @Test
    public void onlyHttpsWikimediaLinksAreTrusted() {
        assertEquals("https://upload.wikimedia.org/a.jpg",
                WikiMapper.trustedHttpsUrl("https://upload.wikimedia.org/a.jpg"));
        assertNull(WikiMapper.trustedHttpsUrl("http://es.wikipedia.org/wiki/Dominada"));
        assertNull(WikiMapper.trustedHttpsUrl("https://es.wikipedia.org.evil.example/x"));
        assertNull(WikiMapper.trustedHttpsUrl("javascript:alert(1)"));
    }
}
