package com.goreecloud.launcher;

public final class DockMaterial {
    public static final String KEY = "dock_material_v1";
    public static final String FROSTED = "frosted";
    public static final String CLEAR = "clear";
    public static final String SOLID = "solid";
    private DockMaterial() {}

    public static String normalize(String value) {
        return CLEAR.equals(value) || SOLID.equals(value) ? value : FROSTED;
    }

    public static int index(String value) {
        return CLEAR.equals(normalize(value)) ? 1 : SOLID.equals(normalize(value)) ? 2 : 0;
    }

    public static String fromIndex(int index) {
        return index == 1 ? CLEAR : index == 2 ? SOLID : FROSTED;
    }

    public static String label(String value) {
        switch (normalize(value)) {
            case CLEAR: return "Clear Glass";
            case SOLID: return "Solid";
            default: return "Frosted Glass";
        }
    }
}
