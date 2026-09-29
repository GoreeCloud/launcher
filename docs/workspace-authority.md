# Workspace authority transition

GoreeCloud Launcher uses an explicit persisted authority state while it moves from the accepted Preferences DataStore Favorites/Dock representation toward the Room relational workspace.

This document describes the Milestone 1 authority-state, dual-read, restart-recovery, reserved Room-placement I/O, and observational cutover-readiness foundation. It does **not** declare the Room cutover complete.

## Persisted phases

`WorkspaceAuthority` has three durable phases:

- `DATASTORE` — Preferences DataStore is the live workspace authority. This is the current production source consumed by Home.
- `ROOM_VERIFIED` — the exact current DataStore-shaped Favorites/Dock snapshot has been mirrored to Room, verified by DAO readback, and is eligible for independent pre-cutover checks.
- `ROOM` — a guarded terminal authority marker reserved for the future cutover where Home will consume Room placement as the source of truth.

Unknown or malformed persisted authority values fail closed to `DATASTORE`.

## Snapshot binding

A `ROOM_VERIFIED` marker is bound to the exact initialized Favorites/Dock content with a local SHA-256 fingerprint. The fingerprint is deterministic and order-sensitive, includes the initialization state, separates Favorites from Dock, and is stored only as local migration metadata.

The fingerprint is not telemetry, is not exported, and does not contain a reversible copy of the installed-application inventory.

Every legacy DataStore workspace mutation invalidates `ROOM_VERIFIED` back to `DATASTORE` before the changed workspace can be treated as verified. This prevents a stale verification marker from surviving a Favorite/Dock change.

## Mirror reconciliation

Initialized DataStore state is mirrored through `WorkspaceRelationalMirror` only while `DATASTORE` is authoritative.

- `Verified` may advance the compatibility state to `ROOM_VERIFIED` only if the exact DataStore snapshot is still current.
- `Mismatch` or sanitized `Failed` keeps or returns the launcher to `DATASTORE` authority.
- `Skipped` leaves the current not-yet-initialized state alone.
- `ROOM_VERIFIED` does not cause another mirror write. It enters the independent dual-read phase instead.
- A future `ROOM` authority state stops the compatibility path entirely so DataStore cannot overwrite relational authority after cutover.

`markRoomVerified` rechecks the exact current DataStore snapshot inside the same DataStore edit that writes the authority metadata. If the workspace changed while Room verification was running, the stale result is rejected and the repository stays on DataStore authority.

## Independent dual-read reconciliation

`WorkspaceRelationalReader` reconstructs the current Favorites and Dock ordering from the two Room compatibility pages instead of trusting the mirror result alone. `WorkspaceRelationalReadMapper` requires the persisted rows to remain canonical: both compatibility pages must exist, ranks and item identities must match the deterministic legacy mapping, app records must remain structurally valid, and query-return ordering itself is not treated as semantic ordering.

The dual-read phase runs only for `ROOM_VERIFIED` state and produces categorical results:

- `Match` — reconstructed Room Favorites/Dock exactly equal the still-authoritative DataStore Favorites/Dock.
- `Mismatch` — Room is readable but its canonical reconstructed state does not match DataStore.
- `Failed` — Room could not be read. Only the exception type is retained; application keys are not placed into the result.
- `Skipped` — the workspace is not initialized or is not in `ROOM_VERIFIED`.

A mismatch, read failure, or inability to obtain a Room DAO during startup reconciliation causes a bounded fallback to `DATASTORE`. That fallback preserves the user's existing DataStore Favorites and Dock instead of clearing or replacing them. Returning to `DATASTORE` allows the mirror path to attempt a fresh verified compatibility snapshot later.

`WorkspaceCanonicalRoomPlacementReader` now owns the shared canonical `home:0` / `dock:0` reconstruction primitive. Both the independent dual-read path and the reserved post-cutover placement repository use the same strict canonical parser while retaining separate authority gates.

## Deterministic startup and restart reconciliation

`WorkspaceStartupReconciler` owns the pre-cutover startup decision path that previously lived in separate `MainActivity` effects. It reads the current durable authority state, obtains Room through a retryable DAO provider, and returns a categorical `WorkspaceStartupResult` without exposing application keys.

The coordinator behavior is deliberately conservative:

