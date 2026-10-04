# GoreeCloud Launcher

> **Repository authority:** Active development is maintained in `GoreeCloud/android-app-defaults` under `apps/launcher/`. The standalone `GoreeCloud/launcher` repository is a temporary legacy source pending final reconciliation and required deletion.
>
> **Project governance:** [PROJECT-SPECIFICATIONS.md](PROJECT-SPECIFICATIONS.md) is the canonical project specification and [PROJECT-RECORD.md](PROJECT-RECORD.md) preserves significant history and evidence.


GoreeCloud Launcher is GoreeCloud's privacy-first, original Android HOME application and the intended native home, application-navigation, personalization, contextual-access, and Universal Search experience for GoreeCloud devices. Launcher is not a fork or visual clone of another launcher.

## Status

**Development — not a signed production/Stable release.**

The current repository contains a usable native daily-launcher foundation, a rebuilt Home / Apps / Launcher Settings product shell, guarded Room-authority multi-page workspace foundations, the first Launcher-owned Universal Search provider foundation with a built-in installed-app provider, and an integrated **GLAZE UI V1.6 / 1.6.0 source mapping** with a reachable native Theme Manager. Source-bearing PR #128 mapped Launcher to exact Stable release source `a7180679ea851389e0f3004515f9a25f420e716d` and added repository-local V1.6 material/accessibility/performance presentation policy without claiming downstream acceptance. Passing source/CI/emulator tests does not establish V1.6 consumer conformance, complete rendered/native/accessibility or physical-device acceptance, signed release, complete platform integration, or Stable qualification.

The complete approved target capability inventory is maintained in [FEATURES.md](FEATURES.md). Target capabilities are not implementation claims unless separately identified as current Development behavior and supported by repository evidence.

### Development package and update continuity

The side-by-side Development package remains `com.goreecloud.launcher.dev`. Current build configuration now accepts a positive `GOREECLOUD_DEV_VERSION_CODE`, falling back to the GitHub Actions run number in CI, so newly built Development artifacts no longer have to reuse a fixed versionCode. It also supports a protected external Development signing identity through the complete `GOREECLOUD_DEV_KEYSTORE_PATH`, `GOREECLOUD_DEV_KEYSTORE_PASSWORD`, `GOREECLOUD_DEV_KEY_ALIAS`, and `GOREECLOUD_DEV_KEY_PASSWORD` environment-variable set; partial signing configuration fails closed.

No signing key or password is stored in this repository. Ordinary PR CI still emits the explicitly labeled CI-debug-installability artifact and does not establish update-in-place continuity. Before upload, CI reads the assembled APK back and requires its package ID, versionName, and versionCode to match the expected Development identity; it also verifies the APK signature and records signer-certificate and APK digests in provenance. Provisioning a persistent protected Development key, securing the distribution workflow, and verifying an on-device update without uninstall remain open acceptance gates.


## Product role

Launcher is intended to become the personalized front door to GoreeCloud: bringing applications, files, people, devices, search, information, services, privacy controls, security state, continuity features, and contextual actions together within one adaptive interface.

**GoreeCloud Launcher owns Universal Search.** Launcher owns the user-facing search surface, provider framework, core local search path, result aggregation/ranking, and search actions. GoreeCloud Search and GoreeCloud Index remain separate systems that may later enhance Launcher as optional providers/backends once stable; neither is required for core Launcher search.

Launcher intelligence and personalization should remain transparent and user-controlled.

## Product rules

- No ads, sponsorships, promoted apps, affiliate placement, or monetized search ranking.
- No behavioral advertising or mandatory analytics.
- Core Home and local application use remain offline-capable.
- No required GoreeCloud server or account for core launcher use.
- Minimal, documented permissions; core Home/Apps/local Search remain offline-capable. The current PR #248 Development candidate declares Android `INTERNET` only for explicitly enabled connected Search adapters, with cleartext traffic disabled.
- No broad `QUERY_ALL_PACKAGES` access for ordinary launcher discovery.
- Core Universal Search is Launcher-owned and remains usable without GoreeCloud Search or GoreeCloud Index.
- All GoreeCloud logos/icons/artwork are canonical in `GoreeCloud/goreecloud-branding-assets`; this repository carries only traceable Android derivatives required by Launcher.
- Glaze UI is the Design Center authority for applicable interface behavior.
- Privacy Shield, Wardveil Security, Everkeep, GoreeCloud Identity, and GoreeCloud Mesh govern their applicable platform boundaries.
- A platform integration is not considered implemented merely because it appears in approved scope or UI copy.

## Current rebuilt daily-launcher shell

Current Development source includes:

