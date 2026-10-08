# Launcher portable preference import

Status: Development source only.

This slice adds a bounded persistence seam for the existing `goreecloud-launcher-preferences/1` format. It does not broaden the format or turn the two current Launcher portability codecs into a complete backup/restore system.

## Validation before persistence

`LauncherPortablePreferenceImport.apply(...)` decodes the complete snapshot before a writer is invoked. Tampered, oversized, malformed, expanded, out-of-range, or unsupported input therefore produces zero persistence calls.

The importer accepts exactly the seven already-approved v1 values:

- Home columns;
- Home rows;
- Drawer columns;
- show labels;
- icon scale;
- layout locked; and
- GoreeCloud Index Home mode.

## Atomic DataStore application

`LauncherPreferencesRepository` implements the minimal `LauncherPortablePreferenceWriter` boundary and writes all seven values in one DataStore `edit` transaction. It does not call the older independently scheduled per-setting setters.

Before the transaction, the repository reuses the portable codec's strict validation. Imported values are not silently clamped through `LauncherPreferences.sanitized()`.

A storage exception is allowed to propagate. The importer does not return an `Applied` result after a failed persistence commit.

## Excluded authority

This seam does not read or write:

- Room workspace/page/placement state;
- dock/favorites compatibility state;
- folders;
- widgets or widget-provider bindings;
- packages or Android profiles;
- Theme Manager state or wallpaper;
- gestures;
- hidden applications;
- search state;
- GoreeCloud Identity state; or
- Everkeep metadata.

It also provides no user-facing file picker, export/import screen, artifact ownership proof, conflict policy, rollback coordinator, clean-target restore orchestration, cross-device synchronization, or multi-format recovery transaction.

## Acceptance boundary

This is a real but partial local write capability for the seven low-level Launcher preference values. Workspace recovery, complete Launcher backup/restore, Everkeep acceptance, Privacy Shield/Wardveil acceptance, representative-device validation, release, production eligibility, and Stable qualification remain separate gates.
