# Architecture

GoreeCloud Launcher is Android-native because its role depends on HOME-role integration, `LauncherApps`, shortcuts, widgets, wallpaper/system behavior, profiles, and launcher lifecycle callbacks.

The repository remains a single Android application module during the early milestones so launcher behavior stays easy to inspect while product boundaries stabilize. Planned long-term module boundaries are `core:launcher`, `core:data`, `core:widgets`, `core:theme`, `core:designsystem`, `feature:home`, `feature:drawer`, `feature:search`, `feature:settings`, `feature:onboarding`, and `feature:backup`.

The first-party Glaze Theme Engine begins with local semantic colors and persistent System/Light/Dark appearance. Later milestones add wallpaper palettes, user accents, icon masks, themed icons, dock/folder/drawer surfaces, motion controls, and exportable presets.

## Current source state

Milestone 0 establishes the Android HOME contract, app/profile discovery, Glaze UI shell, local appearance persistence, and privacy/manifest guards.

Milestone 1 currently includes these daily-launcher and persistence foundations:

1. Local application search by installed-app label or package name.
2. Durable Preferences DataStore state for ordered Favorites and Dock membership.
3. Explicit accessible Favorite/Dock movement plus direct Reorder-mode drag/drop using one ordering source of truth.
4. A staged Room 3 relational workspace schema with deterministic DataStore import mapping.
5. Transactional Room mirror write/readback verification and strict canonical comparison.
6. Durable `DATASTORE`, `ROOM_VERIFIED`, and guarded terminal `ROOM` authority phases bound to an order-sensitive local fingerprint.
7. Independent Room dual-read reconstruction that fails verified compatibility back to DataStore on mismatch or read failure.
8. `WorkspaceStartupReconciler`, which centralizes pre-cutover startup/recovery behavior and retries Room acquisition without making Room mandatory for current Home use.
9. `WorkspaceRoomPlacementRepository`, a reserved Room-backed placement read/write primitive that refuses access until persisted authority is already `ROOM`.
10. Android API 36 instrumentation plus CI schema/history gates for the relational transition.

Preferences DataStore remains the live workspace authority. Production does not invoke guarded `ROOM` promotion and Home does not consume `WorkspaceRoomPlacementRepository` yet.

## Current DataStore workspace authority

Android Preferences DataStore remains authoritative for live Home Favorites and Dock during the relational-transition stage. Each stored application key combines a public `UserHandle` discriminator with the flattened component name so identical package names across supported profiles do not collapse into one item without depending on hidden Android APIs.

The first successful application inventory initializes up to twelve Favorites and four Dock items, excluding the launcher itself. After initialization, user choices are authoritative; empty Favorites or Dock containers are not silently reseeded.

Favorites and Dock items resolve against live `LauncherApps` inventory at render time. Missing or uninstalled components therefore disappear safely from rendered Home without requiring a launch attempt. The relational cleanup/cutover layer can later prune stale placement records after runtime migration acceptance exists.

The Dock is capped at five items. `WorkspaceCodec.moved` implements deterministic one-step accessible ordering. `WorkspaceCodec.movedToTarget` implements direct target-based Reorder-mode movement. Both paths persist through the same `WorkspaceRepository` contract.

## Room 3 relational workspace transition

The relational foundation uses AndroidX Room 3.0.1 with Kotlin Symbol Processing and AndroidX SQLite 2.7.0 through `AndroidSQLiteDriver`.

`LauncherDatabase` version 1 contains:

- `workspace_pages` — stable page identity, container type, and rank. The compatibility model currently uses `home:0` and `dock:0`.
- `workspace_items` — stable item identity, parent page, item type, application key when applicable, deterministic rank, optional cell coordinates, and spans. Item types reserve APP, SHORTCUT, FOLDER, and WIDGET without claiming the later behaviors are implemented.

`WorkspaceLegacyImportMapper` converts ordered DataStore Favorites/Dock into deterministic relational rows. Duplicate keys inside one legacy container are collapsed defensively while order is retained; the same application may legitimately appear independently on Home and Dock.

