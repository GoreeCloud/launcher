# GoreeCloud Launcher — Local Backup Format (v1)

This document specifies the manual user-directed backup, not a cloud sync or security product.

## User controls

The native Home settings screen offers Export backup and Import backup via Android's Storage Access Framework (ACTION_CREATE_DOCUMENT and ACTION_OPEN_DOCUMENT). The user chooses a document provider/location. No broad storage permission, networking permission or background cloud connection is required.

**Privacy:** The saved file contains package/component identifiers, workspace page order, dock pins, grid columns, and icon-label preference. It contains no passwords, account tokens, media, or widget data. It is plaintext and not encrypted; the checksum detects accidental corruption but does not authenticate the file. Do not share backups publicly. A chosen cloud document provider might synchronize the file under its own terms.

## Wire format

UTF-8 text, strict LF delimiters, maximum 65,536 bytes. Seven lines and final newline:

    GOREELAUNCHER/1
    page=<0-based selected page>
    columns=<integer 3..6>
    labels=<0|1>
    workspace=<unpadded base64url WorkspaceState.encode()>
    pins=<comma-separated unpadded base64url component IDs>
    sha256=<lowercase SHA-256 of preceding lines including LF endings>

Import validation also checks the semantic shape of every saved Home shortcut and dock pin as an Android component identifier (`package/activity`), not just whether the Base64URL workspace encoding is canonical. A checksum is intentionally not treated as authentication: a sender can recompute it. The [backup-validation tests](https://github.com/GoreeCloud/launcher/blob/main/app/src/test/java/com/goreecloud/launcher/BackupCodecValidationTest.java) include deliberately modified, correctly checksummed files with bad component IDs, invalid settings, duplicate shortcuts/pins, and malformed encodings. [Android CI 40271012691](https://github.com/GoreeCloud/launcher/actions/runs/40271012691) passed these Java tests. This is code/unit evidence, not a claim of end-to-end physical-device provider coverage.

The page data uses the existing canonical serialization: 1–7 pages, up to 24 unique shortcuts per page; dock pins are max 5 unique component IDs. Unknown versions, noncanonical tokens, corruption, excessive sizes, unsafe bounds and duplicate pins are rejected before confirmation. Confirmed import writes supported settings through one SharedPreferences.Editor.commit() and returns to Home for refresh. Missing apps stay represented in saved state.

The format is intentionally limited: no widget IDs, work profiles, folder models, profiles, wallpaper, login/credential information, or private application storage. Future schema versions require separate migration planning and regression tests.

## Verification stages

BackupCodecTest covers portable round-trip, canonical serialization, checksum tampering, unsupported/oversized input, bounds, duplicate pins and empty layouts. API 35 emulator run 40251850733 verified real synthetic backup file saving, hash inspection, import, cancel, confirm, immediate Settings refresh and page/dock persistence after restart. CI 40251831635 passed. Evidence artifact contains screenshots, not the saved file. This is single-emulator acceptance. A later [API 35 negative-import run 40253831973](https://github.com/GoreeCloud/launcher/actions/runs/40253831973) also passed on commit 38d6e13. The test replaced only its synthetic emulator Downloads document with: (1) a modified setting with stale SHA-256 digest, (2) 65,537 bytes exceeding the 65,536-byte cap, and (3) an unsupported format header. All three file imports returned to Settings without showing the destructive replacement dialog; the restored label preference, Home page and dock shortcut remained intact. This run also repeated the valid export/import/cancel/confirm cycle and bounded sparse-grid check. Evidence artifact 12622155934 contains screenshots and UI hierarchy files, never exported backup contents. Different document providers, access-denied/low-space errors, accessibility and physical devices remain unverified.
