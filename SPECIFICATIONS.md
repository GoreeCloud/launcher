# GoreeCloud Launcher Specifications

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

The rebuilt primary Home is a launcher-style surface rather than an engineering Favorites screen. Android renders the system wallpaper behind the launcher window through the native window-wallpaper contract, requiring no wallpaper/storage privilege. The primary surface renders the current Home application grid, Dock, Apps affordance, Launcher Settings affordance, and—when the selected entry mode is **Permanent on Home**—a Search GoreeCloud affordance that opens Launcher Universal Search.

A one-finger downward gesture on the unobstructed Home search zone opens Launcher Universal Search in both supported Home-entry modes. **Swipe down only** removes the persistent Search GoreeCloud affordance while retaining the Launcher-owned search gesture.

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

### Progressive-disclosure presentation requirement

Opening Universal Search from a Home gesture or other Launcher-owned entry point must begin in a deliberately minimal state. Before the user types, the surface presents only one refined Glaze search field: a search glyph on the left, the prompt **“Find anything on your device…”**, and a settings control on the far right for Universal Search sources and related search settings.

The idle Search surface must not display a redundant **Universal Search** title, explanatory privacy subtitle, category list, source inventory, result placeholder, or management row. Results, categories, provider-status notices, connected-provider handoffs, and other contextual controls may appear only after the query or an explicit user action makes them relevant.

The interaction goal is: **open Search → see one search field → type**. Progressive disclosure must simplify presentation without weakening source consent, profile isolation, local-first execution, explicit connected-provider handoff, permission, privacy, or accessibility requirements.

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

All canonical Launcher logos, icons, symbols, illustrations, and artwork must be stored, reviewed, and approved in **`GoreeCloud/goreecloud-branding-assets`**. The current canonical Launcher source is `products/launcher/app-icon.svg`. `GoreeCloud/launcher` is the current consumer repository and may carry only traceable synchronized/generated/packaged Android derivatives required for the application.

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