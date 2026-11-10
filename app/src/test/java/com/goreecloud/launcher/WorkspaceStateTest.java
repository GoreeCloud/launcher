package com.goreecloud.launcher;

import org.junit.Test;
import static org.junit.Assert.*;

public final class WorkspaceStateTest {
    @Test public void persistsPagesAndOrderedItems() {
        WorkspaceState state = new WorkspaceState();
        assertTrue(state.place("org.example/.Alpha"));
        assertTrue(state.place("org.example/.Beta"));
        assertTrue(state.addPage());
        assertTrue(state.place("org.other/.Gamma"));
        WorkspaceState restored = WorkspaceState.decode(state.encode());
        assertEquals(2, restored.pageCount());
        assertEquals(0, restored.currentPage());
        assertEquals(2, restored.currentItems().size());
        assertEquals("org.example/.Alpha", restored.currentItems().get(0));
        assertTrue(restored.selectPage(1));
        assertEquals("org.other/.Gamma", restored.currentItems().get(0));
    }

    @Test public void movesAppWithoutDuplication() {
        WorkspaceState state = new WorkspaceState();
        assertTrue(state.place("one/.One"));
        assertFalse(state.place("one/.One"));
        assertTrue(state.addPage());
        assertTrue(state.place("one/.One"));
        assertEquals(1, state.pageFor("one/.One"));
        assertTrue(state.selectPage(0));
        assertTrue(state.currentItems().isEmpty());
    }

    @Test public void refusesDeletionOfNonemptyOrLastPage() {
        WorkspaceState state = new WorkspaceState();
        assertFalse(state.removeCurrentEmptyPage());
        assertTrue(state.addPage());
        assertTrue(state.place("one/.One"));
        assertFalse(state.removeCurrentEmptyPage());
        assertTrue(state.remove("one/.One"));
        assertTrue(state.removeCurrentEmptyPage());
        assertEquals(1, state.pageCount());
    }

    @Test public void appliesCapacityAndBoundaryLimits() {
        WorkspaceState state = new WorkspaceState();
        for (int i = 0; i < WorkspaceState.MAX_ITEMS_PER_PAGE; i++) {
            assertTrue(state.place("app/.Item" + i));
        }
        assertFalse(state.place("app/.Overflow"));
        assertFalse(state.selectPage(-1));
        for (int i = 1; i < WorkspaceState.MAX_PAGES; i++) assertTrue(state.addPage());
        assertFalse(state.addPage());
    }

    @Test public void reordersWithinPageAndPersistsOrder() {
        WorkspaceState state = new WorkspaceState();
        assertTrue(state.place("org.test/.One"));
        assertTrue(state.place("org.test/.Two"));
        assertTrue(state.place("org.test/.Three"));
        assertFalse(state.moveWithinCurrentPage("org.test/.One", -1));
        assertFalse(state.moveWithinCurrentPage("org.test/.Absent", 1));
        assertFalse(state.moveWithinCurrentPage("org.test/.Two", 2));
        assertTrue(state.moveWithinCurrentPage("org.test/.Three", -1));
        assertEquals("org.test/.Three", state.currentItems().get(1));
        assertTrue(state.moveWithinCurrentPage("org.test/.Three", 1));
        assertEquals("org.test/.Three", WorkspaceState.decode(state.encode()).currentItems().get(2));
    }

    @Test public void adjacentPageMoveIsAtomicAndDurable() {
        WorkspaceState state = new WorkspaceState();
        assertTrue(state.place("pkg/.App"));
        assertFalse(state.moveToAdjacentPage("pkg/.App", -1));
        assertTrue(state.addPage());
        assertTrue(state.selectPage(0));
        assertTrue(state.moveToAdjacentPage("pkg/.App", 1));
        assertEquals(1, state.currentPage());
        assertEquals(1, state.pageFor("pkg/.App"));
        assertFalse(state.moveToAdjacentPage("pkg/.App", 1));
        assertTrue(state.moveToAdjacentPage("pkg/.App", -1));
        assertEquals(0, state.currentPage());
        assertEquals(0, WorkspaceState.decode(state.encode()).pageFor("pkg/.App"));
    }

    @Test public void fullDestinationDoesNotLoseSourceShortcut() {
        WorkspaceState state = new WorkspaceState();
        assertTrue(state.place("pkg/.Source"));
        assertTrue(state.addPage());
        for (int i = 0; i < WorkspaceState.MAX_ITEMS_PER_PAGE; i++) {
            assertTrue(state.place("pkg/.Target" + i));
        }
        assertTrue(state.selectPage(0));
        String original = state.encode();
        assertFalse(state.moveToAdjacentPage("pkg/.Source", 1));
        assertEquals(original, state.encode());
        assertEquals(0, state.pageFor("pkg/.Source"));
    }

