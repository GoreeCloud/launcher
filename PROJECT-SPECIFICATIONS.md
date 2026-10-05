# GoreeCloud Launcher — Project Specifications

> **Current repository authority — September 29, 2026:** GoreeCloud Launcher is maintained in `GoreeCloud/android-app-defaults` under `apps/launcher/`. The standalone `GoreeCloud/launcher` repository is a temporary legacy source pending final reconciliation and required deletion. The mandatory cutover imported exact Development revision `1eb8dd6d8178f6d730b100559e8f8d149501d18c` and merged into monorepo `main` as `20ca33c7c0c565f361deda206c9c950154dec327`. This repository-location statement supersedes legacy repository or “current candidate” wording preserved below when that wording predates the cutover. Historical legacy PR/commit references remain valid evidence, but new project-specification maintenance belongs here.


**Repository:** `GoreeCloud/android-app-defaults` (`apps/launcher/`)  
**Project type:** Native Android launcher application  
**Repository lifecycle declaration (legacy Contract 0.4):** `development`; canonical Contract 2.0 lifecycle reclassification remains pending and must be evidence-backed  
**Repository visibility:** Public  
**Default branch:** `main`  
**Migration baseline:** `c853fa115a964c3cff531af555039f2e3cdd944e`  
**Primary implementation:** Kotlin, Jetpack Compose, Android launcher APIs  
**License:** GPL-3.0  
**Target platforms:** Supported Android phones and tablets  
**Deployment model:** Independently installed Android HOME application; core launcher use remains local/offline-capable and does not require a GoreeCloud account or server  
**Canonical authority:** This file is the authoritative project specification once accepted on the default branch.

## Migration and precedence

This file consolidates the former repository `SPECIFICATIONS.md` and the current verified Google Drive migration source **Project Specification — Launcher.docx** (Drive file `1wtNYhNNN7zwHcT8d01m5lLXnIDYraBt7`, v0.65, last updated September 27, 2026). That DOCX replaced the retired native Google Doc source `19wyxWRf-b-pKMF2vb_ogbCadtWRt1h2B5wupIqLaN3I`; the repository migration must use the verified replacement rather than the retired source.

The first body below is the verified repository-native specification that existed on the migration baseline. It controls current architecture, current implementation boundaries, and current lifecycle interpretation. The later **Migrated Drive requirement provenance** appendix preserves still-material Drive requirements and approved-scope language so migration does not silently discard obligations. Where a migrated Drive statement conflicts with verified repository state, newer repository evidence controls and the older statement is historical/superseded rather than a current implementation claim.

Implementation state remains evidence-backed through `IMPLEMENTED-FEATURES.md`, `PLANNED-FEATURES.md`, `CHANGELOGS.md`, tests, CI, pull requests, commits, releases, and accepted runtime evidence. Source/build success alone does not establish production, release, or Stable acceptance.

## Current canonical specification

## Status

**Active Development. Not Stable. Production/release acceptance is incomplete.**

GoreeCloud Launcher is GoreeCloud's first-party native Android launcher and the intended home, application-navigation, personalization, contextual-access, and native Universal Search experience for GoreeCloud devices. Current Development source contains the first Launcher-owned Universal Search foundation; earlier GoreeCloud Index handoff behavior is preserved only as exact-revision historical evidence.

This repository specification describes both current implementation architecture and approved product direction. [FEATURES.md](FEATURES.md) maintains the detailed capability inventory. A target capability is not an implementation or acceptance claim unless repository evidence separately establishes that state.

## Product role

Launcher is intended to serve as the personalized front door to GoreeCloud: applications, files, people, devices, search, information, services, privacy controls, security state, continuity features, and contextual actions in one adaptive interface.

Universal Search presented from Launcher is a first-party Launcher capability. Launcher owns its search UI, provider framework, core local indexing required for the Launcher experience, result aggregation/ranking, permission-aware filtering, categories, recents/history, commands, shortcuts, and contextual actions. GoreeCloud Search and GoreeCloud Index may later enhance this experience as optional providers/backends once stable, but neither is required for core Launcher Universal Search.

Launcher intelligence and personalization should remain transparent and user-controlled.

## Native application requirement

Launcher must remain original GoreeCloud-owned software built from the ground up as a native Android application. Android platform APIs and narrowly justified foundational dependencies may be used where direct operating-system integration, standards compatibility, security, interoperability, rendering, or maintainability requires them.

## Current architecture

- Kotlin + Jetpack Compose with Android-native APIs where launcher contracts require them.
- Android HOME-role onboarding remains user-controlled through platform role authority.
- `LauncherApps` remains authoritative for launchable-activity discovery and profile-aware application identity.
- Android package visibility is scoped to launchable `MAIN` + `LAUNCHER` activities. The legacy GoreeCloud Index search-action query is removed from current source, and broad `QUERY_ALL_PACKAGES` access is not used.
- Home, Apps, and Launcher Settings are separate product surfaces.
- Presentation and Launcher-policy preferences are persisted locally with DataStore.
- Workspace persistence and placement use the guarded Room-backed workspace model for terminal Room paths.
- Rendered paged Home state is projected from authoritative workspace state; UI convenience is not a second placement authority.
- Home layout lock is a Launcher mutation policy layered over the authoritative workspace APIs; it is not a workspace persistence authority.
- Current Development source routes Home search directly into the Launcher-owned Universal Search surface and includes the first native provider/result aggregation layer. Historical Index action-contract revisions remain provenance only.
- GLAZE UI V1.6 / 1.6.0 is the current required design-system target for Launcher. Integrated source-bearing PR #128 maps authoritative repository source to exact Stable release source `a7180679ea851389e0f3004515f9a25f420e716d` and adds first-party V1.6 presentation-policy primitives. Fresh rendered/accessibility/adaptive/device/performance/rollback/release acceptance remains required; source mapping alone is Development evidence only.
- Privacy, security, continuity, identity, design, and cross-service responsibilities remain separated into applicable GoreeCloud platform-system boundaries.

## Current daily-launcher shell

### Home

The rebuilt primary Home is a launcher-style surface rather than an engineering Favorites screen. Android renders the system wallpaper behind the launcher window through the native window-wallpaper contract, requiring no wallpaper/storage privilege. The primary surface renders the current Home application grid, Dock, Apps affordance, Launcher Settings affordance, and—when the selected entry mode is **Permanent on Home**—a Search with GoreeCloud… affordance that opens Launcher Universal Search.

A one-finger downward gesture on the unobstructed Home search zone opens Launcher Universal Search in both supported Home-entry modes. **Swipe down only** removes the persistent Search with GoreeCloud… affordance while retaining the Launcher-owned search gesture.

Current supported settings include Home grid presets within the 4–6 column / 4–7 row bounds exposed by the UI, Apps layout modes of Grid/Compact/List, Apps columns of 4/5/6 for grid-based modes, Small/Medium/Large icon presentation, app-label visibility, System/Light/Dark appearance, Home layout lock, and Launcher Universal Search Home-entry mode.

When layout lock is enabled, current Favorite, Dock, primary Home cell placement, Home-page create/delete/reorder, secondary-to-secondary movement, and current secondary spatial mutation callbacks are blocked at the Launcher composition boundary. App launching, Home page selection, navigation, and non-placement presentation settings remain usable. Primary placement-dialog mutation controls are disabled while locked.

The current locked-state Home UI provides an intentional five-second hold control with visible progress. Completing the hold disables the persisted lock. Launcher Settings remains the deterministic non-gesture unlock path. Representative physical-device hold/gesture/accessibility acceptance remains separately gated.

The primary `WorkspaceLegacyImportMapper.HOME_PAGE_ID` page remains protected at HOME rank zero. Before spatial activation its canonical Favorites rows may retain null grid coordinates; under terminal Room authority a guarded exact-snapshot migration can convert those rows to bounded authoritative coordinates for the configured Home grid. Direct primary Home drag then persists empty-cell placement or occupied-cell swaps through the same Room authority.

### Apps

The Apps surface displays the launchable inventory provided through `LauncherApps`, supports user-selectable Grid, Compact, and List presentation modes, supports a narrow local filter by label/package, and launches selected applications. Grid and Compact consume the existing bounded column setting; List is a single-column row presentation. Long-press opens current placement management. Home page controls do not overlay the Apps surface.

The Apps filter is a specialized Launcher-owned view backed by the installed-app Universal Search provider. Android `LauncherApps` remains inventory authority, and the drawer remains narrower than the full Universal Search surface. While layout lock is enabled, placement management may still be opened to explain state, but current placement mutation controls are disabled.

### Launcher Settings

Launcher Settings is a distinct scrollable surface. Current persisted settings include Home grid, Apps layout (Grid/Compact/List), Apps columns, app-label visibility, icon-size presentation, System/Light/Dark appearance, **Lock Home screen layout**, and the Launcher Universal Search **Permanent on Home / Swipe down only** entry choice. Drawer layout remains local presentation state outside the strict seven-field `goreecloud-launcher-preferences/1` backup/recovery format until a separately versioned portability extension is accepted.

Most settings remain presentation/policy state. Home-grid changes are the bounded exception after primary spatial activation: Launcher validates or safely reflows authoritative primary Home coordinates through the existing Room mutation boundary before persisting a smaller/different grid. The layout-lock preference restricts Launcher mutation dispatch; the Universal Search entry preference controls Launcher-owned invocation presentation only.

The native Theme Manager catalog/surface and direct persisted appearance-selection API are composed into Launcher Settings under the integrated V1.6 source mapping. Settings uses a saveable/fail-closed sub-destination model; stale destination values restore to Settings root. The current appearance is represented as non-actionable selected state, and only a different supported appearance invokes caller-owned persistence. V1.6 presentation context can simplify raised material when Reduced Transparency or constrained performance is authoritatively supplied, while broader runtime profile wiring and complete Glaze Theme Engine behavior remain separately implementation- and acceptance-gated.

Launcher Settings must also provide explicit local/offline-capable **Backup Launcher configuration** and **Restore Launcher configuration** actions. Backup/restore remains separately implementation- and acceptance-gated.

## Home layout lock behavior and boundary

The current layout lock protects every placement-changing path presently implemented by the Launcher composition layer: primary Favorite/Dock membership and ordering, primary Home cell placement, Home page creation/deletion/reordering, secondary-to-secondary application moves, and current within-secondary-page spatial movement. UI controls are disabled where practical, and the underlying callbacks are also gated so a stale or missed presentation control cannot dispatch a mutation while the current preference is locked.

Normal application launching, page selection, search invocation, Apps navigation, Settings access, and presentation settings remain available because they do not mutate authoritative workspace placement.

The deterministic accessible unlock path is the Launcher Settings switch. The current Home also exposes an intentional five-second hold on its locked-state control with progressive feedback. Releasing early cancels the hold. The gesture does not require a privileged accessibility service and is not the only unlock path.

Folders, shortcuts, widgets, and other placeable item types are approved target scope but not yet implemented. The lock policy must extend to those mutation paths when they become real; current source must not be represented as runtime coverage for item types that do not yet exist.

## Theme Manager and icon presentation

Launcher now contains a bounded native Theme Manager for the currently implemented System/Light/Dark appearance modes. The Theme Manager is reachable from Launcher Settings through the saveable Settings destination host, provides the application-owned mode catalog and Compose preview presentation, and delegates persisted mode selection through the existing local theme repository. The selected mode is state rather than another action, so only a different appearance choice may call persistence.

The approved broader target includes applicable Glaze Theme Engine modes, richer previews, wallpaper-derived/user-selected palettes where supported, icon-pack discovery/application, icon masking, bounded icon scaling/optical normalization, fallback/reset behavior, and coherent styling across Launcher-owned surfaces.

Third-party application identity must remain recognizable. Icon packs, masks, frames, normalization, and scaling may adapt presentation but must not misleadingly replace third-party brand identity, crop essential icon content, or bypass the current GoreeCloud/Glaze UI icon standards.

## Launcher configuration backup/restore target

Launcher Settings must expose explicit backup and restore actions using a versioned documented format. Core backup/restore must remain local/offline-capable and must not require a GoreeCloud account or network service. Imported backup data is untrusted input and must be structurally/version validated before any mutation; malformed or unsupported data must fail safely.

Backup scope should include supported Launcher preferences, Home/Dock/page organization, Theme Manager and icon-presentation choices, Universal Search Home-entry mode, layout-lock state where appropriate, folders/shortcuts, and other Launcher-owned configuration. Widget restoration must use safe rebinding/reconfiguration semantics rather than copying stale Android AppWidget IDs. Everkeep, GoreeCloud Backup, Drive, and Sync may later transport/preserve the same validated format when separately authorized and accepted.

## Current multi-page Home behavior

Current Development source supports:

- authoritative Home page rendering and selection;
- compact/lazy page navigation with authoritative accessibility context;
- guarded page creation, eligible empty-secondary deletion, and secondary page reordering without crossing protected primary rank zero;
- app launching from supported secondary pages;
- ordinary icon-grid rendering for secondary pages rather than permanent engineering controls;
- long-press management for secondary movement actions;
- secondary-to-secondary page movement;
- within-secondary-page nearest-free-cell earlier/later movement;
- guarded exact one-cell left/right/up/down movement;
- fail-closed movement for collisions, invalid bounds, malformed/ambiguous placement, or stale snapshots;
- guarded primary compatibility-to-spatial migration and within-primary-page cell placement while preserving primary rank zero;
- canonical primary/Dock validation before secondary spatial writes; and
- a Launcher-level layout-lock gate that prevents these implemented page/item mutation calls from being dispatched while locked.

Primary↔secondary spatial item movement remains separately gated; current primary spatial authority is within-page only.

## Native Universal Search architecture and optional Search/Index integration

### Authority model

**GoreeCloud Launcher owns Universal Search.** Launcher owns the user-facing search UI, query/result orchestration for the Launcher experience, native provider framework, core local indexing required for that experience, result aggregation and ranking, permission-aware source filtering, categories, recents/history, commands, shortcuts, contextual actions, and standardized provider APIs.

Core Launcher Universal Search must remain functional when GoreeCloud Search and GoreeCloud Index are absent, disabled, unavailable, or not production-ready. Search and Index may enhance Launcher later, but neither service is a prerequisite or authority owner for core Launcher Universal Search.

Android remains authoritative for installed/launchable application identity through `LauncherApps` and the applicable profile/package contracts. Launcher Universal Search must consume authoritative source contracts rather than inventing competing application identity.

### Search as a discovery and action layer

Universal Search is both a discovery surface and an action surface. Depending on source capability and authorization, results may expose actions such as:

- launch an application or invoke an application shortcut;
- open, share, move, or inspect a supported file/resource;
- open a person/profile and invoke an approved communication action;
- navigate directly to a Launcher or supported system setting;
- execute a GoreeCloud command or registered provider action;
- invoke a contextual workspace action; and
- resolve an appropriate natural-language request where a separately accepted provider supports it.

External intents, deep links, provider payloads, command definitions, and cross-application records are untrusted input and require validation before action dispatch.

### Native provider architecture

GoreeCloud applications and platform services should register searchable resources, actions, shortcuts, commands, and supported metadata through versioned Launcher provider contracts. Provider contracts should define, as applicable:

- provider identity and source provenance;
- supported resource/result types;
- permission and authorization requirements;
- searchable fields and query capabilities;
- result and action schemas;
- ranking metadata and freshness semantics;
- offline behavior and cache/index policy;
- privacy, minimization, retention, and remote-processing boundaries;
- cancellation, timeout, failure isolation, and partial-result behavior; and
- compatibility/versioning and migration behavior.

Launcher must fail soft around optional providers: one provider failure must not disable core Universal Search.

### Core searchable capability scope

Launcher Universal Search is intended to discover and act on, where authorized and implemented:

- applications and services;
- files, folders, and documents;
- people, teams, and organizations;
- Launcher settings and supported system controls;
- application actions and shortcuts;
- GoreeCloud commands;
- recent and frequently used resources;
- cloud resources;
- connected services;
- contextual workspace resources; and
- AI-assisted answers/actions where appropriate and separately governed.

Work/private-profile isolation, user policy, source authorization, and privacy controls must prevent cross-profile or unauthorized result leakage.

### GoreeCloud Search boundary

GoreeCloud Search remains a separate first-party search/retrieval system. Once stable, it may integrate with Launcher as an optional advanced search/query/discovery provider or backend for capabilities such as advanced content retrieval, full-text search, cross-service search, semantic retrieval, query interpretation, advanced ranking, federation, filters, and operators.

GoreeCloud Search does not become required for basic Launcher search. Local files, photos, contacts, application inventory, Launcher history, and unrelated local result payloads must not be uploaded merely to produce local results.

### GoreeCloud Index boundary

GoreeCloud Index remains a separate scalable indexing/retrieval system. Once stable, it may integrate with Launcher as an optional provider/backend for large-scale resource indexing, metadata/content catalogs, semantic indexing, cross-service indexing, high-performance retrieval, and background indexing pipelines.

GoreeCloud Index does not own Launcher Universal Search and must not be required for the core Launcher experience.

### Long-term layered responsibility

- **Launcher** owns user-facing Universal Search, search UI, aggregation, actions, commands, shortcuts, provider framework, and the core local search/index path required for Launcher.
- **GoreeCloud Search** may provide advanced search, retrieval, query processing, and discovery capabilities.
- **GoreeCloud Index** may provide scalable indexing, metadata/content catalogs, and retrieval infrastructure.

The controlling architecture principle is: **GoreeCloud Launcher owns Universal Search. GoreeCloud Search and GoreeCloud Index enhance Launcher once stable, but Launcher must not depend on either for core Universal Search.**

### Current Development handoff provenance and migration boundary

Historical Development revisions include an explicit Index handoff through `com.goreecloud.index.action.SEARCH`, production package `com.goreecloud.index`, Development package `com.goreecloud.index.dev`, persisted Index-oriented Home-entry terminology, and tests/PR evidence that previously treated Index as universal provider/ranking authority. Current source removes that mandatory activity handoff and package-visibility query, routes Home search directly to Launcher, and preserves the legacy v1 `index_home_mode` persistence/wire key only for backup/recovery compatibility.

Those records remain truthful for the exact revisions that implemented them. They are historical implementation evidence, not the controlling architecture. The first source-bearing migration tranche implements the Launcher provider/result foundation for installed apps, removes the mandatory Index handoff, updates active search terminology, preserves strict v1 backup compatibility, and routes Home entry directly into Launcher search. Broader providers, actions, categories, commands, shortcuts, history/recency, contextual resources, and optional Search/Index backends remain open.

Launcher PR #53 and PR #57 remain historical evidence for the original Index handoff and persisted entry modes. Later PRs, including the local installed-app fallback work, also remain provenance. None of that evidence by itself establishes completion of the Native Universal Search migration, representative-device acceptance, Release Candidate qualification, production readiness, or Stable status.

## Official Launcher product identity specification

Launcher requires a unique first-party visual identity distinct from framework defaults, upstream products, and the generic GoreeCloud platform/corporate logo.

All canonical Launcher logos, icons, symbols, illustrations, and artwork must be stored, reviewed, and approved in **`GoreeCloud/goreecloud-branding-assets`**. The current canonical Launcher source is `products/launcher/app-icon.svg`. `GoreeCloud/android-app-defaults` (`apps/launcher/`) is the current consumer repository and may carry only traceable synchronized/generated/packaged Android derivatives required for the application.

`branding/provenance.json` records the canonical repository/path/blob used to create the current Android adaptive, round, and monochrome derivatives. A consumer derivative must never be edited into an independent canonical source. Future visual revisions begin in `goreecloud-branding-assets`, then propagate through a traceable derivative update.

The previous Launcher-local portal/activity-tile source is superseded and removed because it conflicted with the project-wide branding source-of-truth rule.

Launcher PR #55 reconciled this authority at exact head `a9ced7e136cf02aa0f9c1301df4a6407c6999fa2` and merged as `d845803e0a7af88c8394602a5545c44193ed7ad7`. PR-head run `33419656151` and push-triggered main run `33420478729` both passed validate and Android 16/API 36 runtime-emulator jobs.

The existence of a canonical asset and synchronized Development derivatives does not by itself establish production visual-identity acceptance. Rendered small-size, adaptive-mask, themed-icon, representative-device/system-chooser, and release review remain required.

## Approved product capability domains

Detailed approved features are maintained in [FEATURES.md](FEATURES.md). Major domains include customizable Home/workspace, rich Apps organization, Launcher-owned native Universal Search with optional future GoreeCloud Search and GoreeCloud Index provider/backend integration, native Theme Manager/icon packs/masking/scaling, Home layout locking, configurable Universal Search Home entry, appearance/personalization, gestures, contextual experiences, feeds/cards, notifications, folders, widgets, adaptive Dock/layout/motion, application management, versioned local backup/restore plus optional continuity transports, and applicable GoreeCloud platform integrations.

## GoreeCloud platform integration boundaries

Naming an integration establishes no implementation claim. Each participating system must satisfy its own implementation, authorization, privacy, security, availability, and acceptance boundary.

