# Rendered HOME Page Navigation

## Purpose

This Development surface connects the established terminal Room multi-page workspace model to rendered Home navigation and bounded user-facing page lifecycle/item-placement controls without creating a second workspace authority.

## Authority boundary

`WorkspacePagedHomeObserver` emits paged HOME state only after durable terminal `WorkspaceAuthority.ROOM` is established. Before terminal Room authority, or when the Room workspace cannot be read safely, the observer does not fabricate authoritative secondary-page state.

The observer combines Room page and item streams and maps them into ordered rendered page state. Application items are exposed by `appKey`; unsupported item types are counted so folders, shortcuts, widgets, or malformed rows are not silently misrepresented as launchable applications.

The current primary HOME page, `WorkspaceLegacyImportMapper.HOME_PAGE_ID`, is also the terminal-Room compatibility projection used by the existing Favorites path. It intentionally remains rank zero and retains the canonical legacy item representation until a separately accepted primary-grid migration replaces that compatibility boundary.

## User-interface boundary

`MainActivity` observes the paged Room state alongside the existing authoritative placement path.

When authoritative HOME pages exist:

- a page selector is rendered, including when only the primary page exists so a new page can be intentionally created;
- the primary page keeps the existing Favorites/Dock UI and mutation callbacks;
- secondary pages render their launchable application items from Room;
- secondary-page ordering can be changed through the guarded Room page-order mutation without moving the protected primary page away from rank zero;
- `Add page` requests creation of a new empty page through the terminal-Room mutation repository;
- `Delete empty page` is exposed only when the selected page is non-primary and the authoritative rendered snapshot contains no application or unsupported items; and
- application tiles on a secondary page can request a move to another existing secondary Home page through a bounded `Move` menu.

A successful page creation returns the authoritative generated page ID/rank and the UI selects that page only after the Room transaction succeeds. A successful deletion returns the authoritative remaining page identity order; if the selected page was removed, the UI returns to the protected primary page. A successful secondary application-page move returns `UpdatedItem`, and the UI selects the authoritative target page only after that write succeeds. Selection is otherwise reconciled against the latest authoritative page identity set.

## Page-creation mutation boundary

`WorkspacePagedRoomMutationRepository.createHomePage` requires terminal Room authority. It reads the complete HOME page snapshot, requires contiguous current ranks with the protected primary page at rank zero, verifies the requested page ID is globally unused, and appends one empty page at the next rank through `WorkspaceDao.appendPageIfSnapshotMatches`.

The DAO transaction compares the complete observed HOME page snapshot again before writing. A concurrent page-set change fails closed rather than silently creating against stale rank state. The transaction creates no child workspace item and moves no existing item.

The UI generates opaque `home:user:<UUID>` page identities locally for the mutation request. Those IDs carry no authority by themselves; Room authority and the transaction remain decisive.

## Empty-page deletion boundary

`WorkspacePagedRoomMutationRepository.deleteEmptyHomePage` requires terminal Room authority and refuses the protected primary HOME page, an unknown page, the last remaining page, or any page that contains an item in the observed authoritative snapshot.

`WorkspaceDao.deleteEmptyPageIfSnapshotMatches` then repeats the complete HOME page and item snapshot comparison inside the Room transaction and re-checks that the target page is empty before deletion. This is the critical cascade-safety boundary: if a child item appears after the caller's initial read, the transaction fails closed instead of allowing the `ON DELETE CASCADE` foreign key to remove that item. Only after those checks pass is the empty page removed and remaining page ranks compacted transactionally. The protected primary page remains rank zero.

This is deliberately not general destructive page deletion. Populated pages cannot be deleted from the UI or repository through this path. Moving/removing their contents, confirmation UX for destructive operations, recovery/undo semantics, and broader page management remain separate milestones.

## Protected page-order boundary

