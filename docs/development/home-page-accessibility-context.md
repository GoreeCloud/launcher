# Home Page Accessibility Context — Development

## Scope

The Home page switcher now exposes one coherent accessibility semantic per page. The label is derived from the same Room-projected page context used by the visible switcher and includes the page number, app count, unsupported-item count, and selected state.

## Authority boundary

This is read-only presentation derived from existing terminal Room workspace authority. It does not create, move, delete, synchronize, or otherwise mutate workspace state.

## Compatibility

The implementation is based on the current main branch and preserves the primary Home/Favorites compatibility protections that keep the primary page at rank zero and prevent secondary spatial editing from crossing that boundary.

## Acceptance boundary

Development only. This does not establish folder/widget presentation, complete assistive-technology device acceptance, Room production cutover, synchronization acceptance, or Stable status.
