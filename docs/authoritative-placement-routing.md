# Authority-aware Home placement routing

GoreeCloud Launcher remains pre-activation. Preferences DataStore is still Home's live Favorites/Dock source even though the guarded production promotion, terminal-ROOM startup recovery, Room placement I/O, and authority-aware placement router now exist as separately reviewed infrastructure.

## Legacy write freeze

`WorkspaceRepository` is the legacy DataStore placement repository. Once durable authority is terminal `ROOM`, its first-run defaults and all Favorite/Dock membership and ordering mutation methods now return from the DataStore edit without changing legacy placement.

This freeze prevents a future post-cutover launcher path, race, or stale callback from accumulating misleading DataStore placement after Room has become authoritative. Terminal ROOM remains one-way.

## Authority-aware placement repository

`WorkspaceAuthoritativePlacementRepository` provides one compatibility routing contract for the current Home Favorites/Dock model.

For `DATASTORE` and `ROOM_VERIFIED` authority:

- reads return the current DataStore placement with source `DATASTORE`;
- membership/order mutations use the existing `WorkspaceRepository` operations;
- a mutation from `ROOM_VERIFIED` naturally invalidates stale verification back to `DATASTORE` through the existing repository contract.

For terminal `ROOM` authority:

- reads use `WorkspaceRoomPlacementRepository` and return source `ROOM`;
- Favorite/Dock membership and ordering mutations read canonical Room placement, apply the same `WorkspaceCodec` ordering semantics, then replace and verify Room placement through the guarded Room repository;
- the five-item Dock limit remains enforced;
- unavailable or malformed Room fails closed rather than falling back to legacy DataStore placement.

If authority changes across a routing boundary, the router returns `AuthorityChanged` rather than pretending a mutation succeeded against the wrong store.

## Activation guard

This router is not wired into `MainActivity` or `LauncherRoot`. `scripts/check_room_cutover.py` permits exactly one `WorkspaceRoomPlacementRepository` instantiation inside the reviewed authority-aware router while rejecting production instantiation of the router itself anywhere else.

The existing activation restrictions remain:

- `WorkspaceProductionPromotionCoordinator` remains unwired;
- `WorkspacePostCutoverStartupCoordinator` remains unwired;
- direct production Room placement access outside `WorkspaceAuthoritativePlacementRepository` remains prohibited.

A later explicit cutover PR must change Home activation and routing together.

## API 36 runtime evidence

`WorkspaceAuthoritativePlacementRuntimeTest` exercises both routing sides with file-backed DataStore and the production Room database.

The intended cases are:

1. Pre-cutover mutations route to DataStore.
2. A mutation from `ROOM_VERIFIED` invalidates verification and returns to DataStore authority.
3. After test-only accepted production promotion, router mutations go to Room and router reads return Room placement.
4. Direct legacy DataStore membership mutations after terminal ROOM are frozen and do not change the legacy snapshot.
5. Unavailable Room after terminal authority returns an explicit unavailable write result and does not mutate legacy DataStore.

## Privacy and security

No Android permission, `INTERNET` permission, cloud dependency, analytics, advertising, sponsorship, attribution, tracking SDK, or remote workspace service is introduced. Test application keys are synthetic. Placement failures are categorical and ordinary database errors retain only exception type.

## Remaining gates

This routing layer is a cutover prerequisite, not production activation. GoreeCloud Launcher still needs an explicit Home activation/routing PR, activated-path lifecycle and Android OS process-death acceptance, schema-version upgrade recovery, representative physical-device/default-HOME validation, signing, and release acceptance.