- Android HOME activity and user-controlled `ROLE_HOME` onboarding;
- lifecycle-aware default-HOME status;
- scoped Android package visibility for `MAIN` + `LAUNCHER` activities and `LauncherApps` discovery across available profiles;
- package/profile lifecycle refresh and stable launcher-item deduplication;
- a distinct wallpaper-backed **Home** surface rather than an engineering Favorites screen;
- a separate **Apps** surface with local label/package filtering and launching;
- a separate scrollable **Launcher Settings** surface;
- locally persisted Home grid presets, Apps-grid columns, app-label visibility, icon-size preference, System / Light / Dark appearance, Home layout-lock state, and Launcher Universal Search Home-entry mode;
- ordered persisted Home Favorites, authoritative primary-Home grid coordinates after guarded spatial activation, and a five-item Dock;
- long-press placement management with accessible earlier/later controls and direct primary-Home drag placement into occupied or empty configured cells;
- a persisted **Lock Home screen layout** policy that blocks current Favorite, Dock, secondary-app, Home-page create/delete/reorder, and secondary spatial mutation callbacks while leaving app launching and page selection available;
- a visible locked-state Home control that can unlock the layout after an intentional five-second hold with progress feedback, while the Settings switch remains the deterministic accessible unlock path;
- terminal-Room multi-page HOME observation, page selection, protected-primary/secondary-page reordering, empty-page creation/deletion, and secondary application pages;
- a compact/lazy Home page selector with authoritative accessibility context;
- secondary-page icons rendered as normal launcher tiles, with movement/cell controls moved behind long-press management instead of permanently shown beneath every app;
- bounded secondary-to-secondary page movement, nearest-free-cell movement, and guarded exact one-cell movement;
- persisted Launcher Universal Search Home-bar choices for **Permanent on Home** and **Gesture only**; the legacy stored value remains unchanged for strict v1 backup/recovery compatibility;
- a **Search with GoreeCloud…** Home affordance when Permanent mode is selected;
- configurable Home gestures for Swipe up, Swipe down, Swipe left, Swipe right, Double-tap, and Tap and hold; defaults preserve Swipe up → Apps, Swipe down → Launcher Universal Search, and Tap and hold → Home editor;
- no legacy Index search-action package-visibility dependency for core Launcher search;
- GLAZE UI V1.6 source mapping pinned to exact Stable provenance, with inherited Stable spacing/material/state semantics, a conservative Launcher 48 dp touch floor, a 56 dp accessibility-oriented target, and fail-closed application acceptance;
- a reachable Settings → Theme Manager path with saveable/fail-closed sub-destination restoration, direct persisted System/Light/Dark theme selection, concise preview semantics, and a non-actionable selected-state surface; and
- Android system wallpaper presentation through the native window-wallpaper mechanism without requesting wallpaper/storage privileges.

The current Theme Manager remains deliberately bounded to System, Light, Dark, and Deep Dark. Icon-pack discovery/application, masking, wallpaper-derived palettes, expression controls, and complete Glaze Theme Engine behavior remain separate implementation and acceptance work.

## Complete app discovery boundary

Android 11+ package visibility requires launchers to declare which external activity class they need to discover. GoreeCloud Launcher declares a scoped `MAIN` + `LAUNCHER` visibility query and continues to use `LauncherApps` for actual launchable-activity discovery.

Launcher no longer declares the legacy `com.goreecloud.index.action.SEARCH` visibility handoff for core search. App discovery remains scoped to `MAIN` + `LAUNCHER` through Android `LauncherApps`, without `QUERY_ALL_PACKAGES`, Internet access, analytics, or an installed-application export path.

## Home, Apps, and Settings

### Home

Home uses the system wallpaper behind the launcher-owned surface, renders the current app grid and Dock, and keeps placement management behind long-press. The primary Home remains protected at rank zero; terminal Room authority can migrate its legacy compatibility rows into authoritative cell coordinates and persist direct drag placement, including empty-cell drops and occupied-cell swaps.

By default, the one-finger downward Home gesture opens Launcher Universal Search. **Permanent on Home** additionally keeps the Search with GoreeCloud… control visible; **Gesture only** removes that permanent control. The **Gestures** settings section can reassign Swipe up/down/left/right, Double-tap, and Tap and hold to supported Launcher actions or any currently launchable app. Core search does not depend on GoreeCloud Index or GoreeCloud Search.

When Home layout lock is enabled, current placement/page mutation callbacks are rejected at the Launcher composition boundary. Normal app launching, page selection, Apps navigation, and Settings access remain usable. The lock can be disabled from Launcher Settings or by intentionally holding the visible locked-state Home control for five seconds. The hold path has progress feedback; Settings remains the non-gesture accessible path.

### Apps