`WorkspaceDao.replaceLegacySnapshot` transactionally replaces the current compatibility pages and cascading item rows. `WorkspaceRelationalMirror` then reads the written rows back. `WorkspaceRelationalVerifier` compares full canonical content while ignoring SQL return-list order itself; persisted `rank` remains semantic order, so a changed rank is a mismatch.

Ordinary database failures return typed failures instead of replacing the accepted DataStore launcher path, and coroutine cancellation is rethrown. Diagnostic result types do not include installed-application keys.

## Durable authority and dual-read state

`WorkspaceAuthority` has three persisted phases:

- `DATASTORE` — current live authority.
- `ROOM_VERIFIED` — exact current DataStore-shaped snapshot was mirrored, read back, fingerprint-bound, and is eligible for independent dual-read reconciliation.
- `ROOM` — terminal marker reserved for an accepted future cutover.

`WorkspaceRepository.markRoomVerified` writes verification only when the DataStore snapshot still matches the expected fingerprint in the same edit. Any pre-cutover Favorite/Dock mutation invalidates stale verification back to `DATASTORE`.

`WorkspaceRelationalReader` independently reconstructs Favorites/Dock from canonical Room rows while `ROOM_VERIFIED`. Equivalent state returns `Match`; canonical divergence returns `Mismatch`; ordinary read problems return a sanitized `Failed`. Mismatch or failure returns pre-cutover authority to DataStore without clearing user choices.

`WorkspaceRepository.promoteRoomAuthority` is a guarded one-way primitive. It requires initialized `ROOM_VERIFIED` state plus matching current and verified fingerprints. Production runtime does not call it. Once `ROOM` exists, ordinary compatibility fallback helpers do not silently demote it; post-cutover rollback must be an explicit recovery migration.

## Deterministic startup/recovery coordinator

`WorkspaceStartupReconciler` is the single pre-cutover reconciliation coordinator used by `MainActivity`.

Its behavior is deliberately bounded:

- Uninitialized -> wait.
- `DATASTORE` with unavailable Room -> keep DataStore-only operation.
- `DATASTORE` with available Room -> mirror, verify, and independently dual-read.
- `ROOM_VERIFIED` with matching canonical Room -> retain verified compatibility.
- `ROOM_VERIFIED` with unavailable/mismatched/failing Room -> return to DataStore without workspace loss.
- `ROOM` -> reserve the terminal state and do not run legacy mirroring.

After a dual-read Match, the coordinator re-reads current DataStore state so stale verification success is not treated as current readiness evidence after a concurrent local mutation. The Room DAO provider is evaluated on each attempt, allowing a later retry after transient pre-cutover open failure.

## Reserved Room-backed placement repository

`WorkspaceRoomPlacementRepository` establishes the first post-cutover-style read/write contract while remaining disconnected from production Home.

Before every operation it reads persisted authority from `WorkspaceRepository`. Uninitialized, `DATASTORE`, and `ROOM_VERIFIED` states return `Reserved`, so merely constructing the repository cannot bypass the migration state machine. A missing DAO returns `Unavailable`; ordinary exceptions return only their type; cancellation is rethrown.

When authority is already `ROOM`:

- `read()` reconstructs the canonical current `home:0` / `dock:0` snapshot from Room.
- `replace()` normalizes ordered Favorite/Dock input, deduplicates each container, preserves the five-item Dock limit, transactionally replaces Room rows, reads them back, and returns `Written` only if canonical readback equals the normalized request.

This is not the final workspace repository. It currently covers only application placements in the compatibility containers. Multi-page placement, folder membership, shortcuts, widgets, cell/span editing, and richer transactional operations still require later schema/domain work.

The key architecture boundary is now explicit: DataStore APIs own legacy pre-cutover placement; Room placement APIs refuse access until the durable authority marker is already `ROOM`. Production routing between those stores remains a future cutover-coordinator responsibility.