- Uninitialized workspace -> `WaitingForInitialization`.
- `DATASTORE` + unavailable Room -> `DataStoreOnly`; Home remains fully usable from DataStore.
- `DATASTORE` + successful mirror/readback -> records `ROOM_VERIFIED` and immediately performs the independent Room read.
- `ROOM_VERIFIED` + canonical equivalent Room state -> `RoomVerifiedMatch`.
- `ROOM_VERIFIED` + unavailable Room, mismatch, skipped verification, or ordinary Room failure -> clears pre-cutover verification and returns `FellBackToDataStore` without changing Favorite/Dock contents.
- Reserved terminal `ROOM` -> `RoomAuthorityReserved`; the startup reconciler does not silently demote or rewrite it.

After a dual-read `Match`, the coordinator rechecks the current DataStore authority, Favorites, Dock, and verification fingerprint. If local workspace state changed while the database check was running, stale success is not treated as current readiness evidence.

`MainActivity` invokes this single reconciler when relevant workspace state changes. The DAO provider is evaluated on each reconciliation attempt rather than being permanently captured during Activity creation, so a pre-cutover transient Room-open failure can remain DataStore-only and a later reconciliation can retry Room.

The coordinator never calls `promoteRoomAuthority()`. Its purpose is to make startup and recovery deterministic before any production cutover is allowed.

## Reserved Room-backed placement I/O

`WorkspaceRoomPlacementRepository` is the first Room-backed live placement repository primitive. It is deliberately present **without** production wiring.

The repository checks the persisted `WorkspaceRepository` authority before every read or write. `DATASTORE`, `ROOM_VERIFIED`, and uninitialized states return `Reserved`; no Room placement operation is accepted before the durable phase is already `ROOM`. An unavailable DAO returns a categorical `Unavailable`, ordinary failures retain only the exception type, and coroutine cancellation is rethrown.

For the current compatibility model, `read()` reconstructs the canonical `home:0` and `dock:0` snapshot from Room. `replace()` normalizes ordered Favorites and Dock state, removes duplicate keys, retains the five-item Dock limit, transactionally replaces the compatibility pages/items, reads the result back, and returns `Written` only when the canonical readback exactly equals the normalized request.

This repository is intentionally narrower than the final workspace model. It handles only the current app-based Home/Dock compatibility containers and does not claim multi-page placement, folders, shortcuts, widgets, or live cell/span editing.

The production launcher does not instantiate this repository for Home and does not call it from `MainActivity`. It exists so the post-cutover Room read/write contract can be runtime-tested before the production cutover coordinator is allowed to promote or route Home through Room.

## Observational cutover readiness

`WorkspaceCutoverReadinessCoordinator` adds a separate pre-cutover evidence gate. It does not modify either store and cannot promote authority.

Readiness is evaluated only when the persisted workspace is already initialized and still in `ROOM_VERIFIED`. A `DATASTORE` workspace returns `NeedsVerification`; an already-terminal `ROOM` workspace returns `AlreadyRoomAuthoritative`; and an unavailable DAO returns `Unavailable` without changing authority.

A `Ready` result requires all of the following in one evaluation:

1. The starting DataStore state is `ROOM_VERIFIED` with a non-null verified fingerprint.
2. `WorkspaceRelationalReader` performs an independent Room dual-read and returns `Match` against that exact DataStore state.
3. A second fresh canonical Room placement read through `WorkspaceCanonicalRoomPlacementReader` returns the same ordered Favorites and Dock.
4. The coordinator re-reads DataStore after the Room checks and verifies that initialization, `ROOM_VERIFIED` authority, Favorites, Dock, and verified fingerprint are still identical to the evidence that started the evaluation.

Readable but divergent relational state returns `Mismatch`. A changed authority, workspace list, or verification fingerprint returns `StaleEvidence`. Ordinary Room failures retain only the exception type in `Failed`; coroutine cancellation is rethrown.

The coordinator deliberately does **not** demote on a failed readiness probe. Startup reconciliation remains responsible for pre-cutover fallback and re-verification. This keeps the readiness check observational and prevents a read-only promotion decision helper from becoming a second authority-state writer.

Most importantly, `Ready` does not call `promoteRoomAuthority()`. It is evidence that a later, separately reviewed production cutover coordinator may consider; it is not permission for Home to switch stores automatically.

## Guarded one-way promotion

