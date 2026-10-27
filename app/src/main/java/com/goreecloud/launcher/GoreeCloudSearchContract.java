package com.goreecloud.launcher;

import java.net.URI;
import java.util.Locale;

/** Contract owned by GoreeCloud/search, not a guessed third-party gateway. */
public final class GoreeCloudSearchContract {
    public static final String ENDPOINT = "https://search.goreecloud.com/api/search";
    public static final int MAX_QUERY_LENGTH = 500;
    private GoreeCloudSearchContract() {}

    public static boolean validQuery(String question) {
        if (question == null) return false;
        String q = question.trim();
        if (q.length() < 2 || q.length() > MAX_QUERY_LENGTH) return false;
        for (int i = 0; i < q.length(); i++) {
            char ch = q.charAt(i);
            if (ch < 0x20 || ch == 0x7f) return false;
        }
        return true;
    }

    /** Filter upstream links before exposing them inside the Launcher. */
    public static boolean safeResultUrl(String value) {
        if (value == null || value.isEmpty() || value.length() > 4096) return false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch <= 0x20 || (ch >= 0x7f && ch <= 0x9f)
                    || (ch >= 0x202a && ch <= 0x202e)
                    || (ch >= 0x2066 && ch <= 0x2069)) return false;
        }
        try {
            URI u = new URI(value);
            String scheme = u.getScheme();
            return scheme != null && ("http".equals(scheme.toLowerCase(Locale.ROOT))
                    || "https".equals(scheme.toLowerCase(Locale.ROOT)))
                    && u.getHost() != null && u.getUserInfo() == null;
        } catch (Exception ignored) {
            return false;
        }
    }
}
