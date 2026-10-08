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

Current Development source implements Grid, Compact, List, Category, persisted presentation settings, sort controls, local app search, page/scroll navigation, stable grid geometry, and profile pages. Protected PR #243 integrated profile-aware **Recently installed** ordering using Android LauncherActivityInfo first-install time plus a 48 dp alphabetical jump index for the ordinary A-Z List view. Protected PR #245 integrated **Recently updated** ordering and bounded discovery filters. The current Development continuation moves **All / Pinned / Suggested / New / Updated** into a dedicated Filter menu in the App Drawer header so the app grid remains the visual focus. **Suggested** remains explicitly opt-in and disabled by default while Pinned/New/Updated remain available without usage-derived ranking.

## Universal Search relationship

Universal Search is Launcher-owned and remains distinct from the Drawer inventory surface. Search-first Drawer entry can open with the Drawer search field focused, while full Universal Search remains the broader resource/action system.

Core local search can cover installed applications, Android shortcuts, Launcher actions, and explicitly enabled local sources. Optional connected providers require explicit enablement and applicable authorization. Network-backed sources are not queried merely because the user types in the local Apps filter.

## Categories

Category presentation groups applications using deterministic Android/Launcher metadata. Categories are organizational presentation only and do not change installed packages.

Current Development source groups supported categories such as Productivity, Social, Media, Games, Travel & maps, News, and Other. App Drawer folders remain visible in Category mode under their own Folders group rather than forcing the entire Drawer back to one undifferentiated grid.

Future category work may add richer local classification when the source is transparent, deterministic, user-controllable, and privacy-preserving.

## User-created tabs

Users can create named App Drawer tabs for collections such as Work, Games, Media, or Development.

Current Development behavior:

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

Current Development includes the bounded local Smart Folder views described below; richer editable rule-based folders remain gated by deterministic local rules and clear user control.

## Smart folders

Smart folders provide dynamic App Drawer collections without mutating manual folders, tabs, Home, or Dock placement. Current Development derives bounded local-only **Pinned**, optional **Suggested**, **New**, and **Updated** smart folders from explicit pins, Launcher-local launch signals, and Android freshness metadata already used by Drawer discovery. Empty smart folders are omitted. Suggested smart-folder membership respects the existing Suggested apps control; when truthful usage does not exist, its membership uses the same deterministic A-Z fallback rather than fabricating behavior.

The October 6 Development continuation adds per-application exclusions for **Suggested**, **New**, and **Updated**. Exclusions are keyed by exact profile-qualified Launcher app identity, affect only the selected Smart Folder kind, remain device-local, and can be cleared with an explicit **Restore** action. A Smart Folder remains recoverable even when every current candidate is excluded. **Pinned** remains controlled by the existing explicit pin/unpin authority instead of a second exclusion mechanism. Custom rule composition, persisted Smart Folder ordering/naming, and portable recovery remain planned. Profile identity remains bounded by the currently selected Drawer profile, and unavailable non-primary update metadata continues to fail closed rather than borrowing another profile's state.

## Hidden applications

Hidden state is a Launcher discovery preference, not package disablement.

Hide does not inherently uninstall, disable, remove Home placement, remove Dock placement, remove folder membership, remove widgets, or activate App Lock.

Hidden state is profile-qualified. Launcher Settings provides a Hidden apps recovery surface.

Hide != Disable != Uninstall != Lock.

## Profile awareness

Android remains authority for application/profile identity.

The Drawer separates User Apps and Work Apps when applicable. Organizational state including hidden apps, pins, and custom-tab membership uses profile-qualified identities so identical package/component names in different profiles are not treated as one item.