- **GoreeCloud Launcher Universal Search** owns the user-facing Universal Search experience and provider framework. **GoreeCloud Index** may later provide optional scalable indexing/retrieval infrastructure without owning Launcher search authority.
- **Glaze UI / Design Center** governs visual hierarchy, components, motion, responsiveness, accessibility, adaptive layouts, wallpaper-aware presentation, and design-system acceptance.
- **Privacy Shield / Privacy Center** governs personalization signals, sensitive search sources/results, history exposure, consent, location/usage-derived behavior, and user control.
- **Wardveil Security / Security Center** governs applicable package trust, risky cross-application actions, security-state surfaces, and protection of Launcher configuration/search integration state.
- **Everkeep / Continuity Center** governs accepted preservation, backup/recovery, portability, and device-transition continuity.
- **GoreeCloud Identity** governs profiles, authentication/authorization, managed application visibility, and identity-aware continuity.
- **GoreeCloud Mesh** governs authorized cross-device coordination, device awareness, handoff, connected-device results/cards, and coordinated Launcher state.
- **GoreeCloud Drive** may provide authorized recent/searchable files/folders through Launcher provider contracts plus launcher shortcuts/widgets; an optional future Index backend may accelerate retrieval.
- **GoreeCloud Search** may provide optional advanced search/retrieval, Web/current-information, query-processing, or federated discovery capabilities through Launcher provider/backend contracts without becoming required for core Launcher search.
- **Sync/Backups/Location/Maps/Mail/Messenger/Calendar** may provide their approved integrations only where substantively implemented and accepted.

## Authority and privacy principles

- Android remains authoritative for installed/launchable applications, platform roles, and operating-system launcher capabilities.
- GoreeCloud Launcher remains authoritative for the user-facing Universal Search experience, provider framework, core local search/index path, aggregation, ranking, actions, commands, and shortcuts. GoreeCloud Search and GoreeCloud Index are optional future enhancers once stable.
- `GoreeCloud/goreecloud-branding-assets` remains authoritative for all GoreeCloud logos/icons/artwork; consumer repositories carry derivatives only.
- GoreeCloud workspace persistence maintains one accepted placement authority at a time.
- Home layout lock is a Launcher dispatch/mutation policy and must not create a second writable placement source of truth.
- Compatibility and secondary spatial models must not be mixed in ways that invalidate authority/recovery.
- Cross-device continuity must not create ambiguous writable workspace authorities.
- Search/personalization signals should remain transparent and user-controlled.
- Sensitive content must not be exposed through search/cards/widgets/notifications without applicable authorization/privacy policy.
- Platform integration must be substantive; visual labels do not prove integration.
- Core Home and current Apps behavior remain offline-capable and currently request no Android `INTERNET` permission.

## Stable blockers

Stable qualification still requires, as applicable:

- accepted production Launcher identity artwork and derivative asset pipeline from the canonical branding repository;
- accepted Launcher Native Universal Search core behavior, provider framework, migration from legacy Index-oriented handoff/terminology, and any optional release-intended Search/Index provider integrations;
- accepted native Theme Manager/icon-pack/masking/scaling behavior for release scope;
- accepted Home layout-lock enforcement and accessible unlock behavior across all release-supported placeable content and representative devices;
- accepted configurable Universal Search Home-entry modes and non-gesture accessibility behavior;
- accepted versioned Launcher backup/restore behavior and safe migration/rebinding semantics;
- complete intended workspace/user flows and recovery semantics;
- accepted primary spatial placement behavior and complete intended cross-page movement semantics;
- folders/widgets/shortcuts required by release scope;
- mature cross-page placement editing and accessible alternatives;
- representative-device, rotation/posture, performance, physical-interaction, universal-search gesture, five-second unlock, and accessibility acceptance;
- complete current Glaze UI application acceptance for the integrated GLAZE UI V1.6 / 1.6.0 Stable source mapping;
- accepted applicable Privacy Shield, Wardveil Security, Everkeep, Identity, Mesh, optional Index/Search providers, Sync, Backup, and continuity integrations;
- Android process-death and schema-upgrade recovery evidence;
- signed distribution and upgrade/recovery validation; and
- release/production evidence supporting every capability represented as implemented.

# Migrated Drive Requirement Provenance

The following sections preserve normative/current/future requirement material from the former Drive specification. They are retained for content-preservation and traceability. Historical milestone/candidate evidence from the same Drive source is stored in `PROJECT-RECORD.md`.

## Drive Section 1 — Project Definition

I am developing GoreeCloud Launcher as an original Android home-screen replacement and application launcher. I will use it as the primary launcher experience on supported Android phones and tablets while preserving Android compatibility, user choice, privacy, security, accessibility, and device performance.


I will not fork Nova Launcher, Pixel Launcher, Samsung One UI Home, or another proprietary launcher. I will use mature launcher interaction patterns as design references while implementing GoreeCloud-owned source code, architecture, product behavior, settings, theme logic, and Glaze UI presentation.

## Drive Section 2 — Role and Purpose

Role: Private, customizable Android home-screen and application-launching environment.


I will use GoreeCloud Launcher to provide:
- A fast, stable home screen.
- A customizable application drawer.
- Folders, dock pages, widgets, shortcuts, and search.
- A first-party theme engine.
- Strong privacy and security defaults.
- A consistent Glaze UI Android experience.
- Local backup and restore of launcher configuration.
- No advertising, sponsored suggestions, affiliate placement, or monetized ranking.

## Drive Section 3 — Product Principles

I will design the launcher according to these principles:
- The launcher remains useful without a network connection.
- Core launcher behavior never requires a GoreeCloud server or cloud account.
- User layout, folders, search history, favorites, themes, and usage-derived suggestions remain local by default.
- No advertisement SDK, sponsorship SDK, analytics SDK, tracking SDK, remote font, or remote icon runtime is required.
- Search and recommendation features must not sell, share, or monetize launcher behavior.
- Every permission must have a documented launcher-specific purpose.
- User data should be exportable, restorable, and removable.
- The launcher must remain usable when optional intelligence, integrations, or online services are unavailable.

## Drive Section 4 — Platform Architecture Decision

I will build the primary launcher as a native Android application using Kotlin and Jetpack Compose with platform-native Views where Android launcher contracts require them.


I am not using Flutter as the primary implementation because this application must integrate deeply with Android HOME-role behavior, LauncherApps, application shortcuts, widgets, AppWidgetHost, package/profile lifecycle events, wallpaper APIs, drag-and-drop, launch animations, and other Android-specific system contracts. Cross-platform abstraction would not provide meaningful value for a product whose Role and Purpose is specifically to replace the Android home screen.


The project will target Android 16 / API level 36 as the current target baseline and will select a practical minimum Android version after compatibility testing. Initial planning baseline: minSdk 29, targetSdk 36, compileSdk 36, subject to build-time verification before release.

## Drive Section 5 — Android Launcher Core

The application will declare an Android HOME activity and support the user-controlled default-launcher selection flow.


Core platform services will include:
- HOME-role onboarding and default-launcher status.
- LauncherApps-backed application discovery.
- LauncherApps callbacks for package and profile changes.
- App shortcut discovery, pinning, and launching.
- AppWidgetHost-based widget hosting.
- Work-profile and supported private-profile awareness.
- Package install, update, archive, suspend, enable, disable, and uninstall state handling where exposed by Android.
- Safe activity launching with source bounds and animation support where available.
- Wallpaper-aware rendering and system appearance integration.

## Drive Section 6 — Home Screen

The home screen will support:
- Multiple pages.
- Default Home grid: 5 columns × 6 rows, with supported user-configurable grid presets retained.
- Independent portrait and landscape behavior where useful.
- App icons, pinned shortcuts, folders, widgets, and first-party launcher cards.
- Direct drag, drop, reorder, remove, resize where applicable, Home-to-Dock/Dock-to-Home movement, and App Drawer copy-to-Home/copy-to-Dock interactions.
- Optional page looping.
- Default Dock: up to five applications, preferring Phone, Messages, Email/Mail, Browser, and Camera when matching launchable applications are available.
- Dock search affordance.
- Page indicator styles.
- Icon labels on/off and label size controls.
- Configurable home-screen padding.
- Lock-layout mode to prevent accidental changes.
- Long-pressing empty Home must enter Edit Home; Edit Home exposes Wallpaper, Widgets, Pages, Apps, and Launcher Settings. Direct Launcher Settings entry from the App Drawer is prohibited.
- On a default installation, up to 10 starter applications occupy the bottom two Home rows directly above the Dock. Existing Launcher-local aggregate launch counts may rank them when available; a true first install with no truthful usage history must use a deterministic common/GoreeCloud fallback rather than requesting Android Usage Access or fabricating usage.
- Settings may offer “Add new apps to Home”; it must be disabled by default, must not backfill the existing installed-app inventory when first enabled, and must preserve user placement authority.
- Long-pressing a Home application enters visible edit mode with grid/edit affordances; icon context actions disappear while that application is actively being dragged.
- Launcher includes at least four high-quality GoreeCloud/Glaze wallpapers accessible from Edit Home → Wallpaper, with Android/system wallpaper selection retained as an explicit fallback.
- Double-tap, swipe, pinch, and configurable gesture actions.
- Optional left-side Glance surface for local cards and shortcuts.

## Drive Section 7 — Application Drawer

The application drawer will support:
- Alphabetical application list and grid modes.
- Configurable grid density and icon size.
- Search-first drawer behavior.
- The App Drawer header must not contain Settings, close, or other top action buttons.
- The App Drawer is dismissed by the supported downward gesture; Launcher-owned Settings is not opened from the drawer header.
- Application categories and user-created tabs.
- User-created folders.
- Smart folders implemented locally from deterministic rules and optional local usage signals.
- Hidden applications.
- Work-profile separation.
- Recently installed and recently updated filters.
- Optional suggested applications using local-only ranking.
- Fast alphabet scroll.
- Local search over app name, package label, shortcuts, contacts only when explicitly enabled, and launcher actions.

## Drive Section 8 — Folders

Folders will support:
- Home-screen folders.
- App-drawer folders.
- Small and expanded folder presentations.
- Optional large folders that expose direct app launch without opening the full folder.
- User-defined folder names and ordering.
- Folder icon previews.
- Folder background, opacity, corner-radius, and layout settings through Glaze Theme Engine.
- Smart folders with transparent, user-visible rules and manual override.

## Drive Section 9 — Widgets and Launcher Cards

GoreeCloud Launcher must provide a compliant AppWidgetHost for Android third-party widgets and Launcher-owned built-in GoreeCloud widgets.
Built-in widgets must remain Launcher-owned/local-first surfaces and third-party widgets must use Android platform hosting/configuration contracts rather than an imitation widget system.

Fresh/default Home requirement: built-in widgets and Glaze Cards must remain opt-in Home content. A fresh starter workspace must not automatically place Calendar, Weather, Glance, Quick actions, Universal Search, or another first-party widget/card on Home. Historical Development migrations may remove only reserved GoreeCloud starter-placement identities that are explicitly recognized as prior defaults; they must not remove user-created widget placements. A truly content-empty Home fallback must remain compact and actionable rather than occupying a large portion of the page with instructional chrome.


Built-in widget catalog requirement: the first-party catalog must not be clock-centric. It must include useful non-time surfaces. The current Development candidate includes Universal Search, Quick actions, Battery, Date, Digital clock, Compact clock, Analog clock, and Launcher Status. The Launcher widget-picker search must cover both GoreeCloud built-ins and installed Android widgets so first-party utilities remain discoverable by name, description, and canonical widget identity while installed widgets remain searchable by provider/app metadata. Launcher-owned widget-picker structure must also expose meaningful accessibility semantics: the gallery title and catalog section labels are headings, decorative preview glyphs/provider initials are excluded from assistive semantics, and first-party cards remain actionable nodes. First-party catalog entries should remain information-dense single-row surfaces where practical, with compact previews and span metadata; Launcher-owned vector artwork is preferred over Unicode/font pseudo-icons for utility/widget identity. Current Android 16 runtime coverage verifies the unified picker/search and an actionable Battery card without adding a widget or mutating Home; representative-device TalkBack/Switch Access, large-text, landscape, IME, touch, resizing, and binding acceptance remain separate gates. Richer optional Glaze Cards such as calendar glance, tasks, favorite contacts, media controls, weather from an explicitly selected provider, and GoreeCloud integrations remain separate permission-, provider-, privacy-, and acceptance-gated work.
Widget capabilities will include:
- Widget picker.
- Widget configuration activities.
- Resize handles and grid snapping.
- Widget size persistence.
- Dynamic-color propagation where supported.
- Widget stacks as a later milestone after core widget hosting is stable.


The launcher may also provide first-party Glaze Cards. Glaze Cards are launcher-owned surfaces for functions such as calendar glance, tasks, device status, favorite contacts, battery, weather from an explicitly selected provider, media controls, or GoreeCloud integrations. Cards must remain optional and must not become an advertising or sponsored-content surface.

## First-run onboarding presentation

First-run setup must communicate the minimum decisions needed to begin using Launcher without presenting a documentation-style wall of text. The three-step flow should use a visible progress treatment near the top, an explicit current-step count, visual or diagrammatic examples for spatial/Search choices, concise feature cards with distinct semantic accents, and optional **Learn more** expansion for longer interaction/privacy detail. The post-setup Home hint should continue the same compact visual language rather than reintroducing long instructional paragraphs. Essential controls and explanations must remain accessible without relying on color alone. Gesture explanations should use consistent Launcher-owned visual cues and plain-language labels rather than depending on Unicode arrow/star/font glyphs for meaning.

## Drive Section 10 — Search

GoreeCloud Launcher owns Universal Search. Core Universal Search must be a native, first-party Launcher capability rather than a mandatory dependency on GoreeCloud Index or GoreeCloud Search.

Universal Search presentation requirement: typed results must prioritize information density and preserve a distinct Top result without making ordinary application and shortcut results visually equivalent in weight. Application shortcuts should use compact per-application rows with recognizable, accessible action glyphs rather than vertically stacked full-width text-button cards when the action can be represented clearly. Long labels must use bounded consistent ellipsis.

Search Sources presentation requirement: the source manager must use compact navigation chrome rather than a large repeating page-title card. Appearance controls such as Suggestion tabs must participate in the normal scroll content and must not stay pinned over the source list. Source rows should surface concise status language; detailed provider/privacy/authorization explanation belongs in the expandable source details. Visual density should come from reduced padding, flatter elevation, and restrained nested surfaces rather than shrinking actionable controls below a comfortable touch target. Utility actions such as reset/order and recurring result affordances should use compact accessible vector controls when their meaning is clear, while keeping text available through semantics or adjacent state labels. Repeated section containers should avoid card-within-card presentation when a lighter grouped-row treatment communicates hierarchy.
Launcher Universal Search should provide one interface for discovering and accessing applications, services, files, folders, documents, people, teams, organizations, settings, system controls, application actions, shortcuts, GoreeCloud commands, recent and frequently used resources, cloud resources, connected services, contextual workspace resources, and AI-assisted answers or actions where appropriate.
Universal Search must function as both a discovery system and an action surface. Supported results may expose direct actions such as launch, open, share, move, inspect, communicate, navigate to a setting, execute a command, invoke an application shortcut, or resolve a natural-language request to the appropriate GoreeCloud resource or capability.
The initial native architecture is owned and operated by Launcher and includes search indexing, providers, aggregation, ranking, categories, permission-aware filtering, application/service discovery, recents and frequency signals, search history, commands, shortcuts, contextual actions, provider registration, and standardized search APIs for GoreeCloud applications and services.
Current local Search scope includes installed applications, Android application shortcuts, and user-enabled Contacts, Call history, Messages, and user-selected file roots. Sensitive local sources remain opt-in and permission-gated.
Connected sources such as Google Drive, Dropbox, Brave Search, and future reviewed providers must use explicit user handoff. Typed queries must not be automatically fanned out to third-party/network providers.
GoreeCloud Search and GoreeCloud Index remain separately developed capabilities. When each becomes sufficiently stable and production-ready, Launcher may consume them as optional providers or advanced backends. Launcher must continue to provide its core Universal Search experience when either or both are unavailable.
Detailed authority, provider, migration, and long-term architecture requirements are defined in Section 64, which supersedes conflicting earlier planning language.

## Drive Section 11 — Glaze Theme Engine

I will build a first-party theme engine inside GoreeCloud Launcher. The Glaze Theme Engine will control launcher-owned visual presentation without pretending to modify portions of Android or third-party applications that a normal launcher cannot safely control.


Initial theme capabilities:
- System, Light, Dark, and scheduled appearance.
- Wallpaper-derived local color extraction.
- User-selected accent palettes.
- Glaze UI Canvas, Solid, Raised, Glaze, and Overlay surface mapping.
- Icon size.
- Icon shape and adaptive-icon masking.
- Themed icon treatment using supported monochrome/adaptive icon data.
- First-party fallback icon treatment for apps without compatible themed assets.
- Optional standard Android icon-pack compatibility in a later milestone without making third-party icon packs a requirement.
- Folder styling.
- Dock styling.
- Drawer background and translucency.
- Search-field styling.
- Widget-container styling where the launcher owns the container.
- Typography scale within launcher-owned text.
- Blur, translucency, gradients, shadows, and reduced-transparency fallbacks.
- Motion intensity and reduced-motion mode.
- Exportable theme presets.


Theme processing will be local. Theme packs distributed by GoreeCloud must be inspectable assets rather than executable remote code.

## Drive Section 12 — Glaze UI Experience

The launcher must be recognizably Glaze UI rather than a Material, Pixel, Nova, or One UI skin.


I will apply:
- Rounded, ergonomic controls and containers.
- Selective translucency rather than glass everywhere.
- Soft depth and restrained shadows.
- Purposeful gradients.
- Large, comfortable touch targets.
- Strong light and dark appearances.
- Fluid but bounded motion.
- Reduced-motion and reduced-transparency behavior.
- High-contrast and forced-color resilience where platform behavior permits.
- Purpose-built phone and tablet compositions rather than one stretched layout.

## Drive Section 13 — Gestures and Actions

Configurable gestures may include:
- Swipe up/down.
- Double tap.
- Double-tap swipe.
- Pinch in/out.
- Two-finger swipe.
- Home-button reselect when Android exposes the event safely.


Gesture actions may launch:
- App drawer.
- Notifications or Quick Settings through supported Android mechanisms.
- Search.
- A selected app.
- A selected shortcut.
- Edit Home. Launcher Settings is reachable from Edit Home → Settings and is not a direct configurable gesture or App Drawer header action.
- Screen lock only through a user-approved Android mechanism that does not require unsafe privileges.

## Drive Section 14 — Privacy Shield Requirements

GoreeCloud Privacy Shield will define the launcher privacy contract.


The launcher will:
- Contain no ad network.
- Contain no sponsorship or affiliate-placement system.
- Contain no behavioral tracking.
- Avoid remote analytics by default.
- Keep app-usage-derived suggestions local.
- Provide clear controls to disable suggestions and clear local history.
- Keep contact search disabled until explicitly enabled and permissioned.
- Avoid uploading installed-app inventory, layout, folders, search history, or usage history to GoreeCloud or another party by default.
- Document every network destination used by an optional online integration.

## Drive Section 15 — Wardveil Security Requirements

Wardveil Security applies to launcher security and protection surfaces without replacing the launcher identity.


Security requirements include:
- Minimize requested permissions.
- Reject unnecessary accessibility-service dependence.
- Do not request device-administrator privileges for ordinary launcher features.
- Validate imported backups and theme packages before applying them.
- Treat external intents, deep links, and shortcut data as untrusted input.
- Protect local configuration from malformed imports.
- Use Android Keystore only when cryptographic key material is actually required.
- Keep signing keys, release credentials, and reusable secrets outside the repository.
- Provide safe recovery if a theme, widget, shortcut, or layout item becomes invalid.

## Drive Section 16 — Backup and Restore

The launcher will provide portable local backup and restore for:
- Home pages and item positions.
- Dock configuration.
- Folders.
- Launcher settings.
- Gesture mappings.
- Theme presets.
- Hidden-app choices.
- Search settings.


Backups must use a versioned, documented format with schema validation and forward migration. Widget IDs are device/runtime-specific and must be restored through a safe rebinding workflow rather than copied as if they were portable identifiers.

## Drive Section 17 — Accessibility

The launcher will support:
- TalkBack semantics.
- Meaningful content descriptions.
- Logical traversal and focus order.
- Large touch targets.
- Font scaling without clipped critical controls.
- Reduced motion.
- Reduced transparency / solid fallbacks.
- High-contrast visibility.
- Keyboard and switch-access behavior on tablets and compatible devices.
- Clear drag-and-drop alternatives where practical.

## Drive Section 18 — Performance

Performance goals:
- Fast HOME resume.
- No network dependency on startup.
- Incremental app-index updates rather than full rescans for ordinary package changes.
- Efficient icon caching with bounded storage.
- Lazy app-drawer rendering.
- Smooth page and drawer motion on representative mid-range devices.
- Battery-conscious background work.
- No persistent polling for features that Android callbacks can provide.

## Drive Section 19 — Data Model

