# GoreeCloud Launcher App Drawer

Requirement level: Mandatory product direction.

Status: Development. This document defines required App Drawer behavior and distinguishes integrated behavior from planned or acceptance-open work.

## Product role

The App Drawer is GoreeCloud Launcher's primary application discovery, organization, search, and launch surface. It must remain useful offline, local-first, profile-aware, accessible, responsive, and independent from a GoreeCloud account.

The Drawer is an application library and action surface rather than a flat installed-package list.

## Required presentation modes

The Drawer supports or is required to support:

- Grid.
- Compact grid.
- List.
- Category presentation.
- Configurable columns, spacing, labels, icon size, background, search position, and scroll/page navigation.
- Stable icon and label geometry.
- Phone, tablet, landscape, and foldable-aware composition.
- Alphabetical, reverse alphabetical, local most-recent, local most-frequent, and pinned-first ordering.
- Fast application search.

Current Development source implements Grid, Compact, List, Category, persisted presentation settings, sort controls, local app search, page/scroll navigation, stable grid geometry, and profile pages. The stacked recency/navigation candidate adds a profile-aware **Recently installed** sort using Android `LauncherActivityInfo.firstInstallTime` and a 48 dp alphabetical jump index for the ordinary A-Z List view.

## Universal Search relationship

Universal Search is Launcher-owned and remains distinct from the Drawer inventory surface. Search-first Drawer entry can open with the Drawer search field focused, while full Universal Search remains the broader resource/action system.

Core local search can cover installed applications, Android shortcuts, Launcher actions, and explicitly enabled local sources. Optional connected providers require explicit enablement and applicable authorization. Network-backed sources are not queried merely because the user types in the local Apps filter.

## Categories

Category presentation groups applications using deterministic Android/Launcher metadata. Categories are organizational presentation only and do not change installed packages.

Current Development source groups supported categories such as Productivity, Social, Media, Games, Travel & maps, News, and Other. App Drawer folders remain visible in Category mode under their own Folders group rather than forcing the entire Drawer back to one undifferentiated grid.

Future category work may add richer local classification when the source is transparent, deterministic, user-controllable, and privacy-preserving.

## User-created tabs

Users can create named App Drawer tabs for collections such as Work, Games, Media, or Development.

Current Development candidate behavior:

- Up to eight device-local custom tabs.
- Bounded names.
- Create, rename, and delete.
- Exact profile-qualified application membership.
- An accessible All tab plus custom tabs.
- A 48 dp minimum interaction target for Launcher-owned tab controls.
- A non-gesture edit control for the selected tab.
- Per-application tab assignment from the App Drawer context surface.
- Tabs filter presentation only; they do not modify Android installation, Home placement, Dock placement, folders, hidden state, App Lock, or profile authority.

Tab state currently remains outside the strict portable preference v1 recovery contract. Versioned backup/restore of custom Drawer organization remains separately gated.

## App Drawer folders

Drawer folders are supported as organizational entries and remain distinct from Android package state. They can be created and managed without requiring a corresponding Home placement.

Folder behavior must preserve profile boundaries, explicit user membership, predictable ordering, Glaze folder presentation, accessible open/manage behavior, and compatibility with Home/Dock placement rules.

Smart folders remain future work unless explicitly backed by deterministic local rules and clear user control.

## Hidden applications

Hidden state is a Launcher discovery preference, not package disablement.

Hide does not inherently uninstall, disable, remove Home placement, remove Dock placement, remove folder membership, remove widgets, or activate App Lock.

Hidden state is profile-qualified. Launcher Settings provides a Hidden apps recovery surface.

Hide != Disable != Uninstall != Lock.

## Profile awareness

Android remains authority for application/profile identity.

The Drawer separates User Apps and Work Apps when applicable. Organizational state including hidden apps, pins, and custom-tab membership uses profile-qualified identities so identical package/component names in different profiles are not treated as one item.

## Suggestions and local ordering

Local Most recent and Most frequent ordering use Launcher-local launch history only. Launcher does not request Android Usage Access for these sorts.

Future suggestions or smart groups must remain local-first, truthful, explainable, user-controllable, and free from sponsored or affiliate ranking.

A fresh installation must not fabricate prior usage.

## Direct placement

When Home layout is unlocked, long-press and continued drag from Apps can create a Home placement or Dock placement without removing the application from the Drawer inventory.

Placement changes use authoritative workspace state and respect Lock Home screen layout.

## Context actions

A stationary long-press can expose actions including:

- Add/remove Home placement.
- Add/remove Dock placement.
- App Drawer pinning and manual pinned ordering.
- Custom App Drawer tab assignment.
- Folder assignment.
- Android shortcuts.
- App widgets when available.
- Android App info.
- Hide/show in Apps & Search.
- App Lock.
- Android-confirmed uninstall.

Uninstall is always delegated to Android system confirmation.

## Accessibility

The Drawer must support:

- 48 dp minimum Launcher-owned interaction targets.
- TalkBack semantics.
- Switch Access.
- Keyboard/D-pad where applicable.
- Large text.
- Stable focus order.
- Reduced motion.
- Reduced transparency.
- Sufficient contrast.
- Non-gesture alternatives for important organization actions.
- Phone/tablet/foldable compositions.

Current Development candidate tab controls expose selected-state semantics and explicit create/edit actions. Representative-device accessibility acceptance remains open.

## Privacy and commercial neutrality

Core Drawer operation must not require:

- Advertising SDKs.
- Sponsored app placement.
- Affiliate ranking.
- Behavioral tracking.
- Remote analytics.
- A GoreeCloud account.
- Network access.

Installed inventory, Drawer organization, hidden state, folder state, custom tabs, and Launcher-local usage signals remain local by default.

## Performance

Scrolling, filtering, paging, search, drag/drop, and layout switching must prioritize direct user input over decorative animation. Motion must remain interruptible, reduced-motion aware, state preserving, and power conscious.

## Glaze presentation

The Drawer uses GoreeCloud Glaze rather than imitating another launcher. Rounded surfaces, selective translucency, restrained depth, wallpaper-aware color, accent treatment, icon customization, drawer backgrounds, search styling, reduced-transparency fallbacks, and controlled motion must preserve usability and performance.

## Header philosophy

The primary Drawer surface is for applications, organization, and search. General Launcher configuration belongs in Edit Home or Launcher Settings.

Hierarchy:

App Drawer -> applications and organization

Universal Search -> broad search and actions

Edit Home -> Launcher customization

Launcher Settings -> detailed configuration

## Acceptance boundary

Current source remains Development. Exact-head automated validation is necessary but insufficient for release qualification. Representative-device visual quality, TalkBack/Switch Access, keyboard/D-pad, large text, RTL/localization, phone/tablet/foldable, profile transitions, direct placement, performance/power, protected Development signing/update continuity, Release Candidate, Production Acceptance, Stable, Seal, and Anchor remain separate gates.
