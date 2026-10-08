# Observable authority-aware Home placement state

GoreeCloud Launcher remains pre-activation. Preferences DataStore is still the workspace state collected by `MainActivity`, and the production promotion coordinator, post-cutover startup coordinator, authority-aware placement router, and authority-aware placement observer remain deliberately unwired from Home.

## Observable placement contract

`WorkspaceAuthoritativePlacementObserver` exposes the placement state a future accepted Home runtime can collect.

The states are:

- `WaitingForInitialization` — no initialized workspace exists yet.
- `Ready(snapshot)` — placement is available from the currently authoritative source.
- `RecoveryRequired(reason)` — terminal Room authority exists but authoritative placement cannot be safely observed.

Recovery reasons are categorical:

- `Unavailable` — no Room DAO can be acquired after terminal authority.
- `Mismatch` — observed Room rows are not canonical for the current Home/Dock compatibility model.
- `Failed` — an ordinary database/observation failure occurred; only the exception type is retained.

## Authority switching

For `DATASTORE` and `ROOM_VERIFIED`, the observer emits the ordered Favorite/Dock state directly from `WorkspaceRepository` with source `DATASTORE`.

When durable authority changes to terminal `ROOM`, the flow switches to Room observation through `WorkspaceDao.observePages()` and `WorkspaceDao.observeItems()`. The observed canonical compatibility pages/items are reconstructed with the same strict `WorkspaceRelationalReadMapper` used by existing migration/readback gates.

Room mutations therefore update the observable state reactively without polling. A terminal Room failure never falls back to legacy DataStore placement.

## Canonical scope

The observer filters the DAO's broader page/item streams to the current compatibility page IDs before canonical reconstruction. This keeps the current Favorites/Dock observer isolated from unrelated future relational pages while still enforcing exact Home/Dock row identity, rank, coordinates, spans, and application records.

## Activation guard

`scripts/check_room_cutover.py` recognizes the observer as reviewed infrastructure but rejects production instantiation of `WorkspaceAuthoritativePlacementObserver` outside its class declaration. Existing activation restrictions remain in place for:

- `WorkspaceProductionPromotionCoordinator`;
- `WorkspacePostCutoverStartupCoordinator`;
- `WorkspaceAuthoritativePlacementRepository`.

Direct production Room placement access outside the reviewed router also remains prohibited. `MainActivity` and `LauncherRoot` are unchanged in this slice.

## API 36 runtime evidence

`WorkspaceAuthoritativePlacementObserverRuntimeTest` uses file-backed Preferences DataStore and the production Room database.

The intended cases prove that:

1. An uninitialized workspace emits `WaitingForInitialization`.
2. Initialized pre-cutover placement emits a `DATASTORE` snapshot.
3. DataStore mutations are reflected in the observer before cutover.
4. After test-only accepted promotion, the observer switches to a `ROOM` snapshot without changing the user's ordered placement.
5. Room mutations through the reviewed authoritative router are reflected reactively in the Room-backed observer.
6. Terminal Room with an unavailable DAO emits explicit `RecoveryRequired(Unavailable)`.
7. Malformed/noncanonical Room compatibility rows emit `RecoveryRequired(Mismatch)` rather than stale legacy placement.

## Privacy and security

No Android permission, `INTERNET` permission, cloud dependency, analytics, advertising, sponsorship, attribution, tracking SDK, or remote workspace service is introduced. Runtime application keys are synthetic. Ordinary failures remain categorical/sanitized and coroutine cancellation is rethrown.

## Remaining gates

This observer is the final state-observation prerequisite before a separately reviewed activation slice can replace `MainActivity`'s direct DataStore collection and mutation callbacks. Android lifecycle/process-recreation and OS process-death acceptance around the activated path, schema-version upgrade recovery, representative physical-device/default-HOME acceptance, signing, and release acceptance remain separate gates.
