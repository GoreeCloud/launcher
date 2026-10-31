package com.goreecloud.launcher;

/**
 * Native Android mapping of the GC UI semantic accent pairs.
 *
 * Palette values mirror GoreeCloud/gc-ui/tokens/gc.tokens.json (alpha),
 * with neutral as the unconfigured ecosystem default. UI components use roles;
 * the palette ID is never inferred from the app's previous hardcoded color.
 */
final class GcUiPalette {
    static final String PREFERENCE_KEY = "gc_ui_accent_v1";
    static final String DEFAULT = "neutral";
    static final String[] IDS = {"neutral", "ocean", "lagoon", "moss", "iris", "ember"};
    static final String[] LABELS = {"Neutral", "Blue", "Teal", "Green", "Purple", "Terracotta"};
    static final int CANVAS = 0xFFF1F5F9;
    static final int SURFACE = 0xFFFFFFFF;
    static final int SURFACE_TONAL = 0xFFE2E8F0;
    static final int ON_SURFACE = 0xFF1F2937;
    static final int ON_SURFACE_MUTED = 0xFF475569;

    static final class Roles {
        final int primary, onPrimary, container, onContainer;
        Roles(int primary, int onPrimary, int container, int onContainer) {
            this.primary = primary;
            this.onPrimary = onPrimary;
            this.container = container;
            this.onContainer = onContainer;
        }
    }
    private static final Roles NEUTRAL = new Roles(0xFF374151, 0xFFFFFFFF, 0xFFE5E7EB, 0xFF111827);
    private static final Roles OCEAN = new Roles(0xFF1D4ED8, 0xFFFFFFFF, 0xFFDBEAFE, 0xFF1E3A8A);
    private static final Roles LAGOON = new Roles(0xFF087C8D, 0xFFFFFFFF, 0xFFD1F4F5, 0xFF07565D);
    private static final Roles MOSS = new Roles(0xFF3F744B, 0xFFFFFFFF, 0xFFD8F1D9, 0xFF25542D);
    private static final Roles IRIS = new Roles(0xFF5145D9, 0xFFFFFFFF, 0xFFE8E6FF, 0xFF2C227C);
    private static final Roles EMBER = new Roles(0xFFAE4D3F, 0xFFFFFFFF, 0xFFFFE2D7, 0xFF6E3024);

    static String normalize(String preference) {
        if (preference == null) return DEFAULT;
        for (String id : IDS) if (id.equals(preference)) return id;
        return DEFAULT;
    }
    static int selectedIndex(String preference) {
        String normalized = normalize(preference);
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(normalized)) return i;
        return 0;
    }
    static Roles resolve(String preference) {
        switch (normalize(preference)) {
            case "ocean": return OCEAN;
            case "lagoon": return LAGOON;
            case "moss": return MOSS;
            case "iris": return IRIS;
            case "ember": return EMBER;
            default: return NEUTRAL;
        }
    }
    private GcUiPalette() {}
}
