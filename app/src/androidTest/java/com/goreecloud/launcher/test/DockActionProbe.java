package com.goreecloud.launcher.test;
import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Emulator-only probe; does not establish physical screen-reader acceptance. */
public final class DockActionProbe extends Instrumentation {
    private static final String[] BEFORE = {
        "Launcher QA Fixture", "Launcher Fixture Notes", "Launcher Fixture Beta",
        "Launcher Fixture Delta", "Launcher Fixture Gamma"
    };
    private static final String[] AFTER = {
        "Launcher QA Fixture", "Launcher Fixture Notes", "Launcher Fixture Beta",
        "Launcher Fixture Gamma", "Launcher Fixture Delta"
    };
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setClassName("com.goreecloud.launcher",
                    "com.goreecloud.launcher.LauncherActivity");
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getTargetContext().startActivity(home);
            UiAutomation auto = getUiAutomation();
            awaitOrder(auto, BEFORE);
            check(!hasAction(tile(auto, BEFORE[0]), "Move left in dock"), "First tile allows left");
            AccessibilityNodeInfo delta = tile(auto, "Launcher Fixture Delta");
            check(hasAction(delta, "Move left in dock"), "Middle tile lacks left");
            invoke(delta, "Move right in dock");
            awaitOrder(auto, AFTER);
            AccessibilityNodeInfo last = tile(auto, "Launcher Fixture Delta");
            check(!hasAction(last, "Move right in dock"), "Last tile allows right");
            invoke(last, "Move left in dock");
            awaitOrder(auto, BEFORE);
            invoke(tile(auto, "Launcher Fixture Delta"), "Move right in dock");
            awaitOrder(auto, AFTER);
            result.putString("result", "PASS custom accessibility actions in both directions");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("error", String.valueOf(error));
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
    private static AccessibilityNodeInfo tile(UiAutomation auto, String name) {
        AccessibilityNodeInfo root = auto.getRootInActiveWindow();
        check(root != null, "No active accessible root");
        List<AccessibilityNodeInfo> matches = new ArrayList<>();
        find(root, name, matches);
        check(matches.size() == 1, "Expected one tile " + name + ": " + matches.size());
        AccessibilityNodeInfo node = matches.get(0);
        check("android.widget.Button".contentEquals(node.getClassName()), "Not a Button: " + name);
        check(node.isClickable(), "Tile not clickable: " + name);
        return node;
    }
    private static void find(AccessibilityNodeInfo node, String name,
            List<AccessibilityNodeInfo> matches) {
        CharSequence desc = node.getContentDescription();
        if (desc != null && name.contentEquals(desc)) matches.add(node);
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) find(child, name, matches);
        }
    }
    private static boolean hasAction(AccessibilityNodeInfo node, String label) {
        for (AccessibilityNodeInfo.AccessibilityAction action : node.getActionList()) {
            if (action.getLabel() != null && label.contentEquals(action.getLabel())) return true;
        }
        return false;
    }
    private static void invoke(AccessibilityNodeInfo node, String label) {
        for (AccessibilityNodeInfo.AccessibilityAction action : node.getActionList()) {
            if (action.getLabel() != null && label.contentEquals(action.getLabel())) {
                check(node.performAction(action.getId()), "Action rejected: " + label);
                return;
            }
        }
        throw new IllegalStateException("Action missing: " + label);
    }
    private static void awaitOrder(UiAutomation auto, String[] expected)
            throws InterruptedException {
        String observed = "not ready";
        for (int n = 0; n < 20; n++) {
            try {
                List<AccessibilityNodeInfo> tiles = new ArrayList<>();
                for (String name : BEFORE) tiles.add(tile(auto, name));
                tiles.sort(Comparator.comparingInt(node -> {
                    Rect r = new Rect(); node.getBoundsInScreen(r); return r.left;
                }));
                List<String> order = new ArrayList<>();
                for (AccessibilityNodeInfo tile : tiles)
                    order.add(String.valueOf(tile.getContentDescription()));
                if (order.equals(Arrays.asList(expected))) return;
                observed = order.toString();
            } catch (IllegalStateException ex) { observed = ex.getMessage(); }
            Thread.sleep(400);
        }
        throw new IllegalStateException("Expected " + Arrays.toString(expected) + " got " + observed);
    }
}