    @Test public void malformedUtf8AndPaddedTokensCannotInventWorkspaceShortcuts() {
        WorkspaceState state = new WorkspaceState();
        assertTrue(state.place("org.good/.Main"));
        String valid = state.encode();
        // _w is Base64url for a single invalid UTF-8 0xff byte. The legacy
        // decoder turned it into a replacement-character shortcut.
        String padded = java.util.Base64.getUrlEncoder().encodeToString(
                "org.extra/.Other".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        WorkspaceState restored = WorkspaceState.decode("_w." + padded + "." + valid);
        assertEquals(1, restored.pageCount());
        assertEquals(1, restored.currentItems().size());
        assertEquals("org.good/.Main", restored.currentItems().get(0));
        assertEquals(valid, restored.encode());
    }

    @Test public void refusesUnpairedSurrogatesWithoutMutatingWorkspace() {
        WorkspaceState state = new WorkspaceState();
        assertTrue(state.place("org.good/.Main"));
        String original = state.encode();
        // Android identifiers should survive storage with the same code units.
        assertFalse(state.place("org.bad/." + (char) 0xD800));
        assertFalse(state.place("org.bad/." + (char) 0xDC00));
        assertEquals(original, state.encode());
        assertEquals(-1, state.pageFor("org.bad/.?"));
    }

    @Test public void importsMaximumValidUtf8TokenButSkipsOversizedTokens() {
        WorkspaceState state = new WorkspaceState();
        String longest = String.valueOf((char) 0x0800).repeat(256); // 768 bytes, 1024 token chars
        assertTrue(state.place(longest));
        String valid = state.encode();
        assertEquals(1024, valid.length());
        WorkspaceState restored = WorkspaceState.decode("A".repeat(2048) + "." + valid);
        assertEquals(1, restored.currentItems().size());
        assertEquals(longest, restored.currentItems().get(0));
        assertEquals(valid, restored.encode());
        // Multi-code-unit UTF-8 characters remain lossless, unlike unmatched surrogates.
        WorkspaceState supplementary = new WorkspaceState();
        assertTrue(supplementary.place("org.good/." + new String(Character.toChars(0x1F603))));
        assertEquals(supplementary.encode(),
                WorkspaceState.decode(supplementary.encode()).encode());
    }

    @Test public void maximumValidLocalWorkspacePersistsWithoutReset() {
        WorkspaceState full = new WorkspaceState();
        for (int page = 0; page < WorkspaceState.MAX_PAGES; page++) {
            if (page > 0) assertTrue(full.addPage());
            for (int item = 0; item < WorkspaceState.MAX_ITEMS_PER_PAGE; item++) {
                String prefix = "page" + page + "-item" + item + "/";
                // Every identifier remains valid at 256 UTF-16 units but uses
                // nearly four base64url characters per unit after UTF-8 encode.
                String id = prefix +
                        String.valueOf((char) 0x0800).repeat(256 - prefix.length());
                assertTrue("Valid max-sized ID should fit", full.place(id));
            }
        }
        String saved = full.encode();
        assertTrue("The old arbitrary limit discarded a valid Home state",
                saved.length() > 100000);

        WorkspaceState restarted = WorkspaceState.decode(saved);
        assertEquals(WorkspaceState.MAX_PAGES, restarted.pageCount());
        for (int page = 0; page < WorkspaceState.MAX_PAGES; page++) {
            assertTrue(restarted.selectPage(page));
            assertEquals(WorkspaceState.MAX_ITEMS_PER_PAGE, restarted.currentItems().size());
            for (int item = 0; item < WorkspaceState.MAX_ITEMS_PER_PAGE; item++) {
                String prefix = "page" + page + "-item" + item + "/";
                assertEquals(prefix +
                        String.valueOf((char) 0x0800).repeat(256 - prefix.length()),
                        restarted.currentItems().get(item));
            }
        }
        assertEquals("Every page and item must survive exact serialization", saved, restarted.encode());
    }

    @Test public void unreasonableImportedWorkspaceStillFailsClosed() {
        // A valid 7x24 layout cannot serialize to this many characters.
        assertEquals(1, WorkspaceState.decode("A".repeat(180000)).pageCount());
        assertTrue(WorkspaceState.decode("A".repeat(180000)).currentItems().isEmpty());
    }

    @Test public void corruptedLayoutRecoversWithoutDuplicateApps() {
        assertEquals(1, WorkspaceState.decode("not base64!!!!").pageCount());
        WorkspaceState state = new WorkspaceState();
        assertTrue(state.place("app/.One"));
        String serialized = state.encode();
        WorkspaceState restored = WorkspaceState.decode(serialized + "." + serialized + "|%%%");
        assertEquals(2, restored.pageCount());
        assertEquals(1, restored.currentItems().size());
        assertTrue(restored.selectPage(1));
        assertTrue(restored.currentItems().isEmpty());
    }
}