Small independent launcher preferences may use Android Preferences DataStore. Preferences DataStore stores the durable workspace-authority marker and legacy compatibility snapshot, but after accepted PR #19 the current Favorites/Dock Home path performs a guarded one-way cutover to Room and uses Room as the terminal placement authority. A version-1 AndroidX Room 3 relational workspace foundation mirrors that accepted state into source-controlled workspace_pages and workspace_items schema records. The durable authority-state foundation persists DATASTORE, ROOM_VERIFIED, and guarded ROOM phases in DataStore, with order-sensitive SHA-256 verification evidence bound to the exact ordered Favorites/Dock snapshot. PR #10 added independent canonical Room dual-read reconciliation; PR #11 added deterministic pre-cutover startup/recovery; PR #12 added authority-gated Room placement I/O and corrected CI to run the complete instrumentation suite; PR #13 added observational cutover readiness plus the fail-closed source guard; PR #14 added a non-promoting transaction rehearsal and terminal-ROOM health evaluator. PR #15 adds WorkspaceProductionPromotionCoordinator as the separately reviewed production implementation of the authority transaction. It consumes the existing rehearsal, reacquires Room, performs another canonical equality check against the exact verified DataStore snapshot, revalidates ROOM_VERIFIED evidence, invokes the guarded one-way promoteRoomAuthority primitive, verifies terminal ROOM, and immediately evaluates post-cutover Room health. A healthy transaction returns PromotedHealthy; an unhealthy terminal state becomes an explicit recovery-required result and is never silently demoted to potentially stale DataStore placement. The source-level cutover guard is narrowed rather than removed: the only permitted production promotion call outside WorkspaceRepository is inside WorkspaceProductionPromotionCoordinator, while production instantiation of that coordinator and production instantiation of WorkspaceRoomPlacementRepository remain prohibited. PR #19 supersedes that pre-activation boundary: MainActivity now constructs one WorkspaceProductionRuntimeCoordinator, which owns the reviewed promotion, post-cutover recovery, authority-aware observation, and mutation-routing components. The accepted PR #19 activation performs that cutover for the current Favorites/Dock compatibility model. PR #20 then accepts the actual Android HOME-role MainActivity recreation path: Room-authoritative placement is recollected after ActivityScenario recreation and a post-recreation Room-authoritative Favorite mutation propagates reactively into the recreated Compose Home. LauncherApps callback registration is explicitly bound to the main Looper so repository collection is not dependent on caller-thread Looper state. True Android OS process-death/cold-start survival, schema-version upgrade recovery, representative physical-device/default-HOME validation, and later multi-page relational expansion remain separate gates.


Primary entities may include:
- WorkspacePage.
- WorkspaceItem.
- AppItem.
- ShortcutItem.
- Folder.
- FolderMembership.
- WidgetPlacement.
- DockItem.
- GestureBinding.
- ThemePreset.
- LauncherPreference.
- HiddenApp.
- SearchPreference.

## Drive Section 20 — Module Architecture

Recommended source modules:
- app — Android application, HOME activity, navigation, DI bootstrap.
- core:model — domain models.
- core:data — Room, repositories, migration, backup/restore.
- core:launcher — LauncherApps, profiles, shortcuts, package lifecycle.
- core:widgets — AppWidgetHost integration.
- core:theme — Glaze Theme Engine.
- core:designsystem — Android Glaze UI implementation and conformance mapping.
- feature:home — workspace, dock, folders, drag/drop.
- feature:drawer — application drawer, categories, folders, search.
- feature:search — local index and provider handoff.
- feature:settings — launcher and privacy/security settings.
- feature:onboarding — HOME-role and migration onboarding.
- feature:backup — import/export and recovery.

## Drive Section 21 — Distribution

The project will produce reproducible APK artifacts for direct installation and may produce Android App Bundles for app-store distribution. Direct distribution must remain a first-class path. For current Development testing alongside a recovery-preinstalled com.goreecloud.launcher system app, the debug build uses applicationIdSuffix .dev and publishes com.goreecloud.launcher.dev as GoreeCloud Launcher Dev. This sidecar identity is Development-only and does not change the base/release application ID com.goreecloud.launcher or establish production signing authority.


The project must not depend on Google Play Services for essential launcher functionality. Optional integrations may use platform or provider APIs only when they are clearly separable and the launcher remains fully functional without them.

## Drive Section 22 — Repository Recommendation

Repository Name: launcher
Description: Privacy-first, open-source Android home-screen launcher for GoreeCloud with Glaze UI, first-party theming, widgets, folders, gestures, local search, and no ads or sponsorships.
Configuration:
- Owner: GoreeCloud.
- Visibility: Public.
- Default branch: main.
- License: GPL-3.0-only.
- Initialize with README: Yes.
- .gitignore: Android / Kotlin / Gradle.
- Issues: Enabled.
- Discussions: Optional; initially disabled to reduce maintenance overhead.
- Wiki: Disabled; current authoritative project documentation belongs in this repository. Historical Google Drive project-specification material is migration provenance only.
- Actions: Enabled.
- Dependabot/security updates: Enabled where supported.
- Secret scanning: Enabled where supported.
- Branch protection: Require pull requests and required CI before main changes.
- Releases: Enabled for signed APK release artifacts after signing acceptance.
- Packages: Disabled initially unless a reusable package is introduced.

## Drive Section 23 — Continuous Integration

Initial CI should verify:
- Gradle wrapper integrity.
- Debug build.
- Unit tests.
- Android lint.
- Kotlin formatting/static analysis.
- Dependency and license review.
- Manifest permission allowlist.
- No advertising/tracking SDK allowlist violations.
- No accidental cleartext-network configuration.
- Glaze UI conformance assertions.
- Room/KSP schema generation and parse validation.
- Source-controlled Room schema-history drift enforcement.
- Backup schema tests.
- Focused API 36 Room runtime instrumentation and future launcher smoke tests through an immutable-pinned emulator runner.

## Drive Section 24 — Initial Milestones

Milestone 0 — Foundation
- Repository bootstrap.
- Native Android Gradle project.
- HOME-role manifest and onboarding.
- LauncherApps application index.
- Basic Glaze UI home screen and drawer.
- Local settings and theme foundation.
- CI and repository governance.


Milestone 1 — Daily Launcher
- Workspace persistence.
- Dock.
- App drawer search.
- Folders.
- Icon and label customization.
- Gestures.
- Backup/restore foundation.


Milestone 2 — Android Integration
- AppWidgetHost.
- Pinned and dynamic shortcuts.
- Work/private-profile handling.
- Package install/update/archive states.
- Improved launch animations.


Milestone 3 — Glaze Theme Engine
- Wallpaper palettes.
- Icon shapes and themed icons.
- Theme presets.
- Folder/dock/drawer styling.
- Motion and accessibility theme controls.


Milestone 4 — Intelligence Without Surveillance
- Local suggested apps.
- Smart folders.
- Optional Glaze Cards.
- Optional explicitly configured search-provider handoff.


Milestone 5 — Release Candidate
- Representative physical-device testing.
- Phone and tablet acceptance.
- Backup/restore acceptance.
- Accessibility acceptance.
- Performance profiling.
- Permission/privacy/security audit.
- Signed APK and independent signature verification.

## Drive Section 25 — Non-Goals

The first releases will not:
- Implement music or media-server features.
- Replace Android SystemUI, Settings, lock screen, notification shade, or recents provider through unsupported hacks.
- Require root.
- Require an accessibility service merely to imitate privileged launcher behavior.
- Inject ads, sponsored apps, promoted search results, affiliate links, or paid placement.
- Upload launcher behavior for advertising or engagement optimization.
- Claim to theme third-party application interiors.

## Drive Section 26 — Acceptance Criteria

I will consider the first Stable release acceptable only when:
- It can be selected and retained as the default HOME application on supported devices.
- App discovery, launching, folders, dock, search, widgets, shortcuts, settings, and backup/restore work reliably.
- The launcher remains functional offline.
- No advertising, sponsorship, tracking, or required remote analytics are present.
- Glaze UI phone and tablet visual acceptance is complete.
- The first-party Glaze Theme Engine is functional and recoverable.
- Permissions are minimal and documented.
- Import and backup files are validated safely.
- Automated CI passes on the accepted release revision.
- Representative real-device performance and accessibility checks pass.
- A signed APK is reproducibly built and independently signature-verified.
- Known failure and recovery paths are documented.

## Drive Section 51 — Current Source-Validated Multi-Page Room and Glaze UI State

Current GoreeCloud Launcher Development source has advanced to a repository-local Glaze UI 2.2.0 Adoption Candidate on Draft PR #62 exact head 4043895afd26991d0a20461e94d002f91c890703. Earlier Glaze UI 1.x, 2.0, and 2.1 references in historical records remain useful migration and rollback evidence but do not define current conformance. Production eligibility remains false, and complete application-specific rendered/native/accessibility/form-factor/representative-device acceptance remains required before current-Stable conformance can be claimed.


The repository now includes deterministic multi-page workspace domain contracts, authoritative Room-backed page-order persistence for the guarded terminal Room path, and validated cross-page HOME item persistence. The cross-page item path reconstructs the complete stored HOME placement model, validates the requested move through the shared workspace domain contract, and performs a full observed page/item snapshot comparison inside the Room transaction before writing so concurrent workspace drift is rejected rather than overwritten.


Exact cross-page item-persistence head 04a0687aad034c8a00171870b4caf7dcf9b86161 passed Android CI run 33261963092 and was integrated to main as e844a3d06e30fc6e681923af312ed8d521ba2279.


Repository documentation and the Development user manual were reconciled on exact head 233103d3a136e71a83bbc22b23bf0ec43b5fd512. Android CI run 33262399439 passed before that documentation candidate was integrated to main as 12e7acf3d7edd6b28a7dad0904f4f411746f7a45.


This supersedes older specification language that describes Glaze UI 1.x as current or treats multi-page placement solely as future infrastructure. It does not claim a complete rendered multi-page editing experience. Rendered page creation/navigation, live cell/span drag-and-drop, legacy coordinate migration, folders, shortcuts, widgets, broader workspace editing UX, versioned backup/restore, true Android OS process-death and schema-upgrade recovery, representative physical-device/default-HOME acceptance, signed release packaging, and Stable qualification remain separate gates.


Current implementation update — Rendered terminal-Room HOME page navigation — August 29, 2026


GoreeCloud Launcher now has a read-only rendered multi-page bridge for durable terminal Room authority. `WorkspacePagedHomeObserver` combines authoritative Room HOME page/item streams and maps ordered page state into a user-facing page selector. The production runtime coordinator exposes that read path without widening placement-write authority.


When more than one authoritative HOME page exists, the Development Home UI can select secondary pages and launch application items from them. The legacy primary page retains the existing Favorites/Dock editing path. Secondary pages are intentionally launch-only/read-only for placement. Unsupported Room item types are counted and surfaced rather than silently treated as application items, and selected-page identity is reconciled against the latest authoritative page set.


The first candidate head dfdee4110c9344b81626cc1fecce677c54d67b76 failed Android CI #90 / workflow run 33269803028 during Kotlin compilation because the new paged UI imported the wrong Compose weight symbol and referenced a nonexistent Glaze spacing alias. No emulator acceptance ran for that failed candidate. The defects were corrected without changing the authority or product boundary.


Corrected exact head 87d05fbb98c1f7f93ebd5cffedcafa32456bd303 passed Android CI #91 / workflow run 33270087393. The validate job passed privacy, manifest, Glaze UI, Glaze Motion, Room-cutover guards, lint, JVM tests, debug assembly, Room schema checks, and source cleanliness. The Android 16 `room-runtime-emulator` job also passed the Room transition runtime suite, including the new file-backed paged observer acceptance. PR #32, Render authoritative Room HOME pages, was squash-merged to authoritative main as ee20d9d8f45da2582b207a408583a5da4fffd8ec.


This remains a Development rendering bridge. Page creation/deletion, user-facing page reordering, secondary-page placement editing, cross-page drag/drop UI, live cell/span editing, folders, shortcuts, widgets/AppWidgetHost, production Room cutover, representative physical-device HOME acceptance, signed release packaging, and Stable qualification remain separate gates.


Authoritative Home Page Reordering — Current Merged State
The Development launcher now exposes the existing terminal-Room HOME page-order mutation through WorkspaceProductionRuntimeCoordinator and the rendered multi-page Home selector. Users can move the selected authoritative Room page earlier or later with explicit accessible controls, and successful mutations refresh observed page state while retaining the existing transactional page-set and authority checks.


PR #33 passed Android CI workflow run 33270749921 on exact head 0d603d0bd4dfdd1fc56712e65d246ea889f6ade4 and was squash-merged to authoritative main as cd5fbfec96d619e35e5589ad68a9b72add2cb521.


This milestone does not establish page creation/deletion, secondary-page placement editing, live cell/span editing, cross-page drag/drop, widgets/folders/shortcuts, production Room cutover acceptance, signed release, or Stable qualification.


Authoritative Home Page Creation — Current Merged State


Merged repository evidence: 90fa215696c688196adfeca7cb5d2882d7e049de.


The terminal-Room HOME workspace now exposes bounded creation of a new empty HOME page through the production runtime coordinator and Home page switcher. Creation is accepted only under terminal Room authority, compares the observed HOME page snapshot inside the Room transaction before append, assigns the next contiguous page rank, rejects blank or globally duplicate page identifiers, refreshes authoritative observed page state after success, and selects the new page in the UI only after the authoritative mutation succeeds.


This milestone intentionally does not implement page deletion. The current Room relationship uses cascading child-item deletion, so destructive page lifecycle requires an explicit product and recovery design rather than implicit row deletion. It also does not establish secondary-page item editing, cross-page drag/drop UI, production Room cutover acceptance, signed release packaging, representative physical-device acceptance, or Stable qualification.


Safe Empty Home Page Deletion — Current Merged State


Merged repository evidence: 9eadd14e1002a4ea973c7122aff30110a7901905.


Terminal-Room HOME authority now supports deletion of an empty non-primary page only. The repository rejects the protected primary page, an unknown page, the last remaining page, and any page that contains an item. Inside the Room transaction, the DAO re-reads and compares the complete authoritative HOME page/item snapshot and rechecks target emptiness before deleting the page row. This prevents the schema's child-item cascade from removing content that appeared concurrently after the initial read. Successful deletion compacts remaining page ranks transactionally and refreshes observed authoritative state; the UI exposes Delete empty page only when the rendered authoritative page is empty and non-primary.


This is not general destructive page deletion. Populated-page deletion, recovery/undo, secondary-page item editing, cross-page drag/drop, production Room cutover acceptance, representative device acceptance, release, and Stable qualification remain pending.


Guarded Secondary-Page Application Moves — Current Merged State
Merged repository evidence: df9b3e718483af2e3cb7ad2cf131910699d6e0af.
Exact accepted Pull Request #36 head ae243a4f8292f70f1051fb8a1d09123604f851d1 passed Android CI workflow run 33281114059, including the normal privacy, HOME-manifest, Glaze UI, Glaze Motion, Room-cutover, Android lint/JVM/debug-build, Room schema/drift checks and the Android 16 Room runtime-emulator gate.
Terminal-Room secondary Home pages now expose a bounded application move control to another existing authoritative Home page. WorkspaceHomeItemPageMover requires initialized terminal Room authority, resolves exactly one APP item from the selected source page and application key, rejects malformed/null placement state, derives a deterministic coordinate envelope with a four-column minimum, and selects the first collision-free target cell in row-major order while preserving the source item's span. The adapter does not write directly: the final mutation is delegated to the existing WorkspacePagedRoomMutationRepository.moveHomeItem path, which re-reads and validates the complete HOME page/item snapshot and uses the snapshot-checked DAO write so concurrent workspace changes fail closed.
WorkspaceProductionRuntimeCoordinator refreshes observation only after UpdatedItem. The secondary-page UI exposes an explicit Move menu listing other existing Home pages and selects the target page only after the authoritative Room mutation succeeds. Android file-backed runtime coverage proves pre-Room reservation, first-free-cell selection, target persistence, source removal, and same-page/missing-page refusal.
This milestone does not implement arbitrary cell/span editing, cross-page drag-and-drop, primary Favorites-to-secondary-page movement through this menu, populated-page deletion, destructive undo/recovery, folders, shortcuts, widgets, representative physical-device HOME acceptance, signed release packaging, or Stable qualification
Guarded Same-Page Secondary Home App Movement — Current Merged State
Launcher’s terminal-Room secondary Home editor now supports bounded Earlier/Later application movement within the currently selected authoritative page. The first-party mover resolves exactly one APP item, derives the current authoritative grid, selects the nearest collision-free earlier/later row-major cell, and delegates the actual write to the existing snapshot-checked paged Room mutation transaction. Preflight failure state is call-local, preventing concurrent UI actions from overwriting one another’s result.
Exact PR #37 head 86392d6b94b569bc31604bef93194288368dd85c passed Android CI workflow run 33287960738 and was squash-merged to authoritative main as 1c2bbc91a4e6291264ca86487eb1a5274284dc69.
Acceptance boundary: this is Development secondary-page placement editing only. It does not establish arbitrary coordinate editing, swap semantics, drag/drop, primary-page Room editing, folders/widgets/shortcuts, production Room cutover/recovery acceptance, representative physical-device/accessibility acceptance, signed release, or Stable qualification.
Guarded One-Cell Secondary Home Placement Movement — Current Merged State
The terminal-Room Home authority now supports an internal first-party spatial movement contract for an existing app placement: left, right, up, or down by exactly one grid cell. The operation requires terminal Room authority and exactly one authoritative matching app placement, derives the existing authoritative grid, rejects occupied or out-of-bounds targets without swapping or displacing other items, and delegates the write to WorkspacePagedRoomMutationRepository.moveHomeItem so the full Room snapshot is re-read and collision-validated transactionally before persistence.
Validation history: initial PR #38 head 77837f0161b0de219582eb611176f6ea26981bb4 failed the Android 16 Room runtime suite because the newly added test fixture promoted Room authority while leaving the mirrored legacy primary favorite without explicit grid coordinates. The production mover correctly rejected that invalid authoritative snapshot. The fixture was repaired to supply the same explicit valid legacy placement used by established runtime tests; production movement code was unchanged. Repaired exact head f7b160998991f22a317912d39a965e5c9041480b passed the complete validate job in Android CI workflow run 33318239825. The first emulator attempt on that same head failed only because the unrelated pre-existing ActivatedHomeLifecycleRuntimeTest.recreatedMainActivityRecollectsRoomPlacementAndRemainsReactive timed out after 15 seconds. No source or test semantics were changed for that timeout. A targeted rerun of the same workflow's Android 16 Room runtime job passed all 27 emulator tests on the unchanged exact head. PR #38 was then squash-merged with expected-head protection to authoritative main as 2a0bc9e7ae477a48bbead6ef049691b13d59401e.
Acceptance boundary: this is internal Development placement authority. It does not add drag-and-drop UI, an arbitrary coordinate editor, swap/displacement semantics, primary-page policy expansion, production deployment acceptance, release approval, or Stable qualification. The existing accessible non-drag page and ordering controls remain the current user-facing manipulation surfaces.
.


Guarded One-Cell Secondary Home Movement UI — Current Merged State
PR #39 merged from exact candidate head c7bdebe9cf50a8f8fbc888bbb572ebddab0de25c to main as 99c071be321b88f2e57c1d40ca9fb1a3e3526de6 after Android CI run 33320376320 succeeded.
The production runtime coordinator now exposes the already-validated WorkspaceHomeItemPageMover exact one-cell operation for terminal-Room secondary Home pages. Each secondary-page app tile has a Move cell menu with Left, Right, Up, and Down actions. Existing Earlier/Later movement remains available as an accessible non-spatial fallback, and cross-page movement remains separate.
Exact-cell requests still delegate to the authoritative Room mutation repository, which re-reads and collision-validates the complete placement snapshot before writing. Occupied or out-of-bounds targets fail closed; the slice does not introduce swapping, arbitrary coordinate editing, drag-and-drop, primary-page policy expansion, production acceptance, release, or Stable qualification.


Scrollable Home Page Controls — Current Merged Development State


Repository state: GoreeCloud/android-app-defaults (`apps/launcher/`) PR #40 was squash-merged to main as 66c3b84d08ad850c920b4f49e3d94a3f518c586c from exact tested head d5fc4247fb21726e949aa4f54fdf9e1bcef056f0. Exact-head Android CI run #108 passed.


Development capability: the Home page selection/Add page strip and the selected-page move/delete action strip are now horizontally scrollable, preventing larger authoritative Home page sets from overflowing the available width while preserving current page selection, page creation, reordering, and guarded empty-page deletion behavior.


Authority and acceptance boundary: this is a presentation scalability improvement only. It adds no new workspace mutation authority, synchronization behavior, hidden page reordering, deployment acceptance, or Stable qualification.


Home Page Context Counts — Current Merged Development State


Repository state: GoreeCloud/android-app-defaults (`apps/launcher/`) PR #41 was squash-merged to main as daa83b8cce6c107f2ddddf729c607f69be065f10 from exact tested head 83bb59cf6c3e0308e40662e6bfed37b12a1ba624. Android CI run #110 passed.


