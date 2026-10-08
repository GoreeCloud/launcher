# Room promotion transaction rehearsal

GoreeCloud Launcher is still pre-cutover. Preferences DataStore remains Home's live Favorites/Dock authority, production does not invoke `WorkspaceRepository.promoteRoomAuthority`, and production does not route Home through `WorkspaceRoomPlacementRepository`.

This document defines the Milestone 1 promotion-transaction rehearsal that sits between accepted observational readiness and a future explicit production cutover.

## Accepted prerequisite

PR #13 established `WorkspaceCutoverReadinessCoordinator` and the fail-closed `scripts/check_room_cutover.py` production boundary. Exact PR #13 head `c6b1aafacce7aa45a76faa7a9080db2472f2dad1` passed Android CI run `32587613393`; the complete API 36 instrumentation suite ran eight tests successfully before squash merge `c425b00308fbbebd68b467fc65ccfcea2cf4d77d`.

A readiness `Ready` result is evidence only. It does not persist a capability and does not alter authority.

## Promotion candidate rehearsal

`WorkspacePromotionRehearsalCoordinator` consumes the existing observational readiness gate and then deliberately rechecks the promotion boundary again before producing a `WorkspacePromotionCandidate`.

A candidate requires:

1. The existing readiness coordinator to return `Ready`.
2. A fresh DataStore read that is still initialized and `ROOM_VERIFIED` with a non-null verified fingerprint.
3. A fresh DAO acquisition after readiness has already completed.
4. A fresh canonical Room read that still exactly equals the ordered Favorite/Dock state in DataStore.
5. A final DataStore read proving authority, ordered Favorites, Dock, and verified fingerprint are still exactly the evidence used for the candidate.

The candidate contains the exact `WorkspaceState` that a later guarded promotion transaction would have to present to `promoteRoomAuthority`. It is not durable authority and it is intentionally invalidated by any normal DataStore workspace mutation through the existing fingerprint/authority rules.

The production coordinator does not call `promoteRoomAuthority`. Android instrumentation may invoke the existing guarded repository primitive with a candidate to prove the transaction contract, but the production source-level cutover guard remains unchanged.

## Rehearsal outcomes

The coordinator keeps failures categorical:

- `WaitingForInitialization` — no initialized workspace exists.
- `NeedsVerification` — DataStore is still authoritative and must complete the existing mirror/readiness path first.
- `Candidate` — the exact promotion-boundary evidence remained current through both readiness and boundary revalidation.
- `AlreadyRoomAuthoritative` — the terminal marker already exists.
- `Unavailable` — the relational DAO could not be acquired.
- `Mismatch` — canonical Room placement does not equal the verified DataStore snapshot.
- `StaleEvidence` — authority, lists, or fingerprint changed while checks were in progress.
- `Failed` — an ordinary relational failure occurred; only the exception type is retained.

Coroutine cancellation is rethrown rather than converted into an ordinary failure.

## Post-cutover health rehearsal

`WorkspacePostCutoverHealthEvaluator` defines the first explicit terminal-ROOM health semantics without creating rollback behavior.

It is applicable only after the durable authority marker is already `ROOM`. It performs a fresh canonical Room read and reports:

- `Healthy` — terminal ROOM remains current and the compatibility placement is canonical/readable.
- `Unavailable` — no Room DAO is available.
- `Mismatch` — Room is readable but the compatibility placement is not canonical.
- `Failed` — an ordinary Room failure occurred, with only the exception type retained.
- `AuthorityChanged` — authority changed while the health check was running.
- `NotRoomAuthoritative` — the terminal Room phase is not active.

An unhealthy result does **not** silently demote to DataStore. After a real one-way cutover, recovery must be explicit because DataStore placement may be stale relative to later Room writes.

## API 36 runtime scenario

`WorkspacePromotionRehearsalRuntimeTest` exercises this infrastructure with the real file-backed Room and Preferences DataStore implementations.

The intended acceptance cases are:

- DATASTORE cannot produce a promotion candidate.
- A verified/readiness-approved snapshot can produce a candidate without changing authority.
- Mutating the legacy workspace after candidate creation invalidates that candidate, and the existing guarded repository promotion rejects it.
- Re-verification can produce a fresh candidate that the test-only guarded promotion accepts.
- Deliberate Room divergence blocks candidate creation without mutating DataStore authority or lists.
- An unavailable DAO blocks candidate creation.
- After test-only terminal promotion, a canonical Room database reports healthy.
- Unavailable Room after terminal promotion reports an explicit recovery condition while authority remains `ROOM`.
- Reopening the same Room database can restore a healthy result without silent authority changes.

These tests rehearse transaction safety. They do not authorize production cutover, do not constitute Android OS process-death acceptance, and do not establish representative physical-device behavior.

## CI governance

`scripts/check_room_cutover.py` remains active and unchanged in policy. Production Kotlin must still contain no executable call to `promoteRoomAuthority` outside the guarded repository method definition and no production instantiation of `WorkspaceRoomPlacementRepository` outside its class declaration.

A future actual cutover pull request must explicitly modify that guard together with the reviewed promotion/routing implementation. This makes the authority transition visible in source control and prevents an unrelated change from silently turning Room on.

## Remaining production gates

Before Home can use Room as its source of truth, GoreeCloud Launcher still needs an explicitly reviewed production promotion coordinator, post-promotion Home routing, explicit Room-open/schema-migration recovery behavior, Android OS process-death/cold-start acceptance, representative physical-device/default-HOME acceptance, future Room schema migration acceptance, and release/signing acceptance.
