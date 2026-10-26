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

The page data uses the existing canonical serialization: 1–7 pages, up to 24 unique shortcuts per page; dock pins are max 5 unique component IDs. Unknown versions, noncanonical tokens, corruption, excessive sizes, unsafe bounds and duplicate pins are rejected before confirmation. Confirmed import writes supported settings through one SharedPreferences.Editor.commit() and returns to Home for refresh. Missing apps stay represented in saved state.

The format is intentionally limited: no widget IDs, work profiles, folder models, profiles, wallpaper, login/credential information, or private application storage. Future schema versions require separate migration planning and regression tests.

## Verification stages

BackupCodecTest covers portable round-trip, canonical serialization, checksum tampering, unsupported/oversized input, bounds, duplicate pins and empty layouts. The native emulator smoke checks backup controls, document-picker entry/dismissal and regression safety. Complete user-selected export/write and import/read/confirmation workflows require testing before claiming runtime-verified portable backup.
