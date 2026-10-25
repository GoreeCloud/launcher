package com.goreecloud.launcher;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;

public final class AppNameFilterTest {
    @Test public void caseInsensitiveSearch() {
        List<String> names = Arrays.asList("Calendar", "Camera", "Notes");
        assertEquals(Arrays.asList("Calendar", "Camera"),
                AppNameFilter.filter(names, "CA", x -> x));
    }
    @Test public void blankQueryDoesNotDiscardEntries() {
        List<String> names = Arrays.asList("Maps", "Music");
        assertEquals(names, AppNameFilter.filter(names, " ", x -> x));
    }
    @Test public void nullLabelIsIgnored() {
        List<String> names = Arrays.asList("Maps", null);
        assertEquals(Arrays.asList("Maps"), AppNameFilter.filter(names, null, x -> x));
    }
}