Development capability: the scrollable Home page switcher now shows compact authoritative context beneath each page label. App counts and unsupported workspace-item counts are derived from the existing WorkspaceRenderedHomePage Room projection, with singular/plural and unsupported-item presentation covered by pure model tests. Unsupported items remain visible in the summary rather than being silently omitted.


Authority and acceptance boundary: these counts are read-only presentation derived from the existing terminal Room source of truth. This adds no new workspace source, page mutation authority, hidden synchronization behavior, deployment acceptance, or Stable qualification.


Selected Home Page Auto-Focus — Current Merged Development State


Merged on 2026-08-30 through GoreeCloud Launcher PR #42. The terminal-Room Home page switcher now uses a lazy horizontal page list rather than eagerly rendering the full strip, improving behavior as authoritative page count grows. When the selected page changes, page order changes, or the page set changes, the switcher scrolls the selected authoritative page back into view automatically instead of allowing the current page indicator to remain off-screen.


This change preserves the existing page context counts, page creation/reordering/deletion boundaries, Room-authority checks, and separate mutation controls. It adds presentation/navigation behavior only; it does not create a new workspace source of truth or bypass the existing Room transaction and validation contracts.


The same candidate restored previously missing mandatory root SPECIFICATIONS.md, FEATURES.md, BENEFITS.md, and COMPETITIVE-OBJECTIVES.md documents and synchronized README.md and USER-MANUAL.md with the currently implemented multi-page editing/navigation surface.


Exact tested head: f34c5b42504af3fff9c4a93f8a8f7e6ae9ac6d77. Validation: Android CI #112 succeeded, including privacy/manifest/Glaze/Room checks, lint/unit/build validation, schema verification, and the Android 16 Room runtime emulator suite. Squash-merged main revision: 388ead8d3db7fe8effcdfc5bdddb6022cd6ab9a7.


Acceptance boundary: Development only. This does not establish production Room cutover, complete cross-page drag/drop, folders/widgets/shortcuts, physical-device HOME acceptance, signed release, or Stable qualification.

## Drive Section 52 — Built-In Features and Capabilities — Approved Product Scope

Product role


GoreeCloud Launcher is intended to be the native home, application-navigation, search, personalization, and contextual-access experience for GoreeCloud devices. It is designed as an integral part of the GoreeCloud platform rather than a standalone launcher layer, with deep integration across GoreeCloud applications, services, platform systems, and connected devices.


Scope and implementation boundary


This section records the approved built-in product capability scope for GoreeCloud Launcher. Unless a capability is separately identified as implemented and accepted in the current implementation records above, inclusion here establishes a product requirement or target capability rather than a claim of completed implementation, runtime acceptance, release acceptance, or Stable qualification. The current implementation remains governed by the source-validated and runtime-validated state documented elsewhere in this specification.


For feature-scope completeness, this section supersedes earlier abbreviated feature inventories where those inventories are less complete or conflict with the capability scope below. Earlier implementation records remain authoritative for historical implementation and validation state.


Home Screen


- Multiple customizable home-screen pages
- Adjustable home-screen grid sizes
- Independent row and column configuration
- Adjustable application icon sizes
- Drag-and-drop application placement
- Drag-and-drop widget placement
- Precise sub-grid positioning
- Custom screen margins and padding
- Application folders
- Custom folder names
- Intelligent folder-name suggestions
- Home-screen application shortcuts
- Application contextual shortcuts
- Resizable widgets
- Searchable widget picker
- Favorite-application Dock with a five-app starter configuration that is not a capacity limit
- Customizable Dock page density, labels, materials, and optional Launcher Universal Search affordance
- Multiple Dock pages with horizontal navigation independent from Home-page navigation and an optional disabled-by-default looping mode
- Accessibility-floor-aware adaptive Dock layout that pages before shrinking interaction targets below the resolved minimum
- Application and folder/shortcut/action Dock expansion according to the authoritative workspace model; non-app Dock persistence remains planned until separately verified
- Detailed Dock requirements and implementation boundaries are maintained in `docs/home-dock.md`
- Home-screen page scrolling
- Optional infinite page scrolling
- Wallpaper scrolling
- Home-screen page indicators
- Automatic creation of additional home-screen pages
- Remove applications from the home screen without uninstalling them
- Lock the home-screen layout
- Hide application labels
- Adjustable application-label size and positioning
- Overlapping widgets and supported interface elements
- Adaptive layouts based on device size, posture, and display configuration


Application Drawer


- Swipe-up application drawer
- Alphabetically organized application library
- Custom application-drawer grids
- Application-drawer folders
- Application-drawer tabs
- Automatic application categorization
- Intelligent application groups
- Smart folders
- Suggested applications
- Frequently used applications
- Recently installed applications
- Recently used applications
- Application-drawer search
- Hide applications from the drawer
- Custom application-drawer organization
- Vertical scrolling
- Custom drawer backgrounds
- Adjustable drawer transparency
- Independent application-drawer grid configuration
- Context-sensitive application ordering


GoreeCloud Search


- Persistent home-screen search
- Application-drawer search
- Installed application search
- Application-content search
- Contact search
- Device-settings search
- GoreeCloud settings search
- File and document search
- Screenshot search
- Shortcut and action search
- Web search
- Application-store search
- GoreeCloud service search
- Search across compatible connected GoreeCloud devices
- Recently accessed content suggestions
- Recently used application recommendations
- Usage-based application recommendations
- Routine-based recommendations
- Context-aware search results
- Customizable search-result categories
- Search-bar customization
- Search-bar themes
- Search shortcuts
- Search animations
- Direct actions from search results


Icons and Visual Appearance


- Icon-pack support
- GoreeCloud-native themed icons
- Adaptive themed icons
- Custom icon shapes
- Adjustable icon sizes
- Individual application-icon customization
- Custom icons for folders
- Wallpaper-based icon styling
- Wallpaper-derived color palettes
- Automatic interface color extraction
- Custom interface colors
- Custom accent colors
- Light appearance
- Dark appearance
- Automatic appearance switching
- Transparent interface elements
- Custom folder appearance
- Custom dock appearance
- Custom application-drawer appearance
- Custom home-screen appearance
- Adaptive visual themes
- Custom text styling
- Dynamic interface styling based on device context


Gestures and Interaction


- Custom swipe gestures
- Swipe-up actions
- Swipe-down actions
- Horizontal swipe actions
- Double-tap actions
- Pinch gestures
- Two-finger swipe gestures
- Custom gesture assignments
- Gesture-based application launching
- Gesture-based shortcut launching
- Gesture-based GoreeCloud actions
- Double-tap screen locking
- Long-press contextual menus
- Long-press application shortcuts
- Drag-and-drop organization
- Drag-and-drop application grouping
- Gesture-based search access
- Gesture-based notification access
- Gesture-based feed access
- Configurable interaction sensitivity


Smart and Contextual Experiences


- Context-aware application suggestions
- Home-screen application suggestions
- Application-drawer suggestions
- Pinnable suggested applications
- Ability to disable suggestions for individual applications
- Recent-usage recommendations
- Long-term usage recommendations
- Routine-based recommendations
- Time-of-day recommendations
- Location-aware recommendations when permitted
- Device-state-aware recommendations
- Connected-device-aware recommendations
- Context-sensitive launcher content
- Calendar information
- Weather information
- Upcoming-event information
- Package-delivery information
- Travel information
- Flight information
- Navigation and destination information
- Media recommendations and controls
- Relevant file and document suggestions
- Contextual information cards
- Personalized information surfaces
- Suggested GoreeCloud actions
- Adaptive launcher layouts based on context


GoreeCloud Feed and Cards


- Optional side-mounted information feed
- Personalized GoreeCloud information feed
- Customizable information cards
- Application shortcut cards
- Contact shortcut cards
- Widget cards
- Media cards
- Calendar cards
- Weather cards
- Travel cards
- Device-status cards
- Conditional cards
- Pinnable cards
- Context-triggered cards
- Connected-device-triggered cards
- Connected-device-triggered launcher layouts
- Launcher content recommendations
- GoreeCloud service cards
- Privacy and security status cards
- Backup and synchronization status cards
- Cross-device activity cards
- User-configurable card ordering and visibility


Notifications


- Notification dots
- Numeric notification badges
- Custom notification badges
- Unread-status indicators
- Custom badge appearance
- Application-specific badge controls
- Long-press notification previews
- Notification access from application icons
- Notification-aware contextual shortcuts
- Privacy-aware notification visibility


Folders


- Home-screen folders
- Application-drawer folders
- Automatic folders
- Smart folders
- Custom folder names
- Suggested folder names
- Custom folder icons
- Custom folder grids
- Folder background customization
- Folder transparency controls
- Folder-opening animations
- Swipe actions on folders
- Folder organization controls
- Adaptive folder layouts
- Automatic application grouping
- Manual application grouping
- Nested organization where supported


Widgets


- Home-screen widgets
- Resizable widgets
- Searchable widget library
- Widget grouping
- Widget previews
- Widget padding controls
- Overlapping widgets
- Precise widget positioning
- Information widgets
- Dynamic widgets
- Interactive widgets
- Context-aware widgets
- GoreeCloud service widgets
- Smart widget recommendations
- Adaptive widget layouts
- Cross-device information widgets
- Privacy-aware widget content
- Widget refresh and update controls


Dock


- Custom dock size
- Multiple dock pages
- Adjustable dock application count
- Dock background customization
- Dock transparency
- Dock padding controls
- Search placement within the dock
- Dock widgets
- Scrollable dock
- Optional dock removal
- Suggested applications in the dock
- Context-aware dock applications
- Pinned GoreeCloud actions
- Adaptive dock layouts


Layout Customization


- Custom row count
- Custom column count
- Independent home-screen grid
- Independent application-drawer grid
- Independent folder grid
- Icon-label positioning
- Icon-label size adjustment
- Hide icon labels
- Custom screen margins
- Custom interface padding
- Sub-grid positioning
- Precise widget positioning
- Overlapping supported interface elements
- Lock home-screen layout
- Per-page layout customization
- Different layouts for different device modes
- Portrait and landscape layout configuration
- Foldable and multi-display layout adaptation
- Tablet-optimized layouts
- Desktop-style launcher layouts where appropriate


Navigation and Animation


- Custom home-screen page transitions
- Application-drawer animations
- Folder animations
- Search animations
- Home-screen transition effects
- Smooth scrolling
- Spring-based animations
- Gesture-responsive animations
- Wallpaper transition effects
- Application-launch animations
- Application-return animations
- Contextual transition animations
- Reduced-motion options
- Accessibility-conscious animation behavior
- Device-performance-aware animation scaling


Application Management


- Application uninstall shortcuts
- Application-information shortcuts
- Hide applications from the drawer
- Rename application display labels
- Replace individual application icons
- Organize applications into folders
- Organize applications into categories
- Pin applications
- Search installed applications
- Quickly access recently installed applications
- Quickly access recently updated applications
- View application permissions through integrated system controls
- Open application storage information
- Access application notification controls
- Access application privacy controls
- Access application security information
- Application-specific launcher actions


Personalization


- Wallpaper integration
- Dynamic color extraction
- Automatic color matching
- Custom accent colors
- Adaptive visual themes
- Custom icon styling
- Custom text styling
- Custom search appearance
- Custom application-drawer styling
- Custom folder styling
- Custom dock styling
- Custom animation preferences
- Per-device personalization
- Per-display personalization
- Context-based appearance changes
- Time-based appearance changes
- Wallpaper-driven launcher styling
- Accessibility personalization
- Reduced-motion preferences
- High-contrast interface options


Backup, Sync, and Configuration


- Launcher-layout backup
- Launcher-layout restore
- Multiple saved launcher configurations
- Restore home-screen layouts
- Restore folders
- Restore dock configuration
- Restore launcher preferences
- Restore icon customization
- Restore widget placement
- Import compatible launcher layouts
- Migrate launcher layouts between devices
- Synchronize supported launcher preferences across GoreeCloud devices
- Device-specific configuration overrides
- Configuration version history where supported
- Safe recovery after device reset or replacement


GoreeCloud Platform Integration


GoreeCloud Launcher is deeply integrated with the wider GoreeCloud platform and can act as a unified entry point into applications, services, information, devices, and system capabilities. Integration capabilities remain subject to each participating service's implementation, authorization, privacy, security, and availability boundaries.


GoreeCloud Drive Integration


- Surface recently accessed files and folders
- Search with GoreeCloud… Drive directly from launcher search
- Pin files and folders to the home screen
- File and folder shortcut widgets
- Contextual document recommendations
- Quick access to synchronized content


GoreeCloud Sync Integration


- Synchronize supported launcher settings across devices
- Synchronize compatible home-screen layouts
- Synchronize folders and application organization
- Synchronize personalization preferences
- Maintain continuity when switching GoreeCloud devices


GoreeCloud Backups Integration


- Automatic launcher-configuration backup
- Home-screen layout restoration
- Widget and folder restoration
- Launcher preference recovery
- Device-replacement restoration workflows


Everkeep Integration


- Long-term preservation of selected launcher configurations
- Recovery of important personalization state
- Configuration portability
- Device-transition continuity
- Integration with GoreeCloud continuity and recovery experiences


GoreeCloud Identity Integration


- Identity-aware launcher personalization
- User-specific home-screen configurations
- Secure profile switching
- Managed application visibility
- Profile-specific recommendations
- Identity-aware cross-device continuity


Privacy Shield Integration


- Privacy-aware search results
- Privacy-aware contextual recommendations
- Control over personalization signals
- Control over location-based recommendations
- Control over usage-based recommendations
- Sensitive-content visibility controls
- Privacy-status surfaces
- Direct access to relevant Privacy Center controls


Wardveil Security Integration


- Security-aware launcher experiences
- Security-state indicators
- Protected application access
- Suspicious-application warnings where supported
- Direct Security Center access
- Security-sensitive contextual actions
- Protection of launcher configuration and personalization data


GoreeCloud Mesh Integration


- Awareness of compatible GoreeCloud devices
- Cross-device application handoff
- Nearby-device actions
- Connected-device contextual cards
- Device-triggered launcher layouts
- Cross-device content suggestions
- Coordinated launcher state across supported GoreeCloud endpoints


GoreeCloud Location Integration


- Location-aware application suggestions when enabled
- Destination and travel information
- Location-sensitive contextual cards
- Nearby-action suggestions
- Relevant GoreeCloud Maps shortcuts
- User-controlled location personalization


GoreeCloud Mail Integration


- Unread-mail widgets
- Important-message cards
- Mail search from GoreeCloud Search
- Contact-based communication shortcuts
- Contextual email actions


GoreeCloud Messenger Integration


- Recent-conversation shortcuts
- Contact communication shortcuts
- Unread-message indicators
- Conversation widgets
- Contextual messaging actions


GoreeCloud Maps Integration


- Destination shortcuts
- Commute information
- Upcoming-trip information
- Navigation suggestions
- Location-aware launcher cards
- Search destinations directly from the launcher


GoreeCloud Calendar Integration


- Upcoming-event cards
- Calendar widgets
- Meeting shortcuts
- Event-based application suggestions
- Context-aware schedule information


Glaze UI Integration


GoreeCloud Launcher is a native Glaze UI experience and follows the platform-wide GoreeCloud design system across supported devices.


This includes:


- Consistent GoreeCloud visual language
- Adaptive layouts across phones, tablets, desktops, foldables, TVs, and other supported surfaces
- Dynamic materials and interface depth
- Responsive motion
- Wallpaper-aware appearance
- Unified accessibility behavior
- Consistent component design
- Device-appropriate interaction patterns
- Platform-wide theme integration
- Seamless visual continuity with other GoreeCloud applications


Core Design Principle


GoreeCloud Launcher is intended to be more than an application grid. It serves as the personalized front door to GoreeCloud: bringing applications, files, people, devices, search, information, services, privacy controls, security state, continuity features, and contextual actions together within one adaptive interface.


Its intelligence and personalization should remain transparent and user-controlled, while Privacy Shield, Wardveil Security, GoreeCloud Identity, Everkeep, GoreeCloud Mesh, Glaze UI, and other integral GoreeCloud platform systems provide the underlying privacy, security, identity, resilience, coordination, and design foundations.


Move-Target Page Context — Current Merged Development State


The Home workspace Move page destination menu now presents the destination page number together with the same compact item context used by the Home page switcher. Before moving an app, a user can therefore see the destination's current application count and any currently unsupported workspace-item count.


The labels are derived from the existing terminal Room WorkspaceRenderedHomePage projection through WorkspaceHomePageContext. Unsupported folder, shortcut, or widget records remain visible as “other” context rather than being silently ignored. The feature is read-only presentation: it does not add or change workspace mutation authority, page ordering rules, occupancy rules, exact-cell movement behavior, or synchronization authority.


Merged evidence: PR #43, exact validated head 734217ffb51f81bc1a745ff0dde909e28954ad7e, squash merge 3f4930ea5911185c585199a7b6c2840f8b67a2e9. Android CI run #114 completed successfully, including ordinary validation/lint/unit/build checks and the Android 16 Room transition runtime emulator suite.


Acceptance boundary: Development only. Folder/widget presentation, additional workspace capabilities, production Room cutover acceptance, and Stable qualification remain separate milestones.

## Drive Section 53 — Primary Home Compatibility and Secondary Spatial Editing Authority

This section records the accepted Development authority boundary after the primary Home compatibility protection correction merged in pull request #45. It clarifies and supersedes earlier implementation wording wherever that wording implied that the protected primary Home compatibility page participates in the current secondary spatial grid or can be reordered away from rank zero. It does not alter the approved product-capability scope in section 52.


Current accepted authority boundary
- Protected primary HOME page home:0 remains the rank-zero Favorites compatibility projection.
- Canonical compatibility APP rows on the primary page intentionally retain null cellX and cellY; that state is valid and must not be interpreted as malformed secondary spatial placement.
- Secondary HOME pages are the current spatial grid-placement domain.
- Supported current cross-page spatial moves are secondary-page to secondary-page only.
- Primary-to-secondary and secondary-to-primary spatial move requests fail closed with PrimaryPageProtected until a separately accepted primary-grid migration exists.
- Page ordering cannot move the protected primary page away from rank zero or move a secondary page ahead of it; secondary pages may reorder only within secondary positions.
- Before a secondary spatial write, canonical primary/Dock compatibility health is verified through the accepted Room compatibility reader.
- The final Room write still re-reads and compares the complete HOME page/item snapshot transactionally so concurrent page/item drift is rejected.
- The rendered Development UI excludes the protected primary page from secondary app move destinations and disables page-order controls that would cross the rank-zero boundary.


Validation and troubleshooting evidence
- Initial exact-head Android CI run 33388947458 passed the normal validation/build job. Its Android 16 runtime job exposed one stale coordinator test that still expected the formerly allowed unsafe order [home:2, home:0, home:1]. Production correctly returned PrimaryPageProtected; only the test expectation was corrected, with no weakening of production authority guards.
- Final exact PR head 77dfdf6ef4ea582210666b1d68aeec00dba4e0c9 passed both the normal validate job and Android 16/API 36 room-runtime-emulator job in workflow run 33389463089.
- Pull request #45 was squash-merged with expected-head protection to authoritative main as e59586056f6141a8b577bb0e244418327ca45e9d.
- Push-triggered Android CI run 33389921058 then passed both validate and the Android 16/API 36 runtime-emulator job on the exact merge commit.


Privacy, security, and architecture boundary
- No Android permission, INTERNET permission, network behavior, remote workspace service, dependency, Room schema, analytics, advertising, sponsorship, attribution, or tracking capability was added by this correction.
- This is a Development authority-integrity correction. It does not implement a primary-grid coordinate migration, primary-to-secondary or secondary-to-primary spatial movement, arbitrary cross-page drag/drop, folders, shortcuts, widgets/AppWidgetHost, representative physical-device Home acceptance, signed release packaging, production release acceptance, or Stable qualification.


Home Page Switcher Accessibility Context — Current Merged Development State


The Home page switcher now exposes one coherent accessibility semantic per authoritative page. The semantic is derived from the same Room-projected page context used by the visible switcher and includes page number, app count, unsupported workspace-item count when present, and selected/not-selected state. The visible compact page context and all existing workspace mutation authority remain unchanged.


This fifth-pass slice was rebuilt directly on the newer main revision after independent primary-Home compatibility protections landed, preserving primary rank-zero behavior and the secondary-page spatial-editing boundary.


Validation history: original PR #47 was closed unmerged after current main advanced and was superseded by PR #48 built directly on main e59586056f6141a8b577bb0e244418327ca45e9d. The first PR #48 candidate head e7839c20e0289f2f2f890f10efa41d070950c139 was not merged because Android CI #125 failed compilation on an accessibility helper naming mismatch; privacy, manifest, Glaze UI, Glaze Motion, and Room cutover guards had already passed. The helper API was aligned. Replacement exact head 27295d6e681aa47cdedf32a6466bfda94dfa8216 passed Android CI #126, including lint, unit tests, debug assembly, Room schema validation, and the Android 16 Room transition runtime emulator suite. PR #48 was squash-merged to main as d1c3c30ea7fcd654ade316739f35ad0509fbbbef.


