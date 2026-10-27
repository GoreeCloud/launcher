package com.goreecloud.launcher;
import org.junit.Test;
import static org.junit.Assert.*;
public final class GoreeCloudSearchContractTest {
 @Test public void correctPrivateGateway(){assertEquals("https://search.goreecloud.com/api/search",GoreeCloudSearchContract.ENDPOINT);}
 @Test public void validatesQuery(){assertTrue(GoreeCloudSearchContract.validQuery("Weather this week"));assertFalse(GoreeCloudSearchContract.validQuery(" "));assertFalse(GoreeCloudSearchContract.validQuery("x".repeat(501)));}
 @Test public void validatesResults(){assertTrue(GoreeCloudSearchContract.safeResultUrl("https://example.org/info"));assertFalse(GoreeCloudSearchContract.safeResultUrl("file:///tmp/payload"));assertFalse(GoreeCloudSearchContract.safeResultUrl("http://user:pass@example.org"));}
}
