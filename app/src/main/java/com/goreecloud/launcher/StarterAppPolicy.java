package com.goreecloud.launcher;

import java.util.Arrays;
import java.util.List;

public final class StarterAppPolicy {
    private StarterAppPolicy() {}
    public static final String[] ROLES = {"Phone", "Messages", "Email", "Browser", "Camera"};

    public static List<String> aliases(String role) {
        switch (role) {
            case "Phone": return Arrays.asList("Phone", "Dialer");
            case "Messages": return Arrays.asList("Messages", "Messaging");
            case "Email": return Arrays.asList("Email", "Gmail", "Outlook");
            case "Browser": return Arrays.asList("Browser", "Chrome", "Firefox", "Internet");
            case "Camera": return Arrays.asList("Camera", "Open Camera");
            default: return java.util.Collections.emptyList();
        }
    }
    public static int boundedRows(int rows) {
        return Math.max(4, Math.min(7, rows));
    }
}