Acceptance boundary: Development only. This is read-only accessibility presentation derived from existing Room authority; it does not establish folder/widget presentation, complete assistive-technology device acceptance, synchronization acceptance, Room production cutover acceptance, or Stable status.

## Drive Section 54 — Launcher Product Identity and Unified Search — Approved Scope Clarification

Official Launcher product identity


GoreeCloud Launcher requires a unique first-party product-specific visual identity so it is immediately distinguishable from other applications and launchers on a device. The current generic Android/placeholder identity is not an approved official GoreeCloud Launcher identity and must not be treated as release-ready artwork.


The canonical Launcher identity must follow the GoreeCloud System Utility application-family semantics while remaining unique to Launcher. Its visual concept should communicate application/activity access, navigation, launching, discovery, or the personalized front door to GoreeCloud without merely reusing the GoreeCloud corporate/platform logo. All canonical Launcher logos, icons, symbols, illustrations, and artwork must be stored and approved in GoreeCloud/branding-assets. GoreeCloud/android-app-defaults (`apps/launcher/`) may carry only traceable synchronized, generated, packaged, or Android-specific derivatives required by the application. Those derivatives must preserve provenance to the branding-assets canonical source and must not be treated as an independent source of truth.


No automatically generated, upstream, framework-default, or unreviewed substitute artwork qualifies as the official identity. Until a reviewed and approved canonical Launcher identity is supplied and committed with its derivative pipeline, product-identity acceptance remains incomplete.


Launcher Unified Search interaction


A one-finger swipe downward on an unobstructed Home-screen area is the default direct gesture for opening Launcher Unified Search. The gesture opens a first-party GoreeCloud search overlay/sheet rather than the application drawer or an external search application. The search field receives immediate focus and the software keyboard may open automatically when appropriate. The surface must remain usable with touch, hardware keyboard, switch access, and screen-reader navigation, and reduced-motion behavior must avoid relying on gesture animation for comprehension.


Launcher Unified Search is a first-party local-first orchestration and ranking capability owned by GoreeCloud Launcher. It is not merely a web-search field and does not require GoreeCloud Search, a GoreeCloud account, or network connectivity for core local results.


Local and first-party result providers may include installed applications; application shortcuts and direct actions; Launcher and device settings/actions exposed through supported Android mechanisms; user-created Launcher folders and widgets; contacts when the user explicitly enables the source and grants the required permission; photos, screenshots, videos, and other supported media through Android-scoped media APIs; files and documents exposed through supported Android document/provider access; first-party GoreeCloud application content exposed through explicit searchable-content contracts; GoreeCloud Drive content when authorized; recent and contextual local content; and compatible connected-device content when separately authorized and available.


Android platform boundaries apply. GoreeCloud Launcher must not request unrestricted filesystem access merely to imitate a privileged system index. File/document coverage must use supported document providers, user-granted locations, app-owned first-party provider contracts, Android search/index APIs, or other platform-compliant access. Media search must use scoped Android media access. Work, private, and other supported profiles must remain isolated according to Android and GoreeCloud Identity policy.


GoreeCloud Search integration


GoreeCloud Search participates as an optional first-party provider for web, current-information, and other online search categories. It does not become the authority for the Launcher local device index and local files, photos, contacts, application inventory, Launcher history, or local result payloads must not be uploaded to GoreeCloud Search merely to produce local results.


Where practical, online GoreeCloud Search should be integrated through an explicit first-party provider or handoff contract so the Launcher can preserve its offline-capable core and avoid adding direct networking solely for unified-search orchestration. If a future accepted architecture adds direct Launcher networking, its destinations, privacy behavior, failure semantics, and user controls require separate review and acceptance.


Result presentation and ranking


Unified Search should present grouped and clearly identified result categories such as Apps, Actions, People, Files, Documents, Photos, Screenshots, Settings, GoreeCloud services, connected devices, and Web. Exact and prefix matches should generally outrank fuzzy matches. Optional local recency, frequency, or contextual ranking must remain transparent, disableable, and device-local by default. There must be no sponsored, promoted, affiliate, or advertising ranking.


Results should support direct actions where safe and authorized, such as launching an application, opening a file or photo, calling or messaging a contact, opening a setting, invoking an application shortcut, opening a GoreeCloud service result, or handing the query to GoreeCloud Search. External intents, deep links, provider payloads, and cross-application searchable-content records must be treated as untrusted input and validated before invocation.


Privacy and control


Search source categories must be independently controllable where the underlying data is sensitive or permissioned. Search history remains local, independently clearable, and disableable. The user must be able to clear/rebuild local index state and understand which providers are enabled. Local search remains functional when online providers are disabled or unavailable. Personalization and contextual ranking should remain transparent and user-controlled.


Implementation and acceptance boundary


This section establishes the approved product requirement and default interaction contract. It does not by itself claim that the one-finger gesture, unified overlay, local content providers, contacts/media/document indexing, GoreeCloud Search provider integration, or official Launcher artwork are implemented, source-validated, runtime-accepted, release-accepted, or Stable. Each provider and permission-bearing source requires its own implementation and acceptance evidence.

## Drive Section 56 — Launcher Personalization, Universal Search Entry, Layout Lock, and Backup/Restore Clarification

This section records additional approved Launcher product requirements and supersedes less-specific wording where necessary. These requirements define target behavior; they do not by themselves establish source implementation, runtime acceptance, release acceptance, or Stable qualification.


56.1 Current design-system target
Launcher must target the latest applicable Stable Glaze UI release. The current product target is Glaze UI 2.2.0 Stable. Draft PR #62 exact head 4043895afd26991d0a20461e94d002f91c890703 supplies repository-local Glaze UI 2.2.0 Adoption Candidate evidence and keeps production eligibility false. Earlier 2.1, 2.0, and 1.x evidence remains historical migration/rollback context; complete application-specific rendered/native/accessibility/form-factor/representative-device acceptance is still required.


56.2 Launcher Universal Search experience
Launcher Universal Search should achieve the immediacy, clarity, visual polish, and result legibility associated with leading modern universal-search experiences while remaining an original GoreeCloud design rather than a visual clone. The surface must be designed natively with current Glaze UI semantics: strong typography and hierarchy, intentional layered surfaces, restrained depth/translucency where useful, fast query focus, grouped result presentation, clear provider/provenance cues, bounded fluid motion, reduced-motion and reduced-transparency alternatives, and first-class touch, keyboard, TalkBack, and switch-access behavior.


GoreeCloud Launcher is the canonical owner of the user-facing Universal Search experience, native provider framework, result aggregation, ranking, actions, commands, shortcuts, and core local search/index behavior. GoreeCloud Search and GoreeCloud Index are optional future providers or advanced backends once stable; neither is required for core Launcher Universal Search. The application drawer's narrow installed-app filter may remain a specialized Launcher view over the same Launcher-owned search foundations.


56.3 Launcher Universal Search Home entry mode
Launcher Settings should provide an explicit Universal Search Home entry-mode setting with two user-selectable behaviors:
- Permanent on Home — a persistent, native Glaze UI Launcher Universal Search affordance remains available on Home.
- Swipe down only — the persistent Home affordance is hidden and one-finger downward swipe on an unobstructed Home area remains the primary gesture entry.
Both modes must preserve an accessible non-gesture path to search. The setting controls presentation and invocation only; Universal Search authority remains with GoreeCloud Launcher.


56.4 Native Theme Manager and icon presentation
Launcher must provide a dedicated native Theme Manager rather than limiting appearance control to a simple theme-cycle action. The Theme Manager target includes Glaze Theme Engine integration, theme previews, System/Light/Dark and applicable future Glaze appearance modes, wallpaper-derived and user-selected palettes where supported, icon-pack support, icon masking, icon scaling, icon-shape/normalization controls, reset/fallback behavior, and coherent Home/Apps/folder/dock styling.


Third-party application icons must retain recognizable product identity. Icon packs, masks, frames, optical normalization, and scaling may adapt presentation but must not misleadingly replace third-party brand identity or bypass the current Glaze UI icon standards. Masking and scaling must remain bounded so icons are not cropped, distorted, or made inaccessible.


56.5 Home screen layout lock
Launcher Settings must provide a Home screen layout lock switch. While locked, placement-changing operations for supported Home content—including applications, shortcuts, widgets, folders, and other placeable Launcher items—must be blocked so layout remains fixed. Normal app/content launching and non-placement interactions remain available.


The user must be able to unlock the layout through either the Launcher Settings switch or by intentionally holding one finger on an unobstructed Home area for five seconds. The five-second gesture must provide clear progressive/complete feedback, must not require a privileged accessibility service, and must not be the only unlock path. Settings remains the deterministic accessible alternative. Future item types inherit this lock policy when their placement editing becomes implemented.


56.6 Launcher configuration backup and restore
Launcher Settings must expose explicit Backup Launcher configuration and Restore Launcher configuration actions. Core backup/restore must remain local/offline-capable and must not require a GoreeCloud account or network service. The backup format must be versioned and documented, validated before restore, and treated as untrusted input. Restore must fail safely on malformed or unsupported data and provide forward-migration handling for supported older schemas.


Backup scope should include supported Launcher preferences, Home/Dock/page organization, Theme Manager choices, icon presentation settings, Index Home entry mode, layout-lock state where appropriate, folders/shortcuts and other supported Launcher-owned configuration. Widget restoration must use safe rebinding/reconfiguration semantics rather than copying stale Android AppWidget IDs. Everkeep, GoreeCloud Backup, Drive, and Sync may later provide authorized preservation/transport/continuity around the same validated Launcher backup format, but the local backup/restore contract remains independently usable.


56.7 Acceptance boundary
These requirements expand and clarify approved product scope only. Current Development behavior must continue to be documented separately. Theme Manager completion, third-party icon-pack runtime acceptance, layout-lock enforcement across all supported item types, five-second unlock gesture acceptance, permanent GoreeCloud Index Home presentation, full Index provider coverage, versioned backup/restore implementation, representative physical-device acceptance, signed release packaging, and Stable qualification require separate source, runtime, accessibility, security/privacy, and release evidence.

## Drive Section 63 — Planned Feature Expansion — Customizable Intelligent Workspace Launcher

This section records the expanded product vision for GoreeCloud Launcher as a central GoreeCloud interface layer. It extends the existing launcher specification with broader workspace personalization, intelligent access, unified productivity workflows, and ecosystem integration requirements.


Approved capability areas include:


- Customizable home workspaces with drag-and-drop placement, multiple workspace profiles, dashboard layouts, widgets, modular panels, and specialized environments for personal, administration, development, monitoring, and media workflows.


- Glaze UI experience integration including glass-inspired surfaces, adaptive layouts, typography, iconography, dynamic themes, light/dark modes, shared components, motion principles, and accessibility-aware presentation.


- Advanced application organization through folders, smart folders, categories, tags, favorites, recent applications, collections, aliases, and personalized application management.


- Flexible application drawer experiences including grid, list, compact, category, and search-first modes with automatic categorization, manual sorting, hidden applications, private applications, and custom sections.


- Native Launcher Universal Search supporting applications, services, files, documents, settings, users, teams, organizations, servers, containers, workflows, commands, connected services, contextual resources, and other authorized GoreeCloud resources through the Launcher-owned provider framework.


- Command launcher capabilities providing keyboard-first workflows, quick actions, administrative shortcuts, system commands, deployment actions, backup actions, logs, and health checks.


- Gesture and shortcut customization including touch gestures, mouse gestures, keyboard shortcuts, swipe actions, and workflow launching.


- Smart widgets and information panels for system health, server status, storage, backups, security, network state, resource usage, and recent activity.


- Workspace profiles for administrator, developer, user, and guest environments with role-aware application visibility and permissions.


- GoreeCloud branding and icon system integration with official assets, adaptive icons, consistent application identity, and theme support.


- Theme and personalization expansion including wallpapers, transparency, blur, icon styles, typography scaling, density controls, animation controls, exportable themes, and shareable configurations.


- Privacy and security features including private spaces, protected folders, secure workspaces, permission-aware visibility, authentication-required areas, Wardveil Security integration, and Privacy Shield alignment.


- Configuration backup and restore capabilities covering layouts, widgets, themes, shortcuts, preferences, and supported launcher state using validated versioned formats.


- Cross-platform launcher experiences for desktop, mobile, tablets, web interfaces, and GoreeCloud appliances with adaptive input models.


- Future AI-assisted capabilities including intelligent suggestions, automatic organization, workflow recommendations, predictive search, natural language commands, and usage optimization while preserving user control and privacy.


- Native GoreeCloud ecosystem integration with Manager, Metrics, Backup, Security, Storage, Administration, Identity, Mesh, Everkeep, and related services through shared authentication, notifications, settings, and UI systems.


- Developer features including custom commands, dashboards, debugging tools, automation workflows, scripts, and API integrations.


- Enterprise capabilities including organization policies, role-based access, managed layouts, managed applications, centralized configuration, and deployment templates.


Product principles:


Beautiful — A polished interface designed for daily interaction.


Fast — Immediate access without unnecessary complexity.


Flexible — Users control their workspace and workflows.


Intelligent — The launcher adapts to user needs while remaining transparent.


Secure — Privacy, permissions, and protection are built into the experience.


Native — Designed specifically as the front door to the GoreeCloud ecosystem.


Acceptance boundary:


This section expands approved product scope and does not claim implementation, runtime validation, release acceptance, or Stable qualification. Each capability requires appropriate source evidence, testing, security/privacy review, accessibility validation, and release qualification before being considered complete.


63.1 Detailed Planned Features and Capabilities


The following detailed requirements expand the approved product scope in Section 63. Unless a capability is separately identified elsewhere in this specification as implemented and accepted, inclusion below establishes planned product scope only and does not claim completed implementation, runtime acceptance, release acceptance, or Stable qualification. Existing authority boundaries remain controlling, including GoreeCloud Launcher as the owner of Universal Search and its provider framework, official GoreeCloud Branding Assets as the canonical source for approved product identity, and applicable Glaze UI, Wardveil Security, Privacy Shield, GoreeCloud Identity, Everkeep, and other platform contracts.


63.2 Overview and Product Direction


GoreeCloud Launcher is planned as a customizable, beautiful, intelligent, and privacy-conscious native GoreeCloud interface experience. It provides a unified way to access applications, services, tools, files, workflows, contextual information, and permitted system functions while maintaining the current applicable Stable Glaze UI design language.


The Launcher product direction emphasizes personalization, productivity, speed, organization, privacy, consistency, and seamless integration with the GoreeCloud ecosystem. Its long-term role is to function as a central gateway into GoreeCloud rather than merely an application grid.


63.3 Customizable Home Workspace


The Home workspace should support personalized layouts with drag-and-drop application placement, custom grid layouts, adjustable spacing, multiple workspace pages, multiple dashboard or workspace profiles, custom widget placement, resizable application tiles, and modular information panels.


Planned workspace profiles may include Personal, Administration, Development, Monitoring, and Media environments. Each profile may present different applications, widgets, panels, shortcuts, and contextual surfaces while remaining subject to the user's permissions, device capabilities, and applicable identity/security policy.


63.4 Glaze UI Design System Integration


GoreeCloud Launcher must remain a native Glaze UI experience and target the latest applicable Stable Glaze UI contract. Planned visual behavior includes modern glass-inspired surfaces where appropriate, smooth and bounded motion, consistent spacing, refined typography, adaptive layouts, coherent iconography, dynamic themes, and strong light and dark appearances.


Shared design-system components should cover buttons, cards, panels, menus, dialogs, notifications, search surfaces, settings pages, and other common Launcher-owned controls. Glaze UI establishes presentation and interaction semantics and does not replace Launcher state or operating-system authority.


63.5 Application Management and Organization


Users should be able to organize applications and services using custom folders, smart folders, categories, tags, favorites, recent applications, frequently used applications, aliases, and custom collections.


Example collections may include Development for code editors, Git tools, containers, and monitoring utilities; Administration for servers, storage, security tools, and backups; and Personal for media, documents, and communication. Smart organization must remain transparent and user-overridable rather than silently reorganizing user content.


63.6 Advanced Application Drawer


The application drawer should support multiple presentation modes, including Grid, List, Compact, Category, and Search-first views. Organization capabilities should include automatic categorization, manual sorting, custom sections, hidden applications, permission-aware private applications, application aliases, and user-defined groupings.


The narrow installed-application drawer filter may remain a specialized Launcher-owned search view. It should reuse or remain compatible with the Launcher Universal Search provider and ranking foundations rather than creating an unrelated duplicate search stack.


63.7 Native Universal Search Owned by GoreeCloud Launcher


GoreeCloud Launcher should provide an immediate native Universal Search experience that it owns end to end for core functionality. Launcher is the canonical first-party authority for its search interface, provider registration, core local indexing, provider aggregation, permission-aware dispatch, result normalization, ranking, grouping, deduplication, provenance display, commands, shortcuts, contextual actions, and supported result actions.


Through standardized Launcher search providers, Universal Search may surface applications, services, files, folders, documents, people, teams, organizations, settings, system controls, application actions, shortcuts, GoreeCloud commands, recent and frequently used resources, cloud resources, connected services, contextual workspace resources, servers, containers, workflows, and AI-assisted answers or actions where appropriate. Planned UX capabilities include instant results, keyboard shortcuts, natural-language queries, recent searches, search filters, and transparent suggestions subject to provider availability and privacy/security controls.


Representative user intents include “Open Docker Dashboard,” “Show server backups,” “Create new project,” and “Open GoreeCloud Metrics.” Launcher should resolve these through its native provider framework and action layer. Future GoreeCloud Search or GoreeCloud Index integrations may enhance these results without becoming mandatory dependencies for the core experience.


63.8 Command Launcher


Launcher should provide a modern command-palette experience for keyboard-first navigation and authorized quick actions. Planned command categories include global command search, application actions, administrative shortcuts, system actions exposed through supported APIs, and GoreeCloud workflow actions.


Representative commands may include Restart service, View logs, Create backup, Open settings, Deploy application, and Check system health. Every action must be permission-aware, clearly attributed to the responsible system or service, validated before execution, and unavailable when the current user or device lacks authority.


63.9 Gesture and Shortcut System


Users should be able to configure swipe actions, double-click or double-tap actions where the platform supports them, keyboard shortcuts, mouse gestures, and touch gestures. Supported actions may open applications, launch workflows, open dashboards, invoke approved commands, navigate workspaces, or open GoreeCloud Index.


Gesture-only functionality must have accessible non-gesture alternatives for important tasks, and gesture processing must respect operating-system restrictions, reduced-motion preferences, and platform accessibility behavior.


63.10 Smart Widgets and Information Panels


Launcher-owned widgets and panels may surface system health, server status, storage usage, backup status, security alerts, network information, resource usage, and recent activity through authorized first-party integrations.


A Server widget may show CPU usage, memory usage, disk space, and online status. A Backup widget may show the last backup, backup health, and recovery readiness. A Security widget may show current threat status, relevant updates, and audit results. Data ownership remains with the authoritative source application or service; Launcher surfaces must not invent or independently redefine those states.


63.11 Workspace Profiles


Launcher should support multiple specialized environments. Administrator Mode may prioritize infrastructure tools, monitoring, security, and management consoles. Developer Mode may prioritize code tools, Git, containers, testing environments, and developer dashboards. User Mode may prioritize applications, documents, and communication. Guest Mode may expose a restricted, limited application set.


Profile behavior must integrate with supported GoreeCloud Identity and platform permission boundaries where applicable. Workspace profiles must not bypass Android user/profile isolation, managed-device policy, or service authorization.


63.12 Application Icons and Branding


GoreeCloud Launcher should use the official GoreeCloud icon system and approved branding assets where available. Planned capabilities include native GoreeCloud icons, adaptive icons, consistent icon styling, icon themes, custom application presentation, and tooling that can generate platform-specific derivatives from canonical approved assets.


Canonical GoreeCloud logos, icons, symbols, illustrations, and other brand artwork remain governed by GoreeCloud Branding Assets. Automatically generated derivatives must remain traceable to the approved canonical source and must not replace third-party application identity in a misleading way.


63.13 Themes and Personalization


Users should be able to personalize accent colors, wallpapers, transparency, blur effects, icon styles, font scaling, layout density, and animation intensity or speed within the boundaries of the active Glaze UI contract and platform accessibility requirements.


The theme system should support official GoreeCloud themes, user-created themes, exportable themes, and shareable configuration packages where safe. Imported themes must be treated as untrusted data, validated before use, and must not function as arbitrary executable code.


63.14 Animation and Motion System


Planned motion includes application opening, workspace switching, folder expansion, search transitions, widget updates, loading states, and other contextual state changes. Motion should be fast, purposeful, consistent, bounded, and accessible.


Reduced Motion and other applicable accessibility preferences must provide meaningful alternatives. Interaction comprehension must never depend solely on animation.


63.15 Privacy and Security Features


