# Combined Launcher portable restore coordinator

Status: **Development persistence and interruption-reconciliation candidate**

## Purpose

`LauncherPortableRestoreImport` coordinates the two currently approved bounded Launcher portability formats:

- `goreecloud-launcher-workspace-snapshot/1`
- `goreecloud-launcher-preferences/1`

The coordinator decodes and validates both complete inputs, then validates their shared Home-grid contract before it grants any persistence call. A writer receives the validated workspace snapshot and the validated seven-field preference record together through one `replacePortableState(...)` call.

The current Development branch provides `LauncherTransactionalPortableRestoreWriter`, backed by the existing Room workspace DAO plus Launcher Preferences DataStore. This is a concrete persistence implementation for the currently approved bounded pair only; it does not widen either portable format or establish product-wide recovery.

## Pair compatibility and reviewed apply

The workspace format is deliberately framework-independent and permits a broader bounded grid domain than the current Launcher preference format. Two payloads can therefore be individually valid while still describing contradictory Launcher state.

For a combined restore, the workspace grid columns and rows must exactly match the portable Home-grid columns and rows in the preference snapshot. A mismatch is rejected as `COMPATIBILITY` before any writer call. The same validation path is reused by the read-only restore preview so preview and apply cannot disagree about whether a pair is acceptable.

`LauncherPortableRestorePreview` also returns an opaque SHA-256 review token over the exact validated byte pair. `applyReviewed(...)` fails closed when either otherwise-valid payload changes after review. The token is only a local review/apply consistency guard; it is not a signature, ownership proof, artifact provenance, Everkeep lineage, or authorization grant.

## Concrete persistence behavior

`LauncherTransactionalPortableRestoreWriter` now applies the currently supported pair through a bounded journaled sequence:

1. defensively revalidates the workspace and preference values plus their shared Home-grid dimensions;
2. reconciles any valid prior local portable-restore journal and refuses to continue if recovery remains unresolved;
3. reads the previous seven-field portable preference state;
4. asks Room to **plan** the exact HOME replacement without mutating state;
5. fingerprints the exact previous and planned HOME relational states, including page and item fields plus `appKey`;
6. durably stores one strict checksummed private recovery journal in Launcher Preferences DataStore before Room mutation;
7. applies the exact precomputed Room plan only if the complete current HOME state still equals the planned previous state;
8. atomically writes the seven target portable preferences and clears that exact journal in one DataStore edit; and
9. verifies target preference readback and that the journal is absent.

The private recovery journal is not part of either portable format. It contains a transaction identifier, exact previous/applied HOME-state SHA-256 fingerprints, and exact previous/target portable preference values. It is device-local coordination metadata only; it is not exported, synchronized, account-backed, or represented as artifact provenance.

The HOME fingerprint includes page identity/container/rank and every persisted item field: item identity, page identity, item type, `appKey`, rank, cells, and spans. Consequently, an application/profile identity change cannot be treated as equivalent merely because placement geometry still matches.

## Process interruption reconciliation

`LauncherPortableRestoreRecoveryCoordinator` provides an explicit Development reconciliation seam for one journaled interrupted operation. Callers must run it while new workspace/preference mutations are quiescent. It does **not** yet install a production application-startup gate.

The coordinator uses only the exact current Room fingerprint, exact current portable preferences, and the matching checksummed journal:

- no journal -> `Clean`;
- invalid/corrupt journal -> `RecoveryRequired`;
- previous HOME fingerprint + previous preferences -> Room never committed; clear the matching journal and report `AbandonedBeforeWorkspaceApply`;
- applied HOME fingerprint + previous preferences -> Room committed but preference finalization did not; atomically write target preferences and clear the matching journal, then report `FinalizedAfterWorkspaceApply`;
- applied HOME fingerprint + target preferences -> clear the matching journal and report `ConfirmedCommitted`;
- any third workspace or preference state -> `RecoveryRequired` without overwriting it.

Every successful reconciliation path re-reads the exact HOME fingerprint, preferences, and journal absence before reporting success. A concurrent third state or changed journal therefore remains a recovery condition instead of being overwritten.

This closes the previously uncovered **journaled process-interruption state classification and bounded recovery seam** for the current same-resolved-identity operation. It does not make Room and DataStore one physical transaction, guarantee recovery from storage/hardware corruption, or establish production startup sequencing.

