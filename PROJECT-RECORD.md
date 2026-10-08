# GoreeCloud Launcher — Project Record

## 2026-10-08 — Return to independent repository

Owner-directed source ownership returns to `GoreeCloud/launcher`. The standalone repository retains its initial commit and Launcher-specific history extracted from the Android monorepo. Application source, tests, assets, and documentation are preserved without functional rewrites. The monorepo retirement requires separate validation. This supersedes the earlier instruction to delete the standalone repository. Lifecycle remains Development.


## 2026-09-29 — Mandatory repository consolidation

Active source development moved from `GoreeCloud/launcher` to `GoreeCloud/android-app-defaults` at `apps/launcher/`. The imported Development revision was `1eb8dd6d8178f6d730b100559e8f8d149501d18c`; the consolidation merged through target PR #62 as `20ca33c7c0c565f361deda206c9c950154dec327`. Legacy documentation migration PR #249 is preserved as source provenance for this repository-local record. No lifecycle promotion is implied.


> **Current repository authority — October 8, 2026:** GoreeCloud Launcher is maintained in its independent repository `GoreeCloud/launcher` at the repository root. The earlier September 29 monorepo consolidation is historical provenance, not current source ownership. Launcher-specific source history was extracted from `GoreeCloud/android-app-defaults/apps/launcher/`. The monorepo removal is separately gated on transfer and CI acceptance. Development status is unchanged.


**Repository:** `GoreeCloud/launcher` (repository root)

**Lifecycle:** Development  
**Record purpose:** Significant project history, architecture/governance transitions, migration evidence, acceptance evidence, and superseded directions  
**Migration baseline:** `c853fa115a964c3cff531af555039f2e3cdd944e`  
**Canonical authority:** This file is the repository-local project record once accepted on the default branch.

## 2026-08-21 — Project specification established

The former Google Drive **Project Specification — Launcher** records project establishment on August 21, 2026 as an original GoreeCloud-owned native Android HOME application. It defines the privacy-first, local-first, no-advertising product direction, Android launcher architecture, accessibility and performance expectations, and the initial product scope.

## 2026-08-22 onward — Development and Room authority milestones

The Drive record accumulated detailed implementation and acceptance checkpoints covering the native launcher foundation, favorites and Dock work, Room workspace authority, restart/recovery behavior, HOME-role recreation, Glaze adoption, portability candidates, layout-lock behavior, and Universal Search evolution. Those historical records are preserved below as dated/source-era evidence rather than rewritten as present-tense implementation claims.

## 2026-09-22 — Repository-native feature and changelog authority

Launcher migrated feature-state and changelog authority into repository-local `IMPLEMENTED-FEATURES.md`, `PLANNED-FEATURES.md`, and `CHANGELOGS.md`. The changelog records that legacy Drive roadmap/changelog sources were retired after accepted default-branch readback.

## 2026-09-23 — Requirements-review baseline

The Drive project specification reached v0.44 and recorded a Development requirements-review baseline. Its status material referenced the then-current default branch and a synchronized unmerged candidate stack. Those exact-revision claims remain historical evidence; current GitHub state controls current implementation and lifecycle facts.

## 2026-09-24 — Project specification/project record migration candidate

