package com.goreecloud.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Small, privacy-preserving ordering policy for at most five pinned apps. */
public final class DockOrder {
    private DockOrder() {}

    /** Retain known slots; append legacy/missing pins in deterministic order. */
    public static List<String> reconcile(Set<String> pins, String stored) {
        List<String> order = new ArrayList<>();
        if (stored != null && !stored.isEmpty()) {
            for (String key : stored.split("\\|", -1)) {
                if (pins.contains(key) && !order.contains(key)) order.add(key);
            }
        }
        List<String> missing = new ArrayList<>(pins);
        missing.removeAll(order);
        Collections.sort(missing);
        order.addAll(missing);
        return order;
    }

    /** Component identifiers cannot contain the pipe separator. */
    public static String encode(List<String> order) {
        return String.join("|", order);
    }

    /** Swap two visible apps while leaving unavailable app placeholders in place. */
    public static boolean swap(List<String> order, String first, String second) {
        int a = order.indexOf(first), b = order.indexOf(second);
        if (a < 0 || b < 0 || a == b) return false;
        Collections.swap(order, a, b);
        return true;
    }

    public static boolean move(List<String> order, String key, int offset) {
        if (offset != -1 && offset != 1) return false;
        int before = order.indexOf(key);
        int after = before + offset;
        if (before < 0 || after < 0 || after >= order.size()) return false;
        Collections.swap(order, before, after);
        return true;
    }
}
