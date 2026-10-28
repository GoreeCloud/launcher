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