Apps presents the launchable application inventory in user-selectable **Grid**, **Compact**, **List**, and **Category** layouts. Its **Search apps** view is a specialized Launcher-owned projection of the same installed-app search provider used by the Universal Search foundation, while Android `LauncherApps` remains inventory authority.

Long-press placement management remains discoverable while the layout is locked, but current placement controls are disabled and explain that the user must unlock Home first.

### Launcher Settings

Current persisted settings include supported Home-grid presets, Apps layout, Apps columns, icon presentation, app-label visibility, appearance, Home layout lock, and Launcher Universal Search Home entry mode. The strict seven-field `goreecloud-launcher-preferences/1` portability format remains version 1 and intentionally retains the legacy `index_home_mode` wire key for byte-compatible backup/restore; that compatibility key no longer defines runtime search authority.

Launcher Settings now routes Appearance into the native Theme Manager through the saveable Settings destination model. Stale/unknown restored destination values fail closed to Settings root. The selected appearance is presented as status rather than another persistence action; only a different System/Light/Dark choice can invoke caller-owned theme persistence. Icon-pack selection, masking, Deep Dark, wallpaper-derived palettes, and expression controls remain separate work.

## Multi-page Room boundary

When terminal Room authority is active, Launcher can expose page selection, create empty pages, delete only revalidated empty non-primary pages, reorder secondary pages while keeping the protected primary page at rank zero, render secondary application pages, and request supported secondary spatial moves.

Room writes accept either the exact legacy primary compatibility projection or a bounded, collision-free primary spatial projection. Primary cell migration/movement uses complete snapshot comparison so concurrent changes, malformed placement, collisions, invalid bounds, or stale writes fail closed; primary↔secondary spatial transfer remains separately gated.

The Home layout lock is an additional Launcher mutation policy over these authoritative operations. It does not create a second workspace persistence authority. This remains a Development editing bridge, not a complete multi-page drag/drop editor.

## Launcher Native Universal Search

**GoreeCloud Launcher owns Universal Search.** The current Development foundation routes Home search directly into a Launcher-owned search surface and no longer requires the legacy GoreeCloud Index activity handoff.

The first native provider contract is implemented for installed applications. Android `LauncherApps` remains the authoritative application inventory; `LauncherInstalledAppsSearchProvider` projects that inventory into normalized Launcher search results. `LauncherUniversalSearch` aggregates provider results, isolates provider failures, removes duplicate provider/result identities, and applies deterministic local ordering. The current installed-app text ranking prioritizes exact title, title prefix, title substring, then package-name matches, and requires no telemetry or network access.

This is a foundation tranche, not the complete approved Universal Search capability set. Files, folders, documents, people, teams, organizations, settings, commands, shortcuts, cloud resources, connected services, contextual workspace resources, richer direct actions, and AI-assisted answers/actions remain separately implementation- and acceptance-gated.

GoreeCloud Search may later integrate as an optional advanced search/query/discovery provider or backend. GoreeCloud Index may later integrate as an optional scalable indexing/retrieval provider or backend. Neither service is required for the core Launcher search path, and optional provider failure must not disable that core experience.

Sensitive and permissioned providers remain governed by their own Android and GoreeCloud authority boundaries. Android scoped-storage, media, profile, package-visibility, Privacy Shield, Wardveil Security, Identity, Mesh, Policy, Observability, and Everkeep requirements are not bypassed by Universal Search.

## Official Launcher identity

All GoreeCloud Launcher logos, icons, symbols, illustrations, and artwork are canonical only in **`GoreeCloud/goreecloud-branding-assets`**. The current canonical Launcher asset is `products/launcher/app-icon.svg`. This repository is a consumer and may carry only traceable synchronized/generated/packaged Android derivatives required to build Launcher.

`branding/provenance.json` pins the canonical repository, asset path, and source blob used for the current Android derivatives. The previous Launcher-local portal-style artwork source has been removed so it cannot become a competing source of truth. Any future visual revision must be authored, reviewed, and approved in `goreecloud-branding-assets` first.

The Development APK currently uses adaptive, round, and Android 13+ monochrome derivatives synchronized from the canonical four-cell cyan-to-indigo Launcher asset. The existence of those derivatives does not by itself establish production visual-identity acceptance or Stable qualification.

## Privacy and search architecture

Core Launcher search remains local-first and provider-bounded. Launcher providers may expose only data and actions they are authorized to expose; Launcher must not scrape another application's private storage. Contacts require explicit authorization, media must use scoped Android APIs, files/documents must use supported provider access, and work/private profiles must remain isolated.

Search history, recency/frequency signals, provider controls, contextual ranking, and any future remote processing must remain transparent, user-controlled, and governed by the applicable platform contracts. There is no sponsored or paid ranking.