Planned privacy and security capabilities include hidden applications, protected folders, secure workspaces, permission-aware visibility, private spaces, and authentication-required areas where supported and separately accepted.


Wardveil Security and Privacy Shield remain the governing security and privacy systems. Launcher must minimize permissions, keep sensitive personalization local by default where practical, avoid advertising and behavioral tracking, validate external actions and imports, and never represent an area as protected unless the underlying authentication and authorization boundary is real and accepted.


63.16 Backup, Restore, and Configuration Portability


Users should be able to export and import supported layouts, back up themes, restore Launcher settings, and synchronize or transport supported configurations through approved GoreeCloud continuity services when available.


Backup scope should eventually include application arrangement, workspace pages, widgets using safe rebinding semantics, themes, preferences, shortcuts, folders, gestures, and other supported Launcher-owned durable state. The format must be versioned, documented, validated, recoverable, and independently usable locally without requiring a GoreeCloud account for core backup/restore. Existing partial Development portability formats do not by themselves satisfy complete product-wide backup and restore.


63.17 Cross-Platform Experience


The broader GoreeCloud Launcher experience is planned to adapt across desktop systems, mobile devices, tablets, web interfaces, and GoreeCloud appliances. Layouts and interaction models should adapt to touch, keyboard and mouse, large displays, and small screens.


The current native Android Launcher implementation remains the presently documented implementation path for Android phones and tablets. Other form factors and platform-specific Launcher experiences require separate architecture, implementation, security, accessibility, and acceptance evidence before they are treated as delivered products.


63.18 AI-Assisted Features — Future Scope


Future intelligent capabilities may include smart application suggestions, automatic organization, workflow recommendations, predictive search assistance, natural-language commands, and usage optimization.


Representative experiences may include “You usually check backups every morning,” “Your storage server needs attention,” and “Open your development environment.” Any future AI-assisted behavior must be transparent, user-controlled, permission-aware, privacy-preserving, independently disableable where appropriate, and must not convert sensitive Launcher usage into advertising or surveillance signals.


63.19 Integration With GoreeCloud Applications and Services


Launcher should integrate natively with appropriate GoreeCloud systems, including GoreeCloud Manager, GoreeCloud Metrics, GoreeCloud Backup, GoreeCloud security systems, GoreeCloud storage services, GoreeCloud administration tools, GoreeCloud Identity, GoreeCloud Mesh, Everkeep, and other approved platform services.


Integration goals include native Universal Search through the Launcher provider framework, optional future GoreeCloud Search and GoreeCloud Index providers, shared notifications where authorized, common authentication and identity through the approved GoreeCloud Identity model where applicable, consistent UI components, contextual actions, and centralized or coordinated settings where product boundaries permit. Each source application or service remains authoritative for its own data and actions.


63.20 Developer Features


Power-user and developer capabilities may include application shortcuts, custom commands, developer dashboards, debugging tools, workflow automation, script launching, and API integrations.


Developer actions must have explicit authority boundaries, safe input handling, clear provenance, and user-visible failure behavior. Script or command execution must not become an implicit privilege-escalation path or bypass managed-device policy.


63.21 Accessibility Features


Launcher must support strong accessibility across its supported surfaces, including keyboard navigation, screen readers, adjustable text sizes, high-contrast behavior, reduced-motion mode, reduced-transparency or solid fallbacks where applicable, and touch accessibility.


Important drag, gesture, or spatial-editing operations should provide deterministic non-drag and non-gesture alternatives where practical. Accessibility acceptance requires representative assistive-technology and device validation rather than source semantics alone.


63.22 Enterprise and Administration Features


For managed GoreeCloud environments, planned capabilities include organization policies, role-based access, user-specific layouts, managed applications, central configuration, and deployment templates.


Administrative policy must remain distinct from ordinary personal Launcher state and must integrate with the applicable identity, device-management, security, and authorization systems. Managed policy must be explainable to the user and must not silently masquerade as a user preference.


63.23 Core Design Principles


Beautiful — Provide a polished interface users enjoy interacting with while preserving clarity, accessibility, and performance.


Fast — Provide immediate access without unnecessary complexity or network dependency for core Launcher behavior.


Flexible — Give users meaningful control over their workspace, organization, appearance, workflows, and supported input methods.


Intelligent — Adapt to workflows transparently and only with user-controlled, permission-aware signals.


Secure — Build privacy, permissions, validation, recovery, and protection into the experience rather than adding them after the fact.


Native — Design the experience specifically for the GoreeCloud ecosystem while respecting the native contracts of each supported operating system and device class.


63.24 Summary and Product Goal


The long-term GoreeCloud Launcher vision combines an application launcher, native Universal Search and action surface, command center, workspace manager, dashboard system, productivity environment, personalization layer, and unified GoreeCloud entry experience.


Its product goal is to make accessing and managing GoreeCloud feel like using a cohesive premium operating-system interface rather than a collection of unrelated applications, while preserving user choice, clear system boundaries, privacy, security, accessibility, offline-capable core behavior, and truthful implementation status.


Acceptance boundary: This detailed expansion documents approved planned product scope only. Capabilities become implemented or accepted only when supported by the required source evidence, automated and runtime validation, platform-system integration acceptance, security and privacy review, accessibility validation, representative-device testing, signing/release evidence where applicable, and the governing Stable qualification process.

## Drive Section 64 — GoreeCloud Launcher - Native Universal Search

Architecture status: Approved planned architecture. This section defines the controlling product direction and authority boundary; it does not by itself claim implementation, runtime acceptance, release acceptance, or Stable qualification.
64.1 Architecture Decision and Supersession
GoreeCloud Launcher will provide a native, platform-wide Universal Search experience built directly into GoreeCloud. Launcher Universal Search replaces the previously planned initial dependency on GoreeCloud Index and GoreeCloud Search for the Launcher core search experience.
GoreeCloud Launcher must not depend on GoreeCloud Index or GoreeCloud Search to provide its basic Universal Search functionality during the initial implementation. Instead, Launcher provides its own native search, discovery, and action system.
This section supersedes Section 55 and any conflicting current planning language in Sections 56 and 63 that assigns Universal Search ownership, mandatory provider aggregation, or core search availability to GoreeCloud Index or GoreeCloud Search. Section 54 is restored as compatible product-scope context and is expanded by this section. Dated implementation checkpoints remain truthful historical/current-state evidence until deliberately migrated.
64.2 Core Capabilities
Launcher Universal Search should provide a single interface for discovering and accessing content, applications, services, resources, settings, and actions throughout the GoreeCloud ecosystem.
Users should be able to search for:
- Applications and services
- Files, folders, and documents
- People, teams, and organizations
- Settings and system controls
- Application actions and shortcuts
- GoreeCloud commands
- Recent and frequently used resources
- Cloud resources
- Connected services
- Contextual workspace resources
- AI-assisted answers and actions where appropriate
64.3 Search as an Action Layer
Universal Search should function as both a discovery system and an action surface. Search results may expose supported actions directly within Launcher.
Examples include:
- Search for an application and launch it.
- Search for a document and open, share, move, or inspect it.
- Search for a person and open their profile or available communication actions.
- Search for a setting and navigate directly to it.
- Search for a command and execute it.
- Search for an application shortcut and invoke the underlying action.
- Use natural language to locate the appropriate GoreeCloud resource or capability.
All direct actions remain permission-aware, must preserve source authority, and must validate untrusted provider payloads, intents, deep links, commands, and cross-application action records before invocation.
64.4 Native Launcher Search Architecture
The initial Universal Search architecture is owned and operated by GoreeCloud Launcher.
Launcher should provide native capabilities for:
- Search indexing
- Search providers
- Result aggregation
- Result ranking
- Search categories
- Permission-aware filtering
- Application discovery
- Service discovery
- Recent and frequently used resources
- Search history
- Commands
- Shortcuts
- Contextual actions
- Search provider registration
- Search APIs for GoreeCloud applications and services
Applications and platform services should be able to register searchable content, actions, shortcuts, commands, and resources through standardized GoreeCloud Launcher APIs. This allows Launcher to operate independently while maintaining an extensible provider architecture.
Provider contracts should define provider identity, searchable resource types, permissions and authorization requirements, query and result schemas, supported actions, ranking metadata, freshness, failure behavior, offline behavior, privacy handling, cancellation, timeouts, and compatibility/versioning where applicable.
64.5 Future GoreeCloud Search Integration
GoreeCloud Search remains a separately developed GoreeCloud capability until it is sufficiently stable and ready for production integration.
Once ready, GoreeCloud Search may be integrated into GoreeCloud Launcher as an additional search provider or advanced search backend. Potential capabilities include advanced content search, full-text search, cross-service search, semantic search, query interpretation, ranking enhancements, federated search across GoreeCloud services, and advanced filters or search operators.
Launcher should consume these capabilities without becoming dependent on GoreeCloud Search for basic Universal Search functionality. A Search outage or disabled Search provider must not disable core Launcher Universal Search.
64.6 Future GoreeCloud Index Integration
GoreeCloud Index also remains separate until it reaches sufficient stability and production readiness.
Once stable, GoreeCloud Index may be integrated into GoreeCloud Launcher as an indexing and retrieval provider. Potential responsibilities include large-scale resource indexing, metadata indexing, content indexing, semantic indexing, searchable resource catalogs, cross-service indexing, high-performance retrieval, and background indexing pipelines.
Launcher should be capable of using GoreeCloud Index when available while continuing to operate without it. Index integration must not transfer ownership of the user-facing Universal Search experience or make Index a mandatory core dependency.
64.7 Long-Term Layered Architecture
The long-term architecture separates the responsibilities of the three systems:
GoreeCloud Launcher - owns the user-facing Universal Search experience, search interface, core local search/index capability, result aggregation, actions, commands, shortcuts, and provider framework.
GoreeCloud Search - provides advanced search, retrieval, query-processing, discovery, federated-search, and semantic capabilities when integrated.
GoreeCloud Index - provides scalable indexing, metadata organization, content catalogs, retrieval infrastructure, and background indexing capabilities when integrated.
When GoreeCloud Search and GoreeCloud Index become stable, both should integrate into GoreeCloud Launcher through its provider architecture. Launcher remains the primary user-facing experience while Search and Index enhance its capabilities behind the scenes.
64.8 Architecture Principle
GoreeCloud Launcher owns Universal Search.
GoreeCloud Search and GoreeCloud Index enhance Launcher once they are stable and ready for integration, but Launcher must not depend on either service to provide its core Universal Search experience.
64.9 Migration and Acceptance Boundary
The current Development repository may still contain Index-oriented Universal Search contracts, settings labels, provider assumptions, or integration evidence from the superseded architecture. Those records remain truthful evidence of what was implemented or validated at the time, but they are migration targets rather than controlling future architecture.
Implementation work should migrate Launcher toward the native provider framework without falsely claiming that provider categories, cross-service indexing, AI-assisted answers, advanced ranking, Search integration, or Index integration already exist. Each provider and permission-bearing source requires source, runtime, privacy/security, accessibility, representative-device, recovery, release, and platform-system acceptance appropriate to its scope.
September 8, 2026 — Current Development checkpoint — GLAZE UI V1.3 reconciliation


The active Launcher migration candidate is PR #82 at exact head bd553a549276e3d243181c8e10beb403ca6cd752. It targets GLAZE UI V1.3 / 1.3.0 — Adaptive Resonance at exact Stable integration source fc7cc91d2eace8da2371371c2855c24cbcb326a1, with V1.2 / 1.2.0 as the immediate rollback baseline.


The machine-readable Platform Contract, native theme mapping, source guard, README/adoption record, Theme Manager catalog/guidance, and repository Platform Contract caller now align to the same V1.3 authority. The caller is pinned to exact tested central candidate 3204afc4f603f3b29a456f01cbb28755d7f7bd00 from Draft GoreeCloud Platform Contract PR #22 while central main remains on the prior validator; this is Development candidate authority, not merged central-main authority.


Exact-head validation passed Platform Contract run 34282302861 and Android CI run 34282302175.


Launcher remains Development and nonconformant. Rendered/native accessibility, phone/tablet/foldable, Human Visual Excellence, V1.2 rollback, Privacy Shield, Wardveil Security, Everkeep, Identity, Mesh, Manager, signing/provenance, release approval, and Release Candidate qualification remain open unless separately accepted by their authoritative records. Applicable Privacy Shield, Wardveil Security, and Everkeep integration remains independently evidence-gated; the current local/source behavior does not establish accepted runtime integration.


September 8, 2026 — Development checkpoint — strict persisted restore baseline and compensation


The current GLAZE UI V1.3 Development line has a stacked recovery-hardening candidate on draft PR #83, branch agent/launcher-restore-strict-baseline, exact head d571f8e7c0bc209c7d231fc6f5cdd320660c62c1. Before a new portable restore plans Room state or writes its recovery journal, the writer now reads the seven persisted portable preferences through the strict recovery decoder rather than the ordinary sanitized UI read path. Noncanonical persisted preference state therefore produces LauncherPortableRestoreRecoveryRequiredException before new restore mutation begins.


Failed-apply preference compensation now uses that same strict persisted-value decoder. Rollback is allowed only when the canonical current preference subset exactly equals the recorded previous state or the just-applied state. Invalid persisted state or any third canonical state is left untouched, compensation is treated as unverified, and the durable restore journal remains available as recovery evidence instead of normalizing corrupt values while deciding whether rollback is safe.


Exact head d571f8e7c0bc209c7d231fc6f5cdd320660c62c1 passed Android CI run 34284724763. The validation job passed exact-source/Development identity, privacy, manifest, identity, Glaze UI, Glaze Motion evaluation, Room cutover, lint, JVM tests, debug APK build, Room schema verification, provenance staging, and artifact upload. The dependent Android 16 Room runtime emulator job also passed the Room transition runtime suite.


This remains bounded Development recovery evidence. It does not make Room and DataStore a physical transaction and does not complete clean-target reconstruction, package/profile/folder/shortcut/widget rebinding, cross-device recovery, artifact provenance, Everkeep acceptance, Privacy Shield acceptance, Wardveil Security acceptance, production restore acceptance, Release Candidate qualification, or Stable qualification. Platform Contract did not rerun because this stacked candidate changes neither the platform manifest nor its workflow; parent V1.3 PR #82 retains the current Platform Contract evidence.


P0 HOME Return Automated Runtime Checkpoint — September 8, 2026


Draft PR #84 (`Return HOME intents to the Launcher primary surface`) exact head `a3cfab046c3b76696b4b426f113d4f83219274aa` passed Android CI run `34289320297` (run #317).


The exact-head validation job passed source/Development identity checks, privacy, manifest, Identity, GLAZE UI/Motion boundaries, Room cutover, lint, unit tests, debug build, Room schema checks, schema cleanliness, self-verifying Development APK staging, and artifact upload. The dependent Android 16 / API 36 runtime job ran the unfiltered `connectedDebugAndroidTest` suite: 38 tests started and all 38 completed successfully. This includes the existing activated-HOME activity-recreation coverage and the new real HOME-role/Home-key path returning the existing `singleTask` Launcher instance from Settings to the primary Home surface.


During stabilization, a keyed whole-root recreation implementation exposed Compose SlotTable disposal corruption. The candidate was corrected to reset Launcher surface/dialog state in place on HOME generation instead of disposing/recreating the composition subtree; the exact-head unfiltered runtime suite then passed.


This closes only the automated Development evidence boundary for the HOME-return slice of issue #80. Physical-device HOME-return validation remains required. Swipe-up reliability over workspace content, drawer transition quality, local Search fallback/augmentation, configurable drawer layouts, complete app enumeration, icon caching/performance/jank, Quickstep/Recents compatibility, and broader P1/later Launcher capabilities remain open. PR #84 remains Draft/unmerged; no Release Candidate, Stable, or production acceptance is inferred
September 8, 2026 — Development checkpoint — HOME content swipe reliability
Draft PR #86 (`fix/home-content-swipe-reliability`) advances the current Launcher Development stack to exact head `a571d2adf25baa78690a4fd3e48edb0e5755fcd1`. The Home surface now observes vertical pointer movement at the initial event pass so a Home swipe beginning over workspace/application child content can still invoke the established vertical gesture classification without consuming the child tile's normal tap or long-press path. Upward movement maps to the application drawer, downward movement maps to GoreeCloud Index search, and the existing drawer-dismiss path remains separate.
A pure direction/threshold classifier locks the bounded gesture contract with tests for upward, downward, below-threshold, horizontal-dominant, and invalid-threshold input. The first candidate CI failure was limited to the new test using `kotlin.test` on a JUnit-configured Android test source set; the test harness was corrected to the repository's established JUnit API without changing production behavior.
Corrected exact head `a571d2adf25baa78690a4fd3e48edb0e5755fcd1` passed Android CI #320 / workflow run `34297154252`. This remains automated Development evidence. Representative physical-device swipe-up reliability, drawer transition quality, local-search fallback/Index augmentation, complete app enumeration, icon caching/performance/jank, Quickstep/Recents compatibility, remaining issue #80 P0/P1 work, current platform-system integration acceptance, signing, release, production acceptance, and Stable qualification remain separate gates.
.


September 8, 2026 CDT / September 9 UTC — Development checkpoint — local-first Home installed-app search


Draft PR #88 (`fix/local-first-home-search`) is stacked on the exact green PR #86 Home-swipe correction. Search implementation revision `07aa83f757aa5681c240ab65975b17b0a7845d92` passed Android CI run `34301121296`; after the synchronized feature-roadmap control was added, the current PR exact head `11a8f3fd78e29aaccfbd8c13ee513d37c515d816` passed Android CI run `34326833690`. Home search now enters Launcher-owned installed-application filtering rather than requiring GoreeCloud Index. The local matcher uses the current LauncherApps inventory and matches visible label, package name, and launcher activity class with trimmed case-insensitive multi-term AND semantics. The existing drawer search field is reused and focused for explicit Home search. When GoreeCloud Index resolves, Launcher may offer `Search all GoreeCloud` as an optional extended-results handoff using the same query.


Authority boundary: this slice does not create a rival universal search engine, duplicate Index provider/ranking authority, export installed-application inventory, or add network, telemetry, Identity, Mesh, or remote-search authority. GoreeCloud Index remains the canonical universal search/index authority; Launcher owns only its bounded installed-app filtering and invocation presentation.


Validation: the normal source/build/governance job passed immediately on exact head `07aa83f757aa5681c240ab65975b17b0a7845d92`. The first Android 16 emulator attempt passed 37 of 38 instrumentation tests and failed only the existing HOME-return runtime test because no Compose hierarchy was observed. PR #88 did not modify that test or the HOME-reset path. A targeted rerun of the failed emulator job on the unchanged exact head passed, and Android CI #322 / workflow run `34301121296` completed successfully.


Acceptance boundary: PR #88 remains Draft/open/unmerged Development work. This closes the automated local installed-app search fallback/Index-augmentation slice only. Drawer transition quality, configurable drawer layouts/modes, complete application enumeration and Memos visibility on representative devices, icon caching/jank reduction, Quickstep/Recents compatibility, physical-device default-HOME/search/gesture acceptance, accessibility/performance evidence, signing, release, production acceptance, and Stable qualification remain open under issue #80.


LauncherApps Background Refresh and HOME Runtime Synchronization — Development
Draft PR #89 (`perf/background-launcherapps-refresh`) is stacked on exact-green PR #88 head `11a8f3fd78e29aaccfbd8c13ee513d37c515d816` and is validated at exact final head `997f34e3d8b566a9141caf00f2a58b0a860821a0`. `LauncherApps.Callback` delivery remains on the required main callback handler, but callbacks now enqueue only a lightweight conflated refresh signal. One serialized `Dispatchers.IO` worker performs profile/activity enumeration, deduplication, label reads, and sorting before publishing a completed inventory snapshot to the existing conflated app Flow. Existing profile coverage, activity inclusion, launch authority, workspace identity, local search semantics, and user-visible inventory rules are unchanged. The final candidate also uses locale-stable `Locale.ROOT` label ordering and includes a test-only HOME-runtime wait correction; production HOME lifecycle semantics are unchanged.
The initial production-only head `28c79bbb2cc31597fce70049824cc8fc705258fa` passed the validate job but exposed a reproducible synchronization defect in the pre-existing Android 16 HOME-return instrumentation test: immediately after real `KEYCODE_HOME` dispatch, Android can briefly expose no Compose hierarchy while returning the existing singleTask HOME Activity to the foreground. The final head changes only the test polling helper so that transient absence is treated as not-ready-yet; it still waits for the primary Home affordance and verifies the Settings surface is no longer active. Production HOME lifecycle behavior is unchanged.
Validation: exact final head `997f34e3d8b566a9141caf00f2a58b0a860821a0` passed Android CI run `34418001495` / #332 completely, including exact-source/Development identity, privacy, HOME manifest, product identity, GLAZE UI, motion, Room cutover, lint, JVM tests, debug APK assembly, Room schema checks, artifact staging, and the dependent Android 16 runtime-emulator suite.
Acceptance boundary: PR #89 remains Draft/open/unmerged Development work. This closes one verified main-thread inventory-refresh path and the corresponding test synchronization defect only. Shared icon bitmap caching/preloading, drawer-transition profiling, recomposition analysis, complete physical-device app enumeration/Memos visibility, configurable drawer modes, Quickstep/Recents compatibility, representative physical-device performance/accessibility, production signing, Release Candidate qualification, production approval, and Stable qualification remain open.
September 9, 2026 — Unicode-Normalized Local Installed-App Search — Development
Draft PR #90 (`fix/unicode-local-app-search`) is stacked directly on exact-green PR #89 head `997f34e3d8b566a9141caf00f2a58b0a860821a0` and is validated at exact head `8f21fd9fea399f7608e3e2b8bca7b8c7f8e75ea7`. Launcher-owned installed-app filtering now normalizes query, label, package name, and launcher activity class to Unicode NFC, uses locale-stable `Locale.ROOT` case normalization, treats ordinary whitespace plus Unicode separator characters such as NBSP and em space as term boundaries, and removes repeated terms while preserving the existing all-term local-match semantics. GoreeCloud Index remains the explicit optional handoff for universal search; this change adds no Index provider/ranking implementation, network access, query persistence, telemetry, package-visibility permission, Identity authority, or new inventory source.


