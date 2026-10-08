# Post-cutover Room startup recovery

GoreeCloud Launcher remains pre-activation. Preferences DataStore is still the live Home Favorites/Dock source, `WorkspaceProductionPromotionCoordinator` is not instantiated by production runtime, and Home is not routed through `WorkspaceRoomPlacementRepository`.

This slice defines the startup/recovery contract required after a future accepted terminal `ROOM` transition without activating that transition yet.

## Startup coordinator

`WorkspacePostCutoverStartupCoordinator` is read-only. It first requires an initialized persisted `ROOM` authority marker, then delegates to `WorkspacePostCutoverHealthEvaluator`.

Results are intentionally small:

- `NotRoomAuthoritative` — terminal Room authority is not active.
- `Ready` — terminal Room is active and canonical Room placement is readable and healthy.
- `RecoveryRequired` — terminal Room remains authoritative but health is unavailable, mismatched, failed, or otherwise no longer healthy.

The coordinator never demotes authority to DataStore. After a future accepted cutover, later Room writes may make the legacy DataStore placement stale, so recovery must remain explicit rather than creating ambiguous dual authority.

## Persistence-client reopen evidence

`WorkspacePostCutoverStartupRuntimeTest` uses the real file-backed Preferences DataStore and production Room database on Android 16 / API 36.

The runtime cases prove that:

1. A verified workspace can be promoted through the already-reviewed production promotion coordinator in instrumentation.
2. The terminal `ROOM` marker and exact workspace lists survive DataStore client close/reopen.
3. The canonical Room placement survives database close/reopen.
4. A reopened terminal Room workspace returns startup `Ready`.
5. An unavailable Room database after terminal promotion returns `RecoveryRequired(Unavailable)` while authority remains `ROOM` and the persisted workspace is not rewritten.
6. Reopening the same database restores `Ready` without changing authority.

This is stronger restart/recovery evidence, but it is still not an Android OS process-death claim. Process death, representative physical-device behavior, and actual production Home activation remain separate acceptance gates.

## Activation guard

`scripts/check_room_cutover.py` now recognizes the post-cutover startup coordinator as reviewed infrastructure but rejects any production instantiation outside its own class declaration. The existing restrictions also remain:

- the only production `promoteRoomAuthority()` call outside `WorkspaceRepository` must remain inside `WorkspaceProductionPromotionCoordinator`;
- production must not instantiate `WorkspaceProductionPromotionCoordinator` yet;
- production must not instantiate `WorkspaceRoomPlacementRepository` yet.

A future cutover PR must deliberately change activation and Home routing together rather than acquiring authority through an unrelated source change.

## Privacy and security

No Android permission, `INTERNET` permission, cloud dependency, analytics, advertising, sponsorship, attribution, tracking SDK, or remote workspace service is introduced. Runtime tests use synthetic application keys. Recovery outcomes remain categorical and contain no installed-application inventory.

## Remaining gates

Before Room can become Home's live source of truth, GoreeCloud Launcher still needs an explicit activation/routing change, Android lifecycle and OS process-death acceptance around the activated path, schema-version upgrade recovery, representative physical-device/default-HOME acceptance, and release/signing acceptance.
