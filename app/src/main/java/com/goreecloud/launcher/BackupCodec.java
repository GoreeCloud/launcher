package com.goreecloud.launcher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Strict, versioned plaintext launcher configuration export. The SHA-256 checksum
 * detects accidental corruption; it does NOT authenticate or encrypt the backup.
 * This format contains package/component identifiers and should be stored privately.
 */
public final class BackupCodec {
    public static final int MAX_BYTES = 65536;
    private static final String MAGIC = "GOREELAUNCHER/1";
    private BackupCodec() {}

    public static final class Data {
        public final String workspace;
        public final int selectedPage, columns;
        public final boolean labels;
        public final List<String> pins;

        public Data(String workspace, int selectedPage, int columns,
                    boolean labels, List<String> pins) {
            this.workspace = workspace;
            this.selectedPage = selectedPage;
            this.columns = columns;
            this.labels = labels;
            this.pins = Collections.unmodifiableList(new ArrayList<>(pins));
        }
        public int pageCount() { return WorkspaceState.decode(workspace).pageCount(); }
        public int shortcutCount() {
            WorkspaceState state = WorkspaceState.decode(workspace);
            int count = 0;
            for (int i = 0; i < state.pageCount(); i++) {
                state.selectPage(i);
                count += state.currentItems().size();
            }
            return count;
        }
    }

    private static String token(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }
    private static String untoken(String value) {
        if (!value.matches("[A-Za-z0-9_-]*")) throw new IllegalArgumentException("Invalid encoding");
        byte[] decoded = Base64.getUrlDecoder().decode(value);
        String text = new String(decoded, StandardCharsets.UTF_8);
        if (!token(text).equals(value)) throw new IllegalArgumentException("Noncanonical text encoding");
        return text;
    }
    private static void validate(Data data) {
        if (data.workspace == null || data.workspace.length() > 60000)
            throw new IllegalArgumentException("Workspace size invalid");
        WorkspaceState state = WorkspaceState.decode(data.workspace);
        if (!state.encode().equals(data.workspace))
            throw new IllegalArgumentException("Workspace layout damaged");
        if (data.selectedPage < 0 || data.selectedPage >= state.pageCount())
            throw new IllegalArgumentException("Selected page out of range");
        if (data.columns < 3 || data.columns > 6)
            throw new IllegalArgumentException("Unsupported grid columns");
        if (data.pins == null || data.pins.size() > 5)
            throw new IllegalArgumentException("Dock exceeds capacity");
        Set<String> unique = new HashSet<>(data.pins);
        if (unique.size() != data.pins.size())
            throw new IllegalArgumentException("Duplicate dock entries");
        for (String id : data.pins) {
            if (id == null || id.length() > 256
                    || !id.matches("[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+"))
                throw new IllegalArgumentException("Invalid dock component");
        }
    }
    private static String checksum(String payload) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest) hex.append(String.format(java.util.Locale.ROOT, "%02x", b & 0xff));
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    public static String encode(Data data) {
        validate(data);
        List<String> sorted = new ArrayList<>(data.pins);
        Collections.sort(sorted);
        List<String> encodedPins = new ArrayList<>();
        for (String pin : sorted) encodedPins.add(token(pin));
        String payload = MAGIC + "\n"
                + "page=" + data.selectedPage + "\n"
                + "columns=" + data.columns + "\n"
                + "labels=" + (data.labels ? "1" : "0") + "\n"
                + "workspace=" + token(data.workspace) + "\n"
                + "pins=" + String.join(",", encodedPins) + "\n";
        String content = payload + "sha256=" + checksum(payload) + "\n";
        if (content.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES)
            throw new IllegalArgumentException("Backup exceeds size limit");
        return content;
    }
    public static Data decode(String content) {
        if (content == null || content.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES)
            throw new IllegalArgumentException("Backup exceeds size limit");
        String[] lines = content.split("\n", -1);
        if (lines.length != 8 || !lines[7].isEmpty() || !lines[0].equals(MAGIC)
                || !lines[1].startsWith("page=") || !lines[2].startsWith("columns=")
                || !lines[3].startsWith("labels=") || !lines[4].startsWith("workspace=")
                || !lines[5].startsWith("pins=") || !lines[6].startsWith("sha256="))
            throw new IllegalArgumentException("Unsupported backup format");
        String payload = String.join("\n", java.util.Arrays.copyOfRange(lines, 0, 6)) + "\n";
        String actual = lines[6].substring(7);
        if (!actual.matches("[0-9a-f]{64}") || !MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.US_ASCII),
                checksum(payload).getBytes(StandardCharsets.US_ASCII)))
            throw new IllegalArgumentException("Backup integrity check failed");
        try {
            String cols = lines[2].substring(8), page = lines[1].substring(5);
            if (!cols.matches("[3-6]") || !page.matches("[0-6]"))
                throw new IllegalArgumentException("Invalid backup settings");
            String lbl = lines[3].substring(7);
            if (!lbl.equals("0") && !lbl.equals("1"))
                throw new IllegalArgumentException("Invalid label setting");
            String[] rawPins = lines[5].substring(5).isEmpty() ? new String[0]
                    : lines[5].substring(5).split(",", -1);
            List<String> pins = new ArrayList<>();
            for (String pin : rawPins) pins.add(untoken(pin));
            Data restored = new Data(untoken(lines[4].substring(10)),
                    Integer.parseInt(page), Integer.parseInt(cols), lbl.equals("1"), pins);
            validate(restored);
            if (!encode(restored).equals(content))
                throw new IllegalArgumentException("Backup is not canonical");
            return restored;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Cannot import backup: " + ex.getMessage(), ex);
        }
    }
}