Migration pull request: [PR #249](https://github.com/GoreeCloud/launcher/pull/249).

This migration:
- creates root `PROJECT-SPECIFICATIONS.md`;
- creates root `PROJECT-RECORD.md`;
- consolidates the former root `SPECIFICATIONS.md` into the mandatory canonical filename;
- preserves Drive normative/current/future material in the canonical specification;
- preserves historical milestone/candidate material below in this project record;
- updates README navigation and removes the stale statement that canonical project specifications may remain in Google Drive; and
- retires `SPECIFICATIONS.md` from the working tree after its content is incorporated, while Git history preserves the former file.

**Drive migration source:** Project Specification — Launcher  
**Drive file ID:** `1wtNYhNNN7zwHcT8d01m5lLXnIDYraBt7`  
**Source version:** v0.65  
**Source last updated:** September 27, 2026  
**Drive deletion status:** Frozen migration source only. Permanent removal remains blocked until this migration is accepted, read back from authoritative `main`, all validation/review gates are satisfied, and no unresolved discrepancy remains. The Drive source must not be edited or maintained as the project specification.

## 2026-09-27 — Migration source refreshed to verified DOCX v0.65

The active migration source was converted from the retired native Google Doc `19wyxWRf-b-pKMF2vb_ogbCadtWRt1h2B5wupIqLaN3I` to the verified portable DOCX `1wtNYhNNN7zwHcT8d01m5lLXnIDYraBt7`. The replacement is **Project Specification — Launcher.docx**, version v0.65, last updated September 27, 2026.

PR #249 was originally staged from the September 23 v0.44 snapshot. This refresh reconciles the repository-local migration against the current replacement source rather than treating the earlier snapshot as complete. The refreshed PROJECT-SPECIFICATIONS.md now carries the current Section 66 requirements-review mapping and the September 27 Section 67 secondary-Home spatial-placement / exact same-page drag requirement and evidence boundary.

The current Drive source identifies PR #248 as the single unmerged Development integration candidate at exact head `a080ee3f01b6af646460b8cdcf99f0c0af1d1d9f`, with source-bearing head `50f5beaa6ec4c6b3eae7fed8a5ab0c6a0031d77c`. It records Android CI #1150 for the source-bearing head and Android CI #1151 / run `36363613876` for the final documentation-reconciled candidate, including 61/61 Android runtime tests with zero failures. These are Development candidate facts only; PR #248 remains unmerged and does not establish Release Candidate, Production, or Stable acceptance.

The Drive DOCX is now a frozen migration input. Current project-specification maintenance belongs in the repository, and the Drive source must be permanently removed only after PR #249 is accepted, authoritative-main readback verifies the migration, and no unresolved discrepancy remains.

# Imported Historical Project Record

The following source-era sections are retained from the Drive specification because they primarily document significant implementation milestones, candidate evidence, superseded directions, or governance transitions. They do not override newer repository state.

## Historical Drive Section 27 — Current Status

The project is approved and published as a native Android GoreeCloud application. Milestone 0 and twelve Milestone 1 source slices are merged to the public GoreeCloud/android-app-defaults (`apps/launcher/`) main branch. PR #1 established HOME-role integration, LauncherApps discovery/callbacks, the initial Glaze home/drawer surfaces, local appearance persistence, local app-drawer search, and privacy/manifest guards. PR #2 added locally persisted ordered Favorites and Dock membership through Preferences DataStore, first-run seeding, a five-item Dock limit, app placement management, and JVM persistence tests. PR #3 added deterministic Move earlier/Move later operations, long-press management from All apps, Favorites, and Dock, accessible non-drag ordering controls, expanded ordering tests, and the initial native Glaze UI 1.4 metric mapping; exact PR head fdf35f982c3a3792b9e8395128329576e2fc73fa passed privacy, HOME-manifest, Android lint, JVM tests, and assembleDebug before squash merge e700d0170c79eaf7fa36a2f2c15bda6edb5d836e. PR #4 added the fail-closed scripts/check_glaze_ui.py contract, pinned canonical Glaze UI revision 883d40ff51d02885650024723c01d229de456285, and enforced that contract in launcher CI; exact head 30854ae7689dd0b7edbde8bc305ffd96e382f360 passed privacy, HOME-manifest, Glaze contract, lint, unit tests, and assembleDebug before squash merge 79023a0675b0c9f8c700884ecf2214cf5b007e09. PR #5 added explicit Home Reorder mode, direct Favorite and Dock drag/drop target ordering through the existing WorkspaceRepository ordering source of truth, drag lift and valid-target feedback, and JVM target-order regression tests; exact head dfff61d624f5674cda5525950574fb03e0fd969d passed Privacy Shield, HOME-manifest, Glaze UI mapped-subset contract, Android lint, JVM unit tests, and assembleDebug in Android CI run 32577224980 before squash merge 19a9a5b0345693d98023fe7ce37707d51c6cb17e. PR #6 added the staged AndroidX Room 3.0.1 relational workspace foundation with KSP, AndroidX SQLite 2.7.0, version-1 workspace_pages and workspace_items entities, transactional compatibility mirroring from the accepted DataStore state, deterministic legacy mapping tests, the source-controlled generated Room schema, and fail-closed schema-history drift enforcement; exact PR head b1843f43bb97834527163deffc1ba3977aff839f passed Privacy Shield, HOME-manifest, Glaze UI mapped-subset contract, Room/KSP compilation and schema generation, Android lint, JVM unit tests, assembleDebug, Room schema parsing, and committed-schema drift validation in Android CI run 32578518697 before squash merge 78a167518f0c378b9afecf48cf75ed7670c0fc43. PR #7 added write-then-readback verification for the Room compatibility mirror, DAO readback queries, strict deterministic snapshot comparison, typed Verified/Mismatch/Failed results without application-key diagnostics, and JVM regression tests for missing, extra, reordered, retagged, identity-changed, and cell/span-altered records; exact PR head aee6fe956cb2932bfa785ea60f2c463b296f80c6 passed the full Android CI stack in run 32579167978 before squash merge 042280f10dd74ee1a32cf42f3f0861a2df44d6aa. PR #8 added AndroidX Test instrumentation and a second CI job that runs WorkspaceRoomRuntimeTest on an Android 16 / API 36 x86_64 emulator using immutable-pinned Android Emulator Runner commit a421e43855164a8197daf9d8d40fe71c6996bb0d; exact PR head f25ed07d7f2e1f44bd6e120f984973fbda66e301 passed both the full source/build validation job and the runtime emulator job in Android CI run 32579948632 before squash merge 9c3ab9c2fb86c9e9eeb342d096a7e91955416c1a. The runtime test proved database creation, verified mirror write/readback, close/reopen persistence, changed-snapshot replacement, stale-row removal, and initialized empty-container recovery without making Room authoritative. PR #9 added an explicit durable workspace-authority state machine with DATASTORE, ROOM_VERIFIED, and guarded ROOM phases, an order-sensitive local SHA-256 fingerprint binding verified Room evidence to the exact Favorites/Dock snapshot, automatic invalidation when legacy workspace state changes, stale-verification rejection, and a guarded one-way Room-promotion primitive that production code does not invoke. Exact PR head 61c2bb3316e9721ee67970ed5b46f70b3915a078 passed the full source/build validation job and the expanded API 36 emulator runtime job in Android CI run 32582256642 after two fail-closed troubleshooting cycles: an initial JVM fingerprint test failure caused by incorrect hash-byte loop structure, and a subsequent instrumentation-runner initialization failure caused by a Boolean-inferred JUnit teardown. Both source issues were corrected without weakening CI. PR #9 was squash-merged with expected-head protection as 50dc52bdaec6ffaddaafef2fa8669dcc8ab3df92. PR #10 added WorkspaceRelationalReader and a canonical reconstruction mapper that independently reads the Room compatibility pages/items only during ROOM_VERIFIED, compares the reconstructed Favorites/Dock with still-authoritative DataStore state, and returns categorical Match, Mismatch, Skipped, or sanitized Failed results. MainActivity now mirrors only while DATASTORE is authoritative and performs the independent reader gate only while ROOM_VERIFIED; an unavailable DAO, mismatch, or read failure returns the pre-cutover authority to DataStore without clearing user workspace choices. JVM reconstruction tests and the API 36 file-backed runtime test cover canonical reconstruction, malformed-row rejection, successful Match, deliberate divergence/Mismatch, preserved DataStore fallback, and sanitized closed-database failure. Exact PR head c6a6a54948f6f14f648474d141e68e6f5cc9c80e passed both Android CI jobs in run 32584468440 before squash merge fbd5a6197d862b146763c5b88574404cbe5f6a69. PR #11 added WorkspaceStartupReconciler as the single pre-cutover startup/recovery coordinator, removed the competing mirror and dual-read effects from MainActivity, retries Room acquisition on each reconciliation attempt, and keeps ordinary DataStore operation available when Room cannot open. Verified compatibility failure, mismatch, or unavailable Room returns to DATASTORE without changing Favorite/Dock contents. PR #11 added a separate API 36 runtime-test class that repeatedly closes and reopens file-backed DataStore and Room clients, verifies ROOM_VERIFIED and ordering persistence, forces Room failure, verifies lossless DataStore fallback, reopens Room, and checks that verified compatibility can be rebuilt. During PR #12 review, I discovered that PR #11's emulator command still selected WorkspaceRoomRuntimeTest only, so this separate restart-recovery class was not executed by the exact PR #11 CI run. Exact PR head e069c8f9dfa52a7098f3c73556a73b1e828c67d3 passed the full validate job and its API 36 room-runtime-emulator job in Android CI run 32585286346 before squash merge ba761bcb36dce601dd98fd6da860f2c7546dd5a6. However, that emulator job used a single-class instrumentation filter for WorkspaceRoomRuntimeTest and therefore did not execute the separately added WorkspaceStartupReconcilerRuntimeTest. The historical PR #11 evidence is not retroactively upgraded. PR #12 corrected the job to run the complete instrumentation suite and added WorkspaceRoomPlacementRepository as a post-cutover-style Room placement I/O primitive that is guarded by persisted ROOM authority and remains disconnected from production Home. Exact PR #12 head 263b8d435b6e70818caab0350810f49b1c88c450 passed the normal validate job and the corrected API 36 complete instrumentation suite in Android CI run 32586296916. Emulator logs confirm the command was gradle --no-daemon connectedDebugAndroidTest with no class filter and that six tests started and six tests finished successfully. This exact run closes the PR #11 runtime-evidence gap for the separately added restart-recovery class and accepts the new guarded Room-placement runtime test. PR #12 was squash-merged with expected-head protection as ba9273e849f571eadcef2b698d2d7f0268778168. PR #13 added WorkspaceCutoverReadinessCoordinator as a non-mutating pre-cutover evidence gate, shared WorkspaceCanonicalRoomPlacementReader across dual-read/readiness/post-cutover placement paths, and scripts/check_room_cutover.py to fail CI if production begins calling promoteRoomAuthority or instantiating the ROOM-only placement repository before an explicit cutover change. The first PR #13 head e107d44a2ea686fb91480b62e80283a29b373741 was rejected before compilation because the initial source guard incorrectly counted a KDoc mention of promoteRoomAuthority as executable code. The guard was corrected to strip Kotlin comments and literals before checking executable references; the policy itself was not weakened. Final exact head c6b1aafacce7aa45a76faa7a9080db2472f2dad1 passed Android CI run 32587613393, including Privacy Shield, HOME-manifest, Glaze UI, the corrected cutover guard, lint, JVM tests, debug assembly, Room schema validation/drift enforcement, and the complete API 36 instrumentation suite. Emulator logs confirm eight tests started and eight tests finished successfully. PR #13 was squash-merged with expected-head protection as c425b00308fbbebd68b467fc65ccfcea2cf4d77d. A Ready result remains observational evidence only: production still does not call guarded ROOM promotion and Home still remains on DataStore. The canonical GoreeCloud/glaze-ui consumer registry continues to record Launcher as an adoption-candidate for Glaze UI 1.4.0 after PR #34 passed the complete Glaze UI CI stack and merged as 9ae9b16c6dbf61e95882cf7241cbbd3b3c4a6bbd. Exact post-merge launcher-main CI is not claimed through the connected pull-request-run interface. PR #19 activates the reviewed production workspace runtime: the current Favorites/Dock compatibility Home path promotes through the guarded one-way transaction and uses Room as terminal placement authority, while legacy DataStore Favorite/Dock writes are frozen after cutover and terminal-Room failures fail closed instead of silently falling back. PR #20 accepts the real Android HOME-role MainActivity recreation path and post-recreation Room-authoritative UI reactivity on API 36; its exact-head run 32594478982 executed the unfiltered 21-test instrumentation suite successfully. True Android OS process-death/cold-start survival, schema-version upgrade migration recovery, representative physical-device/default-HOME acceptance, multi-page live cell/span placement, folders, gestures, backup/restore, complete Gradle-wrapper publication, full Glaze phone/tablet visual/native acceptance, signed-package acceptance, and release acceptance remain pending.

## Historical Drive Section 28 — Milestone 0 Implementation Record

A local Milestone 0 source bootstrap has been created for GoreeCloud Launcher. This implementation record is intentionally narrower than release acceptance: it records what exists in source form and what has been locally validated without treating source validation as a successful Android build or device deployment.


Implemented in the bootstrap:
- Android application foundation for the planned native Kotlin/Jetpack Compose architectu PR #21 added the pre-persistence WorkspaceGridPlacement validation foundation for positive grid dimensions, nonnegative coordinates, positive spans, duplicate item detection, out-of-bounds rejection, and multi-cell collision detection. Exact head 226639d1a09b3913af298299ecd1db2dd955a6d5 passed Android CI run 32779946533, including source/build validation and the Android 16 Room runtime-emulator gate, before squash merge ec75da552a16c6715b9c21a8251b65d0b7bb7fdf. This does not yet persist cellX/cellY/spanX/spanY or expose live multi-page grid placement UI.re.
- HOME intent-filter and launcher manifest foundation.
- User-controlled Android ROLE_HOME onboarding flow.
- LauncherApps-based installed-application and profile discovery.
- Package/profile lifecycle callback foundation.
- Basic Glaze UI home/favorites surface.
- Basic all-apps drawer and application launching.
- Persistent local System, Light, and Dark Glaze appearance selection.
- Initial repository documentation and GitHub Actions workflow configuration.
- Privacy guard and manifest-contract guard scripts.


Milestone 0 privacy state:
- The authored manifest contains no INTERNET permission.
- The bootstrap contains no advertising or sponsorship system.
- No analytics, attribution, or behavioral-tracking SDK dependency is included.
- Core launcher source behavior remains local and offline-oriented.


Milestone 0 validation completed:
- Authored Android XML parsed successfully.
- Privacy guard passed.
- Manifest contract guard passed.
- Source manifest generation completed successfully.
- Source was packaged as goreecloud-launcher-milestone0.zip for transfer and repository publication.


Acceptance not yet achieved:
- No Android Gradle compilation is claimed because Android SDK and Gradle were unavailable in the execution environment used for the bootstrap.
- No compiled APK or Android App Bundle is claimed.
- No signed package or signature verification is claimed.
- No emulator acceptance is claimed.
- No physical-device default-HOME acceptance is claimed.
- No GoreeCloud/android-app-defaults (`apps/launcher/`) repository or GitHub commit is claimed because connected GitHub actions did not expose repository creation and no existing repository with that name was found.

## Historical Drive Section 29 — Repository Publication and CI Acceptance Record

Repository: `GoreeCloud/android-app-defaults` (`apps/launcher/`)


Publication branch: bootstrap/milestone-0


Published source commit: 6ab54da837d60a275a00f40a712bbd3c53b4b2de


Pull request: #1 — Bootstrap native Android launcher foundation


PR-head CI: Passed — privacy guard, HOME-manifest guard, Android lint, unit tests, and assembleDebug.


Merge: Squash-merged to main as 9e5f8cd774f7b04a2be7efdbb1fbd6bfc294763f; GitHub reports the squash commit as verified.


Milestone 1 started: local app-drawer search by application label/package and lifecycle-aware HOME-role status refresh are included in the merged source.


Build boundary: PR-head debug build is accepted by CI. Exact-head post-merge main CI remains unconfirmed through the connected interface. No emulator, signed APK, release artifact, or physical-device launcher acceptance is claimed.


Repository engineering boundary: generated Gradle wrapper scripts/JAR remain to be added; CI currently installs Gradle 8.11.1 explicitly.

## Historical Drive Section 30 — Next Development Stage

Milestone 1 will continue from the merged main branch. Priority implementation order:


- Add true Android OS process-death/cold-start acceptance for the activated HOME-role path. Seed deterministic terminal-Room placement in one instrumentation phase, terminate the GoreeCloud Launcher application process from the host without reinstalling or clearing application data, relaunch HOME in a fresh process, verify terminal ROOM and exact ordered Favorite/Dock placement survive, verify Home renders the persisted Room state, and verify a fresh Room-authoritative mutation remains reactive. Keep schema-version upgrade recovery and representative physical-device/default-HOME acceptance as separate gates.
- Build folder creation, membership, ordering, previews, and Home/drawer presentations.
- Icon size and label controls.
- Safe configurable gesture bindings.
- Versioned backup/restore foundation with schema validation.
- Complete Gradle wrapper publication and exact-head main CI verification.


The existing merged app-drawer search remains the first Milestone 1 slice and should be expanded into broader local launcher search without adding network dependencies.

## Historical Drive Section 31 — Milestone 1 Persistent Favorites and Dock Record

August 21, 2026 at 9:26 PM CDT


Pull request: #2 — Add persistent Favorites and Dock workspace state.
Development branch: milestone1-workspace-persistence.
Accepted PR head: 02cea12a83b38cb6dc5320a6bc3536c8f31d1f6b.
Main squash commit: e1f804d5aec305f4cea6e7d587e8c870ae8fc32a.


Implemented source behavior:
- Added WorkspaceRepository backed by Android Preferences DataStore for small ordered launcher state.
- Added persistent ordered Favorites and a persistent Dock capped at five items.
- Added one-time first-run seeding from launchable applications while excluding GoreeCloud Launcher itself.
- Added workspace application keys that combine a public UserHandle discriminator with the flattened Android component name so supported profiles do not collapse into one package identity.
- HOME now resolves persisted keys against the live LauncherApps inventory; unavailable or uninstalled components do not render or launch from stale stored keys.
- App-drawer long-press opens an explicit local management dialog for adding or removing an application from Favorites or the Dock.
- Added JVM tests covering codec order preservation, add/remove behavior, and Dock capacity enforcement.
- Updated README.md, docs/architecture.md, and SOURCE_MANIFEST.txt to describe the persistence model and migration boundary.


Persistence decision: Preferences DataStore is intentionally limited to small ordered launcher preferences in this stage. Multiple workspace pages, cell coordinates, spans, folders, widgets, relational ordering, and schema migrations will move to Room or an equivalent Android-native SQLite abstraction when that richer model is introduced. The migration must preserve the existing user-selected Favorites and Dock state.


CI troubleshooting and resolution: The first PR #2 Android CI run, 32546400208, passed the Privacy Shield guard and HOME-manifest guard but failed during Kotlin compilation because the initial workspace-key implementation referenced UserHandle.identifier, which was unavailable to the current compile surface. I did not weaken CI. I replaced that access with the public UserHandle hash-code discriminator already compatible with the launcher source, reconciled the repository documentation, and reran the exact head.


Final validation: GitHub Actions run 32546484811 passed on exact PR head 02cea12a83b38cb6dc5320a6bc3536c8f31d1f6b. Privacy guard passed; HOME-manifest guard passed; Android lint passed; JVM unit tests passed; assembleDebug passed. PR #2 was then squash-merged with expected-head protection to main as e1f804d5aec305f4cea6e7d587e8c870ae8fc32a.


Privacy and security boundary: This Milestone 1 slice adds no Android permission, no INTERNET permission, no cloud-account dependency, no advertising or sponsorship system, and no analytics, attribution, or behavioral-tracking SDK. Favorites, Dock membership, local app search, and appearance remain device-local.


Acceptance boundary: The accepted evidence is exact PR-head CI. The connected GitHub workflow interface currently exposes pull-request-triggered runs and returns no exact-main run for e1f804d5aec305f4cea6e7d587e8c870ae8fc32a, so post-merge exact-main CI is not claimed. Emulator execution, signed release packaging, physical-device default-HOME acceptance, generated Gradle wrapper scripts/JAR, explicit drag/reorder, folders, widgets, gestures, and versioned backup/restore remain separate future gates.

## Historical Drive Section 32 — Milestone 1 Accessible Workspace Ordering Record

August 21, 2026
Pull request: #3 — Add accessible Favorite and Dock ordering.
Development branch: milestone1-workspace-ordering.
Accepted PR head: fdf35f982c3a3792b9e8395128329576e2fc73fa.
Main squash commit: e700d0170c79eaf7fa36a2f2c15bda6edb5d836e.
Validation: Android CI run 32547052919 passed the Privacy Shield guard, HOME-manifest guard, Android lint, expanded JVM workspace-ordering tests, and assembleDebug on the accepted PR head.
Implemented behavior: Favorites and Dock now share deterministic move-earlier/move-later operations in WorkspaceRepository; movement clamps safely at collection boundaries and leaves unknown keys unchanged. Long-press management is available from All apps, Favorites, and Dock. The shared placement dialog reports membership and position, exposes accessible ordering controls, and prevents adding a sixth Dock item. Direct drag/drop remains planned and must call this same ordering source of truth rather than creating a separate persistence path.
Glaze UI mapping: Added GlazeMetrics.kt as a native Android mapping of the Glaze UI 1.4 Stable spacing, radius, 44-dp minimum-target, and 48-dp comfortable-target semantics currently consumed by Launcher. Ordinary content remains Solid/Raised; Functional Glass is not introduced merely for decoration.
Privacy and security boundary: No Android permission, INTERNET permission, advertising, sponsorship, analytics, attribution, tracking dependency, or cloud-account requirement was added.
Acceptance boundary: PR-head source/build validation is accepted. Direct drag/drop, multi-page workspace placement, phone/tablet visual/native acceptance, emulator acceptance, signed release packaging, and physical-device default-HOME acceptance remain separate gates.

## Historical Drive Section 33 — Glaze UI 1.4 Adoption Contract and Consumer Registry Record

August 21, 2026
Canonical design-system target: Glaze UI 1.4.0 Stable.
Reviewed canonical revision: 883d40ff51d02885650024723c01d229de456285.
Launcher evidence: docs/glaze-ui-adoption.md.
Launcher automated contract: scripts/check_glaze_ui.py.
Launcher PR #4 exact head: 30854ae7689dd0b7edbde8bc305ffd96e382f360; Android CI run 32547255101 passed privacy, HOME-manifest, Glaze UI mapped-subset contract, Android lint, JVM unit tests, and assembleDebug. PR #4 was squash-merged as 79023a0675b0c9f8c700884ecf2214cf5b007e09.
Governance correction: The first GoreeCloud/glaze-ui PR #34 validation correctly rejected the Launcher consumer record because adoption-candidate status requires an automated contract. I kept that fail-closed requirement, added the launcher-side contract, reran launcher CI successfully, and then changed the registry record to automatedContract=true.
Central registry validation: Final Glaze UI PR #34 head 1e9440e37ce7a1aacb21371355ae77dd2fcf0364 passed Glaze UI CI run 32547364311, including canonical repository validation, 1.4 form-factor validation, consumer-registry validation, Firefox integration, public design-site validation, and rendered-reference validation. PR #34 was squash-merged as 9ae9b16c6dbf61e95882cf7241cbbd3b3c4a6bbd.
Current Glaze status: GoreeCloud Launcher retains historical Glaze UI 1.4.0 adoption-candidate evidence and an automated mapped-subset contract, but Glaze UI 1.5.0 is now the mandatory current-Stable target. Launcher is migration-required and must not treat its 1.4 evidence as current conformance. A reviewed 1.5.0 mapping, exact-revision automated validation, phone/tablet rendered and native acceptance, reduced-transparency/device behavior, complete motion/material/layout/state/theme mapping, and representative physical-device acceptance remain required before current Glaze UI conformance can be claimed.

## Historical Drive Section 34 — Milestone 1 Direct Favorite and Dock Reorder Record

August 22, 2026


Pull request: #5 — Add direct Favorite and Dock drag reordering.
Development branch: milestone1-drag-reorder.
Accepted PR head: dfff61d624f5674cda5525950574fb03e0fd969d.
Android CI run: 32577224980.
Main squash commit: 19a9a5b0345693d98023fe7ce37707d51c6cb17e.


Implemented behavior:
- Added explicit Home Reorder mode so normal tap-to-launch and long-press management remain unchanged outside editing.
- Added direct pointer drag/drop target ordering for Favorites and Dock.
- Reorder mode disables ordinary tile interaction while active, visually lifts the dragged tile, and highlights the current valid target.
- Added WorkspaceCodec.movedToTarget plus repository methods that persist dropped ordering through the same local WorkspaceRepository used by Move earlier/Move later.
- Preserved Move earlier/Move later as the permanent non-drag, keyboard-friendly, switch-friendly ordering path.
- Added JVM coverage for forward and backward target

## Historical Drive Section 35 — Milestone 1 Room 3 Relational Workspace Foundation Record

August 22, 2026
Pull request: #6 — Add Room 3 relational workspace foundation.
Development branch: milestone1-relational-workspace.
Accepted PR head: b1843f43bb97834527163deffc1ba3977aff839f.
Android CI run: 32578518697.
Main squash commit: 78a167518f0c378b9afecf48cf75ed7670c0fc43.


Relational foundation:
- Added AndroidX Room 3.0.1 with Kotlin Symbol Processing and AndroidX SQLite 2.7.0 using AndroidSQLiteDriver.
- Added version-1 LauncherDatabase with workspace_pages and workspace_items entities for stable page identity, container rank, application placement, optional cell coordinates, spans, and reserved future shortcut/folder/widget item types.
- Added coroutine-first WorkspaceDao operations and transactional replacement of the two legacy compatibility pages.
- Added WorkspaceLegacyImportMapper and JVM tests that preserve ordered Favorites/Dock state, defensively deduplicate within a container, and allow the same application to exist independently on Home and Dock.
- Added WorkspaceRelationalMirror to refresh Room from initialized DataStore state while DataStore remains the live launcher authority.
- Ordinary database failures remain isolated from the accepted DataStore launcher path; coroutine cancellation is explicitly rethrown and is not swallowed.


Schema governance:
- Room schema history is exported under app/schemas and is now source controlled.
- The exact generated version-1 schema is stored at app/schemas/com.goreecloud.launcher.core.workspace.db.LauncherDatabase/1.json.
- Version-1 Room identity hash: 2fa5d8fba0010dd896c671aadaa5dafb.
- scripts/check_room_schema.py fails if the generated version-1 schema is absent, unparsable, or does not contain workspace_pages and workspace_items.
- CI also checks git status for app/schemas after KSP/Room generation, so modified or newly generated schema history fails until the exact generated output is committed.


Validation: Exact PR-head Android CI run 32578518697 passed the Privacy Shield guard, HOME-manifest guard, Glaze UI 1.4 mapped-subset contract, Room/KSP compilation and schema generation, Android lint, JVM unit tests, assembleDebug, Room schema parse/table validation, and committed Room schema-history drift validation. PR #6 was squash-merged with expected-head protection to main as 78a167518f0c378b9afecf48cf75ed7670c0fc43.


Privacy and security boundary: This slice adds no Android permission, INTERNET permission, cloud-account dependency, advertising, sponsorship, analytics, attribution, or tracking SDK. The Room database is application-local. The relational mirror does not add network behavior.


Acceptance boundary: Room is not yet the authoritative workspace source. This merge does not claim live multi-page workspace UI, cell/span placement UI, folders, widgets, runtime migration acceptance, schema-upgrade migration acceptance, emulator/device database acceptance, signed-release packaging, or physical-device default-HOME acceptance. A later cutover must validate relational reads/writes and recovery before DataStore ceases to be authoritative for Favorites and Dock.
moves, self-drops, and unknown targets.
- Updated README.md and docs/architecture.md to document interaction, persistence, privacy, accessibility, and runtime-acceptance boundaries.


Validation: Exact PR-head Android CI run 32577224980 passed the Privacy Shield guard, HOME-manifest guard, Glaze UI 1.4 mapped-subset contract, Android lint, JVM unit tests, and assembleDebug. PR #5 was squash-merged with expected-head protection to main as 19a9a5b0345693d98023fe7ce37707d51c6cb17e.


Privacy and security boundary: This slice adds no Android permission, INTERNET permission, network dependency, cloud-account dependency, advertising, sponsorship, analytics, attribution, or tracking SDK. Drag state and persisted workspace order remain device-local.


Acceptance boundary: The accepted evidence is exact PR-head source/build CI. Emulator and physical-device touch targeting, rotation behavior, TalkBack and switch-access behavior, rendered/native Glaze acceptance, complete Gradle wrapper publication, signed-release packaging, and physical-device default-HOME acceptance remain open gates. Exact post-merge main CI is not claimed unless separately evidenced.

## Historical Drive Section 36 — Milestone 1 Room Mirror Readback Verification Record

August 22, 2026
Pull request: #7 — Verify Room workspace mirror readback.
Development branch: milestone1-room-readback-verification.
Accepted PR head: aee6fe956cb2932bfa785ea60f2c463b296f80c6.
Android CI run: 32579167978.
Main squash commit: 042280f10dd74ee1a32cf42f3f0861a2df44d6aa.


Implemented behavior:
- Added DAO readback queries for the exact Home and Dock compatibility page IDs and their relational item rows.
- Added WorkspaceRelationalVerifier as a pure deterministic comparison layer between the DataStore-derived expected snapshot and persisted Room rows.
- Equivalent snapshots are accepted regardless of DAO return order, while persisted rank remains authoritative and any rank mutation is treated as a mismatch.
- Missing or extra rows, changed application key, item type, identity, cell coordinates, or spans produce a mismatch rather than a verified mirror result.
- WorkspaceRelationalMirror now writes the compatibility snapshot, reads it back, and returns typed Skipped, Verified, Mismatch, or sanitized Failed results.
- Ordinary database errors and verification mismatches do not replace the accepted Preferences DataStore launcher path. Coroutine cancellation remains explicitly rethrown.
- Verification results intentionally do not include application keys or installed-application inventory.
- Added JVM tests for equivalent reordered query results and for missing/extra rows, rank changes, identity/type changes, application-key changes, and cell/span changes.


Validation: Exact PR-head Android CI run 32579167978 passed the Privacy Shield guard, HOME-manifest guard, Glaze UI 1.4 mapped-subset contract, Room/KSP compilation and schema generation, Android lint, JVM unit tests including relational snapshot verification, assembleDebug, Room schema validation, and committed Room schema-history drift validation. PR #7 was squash-merged with expected-head protection to main as 042280f10dd74ee1a32cf42f3f0861a2df44d6aa.


Privacy and security boundary: This slice adds no Android permission, INTERNET permission, network dependency, cloud-account dependency, advertising, sponsorship, analytics, attribution, or tracking SDK. Readback verification remains local and categorical rather than exposing workspace identifiers through diagnostics.


Acceptance boundary: Preferences DataStore remains the live workspace authority. Source/build/JVM verification does not prove on-device Room creation, actual SQLite readback, process-death recovery, schema-upgrade behavior, or authority cutover safety. Emulator or representative-device relational acceptance remains required before Room can become authoritative.

## Historical Drive Section 37 — Milestone 1 Room Runtime Emulator Acceptance Record

August 22, 2026
Pull request: #8 — Add Room runtime emulator acceptance gate.
Development branch: milestone1-room-runtime-emulator.
Accepted PR head: f25ed07d7f2e1f44bd6e120f984973fbda66e301.
Android CI run: 32579948632.
Main squash commit: 9c3ab9c2fb86c9e9eeb342d096a7e91955416c1a.
Runtime acceptance implemented:
- Added AndroidX Test Runner 1.7.0 and AndroidX Test Ext JUnit 1.3.0 for focused Android instrumentation.
- Added WorkspaceRoomRuntimeTest using the production LauncherDatabase, AndroidSQLiteDriver, and a real file-backed database rather than an in-memory substitute.
- The test mirrors a populated DataStore-shaped snapshot and requires WorkspaceMirrorResult.Verified after real DAO readback.
- The test closes Room, reopens the same database file, and verifies the persisted Home/Dock compatibility rows remain intact.
- The test mirrors a changed snapshot and verifies the replacement path removes stale Favorite/Dock rows rather than accumulating them.
- The test mirrors initialized empty Home/Dock containers and verifies both compatibility pages remain while all compatibility items are cleared.
CI runtime gate:
- Added a room-runtime-emulator job after the normal validate job.
- The runtime job executes on Android 16 / API 36 with an x86_64 system image.
- ReactiveCircus Android Emulator Runner v2.38.0 is pinned to immutable commit a421e43855164a8197daf9d8d40fe71c6996bb0d instead of a floating version tag.
- Exact PR-head run 32579948632 passed the normal Privacy Shield, HOME-manifest, Glaze UI, Room/KSP, Android lint, JVM, debug-build, Room-schema, and committed-schema-drift gates, then passed the API 36 Room runtime emulator job.
Privacy and security boundary: No launcher Android permission, INTERNET permission, application network dependency, cloud-account requirement, advertising, sponsorship, analytics, attribution, or tracking SDK was added. The instrumentation uses synthetic application keys and exports no installed-application inventory.
Acceptance boundary: This record accepts the controlled API 36 Room runtime scenario only. A later merged PR #9 now supplies the separately validated durable migration/authority-state foundation, but this runtime record still does not claim Android process-death recovery, schema-version upgrade migration, live Room workspace-authority cutover, representative physical-device storage behavior, physical drag acceptance, signed-release acceptance, full Glaze UI visual/native acceptance, or physical-device default-HOME acceptance. Preferences DataStore remains the live Favorites/Dock authority until a separately validated production cutover explicitly changes that source of truth.

## Historical Drive Section 38 — Milestone 1 Durable Room Authority-State Record

August 22, 2026 at 10:45 AM CDT
Pull request: #9 — Add durable Room authority-state foundation.
Development branch: milestone1-room-authority-state.
Base main commit: 9c3ab9c2fb86c9e9eeb342d096a7e91955416c1a.
Accepted PR head: 61c2bb3316e9721ee67970ed5b46f70b3915a078.
Final Android CI run: 32582256642.
Main squash commit: 50dc52bdaec6ffaddaafef2fa8669dcc8ab3df92.


Authority-state foundation:
- Added durable DATASTORE, ROOM_VERIFIED, and guarded ROOM workspace-authority phases persisted in Preferences DataStore.
- Added a local, order-sensitive SHA-256 workspace fingerprint with separate Favorite and Dock domains so ROOM_VERIFIED evidence is bound to the exact accepted legacy snapshot.
- Legacy Favorite/Dock mutations invalidate stale ROOM_VERIFIED evidence and return pre-cutover authority to DATASTORE; a future ROOM authority state is never silently demoted.
- recordRoomVerification rejects a stale verification result if the current DataStore workspace changed while Room verification was running.
- failRoomVerification returns mismatch or sanitized failure to DATASTORE only before cutover.
- promoteRoomAuthority is a guarded one-way primitive that requires current ROOM_VERIFIED evidence and a matching snapshot fingerprint. Production runtime does not invoke it in this slice.
- MainActivity stops legacy mirroring if a future accepted ROOM authority exists; otherwise it continues to mirror still-authoritative DataStore state and records or invalidates verification outcomes without changing Home's source of truth.
- Added docs/workspace-authority.md, JVM authority/fingerprint tests, and expanded API 36 instrumentation covering file-backed Preferences DataStore reopen, ROOM_VERIFIED persistence, mutation invalidation, stale-promotion rejection, re-verification, guarded promotion, and no-silent-demotion behavior.


CI troubleshooting:
The first PR #9 run, 32581807925, passed the privacy, HOME-manifest, and Glaze UI guards but failed two JVM fingerprint tests. The SHA-256 conversion code had an incorrect loop/trailing-lambda closure that caused the returned digest representation to be wrong. I corrected the source rather than weakening the test gate.
The next run, 32581950904, passed the normal validate job but failed the Android emulator job during JUnit runner initialization. The @After teardown function had inferred a Boolean return type because its final expression was File.delete(). I made the teardown explicitly Unit-compatible and reran the exact head.


Validation:
Final exact PR head 61c2bb3316e9721ee67970ed5b46f70b3915a078 passed Android CI run 32582256642. Privacy Shield, HOME-manifest, Glaze UI 1.4, Kotlin/Room compilation, Android lint, JVM unit tests, assembleDebug, Room schema validation, committed Room schema-history drift validation, and the API 36 x86_64 emulator runtime job all passed. PR #9 was squash-merged with expected-head protection to authoritative main as 50dc52bdaec6ffaddaafef2fa8669dcc8ab3df92.


Privacy and security boundary:
No Android permission, INTERNET permission, cloud-account dependency, advertising, sponsorship, analytics, attribution, tracking SDK, or remote workspace dependency was added. Authority state and verification fingerprints remain local; diagnostic outcomes do not expose installed-app inventory.


Acceptance boundary:
Preferences DataStore remains the live Favorites/Dock source consumed by Home. The guarded ROOM promotion primitive exists but is not called by production runtime. Exact post-merge main CI is not claimed through the connected pull-request-run interface. Live Room workspace reads/writes, dual-read reconciliation and cutover rehearsal, Android process-death recovery, schema-version upgrade migration acceptance, representative physical-device storage/default-HOME acceptance, complete Gradle-wrapper publication, signed-release packaging, and release acceptance remain separate future gates.

## Historical Drive Section 39 — Milestone 1 Room Dual-Read Reconciliation Record

August 22, 2026 at 11:28 AM CDT
Pull request: #10 — Add Room dual-read reconciliation.
Development branch: milestone1-room-dual-read.
Base main commit: 50dc52bdaec6ffaddaafef2fa8669dcc8ab3df92.
Accepted PR head: c6a6a54948f6f14f648474d141e68e6f5cc9c80e.
Android CI run: 32584468440.
Main squash commit: fbd5a6197d862b146763c5b88574404cbe5f6a69.
Dual-read foundation:
- Added WorkspaceRelationalReader and WorkspaceRelationalReadMapper to reconstruct ordered Favorites and Dock state independently from the two canonical Room compatibility pages.
- Canonical reconstruction fails closed when required compatibility pages are missing, application records are malformed, or persisted page/item identity, rank, type, coordinates, or spans no longer match the deterministic legacy mapping.
- Added categorical WorkspaceDualReadResult states: Skipped, Match, Mismatch, and sanitized Failed. Failure results retain the exception type rather than application keys or installed-application inventory.
- MainActivity now creates the Room DAO through a fail-safe initialization boundary. The legacy mirror runs only while DATASTORE is authoritative. ROOM_VERIFIED enters the independent dual-read stage instead of performing another mirror write.
- A dual-read Match leaves verified compatibility state intact. Mismatch, read failure, or unavailable Room DAO returns pre-cutover authority to DATASTORE without clearing or replacing the user's existing Favorites/Dock choices.
- Production runtime still does not call promoteRoomAuthority and Home still consumes Preferences DataStore state.
Validation:
- Added JVM tests for canonical reconstruction despite reversed DAO return order, noncanonical rank rejection, missing-page rejection, and malformed application-record rejection.
- Extended WorkspaceRoomRuntimeTest on the API 36 x86_64 emulator to prove successful independent Match, deliberate relational divergence as Mismatch, preservation of authoritative DataStore Favorites/Dock during fallback, re-mirroring/re-verification, and sanitized Failed behavior after the Room database is closed.
- Exact PR head c6a6a54948f6f14f648474d141e68e6f5cc9c80e passed Android CI run 32584468440. The validate job passed Privacy Shield, HOME-manifest, Glaze UI 1.4, Android lint, JVM tests, assembleDebug, Room schema validation, and committed schema-history drift enforcement. The dependent room-runtime-emulator job also passed.
- PR #10 was squash-merged with expected-head protection to authoritative main as fbd5a6197d862b146763c5b88574404cbe5f6a69; GitHub reports the merge commit as verified.
Privacy and security boundary:
No Android permission, INTERNET permission, cloud-account dependency, remote workspace service, advertising, sponsorship, analytics, attribution, or tracking SDK was added. Dual-read and fallback state remain local and categorical.
Acceptance boundary:
Preferences DataStore remains the live Favorites/Dock source consumed by Home. Exact post-merge main CI is not claimed through the connected pull-request-run interface. Production ROOM promotion, Room-backed live placement writes, a cutover coordinator, Android process-death/cold-start recovery acceptance, schema-version upgrade migration acceptance, representative physical-device storage/default-HOME acceptance, complete Gradle-wrapper publication, signed-release packaging, and release acceptance remain separate future gates.

## Historical Drive Section 40 — Milestone 1 Deterministic Room Restart-Recovery Record

August 22, 2026
Pull request: #11 — Add deterministic Room restart recovery.
Development branch: milestone1-room-restart-recovery.
Base main commit: fbd5a6197d862b146763c5b88574404cbe5f6a69.
Accepted PR head: e069c8f9dfa52a7098f3c73556a73b1e828c67d3.
Android CI run: 32585286346.
Main squash commit: ba761bcb36dce601dd98fd6da860f2c7546dd5a6.
Implemented recovery behavior:
- Added WorkspaceStartupReconciler as the single deterministic pre-cutover coordinator for DATASTORE and ROOM_VERIFIED startup/recovery state.
- Ordinary DATASTORE authority remains usable when Room is unavailable; inability to open Room does not block Home or clear persisted Favorite/Dock state.
- When Room is available, the coordinator performs the existing mirror/verification and independent dual-read stages without invoking promoteRoomAuthority.
- ROOM_VERIFIED mismatch, skipped verification, unavailable Room, or ordinary Room failure falls back to DATASTORE while preserving the user’s existing ordered Favorites and Dock.
- A successful dual-read Match is followed by a fresh DataStore state read so stale compatibility success is not treated as current readiness evidence after concurrent workspace mutation.
- The Room DAO provider is evaluated on every reconciliation attempt, allowing a later attempt to recover after a transient pre-cutover database-open failure.
- MainActivity now uses the single coordinator instead of maintaining separate mirror and dual-read effects that could independently react to authority changes.
Runtime-test source and CI correction:
- Added a focused API 36 file-backed runtime-test class for repeated persistence-client reopen and recovery behavior.
- The test repeatedly closes and recreates Preferences DataStore and the Room database, verifies ROOM_VERIFIED metadata and exact Favorite/Dock ordering survive the reopen cycles, deliberately makes Room unavailable, verifies lossless DATASTORE fallback, reopens Room, and proves verified compatibility can be rebuilt.
- Exact PR head e069c8f9dfa52a7098f3c73556a73b1e828c67d3 passed the Privacy Shield guard, HOME-manifest guard, Glaze UI mapped-subset contract, Kotlin/Room compilation, Android lint, JVM unit tests, assembleDebug, Room schema validation, committed schema-history drift validation, and its API 36 x86_64 room-runtime-emulator job in Android CI run 32585286346. The emulator job itself was class-filtered to WorkspaceRoomRuntimeTest; it did not execute WorkspaceStartupReconcilerRuntimeTest.
- PR #11 was squash-merged with expected-head protection to main as ba761bcb36dce601dd98fd6da860f2c7546dd5a6.
Privacy and security boundary: No Android permission, INTERNET permission, cloud-account dependency, advertising, sponsorship, analytics, attribution, tracking SDK, or remote workspace service was added. Recovery state remains local and failures remain bounded to pre-cutover compatibility behavior.
Acceptance boundary: The repeated-reopen scenario exists as source/runtime-test coverage, but exact PR #11 CI did not execute that separate test class because of the single-class emulator filter. PR #12 is the corrective complete-suite validation slice. This remains distinct from Android OS process-death survival. Preferences DataStore remains Home’s live workspace authority. Production does not invoke guarded ROOM promotion. Room-backed authoritative live placement I/O, explicit production cutover/recovery coordination, process-death acceptance, schema-upgrade migration acceptance, representative physical-device validation, signed release packaging, and physical-device default-HOME acceptance remain separate gates.

## Historical Drive Section 41 — Milestone 1 Guarded Room Placement I/O and Complete Runtime Suite Record

August 22, 2026
Pull request: #12 — Add guarded Room placement I/O.
Development branch: milestone1-room-authoritative-io.
Base main commit: ba761bcb36dce601dd98fd6da860f2c7546dd5a6.
Accepted PR head: 263b8d435b6e70818caab0350810f49b1c88c450.
Android CI run: 32586296916.
Main squash commit: ba9273e849f571eadcef2b698d2d7f0268778168.
Implemented Room placement contract:
- Added WorkspaceRoomPlacementRepository as reserved post-cutover infrastructure for the current Home/Dock compatibility containers.
- Every read/write checks persisted workspace authority. Uninitialized, DATASTORE, and ROOM_VERIFIED states return Reserved; missing Room DAO returns Unavailable.
- Under guarded ROOM authority, read reconstructs the canonical home:0/dock:0 snapshot from Room.
- Replace normalizes ordered Favorite/Dock keys, deduplicates each container, enforces the five-item Dock limit, transactionally writes Room rows, reads them back, and reports Written only when canonical readback exactly equals the normalized request.
- Ordinary failures retain only the exception type, and coroutine cancellation is rethrown.
- Production MainActivity/Home does not use this repository and production still does not invoke promoteRoomAuthority.
CI coverage correction and runtime acceptance:
- During PR #12 preparation, the PR #11 workflow was audited and found to have retained a single instrumentation-class filter for WorkspaceRoomRuntimeTest. The separate WorkspaceStartupReconcilerRuntimeTest added by PR #11 was therefore not executed by the exact PR #11 emulator command. Historical PR #11 evidence was corrected rather than retroactively upgraded.
- PR #12 removed the single-class filter and changed the emulator gate to gradle --no-daemon connectedDebugAndroidTest.
- The corrected API 36 x86_64 job executed the complete Android instrumentation suite. Job logs report “Starting 6 tests” and “Finished 6 tests” followed by BUILD SUCCESSFUL.
- The six-test suite includes the established Room runtime/authority/dual-read scenarios, the restart-recovery persistence-client reopen scenarios, and the new guarded Room placement I/O scenarios.
- Exact PR #12 head 263b8d435b6e70818caab0350810f49b1c88c450 also passed Privacy Shield, HOME-manifest, Glaze UI mapped-subset validation, Android lint, JVM tests, debug APK assembly, Room schema validation, and committed Room schema-history drift enforcement in Android CI run 32586296916.
- PR #12 was squash-merged with expected-head protection to main as ba9273e849f571eadcef2b698d2d7f0268778168, and authoritative main was verified at that SHA.
Privacy and security boundary: No Android permission, INTERNET permission, analytics, advertising, sponsorship, attribution, tracking SDK, cloud account, or remote workspace service was added. Authority gating fails closed before cutover and keeps Room placement access unavailable to current Home runtime.
Acceptance boundary: The corrected complete-suite API 36 emulator evidence is accepted for the six controlled instrumentation tests. It does not constitute Android OS process-death testing, representative physical-device storage/default-HOME acceptance, schema-version upgrade migration acceptance, signed-release acceptance, or production Room cutover. Preferences DataStore remains the live Home workspace authority until a separately accepted production cutover explicitly changes routing.

## Historical Drive Section 42 — Milestone 1 Observational Room Cutover-Readiness Record

PR #13 — Add observational Room cutover readiness gate — was developed from authoritative main ba9273e849f571eadcef2b698d2d7f0268778168 and merged as c425b00308fbbebd68b467fc65ccfcea2cf4d77d.


The slice adds WorkspaceCutoverReadinessCoordinator. Readiness is deliberately observational: DATASTORE returns NeedsVerification, unavailable Room returns Unavailable, readable divergence returns Mismatch, changed verification evidence returns StaleEvidence, ordinary failures are sanitized, and terminal ROOM is reported as AlreadyRoomAuthoritative. Ready requires current initialized ROOM_VERIFIED state, an independent dual-read Match, a second fresh canonical Room placement read, and a final DataStore recheck proving the authority, ordered Favorites, Dock, and verified fingerprint remain unchanged.


WorkspaceCanonicalRoomPlacementReader is shared by dual-read, readiness, and guarded post-cutover placement I/O so those paths use one strict canonical relational parser without sharing their authority policy. The readiness coordinator does not write Room, does not mutate DataStore authority, and does not invoke promoteRoomAuthority.


The slice also adds scripts/check_room_cutover.py and runs it in the normal CI validation job. The guard rejects executable production calls to promoteRoomAuthority outside its repository definition and rejects production instantiation of WorkspaceRoomPlacementRepository before an explicit cutover change. Test code may exercise those contracts. Comments and literals are removed before analysis so documentation text cannot satisfy or trip the executable-code guard.


The first PR #13 head e107d44a2ea686fb91480b62e80283a29b373741 failed CI run 32587555202 before compilation because the initial guard counted a KDoc mention of promoteRoomAuthority as a production call. This was treated as a guard implementation bug, not a reason to weaken the cutover policy. The source-aware stripping correction produced final head c6b1aafacce7aa45a76faa7a9080db2472f2dad1.


Final Android CI run 32587613393 passed Privacy Shield, HOME-manifest, Glaze UI 1.4 contract, corrected Room cutover guard, Android lint, JVM tests, debug APK assembly, Room schema validation, committed-schema drift validation, and the complete API 36 emulator instrumentation suite. The emulator log reported eight tests started and eight tests finished successfully.


Acceptance boundary: Preferences DataStore remains Home's live workspace authority. Production still does not invoke guarded ROOM promotion, MainActivity/Home does not route through WorkspaceRoomPlacementRepository, Android OS process-death acceptance is not claimed, representative physical-device/default-HOME acceptance remains pending, and no signed-release acceptance is claimed.

## Historical Drive Section 43 — Milestone 1 Room Promotion Transaction Rehearsal Record

August 22, 2026
Pull request: #14 — Add Room promotion transaction rehearsal.
Development branch: milestone1-room-promotion-rehearsal.
Base main commit: c425b00308fbbebd68b467fc65ccfcea2cf4d77d.
Accepted PR head: 095654ab7280d07b80ce034181e46472eda89999.
Android CI run: 32588488860.
Main squash commit: 1bb7d9f8a4d56a72464b04d82373f1fa5309fb87.


Implemented promotion rehearsal behavior:
- Added WorkspacePromotionRehearsalCoordinator as a non-promoting transaction rehearsal between accepted observational readiness and a future production authority cutover.
- A promotion candidate is produced only after the existing readiness coordinator returns Ready, the workspace remains initialized ROOM_VERIFIED with a verified fingerprint, Room is acquired again, a fresh canonical Room read still equals the ordered DataStore Favorites/Dock snapshot, and a final DataStore read proves the exact evidence is still current.
- WorkspacePromotionCandidate carries the exact WorkspaceState that the existing guarded promoteRoomAuthority primitive would have to accept. Production code still does not invoke that primitive.
- Added WorkspacePostCutoverHealthEvaluator with explicit Healthy, Unavailable, Mismatch, AuthorityChanged, Failed, and NotRoomAuthoritative outcomes for future terminal-ROOM recovery semantics without automatic rollback to potentially stale DataStore placement.
- Added API 36 file-backed runtime coverage proving stale candidates are rejected, fresh candidates can be accepted in test-only rehearsal, Room divergence/unavailability blocks candidate creation, and terminal ROOM remains one-way when Room becomes unavailable and later reopens.
- Added docs/room-promotion-rehearsal.md and registered the new production source, runtime test, and documentation in SOURCE_MANIFEST.txt.


Validation:
- Exact PR #14 head 095654ab7280d07b80ce034181e46472eda89999 passed Android CI run 32588488860.
- The validate job passed Privacy Shield, HOME-manifest, Glaze UI 1.4 mapped-subset validation, the fail-closed Room cutover guard, Android lint, JVM unit tests, debug APK assembly, Room schema validation, and committed schema-history drift enforcement.
- The API 36 x86_64 room-runtime-emulator job ran the unfiltered gradle --no-daemon connectedDebugAndroidTest suite. Logs report Starting 10 tests, Finished 10 tests, and BUILD SUCCESSFUL.
- PR #14 was squash-merged with expected-head protection to authoritative main as 1bb7d9f8a4d56a72464b04d82373f1fa5309fb87. GitHub reports the merge commit as verified, and main was read back at that exact SHA.


Privacy and security boundary:
No Android permission, INTERNET permission, cloud-account dependency, advertising, sponsorship, analytics, attribution, tracking SDK, or remote workspace service was added. Failure outcomes remain categorical/sanitized, coroutine cancellation is rethrown, and the production cutover guard remains fail closed.


Acceptance boundary:
Preferences DataStore remains Home's live workspace authority. Production did not invoke promoteRoomAuthority and did not route Home through WorkspaceRoomPlacementRepository in PR #14. That merge accepted promotion-transaction rehearsal and terminal-ROOM health semantics only. PR #15 subsequently added the separately reviewed production promotion coordinator while keeping activation and Home-to-Room routing prohibited. Android OS process-death/cold-start acceptance, activated Home-to-Room routing, schema-upgrade migration/recovery, representative physical-device/default-HOME acceptance, signing, and release acceptance remain separate gates.

## Historical Drive Section 44 — Milestone 1 Guarded Production Room Promotion Coordinator Record

August 22, 2026
Pull request: #15 — Add guarded production Room promotion coordinator.
Development branch: milestone1-production-promotion-coordinator.
Base main commit: 1bb7d9f8a4d56a72464b04d82373f1fa5309fb87.
Accepted PR head: 5581de37ab0f74636fc1fc9cf8fa9c10a4c0b915.
Android CI run: 32588866275.
Main squash commit: 0fa8a10bc7b7b5a017eb2cf13b65598b2ba182de.


Implemented production transaction contract:
- Added WorkspaceProductionPromotionCoordinator as the reviewed production implementation of the one-way workspace-authority transaction without activating it from MainActivity or Home.
- The coordinator consumes the accepted promotion-rehearsal path, reacquires Room, performs another fresh canonical Room read, requires exact equality with the verified ordered DataStore Favorites/Dock snapshot, rechecks current ROOM_VERIFIED evidence, and only then invokes the guarded promoteRoomAuthority primitive.
- After a successful authority mutation, the coordinator verifies terminal ROOM and immediately evaluates WorkspacePostCutoverHealthEvaluator. PromotedHealthy is returned only when the terminal marker and Room health both succeed.
- If terminal ROOM has already been recorded but the immediate health check is unavailable, mismatched, changed, or failed, the coordinator returns PromotedRecoveryRequired. It never silently rolls authority back to potentially stale DataStore placement.
- scripts/check_room_cutover.py was narrowed rather than removed. Exactly one production promotion call outside WorkspaceRepository is permitted, and it must be inside WorkspaceProductionPromotionCoordinator. Production instantiation of the coordinator remains prohibited, and production instantiation of WorkspaceRoomPlacementRepository remains prohibited, keeping Home routing inactive.
- Added API 36 file-backed runtime coverage for pre-verification refusal, healthy production-coordinator promotion, deliberate Room divergence blocking, unavailable Room blocking, and preservation of authority/workspace state across failed pre-promotion attempts.
- Added docs/production-promotion-coordinator.md and registered the production source, runtime test, and documentation in SOURCE_MANIFEST.txt.


Validation:
- Exact PR #15 head 5581de37ab0f74636fc1fc9cf8fa9c10a4c0b915 passed Android CI run 32588866275.
- The validate job passed Privacy Shield, HOME-manifest, Glaze UI 1.4 mapped-subset validation, the narrowed fail-closed Room cutover guard, Android lint, JVM unit tests, debug APK assembly, Room schema validation, and committed schema-history drift enforcement.
- The API 36 x86_64 room-runtime-emulator job ran the unfiltered gradle --no-daemon connectedDebugAndroidTest suite. Logs report Starting 12 tests, Finished 12 tests, and BUILD SUCCESSFUL.
- PR #15 was squash-merged with expected-head protection to authoritative main as 0fa8a10bc7b7b5a017eb2cf13b65598b2ba182de. GitHub reports the squash commit as verified, and main was read back at that exact SHA.


Privacy and security boundary:
No Android permission, INTERNET permission, cloud-account dependency, advertising, sponsorship, analytics, attribution, tracking SDK, or remote workspace service was added. Failures remain categorical/sanitized, coroutine cancellation is rethrown, and authority changes remain local to the device.


Acceptance boundary:
Preferences DataStore remains Home's live workspace authority. The production promotion coordinator now exists in merged source, but production runtime does not instantiate it and Home does not route through WorkspaceRoomPlacementRepository. This merge accepts the guarded production transaction implementation only. Post-cutover startup/recovery acceptance, Android lifecycle/process-recreation and OS process-death acceptance, activated Home-to-Room routing, schema-upgrade migration/recovery, representative physical-device/default-HOME acceptance, signing, and release acceptance remain separate gates.

## Historical Drive Section 45 — Milestone 1 Post-Cutover Room Startup Recovery Record

August 22, 2026
Pull request: #16 — Add post-cutover Room startup recovery.
Development branch: milestone1-post-cutover-startup-recovery.
Base main commit: 0fa8a10bc7b7b5a017eb2cf13b65598b2ba182de.
Accepted PR head: c68c316da9ba3e562ff0acb8216f7e852f54d819.
Android CI run: 32589307263.
Main squash commit: bc758e3edbc5de6081c0833d01cbfd4c9be52537.


Implemented recovery contract:
- Added WorkspacePostCutoverStartupCoordinator as a read-only terminal-ROOM startup/recovery evaluator that remains disconnected from MainActivity and Home.
- Startup Ready requires initialized persisted ROOM authority plus a healthy canonical Room placement through WorkspacePostCutoverHealthEvaluator.
- Unavailable, mismatched, changed, or failed terminal Room health becomes RecoveryRequired. The coordinator never demotes authority to DataStore because legacy placement may be stale after a future accepted cutover.
- Expanded scripts/check_room_cutover.py so the reviewed post-cutover startup coordinator may exist in production source but any production instantiation outside its own declaration is rejected. The existing prohibition on production promotion-coordinator activation and WorkspaceRoomPlacementRepository routing remains active.
- Added API 36 file-backed runtime coverage proving terminal ROOM plus exact Favorite/Dock state survive DataStore and Room client close/reopen, unavailable Room produces explicit recovery while authority remains ROOM, and reopening the same database restores startup readiness without authority changes.
- Added docs/post-cutover-startup-recovery.md and registered the new source/test/documentation in SOURCE_MANIFEST.txt.


Validation:
- Exact PR #16 head c68c316da9ba3e562ff0acb8216f7e852f54d819 passed Android CI run 32589307263.
- The validate job passed Privacy Shield, HOME-manifest, Glaze UI 1.4 mapped-subset validation, the fail-closed Room cutover/activation guard, Android lint, JVM tests, debug APK assembly, Room schema validation, and committed schema-history drift enforcement.
- The unfiltered Android 16 / API 36 x86_64 connectedDebugAndroidTest suite reported Starting 14 tests, Finished 14 tests, and BUILD SUCCESSFUL.
- PR #16 was squash-merged with expected-head protection as bc758e3edbc5de6081c0833d01cbfd4c9be52537. Authoritative main was read back at the same SHA and GitHub reports the commit as verified.


Privacy and security boundary:
No Android permission, INTERNET permission, cloud dependency, advertising, sponsorship, analytics, attribution, tracking SDK, installed-application inventory export, or remote workspace service was added. Runtime test identifiers are synthetic and recovery outcomes remain categorical.


Acceptance boundary:
Preferences DataStore remains Home's live workspace authority. The production promotion and post-cutover startup/recovery coordinators are merged infrastructure but remain unwired, and Home still does not route through WorkspaceRoomPlacementRepository. This merge accepts persistence-client reopen/startup recovery evidence only; it is not Android OS process-death acceptance. Authority-aware Home routing, activated-path lifecycle/process-death testing, schema-upgrade recovery, representative physical-device/default-HOME acceptance, signing, and release acceptance remain separate gates.

## Historical Drive Section 46 — Milestone 1 Authority-Aware Workspace Placement Routing Record

August 22, 2026
Pull request: #17 — Add authority-aware workspace placement routing.
Development branch: milestone1-authoritative-placement-routing.
Base main commit: bc758e3edbc5de6081c0833d01cbfd4c9be52537.
Accepted PR head: b942cff6fba66ffc4382c30b08169f3c6b7c836c.
Android CI run: 32589746381.
Main squash commit: 993c9346690a6f0b21f6ae8804557628600f5b30.


Implemented routing contract:
- Added WorkspaceAuthoritativePlacementRepository as one compatibility routing layer for the current Favorites/Dock model. DATASTORE and ROOM_VERIFIED use the existing WorkspaceRepository; terminal ROOM uses guarded WorkspaceRoomPlacementRepository.
- Added consistent read and write result types with explicit WaitingForInitialization, Loaded/Written, AuthorityChanged, Unavailable, Mismatch, and sanitized Failed outcomes.
- Favorite/Dock toggle, move-earlier/later, and move-to-target semantics reuse WorkspaceCodec on both authority sides, preserving the five-item Dock limit.
- Room unavailable, mismatch, or failure after terminal authority fails closed and never falls back to potentially stale DataStore placement.
- Closed a post-cutover ambiguity by freezing legacy WorkspaceRepository first-run placement and Favorite/Dock membership/order writes whenever persisted authority is terminal ROOM. Direct stale legacy callbacks therefore cannot continue changing non-authoritative DataStore placement.
- Expanded scripts/check_room_cutover.py so WorkspaceAuthoritativePlacementRepository may instantiate WorkspaceRoomPlacementRepository exactly once internally, while production instantiation of the authority-aware router itself remains prohibited. Promotion and post-cutover startup coordinators remain unwired.
- Added Android 16 / API 36 file-backed runtime coverage proving pre-cutover router writes use DataStore, ROOM_VERIFIED mutations invalidate verification back to DATASTORE, terminal ROOM router writes/readbacks use Room, direct legacy DataStore mutations remain frozen after cutover, and unavailable Room yields an explicit unavailable result without changing terminal authority.
- Added docs/authoritative-placement-routing.md and source-manifest registration.


Validation:
- Exact PR #17 head b942cff6fba66ffc4382c30b08169f3c6b7c836c passed Android CI run 32589746381.
- The validate job passed Privacy Shield, HOME-manifest, Glaze UI 1.4 mapped-subset validation, the expanded fail-closed Room activation guard, Android lint, JVM tests, debug APK assembly, Room schema validation, and committed schema-history drift enforcement.
- The unfiltered Android 16 / API 36 x86_64 connectedDebugAndroidTest suite reported Starting 16 tests, Finished 16 tests, and BUILD SUCCESSFUL.
- PR #17 was squash-merged with expected-head protection as 993c9346690a6f0b21f6ae8804557628600f5b30. Authoritative main was read back at that same SHA and GitHub reports the commit as verified.


Privacy and security boundary:
No Android permission, INTERNET permission, cloud dependency, advertising, sponsorship, analytics, attribution, tracking SDK, installed-application inventory export, or remote workspace service was added. Runtime identifiers remain synthetic and failure outcomes remain categorical/sanitized.


Acceptance boundary:
Preferences DataStore remains Home's live collected workspace state because the authority-aware router is still unwired from MainActivity and LauncherRoot. The production promotion coordinator and post-cutover startup/recovery coordinator also remain unwired. This merge accepts authority-aware read/write routing and terminal-ROOM legacy-write freezing only. Observable Home state, final production activation, activated-path lifecycle/process-death acceptance, schema-upgrade recovery, representative physical-device/default-HOME acceptance, signing, and release acceptance remain separate gates.

## Historical Drive Section 47 — Milestone 1 Observable Authority-Aware Workspace State Record

August 22, 2026
Pull request: #18 — Add observable authority-aware workspace state.
Development branch: milestone1-observable-authoritative-placement.
Base main commit: 993c9346690a6f0b21f6ae8804557628600f5b30.
Accepted PR head: 187f1a5a7b39b2c65de4dc51ed23dbb70e0af0f4.
Android CI run: 32590179889.
Main squash commit: 4cf2034b7667bf6eaa056833fa155a611b9c9822.
Implemented observation contract:
- Added WorkspaceAuthoritativePlacementObserver as the lifecycle-friendly placement state that Home can consume after an accepted activation.
- DATASTORE and ROOM_VERIFIED emit ordered Favorites/Dock from WorkspaceRepository; terminal ROOM switches to reactive WorkspaceDao page/item observation and strict canonical relational reconstruction.
- Terminal Room unavailability, malformed canonical placement, or ordinary observation failure becomes explicit RecoveryRequired and never falls back to potentially stale legacy DataStore placement.
- Room observation is filtered to the current Home/Dock compatibility page IDs so future unrelated relational pages cannot contaminate the compatibility state.
- Expanded scripts/check_room_cutover.py so the observer may exist as reviewed production infrastructure while any production instantiation outside its own declaration remains prohibited until the explicit activation slice.
- Added Android 16 / API 36 file-backed runtime coverage for uninitialized state, DataStore observation, pre-cutover mutation propagation, authority switching to Room, reactive Room mutations, unavailable Room, and malformed Room recovery behavior.
- Added docs/authoritative-placement-observation.md and source-manifest registration.
Validation:
- Exact PR #18 head 187f1a5a7b39b2c65de4dc51ed23dbb70e0af0f4 passed Android CI run 32590179889.
- The validate job passed Privacy Shield, HOME-manifest, Glaze UI 1.4 mapped-subset validation, the expanded fail-closed Room cutover/activation guard, Android lint, JVM tests, debug APK assembly, Room schema validation, and committed schema-history drift enforcement.
- The unfiltered Android 16 / API 36 x86_64 connectedDebugAndroidTest suite reported Starting 18 tests, Finished 18 tests, and BUILD SUCCESSFUL.
- PR #18 was squash-merged with expected-head protection as 4cf2034b7667bf6eaa056833fa155a611b9c9822. Authoritative main was read back at that same SHA and GitHub reports the commit as verified.
Privacy and security boundary:
No Android permission, INTERNET permission, cloud dependency, advertising, sponsorship, analytics, attribution, tracking SDK, installed-application inventory export, or remote workspace service was added. Runtime identifiers remain synthetic and failure outcomes remain categorical/sanitized.
Acceptance boundary:
Preferences DataStore remains Home's live collected workspace state because the new observer, authority-aware mutation router, production promotion coordinator, and post-cutover startup coordinator are still deliberately unwired. This merge accepts observable authority switching and explicit terminal-Room recovery state only. Final production activation, activated-path lifecycle/process-recreation and OS process-death acceptance, schema-upgrade recovery, representative physical-device/default-HOME acceptance, signing, and release acceptance remain separate gates

## Historical Drive Section 48 — Milestone 1 Production Workspace Authority Activation Record

August 22, 2026
Pull request: #19 — Wire authoritative workspace runtime.
Development branch: milestone1-production-workspace-activation.
Base main commit: 4cf2034b7667bf6eaa056833fa155a611b9c9822.
Accepted PR head: 0e0d4795d4e062364759dcba20008a18ae8d76d9.
Android CI run: 32591361889.
Main squash commit: 69259ee0fb822f43ee1e6bf7af629de8829faaa5.
Implemented production activation:
- Added WorkspaceProductionRuntimeCoordinator as the single production composition boundary for pre-cutover reconciliation, guarded production promotion, post-cutover startup recovery, authority-aware placement observation, and authority-aware placement mutations.
- MainActivity now collects Home Favorites/Dock placement from the production runtime coordinator rather than directly from WorkspaceRepository state.
- Favorite/Dock toggle, move-earlier/later, and direct move-to-target callbacks now route through the authority-aware placement router via the runtime coordinator.
- Before terminal authority, DataStore remains usable while startup reconciliation mirrors and independently verifies the current workspace. If current ROOM_VERIFIED evidence is valid, the production promotion coordinator performs the guarded one-way transition in the same reconciliation call.
- After terminal ROOM, post-cutover health is checked before authoritative Room placement is exposed. Unavailable, malformed, mismatched, or failing Room becomes explicit recovery state and never silently falls back to potentially stale DataStore placement.
- MainActivity re-evaluates the runtime on resume. Legacy WorkspaceRepository Favorite/Dock mutations remain frozen under terminal ROOM.
- scripts/check_room_cutover.py was narrowed rather than removed. CI enforces the exact topology MainActivity -> WorkspaceProductionRuntimeCoordinator -> reviewed promotion/recovery/observer/router -> guarded Room placement repository, and rejects direct bypass construction or promotion calls.
- Added API 36 file-backed end-to-end coverage for production promotion, Room observation, Room Favorite/Dock mutation, DataStore/Room client close-reopen, resumed terminal-Room operation, unavailable-Room recovery, and frozen legacy writes.
- Added docs/production-workspace-activation.md and source-manifest registration.
Validation:
- Exact PR #19 head 0e0d4795d4e062364759dcba20008a18ae8d76d9 passed Android CI run 32591361889.
- The validate job passed Privacy Shield, HOME-manifest, Glaze UI 1.4 mapped-subset validation, the narrowed production activation-topology guard, Android lint, JVM tests, debug APK assembly, Room schema validation, and committed schema-history drift enforcement.
- The unfiltered Android 16 / API 36 x86_64 connectedDebugAndroidTest suite reported Starting 20 tests, Finished 20 tests, and BUILD SUCCESSFUL.
- PR #19 was squash-merged with expected-head protection as 69259ee0fb822f43ee1e6bf7af629de8829faaa5. Authoritative main was read back at that same SHA and GitHub reports the commit as verified.
Privacy and security boundary:
No Android permission, INTERNET permission, network or cloud workspace dependency, advertising, sponsorship, analytics, attribution, tracking SDK, or installed-application inventory export was introduced. Workspace authority and recovery remain device-local; runtime test identifiers are synthetic.


Acceptance boundary:
This merge activates Room as terminal authority for the current Favorites/Dock compatibility workspace path. It accepts production coordinator wiring and persistence-client recreation on API 36, but does not claim true Android OS process-death survival, schema-version upgrade recovery, representative physical-device/default-HOME behavior, multi-page live cell/span placement, signing, or release acceptance.

## Historical Drive Section 49 — Milestone 1 Activated HOME-Role MainActivity Recreation Acceptance Record

August 22, 2026
Pull request: #20 — Validate activated Home activity recreation.
Development branch: milestone1-activated-home-lifecycle-recreation.
Base main commit: 69259ee0fb822f43ee1e6bf7af629de8829faaa5.
Accepted PR head: e28652996fb37d93eb101b90e6abf73b7272a9af.
Android CI run: 32594478982.
Main squash commit: 248198ec7640dcf7c246d0bf81f6082f837fc372.


Implemented lifecycle acceptance:
- Added ActivatedHomeLifecycleRuntimeTest against the real MainActivity and real production WorkspaceProductionRuntimeCoordinator path.
- The test acquires the actual Android HOME role through UiAutomation, verifies RoleManager reports the role held, and restores the emulator's prior HOME-role state after execution.
- The test seeds a real launchable application as the Favorite, waits for terminal WorkspaceAuthority.ROOM, and requires that Favorite to be rendered in the Home UI.
- ActivityScenario recreates MainActivity, after which the test again requires terminal ROOM authority and the same Room-backed Favorite to be rendered.
- A fresh WorkspaceProductionRuntimeCoordinator then performs a Room-authoritative Favorite mutation after recreation; the write must return Written, authority must remain ROOM, and the recreated Compose UI must reactively display the newly added Favorite.
- LauncherAppsRepository now registers LauncherApps callbacks through a Handler bound to Looper.getMainLooper(), removing dependence on the Flow collector thread having a prepared Looper.


CI troubleshooting and validation:
- Run 32591897289 exposed the LauncherApps callback Looper defect. Production callback dispatch was hardened instead of weakening the test.
- Run 32593868716 exposed invalid ActivityScenario launch/recreate/close calls from the Android main thread. The harness was corrected to follow AndroidX threading requirements.
- Run 32594156443 reached the real rendered-UI assertion and exposed that the CI emulator was not actually exercising the default-HOME path. The test was strengthened to acquire the actual HOME role rather than relaxing assertIsDisplayed.
- Final exact PR head e28652996fb37d93eb101b90e6abf73b7272a9af passed Android CI run 32594478982. The full validate job passed, and the unfiltered Android 16 / API 36 x86_64 connectedDebugAndroidTest log reports Starting 21 tests, Finished 21 tests, and BUILD SUCCESSFUL.
- PR #20 was squash-merged with expected-head protection as 248198ec7640dcf7c246d0bf81f6082f837fc372.


Privacy and security boundary:
No Android requested permission, INTERNET permission, network or cloud workspace dependency, advertising, sponsorship, analytics, attribution, tracking SDK, or installed-application inventory export was introduced. HOME-role manipulation exists only in instrumentation acceptance and is restored after the test.


Acceptance boundary:
This accepts actual HOME-role MainActivity activity recreation and post-recreation Room-authoritative reactive placement behavior on API 36. It does not establish true Android OS process-death/cold-start survival. Schema-version upgrade recovery, representative physical-device/default-HOME acceptance, multi-page50. Milestone 1 Workspace Grid Placement Validation Foundation Record
August 24, 2026
Pull request: #21 — Add workspace grid placement foundation.
Development branch: milestone1-grid-placement-foundation.
Accepted PR head: 226639d1a09b3913af298299ecd1db2dd955a6d5.
Android CI run: 32779946533.
Main squash commit: ec75da552a16c6715b9c21a8251b65d0b7bb7fdf.
Implemented foundation: Added a pure Kotlin WorkspaceGridPlacement contract that validates positive grid dimensions, nonnegative cell coordinates, positive spans, duplicate item identities, out-of-bounds placements, and overlap/collision across multi-cell spans. Added JVM coverage for valid mixed application/widget-style placements, bounds rejection, collision rejection, and duplicate identity.
CI troubleshooting and validation: The first PR #21 run failed because the new test used kotlin.test while the Android launcher project uses JUnit. I corrected the test harness without weakening product validation. Final exact head 226639d1a09b3913af298299ecd1db2dd955a6d5 passed the validate job, including Privacy Shield, HOME-manifest, Glaze UI, Room-cutover, Android lint, JVM tests, debug assembly, Room schema, and schema-drift checks. The dependent Android 16 / API 36 room-runtime-emulator job also passed. PR #21 was then squash-merged with expected-head protection as ec75da552a16c6715b9c21a8251b65d0b7bb7fdf.
Privacy and security boundary: No Android permission, INTERNET permission, network dependency, telemetry, analytics, advertising, sponsorship, tracking SDK, or remote workspace service was added.
Acceptance boundary: This merge accepts a pre-persistence grid-placement validation primitive only. It does not change the live Room schema, persist cellX/cellY/spanX/spanY through the active Home path, add multiple rendered Home pages, activate widget placement, or establish true Android OS process-death, schema-upgrade, representative physical-device/default-HOME, signing, release, or Stable acceptance.


 live cell/span placement, signing, and release acceptance remain separate gates.


.


Superseding Native-Build and Platform Integration Mandate
This specification is governed by the platform-wide requirement that this application be built natively from the ground up as original GoreeCloud-owned software. Earlier maintained-fork or upstream-product implementation language is transitional only. Narrow critical foundations may be retained only when independently replacing them would materially increase security, cryptographic, protocol, standards, codec, rendering, operating-system, runtime, or interoperability risk; WireGuard and mature cryptographic or encryption primitives are canonical examples. Such exceptions must remain limited to the minimum technical foundation and must not preserve upstream product architecture, UI, branding, workflows, or general application logic.


This application must remain current with the latest applicable Stable Glaze UI contract and the latest approved Wardveil Security, Privacy Shield, and Everkeep contracts. All four are mandatory. Missing, incomplete, superseded, outdated, unverified, or unaccepted integration with any required platform system blocks Stable qualification.

## Historical Drive Section 55 — Superseded — Former GoreeCloud Index Universal Search Authority

This former clarification is superseded by Section 64. It is retained only as historical planning context because it records the earlier design in which GoreeCloud Index was assigned universal-search authority. Section 64 restores and expands Launcher-owned Universal Search and is the controlling current product architecture.
Historical-only boundary: The remaining paragraphs in Section 55 describe the superseded Index-owned model and must not be used as current architectural authority, implementation direction, or a blocker for Launcher core Universal Search.


Authority model
GoreeCloud Launcher is a primary first-party Android invocation and presentation surface for GoreeCloud Index. Launcher owns Home interaction, gesture detection, launcher-specific navigation, and the contextual information it explicitly exposes. GoreeCloud Index owns the universal query lifecycle, provider discovery, authorization-aware dispatch, local indexing where required, result normalization, source provenance, ranking, grouping, deduplication, and universal result actions.


The user-facing Launcher search experience may be described as Launcher Unified Search when referring to the Launcher entry point, but that surface is powered by GoreeCloud Index and must not maintain a rival hidden universal index or independent cross-provider ranking authority.


Launcher invocation requirements
The approved one-finger downward swipe on an unobstructed Home area should invoke GoreeCloud Index. Launcher should also provide a visible Search GoreeCloud affordance and accessible non-gesture alternatives. Hardware-keyboard, switch-access, screen-reader, and other supported input paths may invoke the same Index surface or contract.


The application drawer may retain its narrow local Search apps filter for navigating Launcher-owned application inventory. That drawer filter is not GoreeCloud Index, does not establish universal search authority, and must not silently expand into contacts, files, calendar, media, third-party, connected-device, or Web search outside the Index provider pipeline.


Provider and source boundaries
Launcher may expose installed applications, shortcuts, launcher actions, settings, folders, widgets, or other Launcher-owned contextual records to Index through a versioned provider contract. Index must not bypass that contract by directly reading Launcher private persistence. Source applications and services remain authoritative for their own data even when Index makes those resources searchable.


Contacts, calendar, files, media, GoreeCloud application content, Drive content, connected-device resources, extensions, and optional third-party services belong to permission-aware Index providers. Android scoped-storage, document-provider, media, profile, and package-visibility boundaries remain mandatory. Universal search does not justify broad filesystem or package access.


GoreeCloud Search relationship
GoreeCloud Search remains the first-party Internet, Web, and current-information search provider. Launcher reaches Web/current-information results through GoreeCloud Index, which delegates the applicable query to GoreeCloud Search only when authorized and enabled. GoreeCloud Search does not become the authority for local Launcher state, and local provider payloads must not be uploaded merely to produce local results.


Failure and offline behavior
If GoreeCloud Index is unavailable, Launcher must report the unavailable state or another separately approved degraded behavior rather than silently substituting a second universal search engine. Core Launcher Home and application navigation remain independently usable offline. Index local providers should remain usable offline where their own authorities permit it, while remote providers fail independently.


Development implementation checkpoint — August 31, 2026
The clarified authority boundary is now implemented in Development source. GoreeCloud Index main is currently cc3cc21d6e11dad026253c3371c3b67663d3b726 after its asynchronous provider-runtime and documentation-reconciliation work. The accepted Index runtime retains Applications · On-device as the only enabled provider, with provider-neutral query/result/action contracts, structured asynchronous execution, bounded provider timeouts, failure isolation, cancellation propagation, deterministic ranking/deduplication, and the explicit com.goreecloud.index.action.SEARCH entry contract. GoreeCloud Launcher PR #53 exact head b18cfa05a1b18243e52046ef581cb67fc3298a5f passed Android CI run 33417830081 including Android 16 runtime validation and merged as fde148081cc292bcdfd7e221312fe830515331fb, adding the visible Search GoreeCloud Home affordance, one-finger downward invocation, bounded Index-action package visibility, and production/Development package handoff support.


This establishes bounded Development implementation evidence, not complete universal-search acceptance. The current Index execution context is an internal eligibility guard and is not accepted Privacy Shield consent/permission logic or GoreeCloud Identity authorization. Files, contacts, calendar, media, Drive, connected-device resources, extensions, third-party providers, GoreeCloud Search Web/current-information runtime integration, complete platform-system authorization/trust integration, complete current Stable Glaze UI conformance, representative physical-device/accessibility acceptance, signed production release acceptance, and Stable qualification remain separate gates.

## Historical Drive Section 57 — Current Development Implementation — Home Layout Lock and GoreeCloud Index Home Entry Modes

This section records the accepted Development implementation of the Section 56.3 Index Home-entry mode and the currently implemented portion of Section 56.5 Home layout lock. It does not convert the remaining Theme Manager, icon-pack, backup/restore, future-item lock coverage, or release-scope requirements into implementation claims.


Source-control and validation history


The implementation was first prepared as draft Launcher PR #56 on branch agent/layout-lock-index-entry-mode at exact head 64b13b67f31a700f4a3583072243d818eb8cec02 against base d845803e0a7af88c8394602a5545c44193ed7ad7. Android CI run 33422522732 passed both the complete validate job and the Android 16 / API 36 room-runtime-emulator job on that exact head.


The connected GitHub interface then failed to transition the draft PR to ready-for-review because its GraphQL mutation encountered an incompatible repository field. GitHub itself correctly refused to merge the PR while it remained draft. No source-control, review, or CI gate was bypassed. PR #56 was closed as superseded and a non-draft replacement PR #57 was opened from the identical branch, head, and base.


Replacement PR #57 triggered a fresh exact-head Android CI run 33433746672. Both validate and room-runtime-emulator passed again on exact head 64b13b67f31a700f4a3583072243d818eb8cec02. PR #57 was then squash-merged with expected-head protection to authoritative main as eca26aa28e1ec8dd18efd1fb5f3cf914be33a361. Main was read back at that exact verified SHA. Push-triggered Android CI run 33434363744 then passed both validate and the Android 16 / API 36 room-runtime-emulator job on the exact merge commit.


Current merged behavior


- Launcher locally persists Home layout-lock state in Launcher DataStore.
- Launcher locally persists GoreeCloud Index Home entry mode with Permanent on Home as the compatibility-preserving default and Swipe down only as the alternate mode.
- The one-finger downward Home invocation of GoreeCloud Index remains available in both modes; only the persistent Search GoreeCloud Home affordance is hidden in Swipe down only mode.
- While layout is locked, current Favorite/Dock membership and ordering changes, Home-page create/delete/reorder operations, secondary-to-secondary application moves, and current secondary spatial movement paths are blocked.
- Primary and secondary placement-management controls and page mutation controls visibly reflect the locked state instead of merely allowing a request that later fails.
- Normal application launching, Home page selection, Apps/Settings navigation, non-placement presentation settings, and GoreeCloud Index invocation remain usable while locked.
- Launcher Settings remains the deterministic accessible unlock path. The Home surface additionally exposes an intentional five-second hold-to-unlock control with progressive feedback and cancellation when the hold ends early.


Authority, privacy, and acceptance boundary


The layout lock is a Launcher-owned mutation policy layered over the existing Room-authoritative workspace paths; it does not create a second workspace authority. GoreeCloud Index remains the universal query/provider/index/normalization/ranking authority; the Home-entry preference controls Launcher presentation/invocation only. No Android permission, INTERNET permission, network behavior, Room schema change, analytics, advertising, sponsorship, attribution, tracking SDK, or new remote dependency was introduced.


This Development acceptance does not establish lock coverage for future folders, shortcuts, widgets, or other placeable item types that do not yet exist in the current editing surface; representative physical-device or assistive-technology acceptance of the five-second hold; icon-pack/masking support; versioned Launcher backup/restore; complete Glaze UI 2.2 application acceptance; signed release packaging; production release acceptance; or Stable qualification. The native System/Light/Dark Theme Manager route now exists in Draft PR #62 Development source but retains separate representative-device and accessibility acceptance gates.


August 31, 2026 Development Continuation — Theme Manager Settings Navigation


The Launcher main branch already includes the merged Glaze UI 2.1 migration from PR #59, including the native `ThemeManagerSurface`, while the current Settings UI still uses its earlier direct appearance-cycle behavior. GoreeCloud Launcher now has Draft PR #60, `Add Theme Manager settings navigation model`, on branch `agent/theme-manager-settings-navigation` from exact main `087a4ff54a0b5d9bdd177c27e954bf116ee557c0`.


`LauncherSettingsNavigation.kt` adds a deterministic, saveable Settings sub-destination model for Settings root and Theme Manager. Unknown persisted destination values fail closed to Settings root, `openThemeManager()` enters the Theme Manager destination, and back navigation returns to Settings root. `LauncherSettingsNavigationTest.kt` verifies restoration, fail-closed decoding, and open/back behavior. The navigation layer owns presentation destination only; it does not change `GlazeThemeRepository`, theme persistence, HOME role, workspace placement, icon packs, wallpaper authority, Privacy Shield, or any broader Glaze UI acceptance claim. Exact head `3283bfde178e48652fda37173b87692bc17d7e68` completed Android CI run 180 successfully. The next rendered slice can replace the Settings appearance-cycle button with an entry into the existing `ThemeManagerSurface` and persist that sub-destination cleanly.


Development continuation — rendered Theme Manager destination host
GoreeCloud Launcher Draft PR #61 adds LauncherSettingsDestinationHost on top of the validated saveable Settings navigation model. ROOT renders the existing root Settings content and THEME_MANAGER renders the existing first-party ThemeManagerSurface; theme persistence remains with the caller/GlazeThemeRepository and the host gains no workspace or launcher-role authority. The complete exact-head Android CI succeeds, including privacy, manifest, Identity, Glaze UI/Motion, Room cutover/schema, lint, unit tests, debug assembly, and the Android 16 Room runtime emulator suite. LauncherBetaRoot still needs the bounded follow-up that replaces its direct Appearance theme-cycle action with saveable destination state. Status remains Development.


Development continuation — Non-Actionable Selected Theme State — September 2, 2026


Draft PR #62 on `agent/theme-manager-settings-composition` now keeps the already-selected System/Light/Dark appearance as a non-actionable `Selected` status surface rather than another persistence button. Actual Done and alternate-theme actions retain `GlazeMetrics.touchAssistanceTarget`, while only a different appearance choice may invoke the caller-owned `onSelectThemeMode` persistence path. The saveable Settings destination model and root Theme Manager composition remain unchanged in authority. Exact head `ca4e31597f9d12204d7de0e708905210e42354e3` passed Android CI #187 / run `33596732574`.


This remains Development source/build evidence. It adds no launcher-role, Home/Apps navigation, workspace placement, wallpaper, icon-pack, account, or system-setting authority, and it does not introduce additional theme modes. Representative-device navigation/persistence/accessibility testing, complete current Stable Glaze UI acceptance, release, and Stable qualification remain separate gates.


Glaze UI 2.2.0 Theme Manager Adoption Candidate — Current Development State


Current validated source: GoreeCloud/android-app-defaults (`apps/launcher/`) Draft PR #62, exact head `ce9ed24678112aa931576f4072967458ae0f6260`. The active repository-local design authority is Glaze UI 2.2.0 Stable, anchored to reviewed Stable promotion head `fb5ecde4a8258503789ffde08ac46a2e524ef71e` and Stable release revision `6731098b28dd0393faa878c70d989a221d714a20`. Earlier Glaze UI 2.1 and older records remain historical migration/rollback evidence and do not define current conformance.


The current Theme Manager composition preserves the validated Settings destination model and caller-owned GlazeThemeRepository persistence. System, Light, and Dark are the only implemented appearance choices in this slice. The already-selected appearance is presented as non-actionable status, preventing redundant persistence calls. Alternate appearance choices and Done retain a 56 dp minimum action floor. The native Glaze contract records the compatible 48 dp normal interaction floor, 56 dp Touch Assistance floor, current 2.2 geometry/token subset, and ordinary System Glaze composition budget. The retained 56 dp Theme Manager controls are a conservative accessible implementation choice; platform Touch Assistance detection is not yet claimed.


Exact-head validation: Android CI #191 / run `33599342201` exposed only a new unit-test framework mismatch (`kotlin.test` imports on a JUnit-configured Android test classpath); product source compiled and repository governance guards passed. The test was repaired to the repository's established JUnit convention. Exact head `ce9ed24678112aa931576f4072967458ae0f6260` passed Android CI #192 / run `33599650851`.


Authority boundary: this Theme Manager work does not add or transfer Android HOME-role authority, Home/Apps routing authority, workspace mutation authority, wallpaper authority, icon-pack authority, account authority, GoreeCloud Index authority, Control Center authority, or system-setting authority. Glaze UI remains presentation and interaction authority rather than Launcher state authority.


Acceptance classification: Glaze UI 2.2.0 Adoption Candidate / Development. Source and CI evidence are not full product acceptance. Representative-device Theme Manager navigation and persistence, screen-reader/assistive-technology behavior, rendered/native 2.2 acceptance, required Wardveil Security / Privacy Shield / Everkeep integration acceptance, signed release packaging, production release acceptance, and Stable qualification remain pending.
Development continuation — Theme Manager Accessibility Semantics — September 2, 2026
Draft PR #62 advanced to exact head `a87f5b1bf20ce8005ca589bd4a38efa8440e7500`. Each visual System/Light/Dark theme preview is now exposed as one concise accessibility semantics node derived from `GlazeThemeChoice` metadata, while decorative preview internals are cleared from the accessibility tree. The non-actionable selected appearance now exposes an explicit state description such as `Dark appearance selected` and a polite live region, preserving the earlier rule that the current appearance is status rather than another persistence action. Unit coverage locks the preview and selected-state accessibility metadata.
Exact-head validation: Android CI #196 / run `33611126346` passed repository privacy/manifest/identity/Glaze/Room governance checks, Android lint, JVM tests, debug assembly, Room schema validation, and the Android 16 Room runtime emulator job. This establishes source/CI semantics evidence only. TalkBack, Switch Access, focus order, spoken-announcement timing, representative-device navigation/persistence/accessibility, Reduced Motion, Reduced Transparency, Increased Contrast/native accessibility-equivalent handling, 200% text/reflow, RTL/localization, complete Glaze UI 2.2 application acceptance, required Wardveil Security / Privacy Shield / Everkeep acceptance, release, and Stable qualification remain separate gates.


September 2, 2026 — Glaze UI 2.2 Authority Reconciliation and Documentation-Complete Theme Manager Candidate — Development


Central exact-source verification exposed a real split in the earlier PR #62 state: the native Glaze metrics, Theme Manager implementation/tests, and later Development records were already on Glaze UI 2.2, while the authoritative repository adoption document and fail-closed Glaze/Motion guards still declared 2.1. Launcher was therefore correctly treated as migration-required until the exact downstream source was reconciled rather than being promoted from a registry-only version claim.


The authoritative repository-local adoption contract, `scripts/check_glaze_ui.py`, and the retained Experimental/test-only Glaze Motion production-authority guard were reconciled to Glaze UI 2.2.0 Stable. The reviewed Stable anchors remain promotion head `fb5ecde4a8258503789ffde08ac46a2e524ef71e`, release revision `6731098b28dd0393faa878c70d989a221d714a20`, and tag `v2.2.0`. The repair checkpoint `864e65c5a6d53e9dbb38e8c2c1665f869f3b0c0f` passed Android CI #197 / run `33669458048`, including validation/build/schema and Android 16 / API 36 Room runtime-emulator jobs.


Repository-control review then corrected stale present-tense README and SPECIFICATIONS wording that still called 2.1 current and still described Theme Manager as unwired. Historical 2.1 records were preserved. The resulting documentation-complete Draft PR #62 exact head is `4043895afd26991d0a20461e94d002f91c890703`.


Android CI #199 / run `33670347744` passed on that exact head. The validate path passed Privacy Shield, HOME-manifest, identity, Glaze UI 2.2 Adoption Candidate, retained Experimental Motion quarantine/authority, Room-cutover, Android lint, JVM tests, debug APK assembly, Room schema, and generated-schema cleanliness gates. The dependent Android 16 / API 36 Room transition runtime-emulator job also passed.


The Theme Manager remains bounded to System, Light, and Dark. The selected appearance is non-actionable status; only a different appearance may invoke caller-owned persistence. Accessibility preview/selected-state semantics remain source-validated. This work adds no launcher-role, Home/Apps navigation, workspace placement, wallpaper, icon-pack, account, Control Center, GoreeCloud Index, notification, authentication, or system-setting authority.


Classification remains Glaze UI 2.2.0 Adoption Candidate / Development with production eligibility false. Complete rendered/native Glaze UI 2.2 product acceptance, Deep Dark where applicable, platform Touch Assistance preference resolution, Reduced Motion, Reduced Transparency/Solid behavior, Increased Contrast/native equivalents, 200% text/reflow, RTL/localization, TalkBack/Switch Access, keyboard/D-pad focus, representative phone/tablet/foldable composition, representative physical-device Theme Manager navigation/persistence/accessibility, required current Wardveil Security / Privacy Shield / Everkeep acceptance, signing/distribution, release, production acceptance, and Stable qualification remain separate gates.

## Historical Drive Section 58 — Development Workspace Portability Snapshot Candidate

GoreeCloud Launcher Draft PR #64 (`agent/workspace-portable-snapshot`) adds the first versioned repository-local portability boundary for the framework-independent HOME workspace grid/page/placement model. The exact candidate head is `5fb72cfab3fc3770e578c9ad3b79ba76d4dc0bcd`. Platform Contract #2 and Android CI #201 both passed on that exact head.


The format is `goreecloud-launcher-workspace-snapshot/1`. Its current scope is deliberately limited to grid dimensions, ordered HOME page identities/ranks, opaque workspace-item identities, cell coordinates, and spans. Encoding is deterministic and bounded, uses canonical UTF-8/LF representation and Base64URL identity tokens, and carries a SHA-256 integrity checksum. Decoding reuses the existing `WorkspacePagedPlacement` and `WorkspaceGridPlacement` validation contracts so malformed records, duplicate pages/ranks/items, unknown page references, collisions, out-of-bounds placement, noncanonical values, integrity failures, and unsupported format/version data fail closed.


This candidate does not write Room or DataStore state and therefore is not a restore path. It does not bind widgets, resolve Android packages or profiles, or mutate the active HOME runtime. The current snapshot also does not establish complete portable coverage for Dock configuration, folders and folder membership, shortcuts, widgets/AppWidget identifiers or rebinding, Theme Manager and icon-presentation state, gestures, hidden applications, search settings, layout-lock state, Index Home entry mode, or other supported Launcher preferences required by Sections 16 and 56.6.


The machine-readable GoreeCloud Platform Contract continues to classify Launcher as Development and not Stable-eligible. Backup and restore remain required but missing; product-wide export/import remains pending; Everkeep integration remains nonconformant; and complete Glaze UI 2.2 application acceptance, GoreeCloud Identity where applicable, Wardveil Security, Privacy Shield, GoreeCloud Mesh registration, representative-device/default-HOME acceptance, signing, release acceptance, and Stable qualification remain separate gates. The snapshot is Development portability evidence only and must not be described as complete Launcher backup or recovery.

## Historical Drive Section 59 — Development Launcher Preference Portability Snapshot Candidate

GoreeCloud Launcher Draft PR #65 (`agent/portable-launcher-preferences`) adds a second bounded portability format on top of the validated workspace-snapshot Development line. The exact candidate head is `5bce937f17f9d60834eed974b3866d7113f4c9ec`. Platform Contract #3 / run `33713336562` and Android CI #202 / run `33713336563` both passed on that exact source revision.


The new format is `goreecloud-launcher-preferences/1`. Version 1 contains exactly the seven explicit values currently represented by `LauncherPreferences`: Home grid columns, Home grid rows, App Drawer columns, app-label visibility, icon scale, layout-lock state, and GoreeCloud Index Home mode. Icon scale is serialized as integer thousandths to avoid ambiguous floating-point text. The codec uses a bounded canonical UTF-8/LF representation, exact record ordering, SHA-256 integrity checking, and strict range/enum/boolean/integer validation.


Portable decoding deliberately does not call `LauncherPreferences.sanitized()` to clamp incompatible external data into supported values. Out-of-range or noncanonical portable state fails closed instead of silently changing the user's incoming preference. The codec is an in-memory transformation only and does not read or write Android DataStore, Room workspace state, packages, profiles, widgets, Theme Manager state, wallpaper, or the active HOME runtime.


`goreecloud-launcher-workspace-snapshot/1` and `goreecloud-launcher-preferences/1` remain separate partial Development formats. Together they increase covered state but do not constitute a complete Launcher backup. Dock/favorites compatibility state, folders and folder membership, shortcut-specific state, widgets/AppWidget identifiers and safe rebinding, broader theme/icon presentation state, gesture configuration outside the current preference model, hidden-application state, broader search settings, Android package/profile rebinding semantics, and other future durable state remain outside the accepted portable/recovery scope.


The machine-readable Platform Contract therefore continues to classify Launcher as Development and not Stable-eligible. Backup, restore, product-wide export/import, Everkeep integration, GoreeCloud Mesh registration, applicable GoreeCloud Identity integration, Wardveil Security, Privacy Shield, complete Glaze UI 2.2 application/accessibility/device acceptance, default-HOME and representative-device acceptance, signing, release evidence, deployment, and Stable qualification remain separate pending gates. Exact-head CI proves the bounded codec source/test candidate only and does not itself establish those broader acceptance states.

## Historical Drive Section 60 — Development Launcher Atomic Preference Import Candidate

GoreeCloud Launcher Draft PR #66 (`agent/portable-launcher-preference-import`) adds the bounded local apply side of `goreecloud-launcher-preferences/1` on top of the exact-head validated preference codec. Exact candidate head `48ee8d5a6423218c9db18fad425ed44346b06165` passed Android CI #203 / run `33725792082`.


`LauncherPortablePreferenceImport.apply(...)` validates and decodes the complete portable snapshot before any persistence boundary is invoked. Tampered, malformed, expanded, unsupported, or out-of-range input therefore produces zero writer calls. A valid snapshot supplies exactly one complete `LauncherPreferences` value to the new minimal writer boundary, and persistence failures propagate instead of being reported as a successful import.


`LauncherPreferencesRepository` implements that writer by reusing the strict portable codec as a defensive validation authority and then committing all seven v1 values—Home columns, Home rows, App Drawer columns, app-label visibility, icon scale, layout-lock state, and GoreeCloud Index Home mode—inside one Android DataStore `edit` transaction. The import path does not route through `LauncherPreferences.sanitized()` and therefore does not silently clamp incompatible portable values.


This is real but partial local preference-import authority only. It does not mutate Room workspace/page/placement state, Dock/Favorites compatibility state, folders, shortcuts, widgets or widget-provider bindings, packages/profiles, Theme Manager or wallpaper state, gesture/hidden-app/search state outside the approved v1 preference subset, GoreeCloud Identity state, or Everkeep metadata. It provides no user-facing file/import workflow, artifact provenance or ownership proof, conflict policy, multi-format transaction, workspace restore, cross-d

## Historical Drive Section 61 — Development GLAZE UI V1.0 and Platform Contract v0.1 Candidate — September 3, 2026

GoreeCloud Launcher Draft PR #67 (`agent/glaze-ui-v1-adoption`) is the current repository-local design-system and Platform Contract continuation on top of the existing portability stack. Its exact candidate head `39a87e335e93fa1153ed6aa75415cd4e0dce584d` passed Platform Contract #9 / run `33790596431` and Android CI #209 / run `33790595752`.


The V1 source mapping pins GLAZE UI V1.0 (`1.0.0`) to exact canonical source revision `70909bbdccad378fb7281ae1842e2f5beed64c38`. Current native source maps the consumed Launcher geometry to V1 12/20/28 dp foundation radius tiers, preserves the 48 dp normal and 56 dp Touch Assistance / far-view interaction floors, maps the active Light/Dark Compose theme to the reset V1 foundation palette, and preserves the bounded native Settings → Theme Manager composition. Deep Dark and complete V1 component/state/material-role, Reduced Motion/Transparency, Increased Contrast/native accessibility-equivalent, 200% text/reflow, RTL/localization, phone/tablet/foldable, TalkBack/Switch Access, representative-device, Human Visual Excellence, signed-release, and production acceptance remain incomplete.


The repository machine-readable declaration is now canonical GoreeCloud Platform Contract v0.1 rather than the superseded repository-local schema-1.0 generation. It records Glaze UI as `partial` at `1.0.0` and overall conformance as `nonconformant`. Manager, GoreeCloud Identity where applicable, Wardveil Security, Privacy Shield, Everkeep, GoreeCloud Mesh registration, complete recovery, product-wide portability, release, production, and Stable qualification remain independently blocked.


The existing `goreecloud-launcher-workspace-snapshot/1` and `goreecloud-launcher-preferences/1` formats remain bounded Development portability subsets. The current workspace format covers framework-independent HOME grid/page/placement state only; the preference format covers the seven explicit Launcher preference values already documented above. The atomic preference import applies only that bounded preference record after full validation. These formats do not establish complete Launcher backup/restore, Everkeep acceptance, Room workspace restore, Dock/Favorites completeness, folders/shortcuts/widgets and AppWidget rebinding, Android package/profile rebinding, Theme Manager/icon presentation portability, cross-device synchronization, or a user-facing product-wide import/export workflow.


Pre-reset Glaze UI 2.x and earlier records, together with the superseded Platform Contract schema-1.0 adoption records, remain historical Development evidence and implementation ancestry only. They do not define the current design-system target or current Platform Contract generation. Platform Contract v0.1 validation and green Android CI establish exact-head Development source/build/test evidence; they do not authorize production deployment, release approval, or Stable status.
evice synchronization, complete backup/restore acceptance, Everkeep acceptance, deployment, release, or Stable qualification. Launcher remains Development and not Stable-eligible.


Development continuation — Portable Restore Coordination and Platform Contract v0.2 — September 3, 2026
GoreeCloud Launcher Draft PR #68 (`agent/portable-restore-coordinator`) adds a validation-before-write coordinator for the two currently approved bounded portability formats: `goreecloud-launcher-workspace-snapshot/1` and `goreecloud-launcher-preferences/1`. `LauncherPortableRestoreImport.apply(...)` fully validates both encoded inputs before invoking the deliberately narrow `LauncherPortableRestoreWriter`, whose only persistence method receives one validated workspace snapshot plus one validated `LauncherPreferences` value. Tests require one combined write only when both inputs are valid, zero writes when either input is invalid, and propagation of persistence failures. Exact head `945dd8d60d28ebfed6287470a35f25a7a79bd407` passed Platform Contract #11 / run `33828872625` and Android CI #211 / run `33828872367`.
This remains a Development persistence seam rather than complete Launcher recovery. No concrete Room/DataStore restore writer is added by this coordinator, and it does not discover or remap packages/profiles, restore folders/dock/widgets, rebind AppWidget IDs/providers, restore hidden-app state, wallpaper, icon packs, Theme Manager state, or system settings, synchronize across devices, establish artifact provenance, or grant Everkeep recovery authority.
Draft PR #69 (`agent/platform-contract-v0.2`) is stacked on that exact feature head and migrates the machine-readable declaration to authoritative GoreeCloud Platform Contract v0.2 using immutable reusable validator revision `8779763fb6c0ee51ff26669205e04fbd7c8b3db2`. The current Stable design-system requirement is now GLAZE UI V1.1 / `1.1.0`; the existing V1.0 implementation evidence remains valid only as historical Development source evidence and is explicitly migration-required. A stale repository-local Glaze guard that still asserted v0.1 / 1.0.0 as the current manifest target was updated to test the truthful split between existing V1.0 source and required V1.1 migration, without changing runtime or UI token behavior. Exact head `a5d1c3a2b9008ba9d1de70b665683c1ac8464eab` passed Platform Contract #13 / run `33831383461` and Android CI #213 / run `33831383140`.
Launcher remains lifecycle Development and overall platform conformance remains `nonconformant`. The v0.2 migration does not establish GLAZE UI V1.1 rendered/application acceptance, GoreeCloud Identity, Wardveil Security, Privacy Shield, Everkeep, GoreeCloud Mesh, Manager visibility, complete backup/restore, package/profile/widget rebinding, deployment, signed release, production acceptance, or Stable qualification. Earlier Platform Contract v0.1 and GLAZE UI V1.0 records remain historical checkpoints rather than current conformance evidence.

## Historical Drive Section 62 — Development GLAZE UI V1.1 Source Mapping Candidate — September 3, 2026

GoreeCloud Launcher Draft PR #70 (`agent/glaze-ui-v1.1-adoption`) advances the validated Platform Contract v0.2 line to a repository-local current Stable GLAZE UI V1.1 / `1.1.0` source-mapping candidate. Exact head `9ca0823dd9682e1299d296164a1bd8abc474d734` passed Platform Contract #14 / run `33834126021` and Android CI #214 / run `33834125628`. The PR remains Draft and unmerged.


The candidate pins GLAZE UI V1.1 release commit `15cc76d2bcd4065552dc31c77145b63f34d9e7b2`. It preserves inherited V1 spacing, 12/20/28 dp structural radius tiers, 48 dp normal and 56 dp Touch Assistance interaction floors, and Light/Dark structural appearance values. V1.1-specific source adds the separate 8/16/24/32 dp optical geometry references plus capsule, the exact Deep Dark structural appearance, and four persisted Launcher Theme Manager choices: System, Light, Dark, and Deep Dark. System continues to follow Android's binary light/dark state; Deep Dark is an explicit Launcher choice rather than an inferred system state.


Deep Teal + Soft Amber atmospheric primitives are held in a separate non-semantic `GlazeAtmosphere` authority. Current rendered use is deliberately limited to a small static decorative Soft Amber counter-light in the Theme Manager preview. The candidate adds no Environmental Color Memory, content sampling, remote color derivation, persistent sample history, semantic inference, or animated atmosphere. Protected producer semantics and accessibility resolution remain higher authority than atmosphere, and removing atmosphere does not remove meaning, actions, focus, semantic state, or hierarchy.


The Platform Contract v0.2 declaration now records `platform_systems.glaze_ui.version: "1.1.0"` while retaining `result: applicable-migration-required` and overall `conformance.status: nonconformant`. Exact-head CI establishes Development source/build/test evidence only. Complete rendered/native application acceptance, TalkBack and Switch Access, keyboard/D-pad focus, 200% text/reflow, RTL/localization, Reduced Motion, Reduced Transparency/solid fallback behavior, Increased Contrast/native equivalents, adaptive phone/tablet/foldable composition, representative physical-device Theme Manager navigation/persistence, Human Visual Excellence review, required Identity/Wardveil Security/Privacy Shield/Everkeep/Mesh/Manager acceptance, signing, release, production acceptance, and Stable qualification remain separate gates.
Development continuation — Transactional Portable Restore Persistence — September 5, 2026
Draft PR #72 (`agent/portable-restore-apply`) now contains a concrete bounded persistence implementation for the currently approved `goreecloud-launcher-workspace-snapshot/1` plus `goreecloud-launcher-preferences/1` restore pair. `LauncherTransactionalPortableRestoreWriter` performs the supported HOME placement replacement through one Room transaction, writes the seven portable Launcher preferences through DataStore, verifies readback, and performs guarded compensating rollback when the cross-store preference stage fails. The writer preserves non-HOME state and fails closed unless the imported opaque item identities exactly match the current resolved HOME APP identity set; it does not invent package/profile/folder/shortcut/widget bindings or claim clean-device reconstruction.


The repository-local `docs/development/portable-restore-coordinator.md` and `goreecloud.platform.yaml` were reconciled so they no longer incorrectly state that no concrete writer exists. The Platform Contract continues to classify recovery as partial/nonconformant because Room and Preferences DataStore do not share one crash-atomic transaction, the current portable formats do not carry the Android identity metadata needed for safe cross-device rebinding, and complete Launcher-owned recovery scope plus accepted Everkeep/Privacy Shield/Wardveil integration remain outstanding.


Exact head `1b75ea7666063bbe1820d6ca6a90be00e9621664` passed Platform Contract #36 / run `33944396333` and Android CI #237 / run `33944395229`, including repository privacy/manifest/Identity/GLAZE UI V1.1/Motion/Room guards, Android lint, JVM unit tests, debug APK assembly, Room schema validation/cleanliness, and the Android 16 / API 36 Room runtime-emulator suite. This is Development recovery evidence only. Clean-target package/profile/folder/shortcut/widget reconstruction, crash/interruption recovery across stores, broader backup scope, complete GLAZE UI V1.1 application acceptance, representative physical-device/default-HOME recovery, accessibility, signing, production release acceptance, and Stable qualification remain separate gates.


Development continuation — Cancellation-Safe Portable Restore Compensation — September 5, 2026
Draft PR #72 (`agent/portable-restore-apply`) advanced to exact head `b13af833f58d9796b98f18f5d8603d24625fcfdc`. When coroutine cancellation interrupts the bounded Room-plus-DataStore portable restore apply path, compensating rollback now executes under `NonCancellable` so cancellation does not itself abandon the guarded rollback. If compensation verifies successfully, the original cancellation is rethrown rather than converted into an ordinary restore failure; existing concurrent-state guards still refuse rollback when current state no longer matches the restore-written state.


Exact-head Platform Contract run `33948424558` and Android CI run `33948424340` both succeeded. Android CI included policy and contract guards, lint, JVM unit tests including the cancellation-compensation regression, debug APK assembly, Room schema validation and cleanliness, and Android 16 / API 36 Room runtime-emulator acceptance.


This remains Development recovery evidence for same-process cooperative cancellation only. It does not create cross-store crash atomicity, process-death recovery, clean-target package/profile/folder/shortcut/widget reconstruction, complete Launcher backup scope, accepted Everkeep/Privacy Shield/Wardveil/Identity/Mesh/Manager integration, corrected immutable current-Stable GLAZE UI V1.1 re-pin and complete application acceptance, representative physical-device/default-HOME recovery acceptance, accessibility acceptance, protected signing/provenance, Release Candidate promotion, production approval, or Stable qualification.

## Historical Drive Section 65 — Repository-Native Feature and Changelog Governance — September 22, 2026

65.1 Current Authority
GoreeCloud Launcher feature state and change history are now maintained in the authoritative `GoreeCloud/android-app-defaults` repository under `apps/launcher/`. IMPLEMENTED-FEATURES.md is the implemented-capability inventory, PLANNED-FEATURES.md carries open, partial, blocked, deferred, and future obligations, and CHANGELOGS.md is the repository-local human-readable change history. This Project Specification remains the authoritative product/technical requirements record; it does not duplicate those repository-native feature/changelog records as a parallel authority.
65.2 Migration Verification
PR #201 established the repository-native records and retired root FEATURE-ROADMAP.md after migration comparison. PR #203 corrected the post-migration authority wording, and PR #204 recorded verified retirement of the legacy Launcher Drive roadmap/changelog sources. That migration remains complete. The earlier September 23, 2026 governance-migration checkpoint verified main 54c7bccb4b1b5865a123af34b9cbc46e538e3f51 from documentation reconciliation PR #239, with latest source-bearing runtime 426e466f62d8347de720258be8492e518e84c4d5 from PR #238. Direct GitHub readback verifies IMPLEMENTED-FEATURES.md, PLANNED-FEATURES.md, and CHANGELOGS.md on main; FEATURE-ROADMAP.md remains retired. The latest point-in-time status is in the front metadata and Section 66; reverify live GitHub before treating any dated source ID as current.
65.3 Drive Retirement
The former Drive records GoreeCloud/Feature Roadmap/GoreeCloud Launcher/FEATURE-ROADMAP.docx and GoreeCloud/Changelogs/Change Log — Launcher.docx were migration sources only. After repository migration and authoritative readback completed, both Drive records were permanently deleted. Subsequent metadata reads returned 404 Not Found, and the Launcher feature-roadmap folder is empty. They must not be recreated as synchronized, mirrored, backup, convenience, or canonical copies.
65.4 Lifecycle Boundary
This governance migration changes documentation authority only. It does not promote Launcher beyond Development or establish representative-device, production, Release Candidate, or Stable acceptance. The latest source-bearing runtime is PR #238 / 426e466f62d8347de720258be8492e518e84c4d5, at that earlier checkpoint with documentation-only main advanced through PR #239 / 54c7bccb4b1b5865a123af34b9cbc46e538e3f51. Issue #80 remains open for the physical-device/default-HOME, accessibility, profile, Quickstep/Recents, performance, recovery, signing/distribution, and release-qualification gates. Those identities are historical and superseded by the front current-status metadata; the original release-gate obligations remain open.

## Ongoing record maintenance

Update this file when significant architecture, governance, ownership, repository, security/privacy, production/recovery, lifecycle, migration, split/merge/rename, deprecation, or retirement events occur. Routine fixes and release-oriented change detail belong primarily in `CHANGELOGS.md`, with cross-references here when the event is project-significant.
