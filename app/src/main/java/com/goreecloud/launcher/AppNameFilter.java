package com.goreecloud.launcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AppNameFilter {
    private AppNameFilter() {}

    public interface Label<T> { String get(T value); }

    public static <T> List<T> filter(List<T> values, String query, Label<T> label) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<T> result = new ArrayList<>();
        for (T value : values) {
            String text = label.get(value);
            if (text != null && text.toLowerCase(Locale.ROOT).contains(needle)) {
                result.add(value);
            }
        }
        return result;
    }
}
