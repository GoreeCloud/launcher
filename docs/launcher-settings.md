# GoreeCloud Launcher Settings

Requirement level: Mandatory product direction.

Lifecycle: Development.

Authority boundary: This document defines the intended Settings information architecture and records current Development behavior. It does not establish Release Candidate, Production, Stable, Seal, Anchor, device-support, or signing acceptance.

## Product role

GoreeCloud Launcher Settings is the centralized configuration surface for Home, App Drawer, Dock, folders, Universal Search, widgets and Glaze Cards, gestures, appearance, notification badges, privacy/permissions, recovery, advanced behavior, and product information.

The primary hierarchy is:

1. Home
2. App Drawer
3. Dock
4. Folders
5. Universal Search
6. Widgets & Glaze Cards
7. Gestures & Actions
8. Appearance
9. Notifications & Badges
10. Privacy & Permissions
11. Backup & Restore
12. Advanced
13. About

Settings is searchable. Category rows and compact controls must preserve the Launcher 48 dp interaction floor, meaningful semantics, large-text resilience, reduced-transparency behavior, and current Glaze hierarchy.

## Home

Home settings own primary workspace presentation and behavior.

Current Development controls include:

- 4×5, default 5×6, and 6×7 grid presets.
- Home spacing.
- Glance/clock presentation and alignment.
- Home labels.
- Home page indicator and page transition.
- Optional Launcher quick actions.
- Automatic local recent/most-used Home app modes.
- Add new apps to Home, off by default.
- Launcher hints.
- Layout Lock.

Planned or acceptance-open Home controls include explicit default-page selection, user-facing page-looping policy where applicable, broader padding controls, and complete representative-device page-management acceptance.

Wallpaper selection remains available through Edit Home and Android-supported wallpaper mechanisms. Home remains wallpaper-first.

## App Drawer

App Drawer settings control installed-app browsing and local organization.

Current Development controls include:

- Grid, Compact, List, and Category layouts.
- 4/5/6 columns where applicable.
- Tight/Standard/Relaxed spacing.
- Glaze or Solid Drawer background.
- optional top/bottom local Apps search.
- Browse/Search-ready entry.
- page or scroll navigation and configurable page rows.
- app labels, page indicator, and app count.
- profile-qualified Hidden apps management.
- user-created tabs and App Drawer folders in the Drawer organization model.
- A–Z, Z–A, local most-recent, local most-frequent, pinned-first, and recent-install ordering/discovery capabilities.
- New and Updated local discovery.
- Suggested apps as an explicit opt-in setting, off by default.

Suggested apps use only truthful Launcher-local launch history. Launcher requests no Android Usage Access for this feature. With no valid local history, the fallback is deterministic A–Z rather than fabricated personalization.

Current Development source includes bounded local Smart Folder views for Pinned, optional Suggested, New, and Updated. They are read-only dynamic views and do not mutate manual folders, tabs, Home, Dock, hidden state, App Lock, or package state. Suggested Smart Folder membership follows the same App Drawer Suggested apps control. Manual override/exclusion rules, richer user-authored category rules, portable recovery, and representative-device acceptance remain open.

## Dock

Dock settings control the persistent Home favorites surface.

Current Development controls include:

- Glaze, Clear, Solid, Raised, and Edge material treatments.
- preferred 4–7 items per page.
- adaptive paging before violating the 48 dp interaction floor.
- optional looping.
- optional labels.
- optional Universal Search affordance.
- independent horizontal Dock page navigation.

The starter Phone, Messages, Mail, Browser, Camera preference remains a first-run seed rather than a capacity limit.

Dock folders, non-app Dock items, richer edge/padding controls, and complete versioned backup/recovery remain open work.

## Folders

Folder settings apply to Launcher-owned folder organization and presentation.

Current Development controls include folder management plus icon preview layout, shape, size, material/background, and outline behavior.

Home and App Drawer folder identity must remain distinct from Android package state. Smart folders and large-folder direct-launch behavior remain planned where not yet implemented.

## Universal Search