Validation: Android CI run `34426712071` / #333 completed successfully on the unchanged exact head. The source/build validation job passed on its first attempt. The first Android 16 runtime-emulator attempt timed out in the pre-existing Room lifecycle recreation test `LauncherStateRoomMigrationTest.v2DatabaseReopensAndRetainsState` while waiting for the Activity lifecycle. PR #90 does not modify Room or Activity lifecycle behavior, and the exact parent had passed that runtime suite. Only the failed emulator job was retried; the retry passed with no source change, and the workflow now records overall success for exact head `8f21fd9fea399f7608e3e2b8bca7b8c7f8e75ea7`.


Acceptance boundary: PR #90 remains Draft/open/unmerged Development work. This is exact-source/build/emulator regression evidence for local matching only. Representative physical-device search ergonomics, full application enumeration including Memos visibility, accessibility, localization/RTL, drawer transition quality, shared icon caching/performance, GoreeCloud Index integration acceptance, Quickstep/Recents compatibility, platform-system acceptance, signing, release, production approval, and Stable qualification remain separate gates
September 11, 2026 — Launcher P0 Icon Cache Cancellation Isolation Development Checkpoint
Status: Development. Repository: GoreeCloud/android-app-defaults (`apps/launcher/`). Source-control stack: Draft PR #92 → Draft PR #93 → Draft PR #94. All remain open, Draft, and unmerged Development work; no child is authorized to bypass its parent integration order.


Draft PR #93 (`perf/shared-launcher-icon-cache-v2`) final exact head `003cf4622f226cc5bbe3618241fa5d9c20872b4e` passed Android CI run `34537425930` / #342. It establishes a bounded 8 MiB process-local LRU presentation cache, one canonical 144 px Android-provided badged-icon decode per cache entry, asynchronous loading across the active Home, app drawer, Dock, and additional Home-page surfaces, package/profile-scoped invalidation, generation-stamped stale-load rejection, and same-generation single-flight decoding. Android `LauncherApps` remains authoritative for app/profile visibility; cached icon bytes do not create inventory authority.


Review of that exact source found a narrower cancellation defect in the shared-load ownership model: the first UI caller that won a same-generation in-flight entry also executed the shared decode in its own coroutine, so cancellation of that transient Compose waiter could complete the shared deferred exceptionally for other surfaces awaiting the same icon. Stacked Draft PR #94 (`perf/icon-load-cancellation-isolation`) moves shared single-flight work into a process-cache-owned `CoroutineScope(SupervisorJob() + Dispatchers.IO)`. Individual UI callers only await the shared result and may cancel independently. Existing generation stamps, package/profile invalidation, stale-result rejection, and the 8 MiB LRU bound remain unchanged. Focused JVM regression coverage starts one shared load, attaches a second waiter, cancels the first waiter, and requires the second waiter to receive the result while the decode block runs exactly once.


PR #94 implementation head `dec7d1453de24b53814178dc85785c29421ada1b` passed Android CI run `34634736707` / #343. Repository-roadmap reconciliation advanced the final exact PR #94 head to `a27c3848097bfd4c151bddc64953abad8efc47d0`; Android CI run `34635453084` / #344 passed on that exact final head, including exact-source and Development identity guards, privacy/manifest/Identity/GLAZE UI/Motion/Room-cutover checks, Android lint, JVM unit tests, debug APK assembly, Room schema validation and cleanliness, artifact staging, and the Android 16 Room transition runtime suite.


The canonical `GoreeCloud/Feature Roadmap/GoreeCloud Launcher/FEATURE-ROADMAP.docx` was synchronized in place under existing Drive file ID `1Y9eFLv1583ffP1k3ra_smZZ0UpFMfRau`. The final four-page document was visually inspected on every page after the final edit, then re-fetched from Drive and verified byte-for-byte at SHA-256 `7bec0318f905682c46b4ab2396f53736468d809c2397ed0b33d46287255f4002`.


Acceptance boundary: This is Development presentation/performance hardening only. It adds no Android permission, package/profile visibility authority, persistence, network behavior, telemetry, analytics, GoreeCloud Index authority, GoreeCloud Identity authority, or new user-data collection. Android 16 Room runtime success is exact-repository regression evidence rather than representative icon-rendering or physical-device performance acceptance. Icon preloading, measured recomposition/drawer-transition profiling, representative physical-device/default-HOME performance, Quickstep/Recents compatibility, complete GLAZE UI V1.3 application acceptance, applicable Manager/Privacy Shield/Wardveil Security/Everkeep/Mesh/Identity acceptance, protected signing/provenance, Release Candidate approval, production approval, and Stable qualification remain open.
.


2026-09-12 — Launcher Experience Expansion and Implementation Kickoff


Status: Proposed capability expansion with first implementation tranche in Development.


The September 12 launcher-reference images are accepted as interaction and presentation inspiration, especially for clean home-screen composition, app-drawer organization, one-handed search, configurable grids, widget discovery, long-press actions, and polished launcher settings. GoreeCloud Launcher must reinterpret these patterns through Glaze UI and GoreeCloud architecture rather than copy Samsung One UI Home or another launcher.


Implementation has begun in stacked Draft PR #96, “Add configurable Grid and List app drawer modes,” based on the current Development stack in Draft PR #95. The first bounded tranche adds a persistent Grid/List app-drawer presentation preference, keeps Grid as the safe default/fallback, and reuses the existing Android LauncherApps inventory, local installed-app filtering, launch path, long-press placement management, shared icon pipeline, and icon-scale controls. The presentation preference remains outside the strict portable backup/recovery v1 seven-field subset so this change does not silently broaden the recovery schema; any future portable inclusion requires an explicit versioned snapshot migration.


GoreeCloud Index remains the canonical universal indexing, provider, ranking, and cross-domain search authority. Launcher continues to own only narrow local installed-app filtering plus Launcher-specific presentation and organization, with safe handoff to Index for broader results.


Expanded planned capabilities now include richer multi-page Home editing and overview; Grid, List, Compact, Category, Search-first, and optional paged drawer modes; alphabetical/custom organization and fast jump; top/bottom search placement; folders, smart folders, collections, tags, and privacy-aware hidden/private organization; Android shortcuts; AppWidgetHost widgets, widget stacks, searchable widget gallery, and first-party Glaze Cards; multi-select edit mode, undo/history, page cleanup, and safer layout recovery; independent Home/Drawer/Folder/Dock density and appearance controls; Launcher Profiles for Personal, Work, Travel, Focus, Minimal, and custom layouts; a future Glaze Shelf for pinned actions, media/device controls, temporary tasks, and compact first-party cards; work-profile/private-space correctness; phone/tablet/foldable adaptation; TalkBack, Switch, keyboard/D-pad, reduced-motion/transparency, and non-drag reordering accessibility; performance/power profiling; Privacy Shield authorization and visibility controls; Wardveil evidence-backed security surfaces; and Everkeep versioned backup/restore and migration recovery.


Additional candidate capabilities include app pairs and multi-window shortcuts where Android supports them, local recently-installed grouping, trusted deep-link shortcuts, explicit local device-state actions, search-driven Launcher settings through GoreeCloud Index providers, workflow/task spaces, first-party Glaze Card stacks, import assistance from other launchers when platform APIs and consent permit it, and user-controlled contextual surfaces that remain local-first, explainable, revocable, and free of behavioral advertising.


Release boundary: these additions are Development work. Grid/List implementation must pass exact-head automated validation and representative physical-device/default-HOME acceptance before release readiness can be claimed. Other capabilities remain planned until separately implemented and verified. No Release Candidate, Stable, production-approved, Privacy Shield-protected, or Wardveil-protected claim is implied by this specification update.


Development checkpoint — Home overview/edit mode — 2026-09-13


Draft PR #98 — Add Home page overview and edit mode — final exact head 6d35ac144e96b6fc61c3d969e2e7af442291099e passed Android CI run 34760609799 / #352 on attempt 1, including validation and the dependent Android 16 Room/runtime emulator suite.


Implemented Development scope:
- explicit Edit Home entry on the primary Home surface;
- ordered Home page overview with count-based page previews;
- explicit page selection;
- accessible non-drag Earlier/Later page reordering;
- page creation through the existing Room-authoritative mutation path;
- confirmed deletion only for completely empty non-primary pages;
- layout-lock enforcement for add/reorder/delete;
- fail-closed structural controls when the canonical primary Home page is not rank zero;
- HOME-return/authoritative-page-loss dismissal behavior; and
- pure policy tests for the page-action safety boundary.


Authority boundary:
Room remains authoritative for post-cutover Home page/item state. The overview does not create a second workspace authority and its preview cells do not claim authoritative Room cell coordinates. The canonical primary page cannot move or delete. GoreeCloud Index universal-search authority, Privacy Shield authority, and Wardveil Security authority are unchanged.


Release boundary:
This is automated Development evidence only. Representative physical-device/default-HOME, TalkBack/Switch Access, large-text/landscape, one-handed ergonomics, performance, review/integration, and release approval remain open. No RC, Stable, or production claim is made. Multi-select, group movement, drag-based direct manipulation, folders, widget hosting, and undo/history remain later bounded tranches.


Development checkpoint — Home multi-select and bounded group movement — 2026-09-13


Status: Development — automated exact-head validation green; representative physical-device/default-HOME, accessibility, ergonomics, performance, and release acceptance remain open.


Repository evidence:
- Draft PR #99 — Add Home multi-select and bounded group movement.
- Branch: feat/home-multiselect-group-move-20260913.
- Exact validated head: 6de20edb93a60c1f13c6bb196c9f48bc9fac334e.
- Android CI run 34761745372 / #353 completed successfully.
- The validate job passed exact-source/Development-identity checks, privacy/manifest/identity/Glaze/Room source checks, lintDebug, testDebugUnitTest, assembleDebug, Room schema verification, committed-schema consistency, and Development APK staging/upload.
- The dependent room-runtime-emulator job passed the Android 16 connected runtime suite.


Implemented Development boundary:
- Adds an explicit multi-select action inside Home overview/edit mode for currently resolved installed applications on a secondary Home page.
- Selection order follows the current Room-rendered page order rather than tap order.
- The canonical primary Home page and the source page are excluded from group-move destinations.
- Existing layout-lock authority is enforced, and stale selected keys are removed when the page inventory changes.
- Group movement reuses the existing Room-authoritative single-app move path and stops on the first rejected move.
- Launcher explicitly reports complete, partial, or zero-move outcomes; it does not misrepresent a partial sequential operation as atomic success.
- Pure policy tests cover primary-page exclusion, layout lock, destination filtering, deterministic ordering, stale-key removal, and destination availability.


Authority and release boundary:
Room remains the only post-cutover workspace mutation authority. This tranche does not create a second workspace store or bypass the existing snapshot guards. Multi-item movement is intentionally sequential and is not an atomic all-or-nothing transaction. If an early move succeeds and a later move is rejected, accepted moves remain applied and Launcher reports the partial outcome. Transactional batch movement, deterministic rollback/undo, direct group drag, folders, widgets, and broader edit history remain follow-on work. This checkpoint does not establish Release Candidate, Stable, production approval, physical-device acceptance, or assistive-technology acceptance.


Development checkpoint — Atomic Home batch movement and guarded rollback — 2026-09-13


Draft PR #99 final synchronized head `0a6e24c4b95df16a5372487666267348cf5689bb` passed Android CI run `34762388664` / #355, including the normal validation and Android 16 Room runtime jobs.


Draft PR #100 introduces the next Home edit-mode hardening tranche. Implementation head `2c48749eed203842968b5ce59a0aaec1745c3123` passed Android CI run `34762929330` / #356, including source/policy checks, JVM tests, lint/build, Room schema validation, Development APK staging, and the Android 16 runtime suite.


The PR #100 implementation replaces sequential multi-application movement with a Room-transactional all-or-nothing batch path for resolved applications on secondary Home pages. The complete HOME page/item snapshot is validated before planning, every target placement is computed before any write begins, and the transaction rechecks the complete authoritative snapshot before committing all selected item rows together. A complete post-write readback is verified before the transaction may succeed.


Selection order remains deterministic and is re-resolved from current source-page rank rather than trusting tap order. The canonical primary Home page remains protected and outside this secondary-page spatial edit path. Room remains the sole post-cutover workspace mutation authority.


PR #100 also introduces an opaque exact-state rollback checkpoint and rollback transaction. Rollback succeeds only while the complete HOME state still equals the batch's applied snapshot. Any intervening HOME page or item mutation causes rollback to fail closed rather than overwrite newer user state.


This rollback primitive is backend transaction/recovery foundation only. GoreeCloud Launcher does not yet claim a user-visible Undo action, durable edit history, process-death-safe undo history, direct group drag, folders, or bulk folder creation from this tranche.


Room entities and database schema version remain unchanged. Portable backup/recovery v1 remains unchanged. No new telemetry, behavioral profiling, networking, cloud personalization, package-visibility expansion, Privacy Shield protection claim, or Wardveil protection claim is introduced.


Status remains Development. Representative physical-device/default-HOME acceptance, TalkBack/Switch Access review, large-text and landscape validation, one-handed ergonomics, performance evaluation, Platform-System acceptance, signing/provenance, and release approval remain open before Release Candidate or Stable status can be considered.


Development checkpoint — One-level in-session Home Undo — 2026-09-13
Draft PR #100 current synchronized head `febefe0da2554e79449a0b5faf041679bdf96cf9` passed Android CI run `34763563436` / #359 after its Android 16 runtime job passed on rerun. Draft PR #101 implementation/test-hardening head `c7f95f3e5ff070795bc5906cb08d3f730d974d6e` passed Android CI run `34765759427` / #366, including normal validation and the Android 16 runtime suite.
PR #101 exposes an explicit `Undo last move` action for the most recent successful atomic secondary-Home multi-app move. Launcher retains exactly one `WorkspaceHomeBatchMoveCommit` in activity-local memory, replaces it after a newer successful batch, disables Undo while layout lock is active, returns to the source page after successful exact-state rollback, and fails closed/clears the checkpoint if authoritative Home state changed after the move. Runtime coverage verifies that a consumed checkpoint cannot be replayed.
The checkpoint is deliberately not persisted in DataStore, Room, portable backup/recovery v1, or Everkeep and is discarded by Activity recreation or process death. This is not durable or multi-step edit history. PR #101 also widens the existing hosted-emulator lifecycle/reactivity test settle bound from 15 to 30 seconds after repeated timeout-only flakes; that is test-harness reliability hardening, not performance acceptance or a product latency change.
Room remains the sole post-cutover workspace mutation authority. The canonical primary Home page remains protected, GoreeCloud Index remains the canonical universal search/index/provider/ranking authority, and the existing seven-field portable backup/recovery v1 contract is unchanged. Representative physical-device/default-HOME, TalkBack/Switch Access, large-text/landscape, one-handed ergonomics, measured performance, Platform-System acceptance, review/integration, signing/provenance, and release approval remain open. No Release Candidate, Stable, production, Privacy Shield-protected, or Wardveil-protected claim is made.


Final synchronized Development checkpoint — 2026-09-13
Draft PR #101 final synchronized head 2759260d3e821d3b1b5eb538367784b1d014e709 passed Android CI run 34766428420 / #368 on attempt 1. Both validate and Android 16 room-runtime-emulator succeeded. The implementation/test-hardening behavior remains the one-level in-session Home Undo validated at c7f95f3e5ff070795bc5906cb08d3f730d974d6e; later commits on PR #101 synchronize repository documentation only and do not expand application behavior. Canonical Drive roadmap bytes were replaced in place and verified against the QA-approved DOCX. Representative physical-device/default-HOME, TalkBack/Switch Access, large-text/landscape, one-handed ergonomics, performance/power, Platform-System, signing/provenance, review/integration, and release acceptance remain open. No RC, Stable, production-approved, Privacy Shield-protected, or Wardveil-protected claim is made.


Development checkpoint — One-level in-session Undo for atomic Home group moves — 2026-09-13
Draft PR #101 (`feat/home-one-level-undo-20260913`) adds an explicit `Undo last move` action on top of the exact-state rollback primitive introduced in PR #100. Launcher keeps only one successful batch checkpoint in activity-local memory, replaces it after a newer successful atomic batch move, disables Undo while layout lock is enabled, returns selection to the source page after a successful rollback, and clears/refuses a stale checkpoint when any intervening Home state change prevents exact-state proof. The checkpoint is deliberately not persisted to DataStore, Room, portable backup, or Everkeep and is discarded across Activity recreation/process death; this is not durable or multi-step history.


PR #101 final synchronized exact head `2759260d3e821d3b1b5eb538367784b1d014e709` passed Android CI run `34766428420` / #368, including both `validate` and the Android 16 `room-runtime-emulator` job. Its implementation/test-hardening head `c7f95f3e5ff070795bc5906cb08d3f730d974d6e` previously passed Android CI run `34765759427` / #366. The hosted-emulator lifecycle correctness wait was widened from 15 to 30 seconds after repeated infrastructure timeouts; this is test-reliability hardening and does not constitute startup/performance acceptance.


Room remains the sole post-cutover workspace mutation authority, the canonical primary Home page remains outside this secondary-page spatial edit path, and portable backup/recovery v1 remains unchanged. Representative physical-device/default-HOME, TalkBack/Switch Access, large-text/landscape, one-handed ergonomics, measured performance/power, platform integration, review/integration, and release acceptance remain open. No Release Candidate, Stable, production, durable-history, or complete drag/drop claim is made.


Development checkpoint — Atomic secondary Home page compaction — 2026-09-13
Draft PR #104 (`feat/home-page-compaction-20260913`) adds an explicit `Compact apps` action for eligible secondary Home pages. Compaction preserves authoritative item-rank order and deterministically packs resolved-shape 1×1 APP items into row-major cells within the current configured Home grid. It fails closed for the canonical primary Home page, malformed state, shortcuts/folders/widgets, non-1×1 items, insufficient capacity, layout lock, or a complete HOME snapshot that changed before commit. No cross-page spill or inferred reorganization is performed.


Room remains the sole post-cutover workspace mutation authority. The compaction transaction rechecks the complete HOME page/item snapshot, updates all changed placements together, and verifies complete readback before success. Room entities/schema and portable backup/recovery v1 are unchanged. The existing one-level batch-move Undo checkpoint is cleared only after successful compaction because compaction is a distinct workspace mutation and does not yet have a user-visible rollback checkpoint.


PR #104 implementation head `5429453ae58d794a11ec5e18b889cfd66ebae74c` passed Android CI run `34767635221` / #369 on attempt 1, including `validate` and the Android 16 `room-runtime-emulator` job. Representative physical-device/default-HOME, TalkBack/Switch Access, large-text/landscape, one-handed ergonomics, measured performance/power, Platform-System integration, signing/provenance, review/integration, and release acceptance remain open. No Release Candidate, Stable, production, folder/widget, durable-history, or complete drag/drop claim is made.


Development validation — PR #104 final synchronized head — 2026-09-13
Draft PR #104 final synchronized head ce140752ff422efaa390c52104acbfa04a86f91d passed Android CI run 34792500724 / #371: both validate and the Android 16 room-runtime-emulator job succeeded. This final documentation synchronization does not expand application mutation behavior beyond the already-validated compaction implementation. Representative physical-device/default-HOME, assistive-technology, large-text/landscape, one-handed ergonomics, performance/power, Platform-System acceptance, signing/provenance, review/integration, and release approval remain open. No Release Candidate, Stable, production, Privacy Shield, Wardveil, Sync, or overall platform-conformance claim is made.


