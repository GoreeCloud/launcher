# Device Visual Acceptance — 2026-09-07

Status: Failed — redesign required before visual-stability claims
Device: OnePlus Nord N200 (`dre`)
ROM: LineageOS 23.2 / Android 16

## Physical-device findings

The current Android Development build is functionally recognizable as a launcher but fails GoreeCloud visual acceptance.

Observed problems:

- The Home surface reads as an internal test harness rather than a finished launcher.
- `Page 1` / `Add` controls dominate the top of the screen.
- `Launcher settings` is presented as a large primary surface instead of a secondary action.
- The permanent GoreeCloud search card is too tall and visually heavy.
- The `Use GoreeCloud as Home` role-prompt card consumes a large portion of the Home surface and should not dominate normal use.
- App labels use visible translucent capsules behind text, creating visual noise.
- Icon spacing and grid density are inconsistent with the available screen area.
- The dock is oversized and visually detached from the workspace.
- The full-width `Apps` button feels like a settings/demo control rather than a natural app-drawer affordance.
- The overall layout contains too many independent rounded rectangles competing for hierarchy.

## Required redesign

### Home

- Wallpaper is the primary canvas.
- Remove persistent developer/demo scaffolding from the normal Home surface.
- Use a restrained, compact top affordance only when necessary.
- Search should be compact and optional; swipe-down remains the primary universal-search gesture.
- If the launcher is not default, show a dismissible/compact role prompt rather than a dominant hero card.
- Use clean app labels without opaque/translucent label pills.
- Increase visual breathing room while retaining useful app density.
- Make the dock a coherent Glaze surface with lower visual mass.
- Replace the full-width `Apps` button with a natural swipe-up/open-drawer affordance; a small optional handle or compact button may remain for discoverability.

### App drawer

- Search remains prominent but compact.
- Preserve app-grid density and legibility.
- Avoid excessive top chrome.
- Maintain a clear route back to Home and Settings without making them peer-level with installed apps.

### Glaze UI

Use material hierarchy rather than stacking many cards. Neutral glass is the material; color is accent. Wallpaper and content must remain visible through restrained translucent surfaces.

## Acceptance criteria

Before visual-stability promotion, verify on a physical Android device:

- Home is immediately readable as a launcher, not a configuration screen.
- No developer-oriented setup card dominates the first viewport.
- Search, dock, and app grid have a clear hierarchy.
- App labels remain readable on both bright and dark GoreeCloud wallpapers.
- Drawer can be opened naturally by gesture and via an accessible discoverability affordance.
- Home/Back/Recents and gesture navigation remain functional.
- Layout survives reboot and launcher-role persistence.

CI success alone is not sufficient for visual acceptance.