Universal Search is a first-party Launcher capability and remains useful without a GoreeCloud account, GoreeCloud Search, GoreeCloud Index, or network access.

Current Settings control Home Search presentation through Swipe down, Movable, fixed Top, or fixed Bottom placement plus applicable bar material.

Search-source authority remains explicit:

- installed applications and Launcher actions are core local sources;
- shortcuts and other local sources use their applicable Android contracts;
- Contacts, Call history, Messages, and selected files remain opt-in and permission/authorization gated;
- connected providers require explicit enablement and authorization;
- typing in a local field must not automatically fan out to an unenabled connected provider.

Search appearance and source-access authority must remain conceptually separate even when surfaced in the same product area.

## Widgets & Glaze Cards

Current Development source hosts third-party widgets through Android AppWidgetHost and includes Launcher-owned utility widgets such as Glance, Universal Search, Quick actions, Battery, calendar/date/time surfaces, and Launcher Status.

Current widget placement and management remains primarily under Edit Home → Widgets.

Glaze Cards are an approved optional direction for useful Launcher-owned information such as calendar context, tasks, device status, favorite contacts, weather, media controls, and approved GoreeCloud integrations. Cards must not become advertising, sponsorship, affiliate placement, or paid ranking.

## Gestures & Actions

Current Development source supports configurable Home gestures including swipe directions and double tap, plus app-launch actions. Tap-and-hold on empty Home remains reserved for Edit Home so customization is always reachable.

Launcher Settings itself is not a configurable gesture action. Edit Home → Settings remains the stable navigation path.

Additional platform-safe gesture/action combinations such as notifications, Quick Settings, pinch gestures, two-finger gestures, and approved screen lock remain gated by Android authority, accessibility, and runtime acceptance.

## Appearance

Appearance is governed by the first-party Glaze Theme Engine.

Current Development controls include:

- Theme Manager appearance modes.
- wallpaper shading.
- icon scale.
- icon shape/adaptive masking.
- compatible Android icon packs.
- Launcher-owned presentation materials for Drawer, Dock, folders, Search, and other surfaces.

Current app-icon rendering normalizes supported adaptive/legacy artwork before applying the selected Launcher mask.

Future appearance expansion includes wallpaper-derived palette integration, broader typography controls, inspectable theme presets/export, and complete reduced-effects/device capability acceptance.

## Notifications & Badges

Notification badges are optional and disabled unless enabled by the user.

Android notification-listener access remains separately granted by Android. Launcher uses this access only for minimized per-app/profile badge state and does not claim to replace Android notifications.

Badge style, size, and corner placement are Launcher presentation choices. Core Dock/Home/App Drawer behavior must never require notification-listener access.

## Privacy & Permissions

Privacy & Permissions centralizes Launcher-specific local-data and optional-access explanations.

Current Development controls include:

- App Lock management.
- Hidden apps management.
- clear Launcher-local app activity/history.
- visibility of whether Suggested apps are enabled.
- explicit notification access under Notifications & Badges.
- Search-source/provider controls under Universal Search.

Core Launcher operation requires no GoreeCloud account or network connection.

GoreeCloud Launcher must contain no advertising network, sponsored application placement, affiliate ranking, behavioral tracking, or monetized search ranking. Installed-app inventory, workspace state, folder state, Search preferences, themes, and Launcher-local usage-derived signals remain local by default.

App Lock is a Launcher-originated launch gate, not a system-wide package blocking boundary.

## Backup & Restore

Launcher contains versioned local portability and restore-recovery foundations. The strict preference format remains goreecloud-launcher-preferences/1, with fail-closed validation and journaled recovery safeguards.

A complete end-user workflow for creating, exporting, importing, previewing, restoring, and resetting all newer Launcher state is not yet complete. In particular, newer Dock-page, custom Drawer organization, folder/widget binding, Search-provider, and Glaze state require explicit versioned adoption rather than silent expansion of the existing format.

Backup and restore must never guess through incompatible state.

## Advanced

Advanced contains controls that should not crowd everyday configuration.

