package com.goreecloud.launcher;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public final class BackupCodecTest {
    private BackupCodec.Data fixture() {
        WorkspaceState state = new WorkspaceState();
        assertTrue(state.place("com.goreecloud.demo/.Main"));
        assertTrue(state.addPage());
        assertTrue(state.place("com.goreecloud.notes/.Notes"));
        return new BackupCodec.Data(state.encode(), 1, 5, false,
                Arrays.asList("com.goreecloud.notes/.Notes", "com.goreecloud.demo/.Main"));
    }
    @Test public void portableRoundTripAndOrdering() {
        String encoded = BackupCodec.encode(fixture());
        BackupCodec.Data restored = BackupCodec.decode(encoded);
        assertEquals(2, restored.pageCount());
        assertEquals(2, restored.shortcutCount());
        assertEquals(1, restored.selectedPage);
        assertEquals(5, restored.columns);
        assertFalse(restored.labels);
        assertEquals(2, restored.pins.size());
        assertEquals(encoded, BackupCodec.encode(restored));
    }
    @Test public void tamperingIsRejectedWithoutChanges() {
        String backup = BackupCodec.encode(fixture());
        try {
            BackupCodec.decode(backup.replace("columns=5", "columns=6"));
            fail("Integrity validation required");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("integrity"));
        }
    }
    @Test public void unsupportedOrOversizedDataFails() {
        for (String bad : Arrays.asList("garbage", "GOREELAUNCHER/99\n", "x".repeat(BackupCodec.MAX_BYTES+1))) {
            try { BackupCodec.decode(bad); fail("Invalid backup accepted"); }
            catch (IllegalArgumentException expected) {}
        }
    }
    @Test public void unsafeSettingsAreRejectedBeforeExport() {
        BackupCodec.Data valid = fixture();
        for (BackupCodec.Data bad : Arrays.asList(
            new BackupCodec.Data(valid.workspace, 7, 5, true, valid.pins),
            new BackupCodec.Data(valid.workspace, 1, 9, true, valid.pins),
            new BackupCodec.Data(valid.workspace, 1, 5, true,
                Arrays.asList("com.test/.A","com.test/.A")),
            new BackupCodec.Data("%%%bad", 0, 4, true, Collections.emptyList()))) {
            try { BackupCodec.encode(bad); fail("Bad state exported"); }
            catch (IllegalArgumentException expected) {}
        }
    }
    @Test public void v2BackupPreservesCustomDockOrder() {
        BackupCodec.Data state = fixture();
        String exported = BackupCodec.encode(state);
        assertTrue(exported.startsWith("GOREELAUNCHER/2\n"));
        assertEquals(state.pins, BackupCodec.decode(exported).pins);
    }

    @Test public void emptyHomeAndDockRestore() {
        BackupCodec.Data state = new BackupCodec.Data("", 0, 4, true, Collections.emptyList());
        BackupCodec.Data restored = BackupCodec.decode(BackupCodec.encode(state));
        assertEquals(1, restored.pageCount());
        assertEquals(0, restored.shortcutCount());
        assertEquals(0, restored.pins.size());
    }
}