`WorkspaceRepository.promoteRoomAuthority(expectedState)` exists as a guarded migration primitive but is not called by the current launcher runtime.

Promotion succeeds only when:

1. The workspace is initialized.
2. The persisted phase is `ROOM_VERIFIED`.
3. The locally stored verified fingerprint matches the expected workspace.
4. The workspace read inside the promotion transaction still has the same fingerprint.

Once `ROOM` is recorded, compatibility fallback helpers do not silently demote it. That one-way rule is deliberate: after a real cutover, rollback must be an explicit migration/recovery operation rather than an accidental background state change.

## Fail-closed production cutover guard

`scripts/check_room_cutover.py` is a source-level CI guard for the current pre-cutover stage.

It fails if production Kotlin code calls `promoteRoomAuthority()` outside the repository method that defines the guarded primitive. It also fails if production code instantiates `WorkspaceRoomPlacementRepository` outside its own class declaration. Android/JVM test code remains free to exercise both contracts for acceptance purposes.

This guard is intentionally temporary governance for the pre-cutover stage. When a future production cutover PR is explicitly ready to wire promotion or Room placement routing, that PR must change the guard and the production source together so the authority change is visible and reviewable rather than occurring silently.

## Runtime validation and CI coverage

`WorkspaceRoomRuntimeTest` is the established API 36 file-backed Room runtime test. It verifies database creation, mirror/readback, close/reopen durability, replacement semantics, stale-row removal, empty-container recovery, authority metadata behavior, guarded test-only promotion, dual-read Match/Mismatch, and sanitized closed-database failure.

`WorkspaceStartupReconcilerRuntimeTest` repeatedly closes and recreates both persistence clients, requires `ROOM_VERIFIED` and exact Favorite/Dock state to survive each reopen, deliberately makes Room unreadable, verifies DataStore fallback preserves the workspace, reopens Room, and requires verified compatibility to be rebuilt.

`WorkspaceRoomPlacementRepositoryRuntimeTest` verifies that Room placement I/O is reserved before cutover, becomes available only after guarded test-only promotion, normalizes writes, preserves the legacy DataStore snapshot after Room writes, and reports unavailable DAO state categorically.

PR #12 corrected the emulator workflow from a single-class filter to the complete `connectedDebugAndroidTest` suite. Exact PR #12 head `263b8d435b6e70818caab0350810f49b1c88c450` passed Android CI run `32586296916`; the emulator log reported six tests started and six tests finished successfully on Android 16 / API 36 before squash merge `ba9273e849f571eadcef2b698d2d7f0268778168`. That run is the first accepted complete-suite evidence for the restart-recovery and Room-placement test classes. It does not retroactively change the narrower historical PR #11 evidence.

`WorkspaceCutoverReadinessRuntimeTest` extends the suite with an observational readiness scenario. It must prove that `DATASTORE` cannot be Ready, a current `ROOM_VERIFIED` canonical snapshot can be Ready without promotion, deliberate Room divergence blocks readiness without changing the DataStore workspace, unavailable Room blocks readiness, and the already-terminal `ROOM` state is reported without mutation. This new test is not accepted runtime evidence until its exact pull-request head passes the complete API 36 suite.

Even after the complete suite passes, persistence-client reopen is not an Android OS process-death claim. The test process is not killed and recreated. Production still does not promote itself to `ROOM`, and Home still reads Favorites/Dock from DataStore.

## Remaining cutover gates

Before Home can actually consume Room as authoritative workspace state, the project still needs:

- A separately reviewed production cutover coordinator that consumes accepted readiness evidence and invokes guarded promotion only after all required checks pass.
- Production wiring that routes Home placement reads/writes through `WorkspaceRoomPlacementRepository` or its evolved multi-page successor only after `ROOM` authority.
- Explicit post-cutover recovery behavior when Room cannot open or a future schema migration fails.
- Android process-death and cold-start acceptance beyond persistence-client reopen testing.
- Representative physical-device storage validation.
- Multi-page and cell/span placement behavior.
- Schema-version migration acceptance for future Room versions.
- Physical-device default-HOME acceptance and release acceptance.

Until those gates are completed, the `ROOM` enum, guarded promotion API, dual-read reader, startup reconciler, readiness coordinator, and reserved Room placement repository are migration infrastructure only, not a claim that Room is the current launcher source of truth.