## Same-process failure compensation

If a failure occurs after Room mutation during the live writer call, compensating rollback still runs under `NonCancellable`. Workspace rollback is allowed only while the HOME state exactly equals the applied plan. Preference rollback similarly refuses to overwrite a third preference state.

The matching durable journal is cleared only after both workspace and preference rollback are verified. If either rollback fails, the journal remains as recovery evidence and the writer reports `LauncherPortableRestoreRollbackException`. After fully verified compensation, ordinary failures become `LauncherPortableRestoreApplyException`; cancellation rethrows the original `CancellationException` unchanged.

## Clean-target and identity policy

The current `goreecloud-launcher-workspace-snapshot/1` format carries framework-independent page/grid placement plus opaque item IDs. It does not carry the Android package, user/profile, shortcut, folder, widget-provider, AppWidget ID, or equivalent identity metadata needed to materialize arbitrary items on a different or clean target.

The current persisted `appKey` is also a **device/profile-resolved runtime key**, not a portable cross-device identity. The recovery journal fingerprints it only to ensure local before/after state equivalence; the journal does not export or rebind it.

For that reason the current writer follows this fail-closed policy:

- it never invents an application/package/profile binding;
- every imported item ID must exactly match the current HOME item identity set;
- the current HOME item set must consist only of already-resolved APP rows with nonblank existing `appKey` values;
- shortcut, folder, widget, unresolved-app, and other unsupported identity types are rejected;
- non-empty portable HOME content cannot be materialized on a clean target that has no corresponding resolved items;
- an empty portable item set may be applied only when the target HOME item set is also empty;
- imported HOME page IDs must not collide with non-HOME page identities; and
- DOCK and other non-HOME state are not modified by this writer.

This is intentionally a **same-resolved-identity placement restore**, not cross-device rebinding. Clean-device reconstruction requires a separately approved, versioned format and policy that can express package/profile/folder/shortcut/widget identity safely.

## Fail-closed behavior

Persistence is not invoked when:

- the workspace snapshot is malformed, tampered, oversized, expanded, unsupported, or fails grid/page/placement validation;
- the preference snapshot is malformed, tampered, expanded, unsupported, or contains an out-of-range value;
- both snapshots are individually valid but their Home-grid dimensions disagree;
- reviewed apply detects that the exact validated input pair changed after preview;
- a prior journal is invalid or cannot be reconciled safely; or
- the HOME state changes after planning but before Room apply.

The concrete Room writer additionally rejects incompatible target identity state, unsupported HOME item types, duplicate identities, and HOME page collisions with non-HOME state before replacing HOME placement state.

## Authority boundary

This source still does not:

- discover or resolve new installed Android packages for import;
- resolve or remap Android user/profile identities;
- recreate folders or shortcuts;
- recreate widgets or rebind widget providers/AppWidget IDs;
- restore dock/favorites state outside the current workspace snapshot;
- restore hidden-app state, wallpaper, icon packs, Theme Manager state, or broader system settings;
- synchronize across devices;
- establish artifact provenance or account/device ownership;
- create an accepted Everkeep backup or recovery path;
- install the reconciliation coordinator as a production startup gate;
- prove recovery from storage corruption, OS kill at every instruction boundary, power-loss durability on every device/filesystem, or hostile/non-cooperating writers; or
- authorize production restore, deployment, Release Candidate promotion, or Stable qualification.

The current workspace format remains framework-independent placement state, and the current preference format remains the seven explicit Launcher settings documented in their individual Development contracts.

## Recovery status

Concrete bounded persistence, pair-level compatibility, review/apply consistency, exact-state planning, durable pre-Room recovery journaling, explicit interrupted-state reconciliation, readback verification, cancellation-safe guarded rollback, and a fail-closed same-identity target policy now exist in Development source. This is meaningful recovery implementation evidence, but it is still **partial** rather than complete product recovery.

Before Launcher recovery can be accepted for RC/production, remaining work includes the complete approved Launcher-owned state inventory, safe clean-target reconstruction/rebinding for package/profile/folder/shortcut/widget identities, production startup sequencing for interruption recovery, provenance and user-control requirements, applicable Privacy Shield/Wardveil/Everkeep integration, representative physical-device/default-HOME recovery and failure testing, signed-candidate evidence, and production recovery validation.
