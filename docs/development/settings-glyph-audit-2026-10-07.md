# Launcher Settings Glyph Audit — October 7, 2026

Status: Development candidate in draft PR #280, restacked directly on protected main after integrated PR #277 and PR #278. Not integrated or physically accepted.

## Source implementation

The Settings overview maps 13 categories to the same Canvas-based `LauncherOutlineGlyph` family. The symbols use a 24 dp unit coordinate canvas, rounded 1.8 dp stroke with rounded caps and joins, theme-derived primary tint and a common centered 26 dp glyph in a 46 dp rounded surface. The glyph implementation is shared with Launcher and Search; it does not fetch third-party icon assets.

| Category | Glyph | Source audit outcome |
| --- | --- | --- |
| Home | HOME | Refined house outline and door proportions |
| App Drawer | APPS | Existing four-tile grid retained |
| Dock | DOCK | Refined three aligned app boxes and rounded tray |
| Folders | FOLDER | Refined rounded folder outline and tab |
| Universal Search | SEARCH | Existing search lens retained |
| Widgets & Glaze Cards | WIDGETS | Refined aligned widget blocks and marker |
| Gestures & Actions | GESTURE | Refined curved swipe and arrow |
| Appearance | APPEARANCE | Existing palette retained |
| Notifications & Badges | BELL | Refined bell contour and clapper |
| Privacy & Permissions | SHIELD | Existing shield retained |
| Backup & Restore | BACKUP | Refined recovery arrow and lower tray; follow-up optical check |
| Advanced | SLIDERS | Refined sliders to remove lines behind thumb circles |
| About | INFO | Existing information mark retained |

## Settings submenus

The current Settings root uses category headings, switches, selection rows, textual descriptions, and Glaze-owned vector action glyphs. Its Search field uses `LauncherOutlineGlyph.SEARCH`; reset/close and header/back actions use `GlazePopupActionGlyph`. These are vector paths, not Unicode imitation icons. Settings submenus share Material/Glaze color semantics and minimum touch-target handling. No separate remote icon fetch is introduced.

## Unverified visual and interaction acceptance

The source review and vector edits are **not** a screenshot or device rendering pass. Required acceptance remains: compare light, dark, wallpaper/Glaze and high-contrast presentations; icon shape/optical alignment at 1x/1.5x/2x/3x densities; 100%-200% font scale; TalkBack/Switch Access and keyboard focus; phone, tablet, foldable and landscape; minimum 48 dp action targets. Check the backup, bell, palette and shield marks optically on a real device and refine if necessary. Do not declare Human Visual Excellence or production-ready status from source review alone.

Owner-supplied Settings and App Drawer screenshots prompted this review; no generated images were produced.