The current Development candidate also gives both identities an explicit Launcher-owned profile badge instead of relying on absence-as-meaning: User apps use a person mark and Work apps use a briefcase mark. The profile mark occupies the **top-left corner of the app or shortcut icon** across Home, Dock, App Drawer, Universal Search, and app-context presentation. Other Launcher marks must use separate slots so pin, notification, and App Lock state cannot displace profile identity. The same profile identity is carried into app-context shortcuts and Universal Search application/shortcut presentation, with explicit **User profile** / **Work profile** accessibility descriptions. Profile badges are presentation-only and do not change Android profile authority, launch identity, package state, App Lock, notification badges, or workspace placement.

## Suggestions, freshness, and local ordering

Local **Most recent** and **Most frequent** ordering use Launcher-local launch history only. Launcher does not request Android Usage Access for these sorts.

Current Development exposes these choices from the header **Filter** action:

- **All** — the current profile/tab inventory.
- **Pinned** — exact profile-qualified Launcher pins.
- **Suggested** — explicit opt-in; bounded local ordering from Launcher-local recent/frequent use; when no truthful usage exists, the fallback is deterministic A-Z rather than fabricated personalization. Disabling the setting removes the Suggested discovery filter and returns an active Suggested view to All.
- **New** — applications whose profile-qualified first-install timestamp is within the bounded freshness window.
- **Updated** — applications with a materially later package update timestamp than first install, when Android exposes package metadata for that profile.

For non-primary profiles, first-install time continues to come from profile-qualified LauncherActivityInfo. Android's public LauncherApps surface does not expose profile-qualified last-update time, so **Updated** currently fails closed for Work and other non-primary profiles instead of borrowing primary-user metadata for a same-package app. Primary-profile update time comes from PackageManager.

The bounded **Pinned / Suggested / New / Updated** Smart Folder views above are implemented Development behavior. The October 6 continuation adds reversible per-app exclusions for Suggested/New/Updated; richer user-authored rule composition and portable recovery remain separately gated and must remain local-first, transparent, explainable, user-controllable, non-sponsored, and manually overridable.

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

Current Development tab and discovery controls expose selected-state semantics, explicit create/edit actions, and a 48 dp interaction floor. Representative-device TalkBack/Switch Access, keyboard/D-pad, large-text, RTL, and form-factor acceptance remains open.

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

The primary Drawer surface remains focused on applications, organization, and search, but the owner-directed header provides a compact five-action toolbar with equal accessible targets:

- **Launcher Settings** — opens Launcher Settings directly.
- **Sort** — opens ordering choices.
- **Filter** — opens All/Pinned/Suggested/New/Updated choices without a permanent chip row.
- **New folder** — starts profile-appropriate Drawer folder management when available.
- **Layout** — cycles the supported Drawer layouts.

A separate close button is unnecessary because the supported downward gesture dismisses Apps. Alphabet navigation is not shown by default; users may explicitly enable it from App Drawer settings.

Hierarchy:

App Drawer -> applications, organization, and compact Drawer controls

Universal Search -> broad search and actions

Edit Home -> Launcher customization

Launcher Settings -> detailed configuration

## Acceptance boundary

Current source remains Development. Exact-head automated validation is necessary but insufficient for release qualification. Representative-device visual quality, TalkBack/Switch Access, keyboard/D-pad, large text, RTL/localization, phone/tablet/foldable, profile transitions, direct placement, performance/power, protected Development signing/update continuity, Release Candidate, Production Acceptance, Stable, Seal, and Anchor remain separate gates.

## Opt-in inline custom tabs

App Drawer Tabs are integrated Development behavior from PR #280, merged to protected main as `142cbe8843041a14c22cc5eba5915b250f953999`. Tabs default to off and are enabled explicitly in Launcher Settings → App Drawer or during optional first-run setup. The All tab, add control and user-created tabs are hidden entirely when disabled. Enabling displays a horizontally scrolling tab strip beside the Apps heading; the old separate row is removed. Local tab names and profile-qualified memberships remain stored while hidden. Existing strict portable v1 backups do not include these settings, so cross-device backup/restore remains pending versioned work. Representative-device layout, large-text, accessibility, protected Development signing/update continuity, and release acceptance remain open.
