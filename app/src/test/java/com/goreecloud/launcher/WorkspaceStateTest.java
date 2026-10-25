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