## Glaze UI boundary

Launcher retains a **GLAZE UI V1.6 / 1.6.0** implementation mapping at `a7180679ea851389e0f3004515f9a25f420e716d`. Current shared consumer authority is **Glaze V1.7 / 1.7.0** at release integration `1a5756daed2294155be2e9972b24f580f6222b7b`; V1.7.0 intentionally inherits accepted V1.6.0 runtime behavior. The existing mapping therefore remains useful implementation provenance, but Launcher is migration-required until it is freshly rebound and accepted as a V1.7 consumer. Retained Launcher pigments, atmosphere, radii, and 20/40 dp composition conveniences remain application-owned. Historical Glaze Motion evaluation remains Experimental/test-only.

This does **not** make Launcher V1.6-conformant, `aligned-current-stable`, or production-eligible. Caller/platform accessibility and performance inputs still require authoritative runtime wiring, and complete component/state coverage, rendered accessibility, localization/RTL, responsive phone/tablet/foldable behavior, representative physical-device validation, performance/power acceptance, rollback, Human Visual Excellence, platform-system acceptance, signing, and release approval remain separate evidence requirements.

## Current limitations

Still incomplete or separately gated:

- mature cross-page drag/drop and live span editing;
- primary↔secondary spatial movement;
- populated-page deletion with confirmation/recovery/undo;
- folders, shortcuts, widgets/AppWidgetHost, and richer workspace editing;
- complete Theme Manager behavior, icon-pack discovery/application, icon masking, Deep Dark, wallpaper palettes, expression controls, and additional gesture/registered-command targets beyond the initial configurable Home-gesture set;
- representative-device Theme Manager navigation/persistence/accessibility, focus order, and spoken-announcement timing acceptance;
- layout-lock coverage for future folders/shortcuts/widgets once those item types are implemented, plus representative physical-device five-second-hold acceptance;
- expanded Launcher Universal Search presentation beyond the current installed-app provider foundation;
- Launcher providers for files/photos/documents/contacts/calendar, settings/actions, and first-party searchable-content contracts;
- optional GoreeCloud Search and GoreeCloud Index provider/backend integrations once separately stable;
- versioned local Launcher backup/restore plus later authorized continuity integrations;
- complete production Launcher identity acceptance;
- complete Glaze Theme Engine behavior;
- accepted cross-device Sync/Mesh/Identity continuity;
- complete Privacy Shield, Wardveil Security, and Everkeep integration acceptance;
- Android OS process-death/schema-upgrade recovery acceptance;
- representative physical-device default-HOME and universal-search gesture/accessibility acceptance;
- signed release packaging/distribution; and
- production/Stable qualification.

## Documentation

Repository-native feature and change-history records:

- [IMPLEMENTED-FEATURES.md](IMPLEMENTED-FEATURES.md) — evidence-backed implemented Development capabilities and material limitations.
- [PLANNED-FEATURES.md](PLANNED-FEATURES.md) — planned, partial, blocked, deferred, and acceptance-gated capability obligations.
- [CHANGELOGS.md](CHANGELOGS.md) — authoritative human-readable repository change history after migration acceptance, with imported historical segments under `docs/changelog-history/`.

Additional product and technical documentation:

- [USER-MANUAL.md](USER-MANUAL.md) — current Development behavior and user guidance.
- [SPECIFICATIONS.md](SPECIFICATIONS.md) — architecture, scope, and authority boundaries.
- [FEATURES.md](FEATURES.md) — implemented Development behavior plus approved target scope.
- [BENEFITS.md](BENEFITS.md) — current and intended benefits.
- [COMPETITIVE-OBJECTIVES.md](COMPETITIVE-OBJECTIVES.md) — product completeness objectives.
- [Rendered HOME page navigation](docs/rendered-home-page-navigation.md) — terminal-Room page behavior.
- [Glaze UI adoption](docs/glaze-ui-adoption.md) — exact current-Stable source mapping and application acceptance boundary.
- `docs/` — architecture, persistence, design-system, validation, implementation, and historical changelog records.

Canonical project specifications and other governed project records may remain in the authorized GoreeCloud documentation hierarchy where applicable. Feature-state authority and Launcher changelog authority are repository-native and must not be synchronized to Google Drive.

## Build baseline

Current Android source uses Kotlin, Jetpack Compose, AndroidX Room/SQLite, DataStore, and Android SDK 36-era tooling. CI enforces privacy/manifest/identity/Glaze/Room guards, Android lint, JVM tests, debug assembly, Room schema validation, and an Android 16 runtime-emulator suite.

## License

GPL-3.0. See `LICENSE`.