Development stabilization — Platform Contract 0.4 reconciliation — Sep 19, 2026
Status: Integrated Development stabilization; PR #119 is merged to main.
Repository: `GoreeCloud/android-app-defaults` (`apps/launcher/`)
Validated candidate head: 70eb9949e28609102c6bb066c4bd60a4304f27a7. Integrated main commit: de03baa3fae9cfbd9902601074660820f625911e.
Implementation result: The integrated stabilization reconciles the root declaration from legacy Platform Contract 0.2/seven-system structure to accepted Contract 0.4/all nine Integral Platform Systems. GoreeCloud Policy and GoreeCloud Observability are now explicitly evaluated as applicable-blocked rather than omitted. The reusable contract workflow is pinned to the accepted Contract 0.4 validator revision. The repository continues to record the actually implemented GLAZE UI V1.1 / 1.1.0 source mapping while requiring migration to current Official Stable V1.5 / 1.5.1; historical unmerged 1.5.1 work is not treated as authoritative current source.
Validation: Platform Contract run 35481939250 / #116 succeeded. Android CI run 35481938883 / #421 succeeded, including exact-source/Development identity validation, repository guards, lint, unit tests, debug assembly, Room schema/cutover validation, and Android 16 Room runtime emulator acceptance.


Integrated Development stabilization — PR #120 workspace drawer gesture
Status: Integrated Development stabilization; PR #120 is merged to main.
Repository: `GoreeCloud/android-app-defaults` (`apps/launcher/`)
Validated candidate head: accf00cf8fc28b086eb825159915b659f72db969. Integrated main commit: f5150b99687946cbf34d963a3935de104e486e23.
Implementation result: The fixed primary Home LazyVerticalGrid no longer accepts user scrolling, allowing the parent Home vertical-gesture detector to receive drawer swipes that begin on actual workspace app tiles. The app drawer grid remains scrollable. Android runtime coverage now exercises this child-content gesture path and requires the Apps heading and Search apps field after the transition.
Validation: Android CI run 35485008474 succeeded on the exact candidate head. The primary validate job and Android 16 room-runtime-emulator job both passed, including the workspace-tile drawer-swipe regression.
Acceptance boundary: This integrated Development change does not establish representative physical-device/default-HOME gesture quality, transition quality, measured performance, accessibility, app discovery, Quickstep/Recents coexistence, current Glaze migration/application acceptance, production signing/distribution, Release Candidate, production, or Stable qualification. Issue #80 remains open.
Documentation reconciliation: repository-local NOTES were corrected through documentation-only PR #121 and PR #122. Readback on authoritative main 3ced7382d15166cb578aea7dcaef92d1926a8fab verifies stable source-bearing-baseline wording without self-referential current-main SHA drift. Runtime behavior and issue #80 acceptance boundaries are unchanged.


Acceptance boundary: This integrated Development change improves governance/validation truthfulness, not Launcher runtime capability. GitHub issue #80 physical-device/default-HOME, gesture/transition, app discovery, measured performance, accessibility, and Quickstep/Recents coexistence remain open. Platform-system runtime acceptance, production signing/distribution, Release Candidate, production, and Stable qualification remain unestablished.
Integrated Development stabilization — PR #123 Home/app-drawer transitions
Status: Integrated Development stabilization; PR #123 is merged to authoritative main.
Repository: `GoreeCloud/android-app-defaults` (`apps/launcher/`)
Validated candidate head: 43f84041f3c892e3d3b47c42e91b620dd525959a. Integrated main commit: f22d39d0801551470c0bb38c99d27e8de4172d62.
Implementation result: The top-level Home/app-drawer swap now uses bounded directional Compose slide/fade transitions, while Settings uses a short fade. Existing Home gesture authority and the fixed Home-grid non-scroll boundary remain unchanged.
Validation: Android CI run 35488393290 succeeded on the exact candidate head, including the primary validate job and Android 16 room-runtime-emulator acceptance. Existing workspace-content swipe regression coverage still reaches the drawer and verifies Apps and Search apps after the transition.
Acceptance boundary: This integration does not establish representative physical-device transition smoothness, measured frame timing or jank, reduced-motion/accessibility acceptance, Quickstep/Recents coexistence, production signing/distribution, Release Candidate, production, or Stable qualification. Issue #80 remains open.
Repository documentation reconciliation completed: PR #124 was exact-head validated and merged as a documentation-only change. Readback on authoritative main 8ac24f1fad635ee4b9bebe974dfeeb02395bf151 verifies that NOTES records PR #123 as integrated and retains f22d39d0801551470c0bb38c99d27e8de4172d62 as the latest source-bearing baseline. Runtime behavior and issue #80 acceptance boundaries are unchanged


Integrated Development stabilization — PR #130 app drawer layout modes
Status: Integrated Development stabilization; PR #130 is merged to authoritative main.
Repository: `GoreeCloud/android-app-defaults` (`apps/launcher/`)
Accepted candidate head: 0c6ba267ae6aad01696767774fd959eea1eb555f. Integrated main commit: 90db5f3c6fac192610872ca8adf5c353a45878de.
Implementation result: Launcher now provides persisted local Grid, Compact, and List app-drawer presentation modes while preserving Android LauncherApps inventory authority, local label/package filtering, launch/placement boundaries, and the existing 4/5/6-column setting for grid-based modes. The final accepted head restores use of the shared launcher icon cache for drawer tile/list rendering. Drawer-layout state remains outside the strict seven-field goreecloud-launcher-preferences/1 portable snapshot; Room workspace and existing recovery formats are unchanged.
Validation: Pre-merge Android CI run 35495421670 succeeded on the exact accepted head, including Android 16 runtime-emulator acceptance. Post-merge Android CI run 35496206431 succeeded on exact authoritative main 90db5f3c6fac192610872ca8adf5c353a45878de.
Acceptance boundary: Category and Search-first drawer presentation remain open. Issue #80 continues to govern representative physical-device/default-HOME behavior, measured performance/jank, accessibility, app discovery, Quickstep/Recents compatibility, complete GLAZE UI V1.6 application acceptance, platform-system runtime acceptance, recovery, protected signing/distribution, Release Candidate, production, and Stable qualification.
.

## Drive Section 66 — Requirements review and acceptance mapping — September 23, 2026
DOCUMENTATION CHECKPOINT  |  DEVELOPMENT  |  NO RELEASE APPROVAL
This section organizes existing requirements into an implementation-versus-acceptance review. It does not authorize new features, replace the original Sections 1–26, override the controlling Native Universal Search decision in Section 64, supersede the Implementation Plan, or duplicate the current repository-native implemented/planned inventories. The verified source snapshot is the September 23 main and synchronized, still-unmerged draft stack described above. Each row remains open wherever device, privacy, security, durability or release evidence is absent.
LCH-P0-01  |  HOME role, Home and Dock
Verified source / candidate: Integrated Development provides user-controlled HOME foundation, primary/secondary Home, a 5 × 6 default, preferred five-app starter Dock, up to 10 starter Home apps, off-default new-app auto-add, and Home/Dock editing/layout lock. Draft PR #248 adds direct App Drawer copy-to-Home/Dock, adaptive Dock capacity, and now carries the exact requested Home cell through the authoritative Room mutation path instead of append-then-move behavior. Edit Home and the page manager also present centered swipeable real-page carousels. The candidate remains unmerged.
Acceptance evidence still required: Representative Android default-HOME and HOME-return testing must verify restart, role loss/regain, stationary long-press/context behavior, hold-then-drag, exact occupied/empty-cell drops, Home/Dock moves, Dock growth/reorder/horizontal overflow, both Home-page carousels, wallpaper/widget/folder collisions, large font/rotation/foldables, one-handed ergonomics, and durable readback without loss. Affected gesture paths must retain non-gesture alternatives.
LCH-P0-02  |  Launcher-owned Universal Search
Verified source / candidate: Integrated Development provides Launcher-owned local Search, provider registration/policy, installed apps, actions/shortcuts, opt-in Contacts/Call-history/Messages and selected SAF file roots. Draft PR #248 now presents handoff-only connected providers as full Search online rows in the same result panel and implements an opt-in inline Google Drive provider using Google Identity authorization for drive.metadata.readonly plus bounded HTTPS Drive API v3 metadata search. Short-lived Drive access tokens remain process-local; Files remains the separate SAF source. Brave Autosuggest remains unavailable pending a governed non-embedded confidential credential path.
Acceptance evidence still required: Verify local/offline Search, deterministic grouping, permission grant/deny/revoke, Contacts name/phone and Call-history results, selected-file-root removal, User/Work isolation, provider failure, IME/large-text, and Search online handoffs. For Google Drive, verify Android OAuth client/package-signing registration, Drive API enablement, consent UI, result opening, shared-drive behavior, restart/expiry/revocation, profile/account isolation, and network failure/latency on representative devices. Remote queries must execute only for explicitly enabled and authorized inline sources or after a user-invoked handoff. Restricted READ_SMS denial is preserved; no permission bypass or default-SMS role substitution is permitted.
LCH-P0-03  |  App drawer, organization and accessibility
Verified source / candidate: Integrated Development: Grid/Compact/List/Category modes, User/Work inventory projection, configurable density and gesture-only downward dismissal. Draft #248 has mixed app/folder ordering, denser presentation and synchronized horizontal User/Work swipe/tab behavior.
Acceptance evidence still required: Exercise all modes with same-label apps, many installed apps, work-profile enable/disable, drawer entry/dismissal, scrolling and Search transitions on representative hardware. Verify screen-reader focus/order and large-font/landscape behavior; do not claim candidate-only functions are on main.
LCH-P0-04  |  Profile-aware inventory and isolation
Verified source / candidate: Integrated Development uses Android LauncherApps with package-plus-UserHandle identity and profile-aware results. The new draft drawer and notification counts retain explicit profile separation.
Acceptance evidence still required: Test personal, Work/Shelter and supported private-space profile lifecycle on representative devices; include same-package/same-label cases, install/update/remove/suspend, profile lock/unlock, cross-profile shortcut attempts and notification revocation. Never infer one profile’s data from another.
LCH-P0-05  |  Responsiveness and Android coexistence
Verified source / candidate: Development includes bounded icon cache/prewarming, async Search, repeatable Android 16 Room/runtime and transition-performance emulator CI, and a Development sidecar artifact pipeline. Exact PR #248 head 585632e0c0a6394d704cdc888318c8e6c17254b6 passed Android CI #1139 / run 36356345719 across the configured validation, APK staging, Android 16 runtime, and transition-performance lanes; the runtime suite completed 60/60 tests successfully with zero failures. The current head preserves bidirectional non-drag primary↔secondary Home app page management, full-screen Edit Home with explicit Done, fixed app-drawer icon/label cell geometry, foreground full-inventory LauncherApps reconciliation, and deterministic bounded adjacent-page Home application edge drops. The edge route carries deterministic landing-cell intent: right edge targets the destination leftmost column, left edge its rightmost column, and vertical release position selects the row. The Room-authoritative mover persists the requested cell only when the configured grid contains it and collision validation succeeds; invalid placement fails closed, while non-drag page movement retains first-available placement. The non-drag move-target presentation now identifies rank-zero explicitly as “Primary Home” while leaving ordinary page-switcher numbering unchanged; focused JVM coverage verifies that label policy. The candidate retains monotonic CI-run-number Development versionCode support and complete externally supplied Development keystore configuration with fail-closed partial-input handling while ordinary PR CI remains intentionally CI-debug signed. Artifact 10943723894 has ZIP SHA-256 5c00908099af0d8a224a8f1c6185b4c18f72a9bfbe9134760b9d9b431f72acf6; embedded APK SHA-256 is 59bdd1616a4aa609735f7a53dca41975e943d129780659ff494844df4af6d6ed; packaged versionCode is 1139 and the CI-debug signer certificate SHA-256 is 0a977786e52a41c095fcd95dfe5de816cf5ba1444cf69d589d2cb4469df6a3fb. The exact archive is stored at GoreeCloud/Artifacts/GoreeCloud Launcher/0.1.0-dev/CI-1139/GoreeCloud-Launcher-Dev-CI-1139.zip as Drive file 1ZUtuQJ1g-M-Tw2nFRTHEbza2fyUi4sNo, and Drive readback reproduced the expected 12,219,280-byte size and archive digest. Persistent protected Development signing and representative-device update-in-place verification remain open, as do representative-device edge-drop/Edit Home/drawer/page-management retesting, pointer-held hover-to-switch, arbitrary destination-cell selection after a live page switch, folder/widget cross-page drag, and broader accessibility/profile/performance/recovery/release gates.
Acceptance evidence still required: Agree measurable device-specific budgets before performance acceptance. Capture cold/warm HOME return, drawer open, first Search result, gesture input latency, frame-time/jank, memory, battery/power and long-running behavior. Validate Quickstep/Recents/default-HOME coexistence on representative supported Android variants; emulator timing is diagnostic only.
LCH-P1-01  |  Folders and advanced workspace editing
Verified source / candidate: Primary/secondary workspace and transactional Room placement exist in integrated Development. Unmerged draft #247/#248 adds folder creation/renaming, visual previews, mixed drawer folders, Home placement and cross-page folder movement.
Acceptance evidence still required: Check explicit folder create/rename/membership/remove/delete and recovery; test widget overlap rejection, concurrent/stale snapshot denial, profile boundaries, large layouts, cross-page moves and no item/identity loss on device. Smart folders and durable advanced edit history remain separately planned.
LCH-P1-02  |  Widgets and Android shortcuts
Verified source / candidate: Integrated Development has Launcher-owned Clock/Status widgets and Android AppWidgetHost-based picker/configuration, sizing and removal. The draft stack extends widget gallery and edit-mode movement.
Acceptance evidence still required: Verify permission/bind refusal, external configuration cancellation, third-party widgets, provider uninstallation, host-ID cleanup, spans, resize/move and process restart on device. Cross-install widget rebinding and rollback require a validated recovery design; no privileged AppWidget access is implied.
LCH-P1-03  |  Glaze UI and personalization
Verified source / candidate: Current main maps to Official Stable GLAZE UI V1.6 / 1.6.0 as source; four built-in Glaze wallpapers are integrated. Draft #243 adds preview-before-Apply; subsequent drafts add widget gallery, icon shapes/packs, folder styles and more wallpapers.
Acceptance evidence still required: Validate native rendered component states, preview-versus-Apply isolation, official asset traceability, icon contrast, color accessibility, TalkBack, touch targets, keyboard, reduced motion/transparency, RTL and phone/tablet/landscape adaptations. A source mapping or screenshot alone does not constitute product-local Glaze acceptance.
LCH-P1-04  |  Backup, restore and continuity
Verified source / candidate: Room placement and existing versioned portability foundations are integrated; newer provider preferences, file-root grants, widget IDs and draft folder state must not be silently treated as covered by the existing strict portable-v1 contract.
Acceptance evidence still required: Define a schema/version policy per new persisted capability; validate export checksum and provenance; show missing-app/profile/widget/provider previews; import to isolated staging; verify atomic commit, clean-target round-trip, process-death recovery, rollback, and device/profile rebind decisions without data loss.
LCH-P1-05 and LCH-RC-01  |  Privacy, security, platform and release
Verified source / candidate: Governing requirements include dedicated Privacy and Security settings, minimal platform permissions, off-default optional AI, no mandatory remote account/telemetry/ads, and applicable nine Integral Platform Systems. Draft #248 includes opt-in notification badges and an explicit Android listener-scope disclosure, not approved production access.
Acceptance evidence still required: Independently review notification-listener OS grant/deny/revoke, transient content-free per-profile counts, disabled-default behavior, sensitive Search permissions, optional integrations and data retention. Establish accepted nine-system runtime evidence, exact-head CI plus review, supported-device/accessibility evidence, recovery, reproducible protected signing, staged deployment and rollback before Release Candidate, production or Stable qualification.
Evidence and ownership. GitHub main, PRs and workflow runs are authoritative for implementation and validation. IMPLEMENTED-FEATURES.md and PLANNED-FEATURES.md own feature dispositions. The canonical Project Specification owns requirements; the Launcher Implementation Plan owns sequencing and test-matrix guidance; GitHub issue #80 and the existing Android Clients and Apps Stabilization Task List own active blocker tracking. Review and refine acceptance conditions without assigning invented device benchmarks or implying that any unmerged draft is an integrated feature.

## Drive Section 67 — Secondary Home spatial placement and exact same-page drag — September 27, 2026
Development result. Secondary Home pages now render applications and folders against their persisted Room cell coordinates rather than compacting visible items into sequential grid slots. Saved empty-cell gaps therefore remain visible and the rendered secondary page stays spatially consistent with the authoritative workspace. Held app drags on a secondary page can resolve an exact measured same-page destination cell. Adjacent-page edge transfer still takes precedence at a valid page edge, preserving the established deterministic cross-page landing-cell policy.
Mutation and safety boundary. The exact same-page mover accepts only secondary Home pages, validates the configured Home grid, rejects occupied and out-of-bounds destinations, and commits through the existing Room snapshot-checked item mutation path. The protected primary page remains outside this mover. Lazy-grid cell bounds are removed when the cell leaves composition so an off-screen cell cannot remain a stale drop target. No schema version, permission, network scope, package identity, or lifecycle state is changed by this tranche.
Validation and artifact evidence. Source-bearing head 50f5beaa6ec4c6b3eae7fed8a5ab0c6a0031d77c passed Android CI #1150. Final documentation-reconciled candidate head a080ee3f01b6af646460b8cdcf99f0c0af1d1d9f passed Android CI #1151 / run 36363613876: validate/build/lint/JVM/schema/APK staging, Android 16 Room/runtime (61/61 tests, zero failures), and Android 16 transition-performance all succeeded. Artifact 10946596360 has ZIP SHA-256 e639d8af50fe96ce279550656d506e717de9c0b5fa970705300bf1d2357bda21; the embedded APK has SHA-256 9ec6e886cba39b151720397a29fcbc6c5a2cc87500293913f600fcf1a5e6848c, package com.goreecloud.launcher.dev, versionName 0.1.0-dev and versionCode 1151. The exact ZIP is archived at GoreeCloud/Artifacts/GoreeCloud Launcher/0.1.0-dev/CI-1151/GoreeCloud-Launcher-Dev-CI-1151.zip as Drive file 1AVtQKHgiExq8ORGbJugxhQS-v6KTUxPb and Drive raw-byte readback matches the GitHub artifact digest and size.
Acceptance boundary. This closes the secondary-page render/persistence mismatch and exact same-page application-cell drag in Development source. Pointer-held hover-to-switch between pages, arbitrary destination-cell selection after a live cross-page switch, folder/widget cross-page drag, representative device touch/accessibility/form-factor acceptance, persistent protected Development signing/update-in-place, repository protection/independent review, and Release Candidate/Production/Stable qualification remain open.

## Canonical documentation relationships

- [README.md](README.md) — repository entry point and current user/developer orientation.
- [PROJECT-RECORD.md](PROJECT-RECORD.md) — significant project history, governance transitions, and migration evidence.
- [IMPLEMENTED-FEATURES.md](IMPLEMENTED-FEATURES.md) — evidence-backed implemented capability inventory.
- [PLANNED-FEATURES.md](PLANNED-FEATURES.md) — open, planned, partial, blocked, or acceptance-gated work.
- [CHANGELOGS.md](CHANGELOGS.md) — release/change-oriented chronology.
- [FEATURES.md](FEATURES.md) — detailed approved target capability inventory.
- [LICENSE](LICENSE) — repository license.


## Drive Section 68 — Home Dock and App Drawer owner requirements — October 4/5, 2026

Requirement level: Mandatory product direction.

Current authoritative Development main is `05303f2813742a1437d26886ca3f3b5a85836395`. Protected PRs #239 and #240 integrated the expandable Home Dock foundation and active-drag cross-page handoff. The Dock begins from the familiar five-app starter selection but is not capacity-limited to five applications. Current Development supports adaptive page density, independent horizontal Dock pages, optional looping, optional labels, direct Universal Search, Glaze/Clear/Solid/Raised/Edge presentation, Home↔Dock/direct Drawer placement, profile-qualified app identities, layout lock, and 48 dp minimum Launcher-owned interaction targets. Dock folders/non-app Dock item persistence, portable backup/recovery expansion, and representative-device/release acceptance remain open.

The App Drawer is required to remain a local-first, offline-capable, profile-aware application library and action surface. Current integrated Development provides Grid/Compact/List/Category layouts, profile pages, local installed-app filtering, A–Z/Z–A/local recent/local frequency/Pinned-first ordering, profile-qualified pins, hidden apps, App Lock, folders, shortcuts/widgets/context actions, and direct Drawer→Home/Dock placement. Draft PR #241 adds bounded device-local user-created tabs with exact profile-qualified membership and keeps Category application grouping active when Drawer folders exist by assigning folders to a dedicated Folders group.

Launcher-wide Settings must not occupy persistent App Drawer header chrome. The Drawer header remains focused on application organization/search; detailed Launcher configuration remains in Edit Home → Settings. Universal Search remains the broader Launcher-owned resource/action surface and connected sources remain explicit opt-in.

No Drawer or Dock capability may introduce sponsored placement, affiliate ranking, behavioral advertising, required remote analytics, or a mandatory GoreeCloud account. Smart folders, richer categories/tags/collections, recently installed/updated views, fast alphabetical jump navigation, portable custom-organization recovery, and representative-device accessibility/performance/profile/signing/release acceptance remain separately gated.

Exact-head automated validation is necessary but does not establish Production Acceptance, Stable, Seal, or Anchor.
