# Production workspace activation

Milestone 1 activates the reviewed DataStore-to-Room workspace transition through one production boundary: `WorkspaceProductionRuntimeCoordinator`.

## Runtime ownership

`MainActivity` constructs exactly one production runtime coordinator. The coordinator owns the reviewed pre-cutover reconciler, production promotion coordinator, post-cutover startup coordinator, authority-aware placement observer, and authority-aware placement repository. `scripts/check_room_cutover.py` enforces this constructor topology and continues to prohibit direct Room placement access outside the reviewed authority-aware router.

Before terminal Room authority, Home receives the ordered Favorites/Dock compatibility state from Preferences DataStore. `reconcileAndActivate()` mirrors and independently verifies that state through the existing startup path. When current `ROOM_VERIFIED` evidence is still valid, the reviewed production promotion transaction performs the guarded one-way authority change.

After terminal `ROOM`, the runtime performs the post-cutover startup health check before exposing Room placement. Healthy Room state is observed reactively through the accepted authority-aware observer. Favorite/Dock membership and ordering mutations go through the accepted authority-aware placement router and therefore write Room after terminal authority.

## Recovery behavior

Terminal Room authority is one-way. If Room is unavailable, malformed, mismatched, or fails after cutover, the runtime exposes an explicit recovery-required placement state. It does not silently return to legacy DataStore placement, because DataStore may be stale after Room-authoritative mutations. Legacy `WorkspaceRepository` placement mutations are already frozen under terminal `ROOM`.

The current Home presentation maps recovery-required placement to a non-authoritative empty compatibility view rather than rendering stale DataStore Favorites/Dock. Application discovery, app launching, the app drawer, appearance, and HOME-role controls remain separate from the Room placement authority transition. A dedicated recovery surface can be added later without changing the persistence authority contract.

## Lifecycle boundary

`MainActivity` re-evaluates the production runtime on resume. The runtime observation source is lifecycle-collected. The API 36 activation test closes and recreates both DataStore and Room clients against the same files after production promotion, then requires terminal authority, Room placement, and Room mutation behavior to resume correctly.

This persistence-client recreation is not a claim of Android OS process-death acceptance. True OS process-death/default-HOME physical-device acceptance remains a separate release gate.

## Privacy and security

Activation adds no Android permission, INTERNET permission, remote workspace dependency, analytics, advertising, sponsorship, attribution, or tracking SDK. Workspace authority, placement, verification, and recovery remain device-local. Runtime test application keys are synthetic.