Current Development behavior includes:

- Default Home role status/action.
- replayable first-use Launcher setup.
- local Development diagnostics/status.

Compatibility and experimental controls must be clearly identified and must never be presented as Stable merely because they exist.

## About

About identifies the product and lifecycle boundary.

Current Development presentation records:

- GoreeCloud Launcher.
- Development build channel.
- GPL-3.0-only license.
- GoreeCloud Privacy Shield.
- Wardveil Security by GoreeCloud.

Installed-version/build identifiers may be surfaced when wired to authoritative runtime build metadata. Static documentation must not invent a version value.

## Accessibility and adaptive behavior

Settings must remain usable with TalkBack, Switch Access, keyboard/D-pad where applicable, large text, reduced motion, reduced transparency, sufficient contrast, and clear focus order.

Phone, tablet, landscape, foldable, and large-window compositions must adapt intentionally rather than merely stretching a phone layout.

Important capability must not depend only on a gesture. Settings must preserve non-gesture alternatives for organization and configuration.

## Acceptance boundary

This Settings architecture is Development work. Source/build/runtime CI is necessary but does not establish representative physical-device visual quality, TalkBack/Switch Access, keyboard/D-pad, large-text, RTL/localization, phone/tablet/foldable, performance/power, protected Development signing/update continuity, complete portable recovery, Release Candidate, Production, Stable, Seal, or Anchor acceptance.

## Pending App Drawer Tabs and glyph refinement (draft PR #280)

App Drawer Tabs default off. Settings and onboarding allow explicit opt-in. The enabled horizontal tabs sit beside Apps; disabling hides the entire control strip without deleting existing tab names or memberships. The v1 portable preference snapshot cannot yet export/import the tab settings and definitions.

The thirteen category icons use the shared `LauncherOutlineGlyph` Canvas/vector family, a uniform rounded stroke and theme-derived colors. Draft PR #280 refines Dock, Folders, Gestures, and Advanced geometry. Remaining submenu vector and device/theme/font/density acceptance are open. This candidate is not on protected main or in the previously delivered APK.


## Universal Search profile and provider corrections (draft Development candidate)

The stacked Development source candidate for owner screenshot feedback introduces the following source changes, which remain **unmerged and not delivered as an APK**:

- Newly Installed/Updated, Frequent, and Recent suggestion icons now show the same Android User/Work profile badge as other app and shortcut Search results. App keys and de-duplication remain profile-qualified. The freshness loader uses each non-primary profile's LauncherActivityInfo install timestamp rather than applying the calling user's package timestamp to Work apps.
- The Bing and Microsoft Copilot Search provider identities and registrations are retired; persisted provider IDs are excluded from active source controls by the existing known-provider normalization. Ordinary locally installed Microsoft apps are not affected.
- Dropbox's Search Sources availability is derived from Android-authorized LauncherApps inventory across accessible User and Work profiles. An app visible only in Work is recognized even when PackageManager cannot resolve its search intent from User. Work-only activation uses LauncherApps to open the authorized profile's Dropbox app, without bypassing profile policies or claiming to pass the query. An unseen app is described as **not visible**, never definitively absent.
- Search Sources distinguishes unconfigured OAuth signing, missing handoff support, profile visibility, and external app/browser behavior. Provider settings re-evaluate when the LauncherApps inventory updates.
- Direct API Answers already supports explicitly configured, credentialed inline OpenAI/Anthropic/Gemini/Perplexity/custom APIs in a separate user-controlled panel; it does not imply that provider-app handoffs offer authenticated inline integration.

**Unfinished:** A reviewed Dropbox OAuth adapter and provider-supported authenticated inline adapters for other app-only services are not part of this code change. No OAuth client, redirect registration, provider credentials, user consent, token refresh/revocation or real provider API test results are claimed. Android 16 instrumented User/Work/Shelter/quiet-profile tests, physical UI/accessibility testing, and all exact-head CI gates remain required before merge or release. On profiles blocked by enterprise policy, Launcher cannot infer whether the package is physically installed.
