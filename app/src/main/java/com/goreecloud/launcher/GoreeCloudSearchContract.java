package com.goreecloud.launcher;

import java.net.URI;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Contract owned by GoreeCloud/search, not a guessed third-party gateway. */
public final class GoreeCloudSearchContract {
    public static final String ENDPOINT = "https://search.goreecloud.com/api/search";
    public static final int MAX_QUERY_LENGTH = 500;
    public static final String RESULTS_HEADING = "GoreeCloud Search — sourced results, not AI text:";

    /** Exact offsets of first-party result URLs, not arbitrary AI-generated text. */
    public static final class SourcedLink {
        public final int start;
        public final int end;
        public final String url;

        private SourcedLink(int start, int end, String url) {
            this.start = start;
            this.end = end;
            this.url = url;
        }
    }

    /**
     * Only a trusted, first-party formatted response is eligible for link spans.
     * A title or snippet cannot create a new line because safePreviewText flattens
     * untrusted whitespace, and URLs are independently validated at tap time.
     */
    public static List<SourcedLink> sourcedResultLinks(String response) {
        if (response == null || !response.startsWith(RESULTS_HEADING)) {
            return Collections.emptyList();
        }
        List<SourcedLink> links = new ArrayList<>();
        int cursor = RESULTS_HEADING.length();
        while (cursor < response.length() && links.size() < 3) {
            int end = response.indexOf('\n', cursor);
            if (end < 0) end = response.length();
            String line = response.substring(cursor, end);
            if (safeResultUrl(line)) {
                links.add(new SourcedLink(cursor, end, line));
            }
            cursor = end + 1;
        }
        return links;
    }
    private GoreeCloudSearchContract() {}

    public static boolean validQuery(String question) {
        if (question == null) return false;
        String q = question.trim();
        if (q.length() < 2 || q.length() > MAX_QUERY_LENGTH) return false;
        for (int i = 0; i < q.length(); i++) {
            char ch = q.charAt(i);
            if (Character.isISOControl(ch) || Character.getType(ch) == Character.FORMAT) return false;
        }
        return true;
    }


    /** Sanitize untrusted snippets before bounding by Unicode code points. */
    public static String safePreviewText(String value, int maxCodePoints) {
        if (value == null || maxCodePoints <= 0) return "";
        StringBuilder result = new StringBuilder();
        int count = 0;
        boolean space = false;
        for (int i = 0; i < value.length() && count < maxCodePoints;) {
            int cp = value.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.getType(cp) == Character.FORMAT) continue;
            if (Character.isWhitespace(cp) || Character.isISOControl(cp)) {
                if (result.length() > 0) space = true;
                continue;
            }
            if (space) {
                result.append(' ');
                count++;
                space = false;
                if (count >= maxCodePoints) break;
            }
            result.appendCodePoint(cp);
            count++;
        }
        return result.toString();
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
