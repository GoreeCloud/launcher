package com.goreecloud.launcher;

import org.junit.Test;
import static org.junit.Assert.*;

public final class GoreeCloudSearchContractTest {
    @Test public void correctPrivateGateway() {
        assertEquals("https://search.goreecloud.com/api/search", GoreeCloudSearchContract.ENDPOINT);
    }

    @Test public void validatesQuery() {
        assertTrue(GoreeCloudSearchContract.validQuery("Weather this week"));
        assertFalse(GoreeCloudSearchContract.validQuery(" "));
        assertFalse(GoreeCloudSearchContract.validQuery("x".repeat(501)));
        assertFalse(GoreeCloudSearchContract.validQuery("a\nquestion"));
        assertFalse(GoreeCloudSearchContract.validQuery("a\u202eb"));
    }

    @Test public void validatesResults() {
        assertTrue(GoreeCloudSearchContract.safeResultUrl("https://example.org/info"));
        assertFalse(GoreeCloudSearchContract.safeResultUrl("file:///tmp/payload"));
        assertFalse(GoreeCloudSearchContract.safeResultUrl("http://user:pass@example.org"));
    }

    @Test public void recognizesOnlySafeFirstPartySourceLinks() {
        String response = GoreeCloudSearchContract.RESULTS_HEADING
                + "\n\n1. One\nSnippet\nhttps://example.org/one"
                + "\n\n2. Two\nSnippet\nhttp://example.net/two";
        java.util.List<GoreeCloudSearchContract.SourcedLink> links =
                GoreeCloudSearchContract.sourcedResultLinks(response);
        assertEquals(2, links.size());
        assertEquals("https://example.org/one", links.get(0).url);
        assertEquals(links.get(0).url, response.substring(links.get(0).start, links.get(0).end));
        assertEquals("http://example.net/two", links.get(1).url);
        assertTrue(GoreeCloudSearchContract.sourcedResultLinks("https://example.org/one").isEmpty());
        assertTrue(GoreeCloudSearchContract.sourcedResultLinks(null).isEmpty());
    }

    @Test public void rejectsUnsafeStandaloneUrlsAndCapsLinksAtThree() {
        String response = GoreeCloudSearchContract.RESULTS_HEADING
                + "\njavascript:alert(1)\nhttp://user:pass@example.net"
                + "\nhttps://a.example/1\nhttps://a.example/2"
                + "\nhttps://a.example/3\nhttps://a.example/4";
        java.util.List<GoreeCloudSearchContract.SourcedLink> links =
                GoreeCloudSearchContract.sourcedResultLinks(response);
        assertEquals(3, links.size());
        assertEquals("https://a.example/3", links.get(2).url);
    }

    @Test public void sanitizesAndBoundsUntrustedPreviews() {
        assertEquals("A B", GoreeCloudSearchContract.safePreviewText("A\r\n B", 140));
        assertEquals("AB", GoreeCloudSearchContract.safePreviewText("A\u202eB", 140));
        assertEquals("Title", GoreeCloudSearchContract.safePreviewText("Title", 140));
        assertEquals("Hello", GoreeCloudSearchContract.safePreviewText("Hello World", 5));
        assertEquals("😀", GoreeCloudSearchContract.safePreviewText("😀A", 1));
        assertEquals("", GoreeCloudSearchContract.safePreviewText("\r\n\u202e", 140));
        assertEquals("", GoreeCloudSearchContract.safePreviewText(null, 140));
    }
}
