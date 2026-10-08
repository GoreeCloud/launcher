# Launcher Portable Preferences — Development Boundary

## Status

Development source foundation only. This checkpoint does **not** establish complete GoreeCloud Launcher backup, restore, export, import, Everkeep integration, or production recovery.

## Purpose

`LauncherPortablePreferences` provides a versioned deterministic serialization boundary for the explicit local settings already represented by `LauncherPreferences`. It extends portability coverage beyond workspace geometry without giving a decoder authority to read or write Android DataStore, Room, packages, profiles, widgets, or the active HOME runtime.

Current format identifier:

`goreecloud-launcher-preferences`

Current format version:

`1`

## Included state

Version 1 contains exactly the seven settings currently represented by `LauncherPreferences`:

- Home grid columns;
- Home grid rows;
- App Drawer columns;
- app-label visibility;
- icon scale;
- layout-lock state; and
- GoreeCloud Index Home mode (`permanent` or `swipe_down_only`).

Icon scale is serialized as integer thousandths (`icon_scale_milli`) to avoid noncanonical floating-point text. The currently supported range is 850–1150, corresponding to the existing 0.85–1.15 Launcher preference contract.

## Validation behavior

The codec uses a bounded canonical UTF-8 line format with LF endings and a SHA-256 checksum over the canonical payload. The decoder requires exactly the supported records in the supported order and rejects:

- oversized input;
- CRLF/noncanonical line endings;
- unknown or extra records;
- missing or reordered records;
- unsupported format or schema versions;
- malformed/noncanonical integers or booleans;
- unknown Index Home modes;
- Home, Drawer, or icon-scale values outside the existing supported ranges; and
- checksum/integrity failures.

Unlike `LauncherPreferences.sanitized()`, portability decoding does **not** clamp invalid external data into a valid range. Invalid portable input fails closed so corruption or incompatible state is not silently converted into another user preference.

The SHA-256 checksum is corruption/integrity detection only. It is not encryption, authentication, a digital signature, or restore authorization.

## Authority boundary

Encoding and decoding are pure in-memory transformations. They do not:

- read or write Android DataStore;
- mutate `LauncherPreferencesRepository`;
- read or write Room workspace state;
- resolve installed applications or Android profiles;
- bind or rebind widgets;
- read wallpaper or Theme Manager state;
- contact GoreeCloud Index or another network/service boundary;
- change the default HOME application; or
- alter the running Launcher UI.

A future import/restore coordinator must separately establish explicit user intent, conflict behavior, transactional persistence, migration rules, and accepted Everkeep/privacy/security authority.

## Relationship to the workspace snapshot

`goreecloud-launcher-workspace-snapshot/1` and `goreecloud-launcher-preferences/1` are separate Development formats with separate scopes. The workspace format covers grid/page/item placement geometry. The preference format covers the seven current explicit `LauncherPreferences` values.

Neither format implies that the other is present, and neither should be merged into a product-wide backup claim simply because both codecs can round-trip their own bounded state.

## State still outside current portable coverage

The product specification requires a broader Launcher recovery scope. Current Development formats still do not establish complete portable/recovery treatment for, among other areas:

- Dock configuration and compatibility favorites state;
- folders and folder membership;
- shortcut-specific state;
- widgets, AppWidget identifiers, provider state, and safe rebinding;
- Theme Manager selections and broader icon/theme presentation state beyond the current icon-scale preference;
- gesture configuration beyond state explicitly represented in `LauncherPreferences`;
- hidden-application state;
- broader search-provider/search-setting state;
- Android package/profile identity and rebinding semantics;
- wallpaper state where product ownership applies; and
- future durable Launcher settings not yet represented by the current preference model.

## Everkeep and recovery boundary

These portable codecs are useful future inputs to an Everkeep recovery design, but they are not Everkeep integration. Launcher backup and restore remain required and pending until the complete approved product-owned state is versioned, transactional restore is implemented, Android package/profile/widget rebinding is defined, clean-target and migration testing passes, corruption/failure/rollback behavior is validated, and the applicable security/privacy/Everkeep acceptance evidence exists.

## Acceptance boundary

Passing codec unit tests proves only the tested source representation and validation behavior at the exact revision. It does not establish a user-facing export/import flow, production recovery, Everkeep acceptance, release acceptance, or Stable qualification.