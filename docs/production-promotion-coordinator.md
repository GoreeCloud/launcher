# Production Room promotion coordinator

GoreeCloud Launcher remains pre-cutover. Preferences DataStore is still Home's live Favorites/Dock authority, `MainActivity` does not instantiate `WorkspaceProductionPromotionCoordinator`, and Home does not route through `WorkspaceRoomPlacementRepository`.

This document defines the next Milestone 1 cutover-safety layer after the accepted promotion transaction rehearsal from PR #14.

## Reviewed coordinator

`WorkspaceProductionPromotionCoordinator` is production source code for the authority transaction itself, but it is deliberately unwired.

The coordinator:

1. Runs `WorkspacePromotionRehearsalCoordinator` and requires a valid current candidate.
2. Acquires Room again after candidate creation.
3. Performs another canonical Room read and requires exact ordered Favorite/Dock equality with the candidate state.
4. Re-reads DataStore and requires the exact verified evidence to remain current.
5. Calls the existing guarded `WorkspaceRepository.promoteRoomAuthority` primitive only after those checks pass.
6. Re-reads persisted authority and requires terminal `ROOM`.
7. Immediately evaluates post-cutover Room health.

A healthy completed transaction returns `PromotedHealthy`. If terminal `ROOM` is recorded but the immediate health check reports an unhealthy condition, the coordinator returns `PromotedRecoveryRequired` and does not silently demote authority.

## Fail-closed activation boundary

`scripts/check_room_cutover.py` now distinguishes implementation from activation.

The guard permits exactly one production call to `promoteRoomAuthority` outside its repository definition, and that call must be inside `WorkspaceProductionPromotionCoordinator.kt`.

The same guard separately rejects any production instantiation of `WorkspaceProductionPromotionCoordinator`. Therefore adding the reviewed transaction implementation does not activate cutover.

The existing placement rule is unchanged: production still may not instantiate `WorkspaceRoomPlacementRepository` outside its own class declaration.

A later cutover pull request must explicitly change both activation and Home placement routing boundaries. That change must be visible and reviewable rather than occurring as an incidental dependency injection or activity edit.

## Runtime acceptance

`WorkspaceProductionPromotionCoordinatorRuntimeTest` uses file-backed Preferences DataStore and the production Room database on API 36.

The runtime scenarios require:

- ordinary DATASTORE state to return `NeedsVerification` rather than promote;
- verified compatible state to promote through the reviewed coordinator and immediately report `PromotedHealthy`;
- exact Favorite/Dock content to survive the promotion marker transition;
- deliberate Room divergence to return `Mismatch` without changing ROOM_VERIFIED authority;
- unavailable Room to return `Unavailable` without changing authority.

These tests validate the production coordinator implementation. They do not activate it in the launcher runtime.

## Remaining gates

Before Home can use Room as its live source of truth, GoreeCloud Launcher still requires an explicit activation/routing pull request, Android OS process-death and cold-start acceptance around the activated path, Room-open and schema-migration recovery behavior, representative physical-device/default-HOME acceptance, future schema-version migration acceptance, signing, and release acceptance.
