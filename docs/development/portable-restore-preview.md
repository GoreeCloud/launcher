# Launcher Portable Restore Preview

Status: Development source candidate.

`LauncherPortableRestorePreview` is a read-only preflight for the two currently reviewed Launcher portability formats: the workspace placement snapshot and the seven-field Launcher preference snapshot.

Both complete payloads must pass their existing strict decoders and the same pair-level compatibility validation used by `LauncherPortableRestoreImport` before a preview is returned. Invalid, tampered, expanded, unsupported, noncanonical, or mutually incompatible input is rejected with a workspace/preferences/compatibility source distinction.

For the current combined contract, the workspace grid columns and rows must exactly match the Home-grid columns and rows in the portable preference snapshot. This prevents a preview from presenting two individually valid payloads as restorable when they would create contradictory Home geometry.

The preview exposes only:

- workspace grid columns and rows;
- page count and total placement count;
- home columns, home rows, drawer columns, label visibility, icon scale, layout-lock state, and GoreeCloud Index Home mode; and
- one opaque SHA-256 review token that binds the exact validated workspace/preference byte pair.

The review token is a local time-of-review/time-of-apply consistency guard only. A future user-facing flow can pass it to `LauncherPortableRestoreImport.applyReviewed(...)`; if either otherwise-valid payload changes after preview, the apply path rejects the pair before the persistence writer is invoked. The token is not a signature, artifact-authenticity proof, owner identity, recovery lineage, Everkeep provenance, or authorization grant.

The aggregate summary intentionally does not expose page IDs, item IDs, package names, Android profile identities, widget bindings, folder/dock contents, shortcuts, wallpaper/theme state, hidden applications, broader search state, or any unreviewed Launcher preference.

The preview has no `LauncherPortableRestoreWriter`, Room, DataStore, package/profile discovery, widget, HOME-role, or persistence authority. A successful preview is therefore not a restore, backup, Everkeep recovery, migration, or proof that the target device can materialize every referenced application item.

A later user-facing restore workflow must still establish explicit user confirmation, use the reviewed-apply guard, provide a concrete transactional writer, define failure/rollback behavior, validate target-device compatibility, establish provenance/recovery lineage where required, and complete representative acceptance before product restore can be claimed.