## Android emulator runtime suite

Runtime instrumentation uses production Room classes, `AndroidSQLiteDriver`, and real file-backed databases on Android 16 / API 36.

`WorkspaceRoomRuntimeTest` covers database creation, mirror/readback verification, Room close/reopen durability, replacement and stale-row removal, empty-container recovery, durable authority metadata, guarded test-only promotion, independent dual-read Match/Mismatch, and sanitized read failure.

`WorkspaceStartupReconcilerRuntimeTest` covers repeated DataStore/Room client close/reopen, verified-state persistence, pre-cutover Room failure fallback, workspace preservation, and later Room recovery.

`WorkspaceRoomPlacementRepositoryRuntimeTest` covers pre-cutover access denial, guarded test-only promotion, canonical Room reads after `ROOM`, normalized Room writes, five-item Dock enforcement, preservation of legacy DataStore values after a Room write, and unavailable-DAO results.

### CI coverage correction

A validation gap was discovered while adding the Room placement repository. The previous emulator command passed a class filter for `WorkspaceRoomRuntimeTest`, so the separate `WorkspaceStartupReconcilerRuntimeTest` added in the preceding source slice was present but was not executed by that exact CI command.

The workflow is corrected here to run `gradle --no-daemon connectedDebugAndroidTest` without a single-class filter. That causes the complete Android instrumentation suite to run and prevents future runtime test classes from being silently excluded. The immutable Android Emulator Runner pin remains `a421e43855164a8197daf9d8d40fe71c6996bb0d`.

The correction is fail-closed governance: historical evidence is not retroactively upgraded. The repeated-reopen test becomes CI-accepted only when an exact head using the corrected complete-suite command passes.

## Schema and migration governance

The Room Gradle plugin exports schema history under `app/schemas`. Version-1 schema history is source-controlled at `app/schemas/com.goreecloud.launcher.core.workspace.db.LauncherDatabase/1.json` with identity hash `2fa5d8fba0010dd896c671aadaa5dafb`.

CI parses the schema, requires both workspace tables, and checks `git status --porcelain -- app/schemas` after Room/KSP generation. Any changed or newly generated schema file fails until exact generated history is reviewed and committed.

Future schema-version changes require explicit migrations or accepted auto-migrations with tests. Destructive fallback is not the normal upgrade strategy.

## Remaining cutover architecture

Before production Home may use Room as authoritative placement storage, a later slice must:

- Define a production cutover coordinator and exact promotion transaction.
- Route Home reads/writes to the Room placement repository only after persisted `ROOM` authority.
- Define explicit post-cutover recovery if Room cannot open or a schema migration fails.
- Add Android process-death/cold-start acceptance beyond persistence-client reopen tests.
- Add multi-page and cell/span placement and evolve the repository beyond the two compatibility containers.
- Add folders, shortcuts, widgets, and their relational transactions.
- Validate schema-version upgrades and representative physical-device storage behavior.

Until those gates pass, Preferences DataStore remains the live Favorites/Dock source consumed by Home.

## Glaze UI native mapping

The launcher targets **Glaze UI 1.4 Stable** as the current canonical design-system baseline. `GlazeMetrics.kt` maps the subset currently consumed by Launcher: canonical spacing steps, semantic radii, 44 dp minimum targets, and 48 dp comfortable targets. Current HOME, Dock, drawer, placement-management, and Reorder controls consume these metrics selectively.

Ordinary content remains Solid/Raised in accordance with the Glaze material hierarchy. Functional Glass is not added merely for decoration. Full phone/tablet rendered/native acceptance remains application-specific work.

## Privacy boundary

Search, Favorites, Dock state, ordering state, drag state, Room workspace data, authority metadata, verification fingerprints, and appearance settings remain local. The relational database, startup reconciler, reserved Room placement repository, and emulator tests add no Android permission, network permission, remote recommendation service, analytics SDK, advertising SDK, sponsorship system, or cloud-account dependency.
