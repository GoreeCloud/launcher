package com.goreecloud.launcher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

/** Validates hostile-but-checksummed local files, not only accidental corruption. */
public final class BackupCodecValidationTest {
    private static String encoded(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String validBackup() {
        WorkspaceState state = new WorkspaceState();
        assertTrue(state.place("org.test/.Main"));
        assertTrue(state.addPage());
        assertTrue(state.place("org.test/.Other"));
        return BackupCodec.encode(new BackupCodec.Data(
                state.encode(), 1, 4, true, Collections.singletonList("org.test/.Main")));
    }

    /** Recompute a VALID SHA-256 after manipulating a line to test semantic validation. */
    private static String replaceAndResign(String valid, int line, String text) throws Exception {
        String[] parts = valid.split("\\n", -1);
        assertEquals(8, parts.length);
        parts[line] = text;
        String payload = String.join("\n", Arrays.copyOfRange(parts, 0, 6)) + "\n";
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte byteValue : digest)
            hex.append(String.format(java.util.Locale.ROOT, "%02x", byteValue & 0xff));
        return payload + "sha256=" + hex + "\n";
    }

    private static void rejected(String input) {
        try {
            BackupCodec.decode(input);
            fail("Invalid checksum-valid backup accepted");
        } catch (IllegalArgumentException expected) {
            // Import must fail closed before replacing any preference.
        }
    }

    @Test public void validNestedComponentIdentifiersStillRoundTrip() {
        WorkspaceState state = new WorkspaceState();
        assertTrue(state.place("org.goreecloud.qa/.Inner$Launch"));
        BackupCodec.Data data = new BackupCodec.Data(state.encode(), 0, 4, true,
                Collections.singletonList("org.goreecloud.qa/.Inner$Launch"));
        assertEquals(BackupCodec.encode(data),
                BackupCodec.encode(BackupCodec.decode(BackupCodec.encode(data))));
    }

    @Test public void invalidWorkspaceShortcutRejectedEvenWithValidDigest() throws Exception {
        String baseline = validBackup();
        WorkspaceState malformed = new WorkspaceState();
        assertTrue(malformed.place("not a component"));
        String forged = replaceAndResign(baseline, 4,
                "workspace=" + encoded(malformed.encode()));
        rejected(forged);
        try {
            BackupCodec.encode(new BackupCodec.Data(malformed.encode(), 0, 4, true,
                    Collections.emptyList()));
            fail("Invalid local workspace should not be exported");
        } catch (IllegalArgumentException expected) {}
    }

    @Test public void checksumCannotBypassPinAndSettingValidation() throws Exception {
        String baseline = validBackup();
        rejected(replaceAndResign(baseline, 5,
                "pins=" + encoded("invalid component id")));
        rejected(replaceAndResign(baseline, 3, "labels=2"));
        rejected(replaceAndResign(baseline, 2, "columns=9"));
        rejected(replaceAndResign(baseline, 1, "page=7"));
    }

    @Test public void duplicateShortcutsAndPinsRejectedEvenIfChecksumMatches() throws Exception {
        String baseline = validBackup();
        String shortcut = encoded("org.test/.Main");
        rejected(replaceAndResign(baseline, 4,
                "workspace=" + encoded(shortcut + "|" + shortcut)));
        rejected(replaceAndResign(baseline, 5,
                "pins=" + shortcut + "," + shortcut));
    }

    @Test public void malformedEncodingCannotBeResignedIntoValidBackup() throws Exception {
        String baseline = validBackup();
        rejected(replaceAndResign(baseline, 4, "workspace=***"));
        rejected(replaceAndResign(baseline, 5, "pins=###"));
        rejected(replaceAndResign(baseline, 0, "GOREELAUNCHER/9"));
    }
}
