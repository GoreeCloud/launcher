# Primary Home Grid Migration and Spatial Placement — Development

## Purpose

This Development slice activates the previously planned primary HOME compatibility-to-grid migration under the existing terminal Room workspace authority. It gives the protected rank-zero primary Home page authoritative cell coordinates without introducing a second placement store or changing the Room schema.

The primary page remains `home:0`, HOME rank 0. Existing legacy Favorites rows may begin with null `cellX` / `cellY`; the first accepted spatial operation migrates those rows into the configured Home grid through a guarded exact-snapshot Room transaction.

## Supported grid contract

Primary Home spatial placement is bounded to the Launcher-supported Home presets:

- 4–6 columns;
- 4–7 rows;
- current primary application items remain 1×1 APP rows;
- item identity remains `legacy:home:<appKey>`;
- application ranks remain contiguous membership/order metadata; and
- coordinates must be unique, non-negative, collision-free, and inside the active grid.

A requested grid that cannot contain every current primary Home item fails closed. The migration planner must not silently expand beyond the requested Home rows or columns.

## Migration and resize behavior

For a canonical null-coordinate primary page, `WorkspacePrimaryHomeGridMigrationPlanner` maps Favorites row-major into the requested configured grid. `WorkspacePrimaryHomeSpatialRepository.ensureGrid` applies the migration only after terminal Room authority and only when the exact primary page/item snapshot still matches the observed state.

For an already-spatial primary page, valid coordinates remain authoritative. If the user selects a smaller supported grid, Launcher preserves coordinates that remain valid and deterministically reflows out-of-bounds items into available cells before persisting the new grid preference. If every item cannot be represented safely in the requested grid, the grid change is rejected.

The primary Home page identity and rank are never changed by this migration.

## Direct primary Home placement

The primary Home renderer consumes authoritative `cellX` / `cellY` values projected through `WorkspacePagedHomeObserver`. All configured grid cells are renderable drop targets, including empty cells.

When Home layout is unlocked:

- long-press and drag an icon to an empty cell to persist that cell;
- long-press and drag onto an occupied cell to swap the two primary Home positions;
- long-press without a movement gesture opens the existing placement-management dialog; and
- the layout-lock gate blocks primary cell mutation when the Home layout is locked.

Movement is written through `WorkspacePrimaryHomeSpatialRepository.moveAppToCell`, which rechecks authority, grid bounds, canonical primary state, collisions, and the complete primary item snapshot before committing.

## Favorite membership and grid changes

After spatial activation, Favorite reorder/removal preserves retained cell coordinates. Adding a Favorite requires the current configured Home grid and places the new item into the first free in-bounds cell. If no safe cell exists, the write fails closed.

Changing the configured Home grid invokes spatial validation/reflow before the DataStore presentation preference is changed, so the UI cannot intentionally switch to a grid that would orphan current authoritative Home placements.

## Canonical read and recovery boundary

`WorkspaceCanonicalRoomPlacementReader` accepts either:

1. the exact legacy null-coordinate primary compatibility form; or
2. a bounded, collision-free spatial primary form.

Dock compatibility rows remain null-coordinate. Mixed null/spatial primary rows, duplicate/colliding positions, malformed identities, non-contiguous ranks, unsupported spans, or coordinates outside the supported maximum Home grid fail closed.

Startup, recreation, and Room-authoritative observation therefore continue to use one canonical workspace authority before and after spatial activation.

## Current acceptance boundary

This Development implementation covers primary within-page spatial placement and migration only. It does **not** establish:

- primary-to-secondary or secondary-to-primary spatial page transfer;
- mature cross-page drag/drop;
- widgets, folders, shortcuts, or arbitrary item spans on primary Home;
- overlapping elements or sub-grid positioning;
- representative physical-device acceptance;
- signed production release acceptance; or
- Stable qualification.

No Android permission, `INTERNET` permission, network behavior, telemetry, analytics, advertising, sponsorship, or Room schema change is introduced by this slice.
