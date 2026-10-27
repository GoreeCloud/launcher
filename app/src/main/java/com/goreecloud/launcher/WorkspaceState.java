package com.goreecloud.launcher;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A small, local, versioned home-page layout. No permission or network dependency. */
public final class WorkspaceState {
    public static final int MAX_PAGES = 7;
    public static final int MAX_ITEMS_PER_PAGE = 24;
    private final List<List<String>> pages = new ArrayList<>();
    private int selected;

    public WorkspaceState() { pages.add(new ArrayList<>()); }

    public int currentPage() { return selected; }
    public int pageCount() { return pages.size(); }
    public List<String> currentItems() {
        return Collections.unmodifiableList(pages.get(selected));
    }
    public boolean selectPage(int next) {
        if (next < 0 || next >= pages.size()) return false;
        selected = next;
        return true;
    }
    public boolean addPage() {
        if (pages.size() >= MAX_PAGES) return false;
        pages.add(new ArrayList<>());
        selected = pages.size() - 1;
        return true;
    }
    public boolean removeCurrentEmptyPage() {
        if (pages.size() == 1 || !pages.get(selected).isEmpty()) return false;
        pages.remove(selected);
        selected = Math.min(selected, pages.size() - 1);
        return true;
    }
    public int pageFor(String id) {
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).contains(id)) return i;
        }
        return -1;
    }
    public boolean place(String id) {
        if (id == null || id.isEmpty() || id.length() > 256 ||
                pageFor(id) == selected || pages.get(selected).size() >= MAX_ITEMS_PER_PAGE) {
            return false;
        }
        remove(id);
        pages.get(selected).add(id);
        return true;
    }
    /** Places or reorders a shortcut in an exact grid slot on any existing page. */
    public boolean placeAt(String id, int page, int slot) {
        if (id == null || id.isEmpty() || id.length() > 256
                || page < 0 || page >= pages.size()) return false;
        List<String> target = pages.get(page);
        int oldPage = pageFor(id);
        if (oldPage != page && target.size() >= MAX_ITEMS_PER_PAGE) return false;
        if (oldPage >= 0) pages.get(oldPage).remove(id);
        target.add(Math.max(0, Math.min(slot, target.size())), id);
        selected = page;
        return true;
    }

    public boolean remove(String id) {
        boolean removed = false;
        for (List<String> page : pages) removed |= page.remove(id);
        return removed;
    }
    /** Moves a shortcut by one grid slot; false means no state change. */
    public boolean moveWithinCurrentPage(String id, int direction) {
        if (direction != -1 && direction != 1) return false;
        List<String> entries = pages.get(selected);
        int from = entries.indexOf(id);
        int to = from + direction;
        if (from < 0 || to < 0 || to >= entries.size()) return false;
        Collections.swap(entries, from, to);
        return true;
    }
    /** Moves the shortcut to an existing neighboring page without dropping any saved data. */
    public boolean moveToAdjacentPage(String id, int direction) {
        if (direction != -1 && direction != 1) return false;
        int target = selected + direction;
        if (target < 0 || target >= pages.size()) return false;
        List<String> source = pages.get(selected), destination = pages.get(target);
        if (!source.contains(id) || destination.size() >= MAX_ITEMS_PER_PAGE) return false;
        source.remove(id);
        destination.add(id);
        selected = target;
        return true;
    }
    /** Base64url tokens separated by dots; pages by pipes. Both separators are outside the alphabet. */
    public String encode() {
        StringBuilder result = new StringBuilder();
        for (int p = 0; p < pages.size(); p++) {
            if (p > 0) result.append('|');
            for (int i = 0; i < pages.get(p).size(); i++) {
                if (i > 0) result.append('.');
                result.append(Base64.getUrlEncoder().withoutPadding().encodeToString(
                        pages.get(p).get(i).getBytes(StandardCharsets.UTF_8)));
            }
        }
        return result.toString();
    }
    public static WorkspaceState decode(String raw) {
        WorkspaceState state = new WorkspaceState();
        if (raw == null || raw.isEmpty() || raw.length() > 100000) return state;
        state.pages.clear();
        Set<String> seen = new HashSet<>();
        String[] serializedPages = raw.split("\\|", -1);
        for (int p = 0; p < serializedPages.length && p < MAX_PAGES; p++) {
            List<String> contents = new ArrayList<>();
            if (!serializedPages[p].isEmpty()) {
                for (String token : serializedPages[p].split("\\.")) {
                    if (contents.size() >= MAX_ITEMS_PER_PAGE) break;
                    try {
                        String id = new String(Base64.getUrlDecoder().decode(token),
                                StandardCharsets.UTF_8);
                        if (!id.isEmpty() && id.length() <= 256 && seen.add(id)) contents.add(id);
                    } catch (IllegalArgumentException ex) {
                        // Invalid imported/corrupted tokens are skipped without crashing Home.
                    }
                }
            }
            state.pages.add(contents);
        }
        if (state.pages.isEmpty()) state.pages.add(new ArrayList<>());
        return state;
    }
}