The primary compatibility page cannot be moved away from rank zero, and no secondary page can be moved into rank zero. `WorkspacePagedRoomMutationRepository.moveHomePage` rejects either attempt with `PrimaryPageProtected` before rewriting Room ranks. The page switcher mirrors this rule by disabling controls that would cross the protected primary boundary.

Secondary pages can still reorder among ranks one and above. The underlying page-order transaction continues to require the complete page identity set and stages ranks before committing the final contiguous order.

## Bounded secondary-page application move boundary

`WorkspaceHomeItemPageMover` adds a narrow UI-facing adapter around the already-existing guarded `WorkspacePagedRoomMutationRepository.moveHomeItem` mutation.

The adapter:

- requires initialized terminal Room authority before reading page-placement state;
- requires nonblank and distinct source/target page identities and a nonblank application key;
- explicitly refuses the protected primary compatibility page as either a spatial source or spatial target;
- resolves exactly one APP item matching the secondary source page and application key, rejecting ambiguous or missing state;
- requires spatial secondary-page items to have valid grid coordinates while allowing the canonical primary Favorites projection to retain its intentional null coordinates;
- derives a deterministic grid envelope from the current secondary-page coordinates with a four-column minimum matching the rendered secondary grid;
- scans target-page placements in row-major order and selects the first collision-free cell preserving the source item's span; and
- delegates the final write to `moveHomeItem` rather than writing directly.

The preflight free-cell selection is intentionally not write authority. `moveHomeItem` first verifies the canonical primary/Dock compatibility projection is healthy, then re-reads the complete HOME pages/items. Spatial validation is limited to secondary HOME pages, while the complete HOME snapshot—including the protected primary page—is still compared inside `WorkspaceDao.replaceItemPlacementIfSnapshotMatches`. If a concurrent mutation changes the snapshot, if the chosen cell becomes invalid, if secondary placement coordinates are malformed, if primary compatibility is malformed, or if any source/target identity no longer matches, the write fails closed.

This boundary repairs an important distinction between the compatibility primary page and the spatial secondary-page model. A healthy primary Favorites page intentionally contains null `cellX`/`cellY` values and must not be forced into the secondary grid merely to permit secondary editing.

This milestone does not expose arbitrary cell/span editing or cross-page drag/drop. It also does not route primary-page Favorites into secondary pages or secondary items into the primary compatibility page. Those operations require a separately accepted primary-grid/compatibility migration.

## Validation

Acceptance for this slice requires:

- Android file-backed Room tests proving page lifecycle and secondary app-page moves are reserved before terminal Room authority;
- proof that an accepted page creation persists one empty page at the next authoritative rank;
- duplicate page IDs fail closed;
- the protected primary page cannot be deleted or moved away from rank zero;
- no secondary page can be reordered ahead of the protected primary page;
- populated pages fail with `PageNotEmpty` and retain their child items;
- an accepted empty-page deletion removes only the target page and compacts remaining ranks while keeping primary rank zero;
- existing child items survive subsequent secondary-page reordering;
- an accepted secondary-to-secondary app move chooses the first free target cell, preserves item identity, persists on the target page, and removes the source-page placement;
- the canonical primary Favorites item retains its null coordinates and the post-cutover compatibility health check remains `Healthy` after secondary spatial mutations;
- attempts to spatially move to or from the primary compatibility page fail with `PrimaryPageProtected`;
- same-page or missing-page secondary app-move requests fail closed;
- existing privacy, manifest, Glaze UI, Room schema/cutover, lint, unit, assembly, and Android 16 instrumentation gates; and
- no promotion to production/Stable based on CI alone.

## Remaining boundaries

This milestone does not establish populated-page deletion, destructive recovery/undo, arbitrary live cell/span editing, cross-page drag/drop UI, primary compatibility-page grid migration, primary-to-secondary or secondary-to-primary spatial placement, production Room cutover acceptance for the complete intended workspace, folders, shortcuts, widgets/AppWidgetHost, representative physical-device HOME acceptance, complete accessibility acceptance, signed release packaging, or Stable qualification.
