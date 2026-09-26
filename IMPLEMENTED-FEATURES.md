# GoreeCloud Launcher — Implemented Features

**Record type:** Repository implemented-feature inventory  
**Repository:** `GoreeCloud/launcher`  
**Lifecycle:** Development  
**Migration state:** **Authoritative on `main` after PR #201 merged as `009371938ac3cab041cfb0893ede68e66e211a4f` and default-branch readback verified this record.**  
**Runtime source baseline:** `03d4c3d2d7e355916412565b531e411d1bba71de` (PR #240 merged September 23, 2026). Repository-native feature records were reconciled after that runtime merge through PR #241.  
**Governing standard:** Standard — Repository Feature Tracking and Changelog Governance, version 1.0, effective September 22, 2026.

## Interpretation

This file records capabilities that are implemented in the current GoreeCloud Launcher Development source. It does **not** claim Release Candidate, production, Stable, representative-device, or complete platform-integration acceptance unless those states are separately supported by authoritative evidence.

Partially implemented capabilities remain open obligations in `PLANNED-FEATURES.md`. The same capability may therefore be described here for the portion that exists and in `PLANNED-FEATURES.md` for the portion that remains incomplete.

## Current verified source baseline

The latest source-bearing Launcher runtime is `03d4c3d2d7e355916412565b531e411d1bba71de`, the guarded squash merge of PR #240, **Stabilize selected file Search roots**. Repository-native feature/changelog records were subsequently reconciled through PR #241 without changing runtime behavior. PR #240 exact head `32c2972d53786d3171ec7f4586bb121fe192a991` passed Android CI run #734 / `35827675978` across repository/privacy/identity/GLAZE/Room-cutover guards, lint/build/unit/schema checks, Development APK staging, Android 16 Room/runtime emulator, and Android 16 transition-performance emulator lanes before merge. Artifact `10735388852`, `goreecloud-launcher-android-dev-sidecar`, is bound to that exact head with archive digest `sha256:1eb903e3f91596065b8d9e1231244dcf3750006d956f1b88d3f352343a70798b`; independent archive inspection matched that digest, its internal `SHA256SUMS` validated `GoreeCloud-Launcher-Dev.apk`, and the APK SHA-256 is `cdd6f5ee01cb32be7f3872a0b44bab7a1b52525fddab89f081430f410d39137d`. The runtime retains PR #238's user-selected local file Search plus explicit Google Drive, Dropbox, and Brave Search handoffs and now adds selected-root visibility/removal, confirmed persisted-read-grant release, and per-root failure isolation while preserving the local-first/no-automatic-third-party-fan-out boundary and PR #235's Home-editor-only Launcher Settings route. This baseline also includes PR #229's opt-in local Contacts/Call history/Messages and local application-shortcut Search sources, PR #228's rendered persisted Search-source controls, PR #226's four built-in Glaze wallpapers, PR #223's gesture-only app-drawer dismissal, PR #221's 5 × 6 starter Home/five-app Dock defaults, PR #219's AppWidgetHost-based Home widgets, PR #215's unified Home/Dock/App Drawer drag-and-edit interactions, and PR #212's source-manifest reconciliation.

This is Development evidence. It does not establish physical-device performance, complete accessibility, personal/work/Shelter/private-space acceptance, Quickstep/Recents compatibility, complete Integral Platform System acceptance, protected production signing/distribution, Release Candidate, production, or Stable qualification.

## September 23, 2026 — PR #240 stabilized selected file Search roots

**Change type:** Universal Search; Storage Access Framework lifecycle; failure isolation; Development stabilization.

PR #240, **Stabilize selected file Search roots**, was guarded-squash merged to `main` as `03d4c3d2d7e355916412565b531e411d1bba71de`.

Implemented:

- shows every currently selected Storage Access Framework Search root in the **Universal Search → Sources** Files control;
- adds per-folder **Remove** with an explicit confirmation explaining that Launcher will stop searching the folder and release its saved read access;
- removes the root from Launcher-local file-search persistence before attempting Android persisted-read-grant release, while keeping the operation reversible by choosing the folder again;
- rebuilds the active file provider when the selected-root list changes so removed roots stop contributing without retaining a stale provider index;
- isolates index construction by selected root so one revoked, malformed, or broken document-provider tree contributes no entries instead of suppressing healthy roots;
- preserves the existing 1,500-file aggregate index bound, depth bound, result bound, filename/MIME-only indexing, and no-file-content boundary;
- adds focused JVM regression coverage for failed-root isolation, global aggregate limits, and zero-limit behavior; and
- reconciles the user manual with current File Search, Home-editor Settings routing, and implemented AppWidgetHost behavior.

Privacy/security boundary:

- no `INTERNET`, broad-storage, `QUERY_ALL_PACKAGES`, telemetry, query-history persistence, file-content indexing, or automatic third-party query fan-out was added;
- file Search remains limited to user-selected SAF roots; and
- removing a root reduces retained Android access rather than broadening it.

Validation:

- exact PR head `32c2972d53786d3171ec7f4586bb121fe192a991` passed Android CI run #734 / `35827675978` across validate/build/unit/schema/APK staging, Android 16 Room/runtime emulator, and Android 16 transition-performance emulator lanes;
- Development artifact `10735388852` has GitHub archive digest `sha256:1eb903e3f91596065b8d9e1231244dcf3750006d956f1b88d3f352343a70798b`;
- independent archive verification matched that digest, internal `SHA256SUMS` validated the APK, APK SHA-256 is `cdd6f5ee01cb32be7f3872a0b44bab7a1b52525fddab89f081430f410d39137d`, and the APK ZIP structure validated successfully; and
- guarded squash merge commit: `03d4c3d2d7e355916412565b531e411d1bba71de`.

**Lifecycle boundary:** Development only. Representative-device folder-picker/removal UX, document-provider behavior, large-tree latency/memory/power, accessibility, profile isolation, provider compatibility, portable file-root recovery, Release Candidate, production, and Stable acceptance remain open under issue #80.

## Implemented capabilities

### Native Android launcher foundation

- Native Android launcher implementation using Kotlin and Jetpack Compose, with platform-native Android contracts where required.
- Android HOME-role onboarding and lifecycle-aware default-HOME state handling.
- Scoped Android package visibility for `MAIN` + `LAUNCHER` activities instead of broad `QUERY_ALL_PACKAGES` access.
- `LauncherApps`-based launchable-application discovery across available profiles, package/profile refresh handling, and launchable-activity deduplication.
- Application launching from Launcher-owned Home, Apps, Search, and supported secondary-page surfaces.
- Shared app-icon loading keeps a bounded stale-while-revalidate bitmap during package/profile cache invalidation and falls back from Android's badged activity icon to the authoritative activity icon when decoding fails, reducing transient placeholder icons without persisting third-party artwork.

### Home, workspace, pages, and Dock

- Wallpaper-backed primary Home surface using Android system wallpaper presentation.
- Four Launcher-owned built-in Glaze wallpapers — Glaze Aurora, Glaze Horizon, Glaze Nocturne, and Glaze Cascade — rendered locally from inspectable source and selectable from the existing Home/Universal Search Wallpaper action.
- Built-in wallpaper application uses the narrowly reviewed `SET_WALLPAPER` permission; the Android system wallpaper picker remains available as an explicit fallback, with no INTERNET, storage, analytics, ads, or remote-art dependency.
- Persisted Favorites and a bounded five-item Dock.
- Default 5 × 6 Home grid for a new Launcher preference store, while the existing supported grid presets remain configurable.
- One-time starter layout that prefers Phone, Messages, Email/Mail, Browser, and Camera for the five Dock positions when matching apps are available.
- One-time starter Home placement of up to 10 apps in the bottom two rows directly above the Dock; Launcher-local most-recent launch order is preferred when available, aggregate launch counts remain a compatibility fallback, and deterministic common/GoreeCloud app-role fallback fills any remaining slots.
- With **Show recent apps on Home** enabled (default), the normal primary Home view keeps up to 10 Launcher-recent apps directly above the Dock, excludes Dock duplicates, and fills missing recent slots from saved Home favorites. This suggestion view does not mutate persisted workspace placement.
- Suggested-but-unpinned recent apps launch normally but are not treated as draggable persisted favorites. A manual Home app placement/edit disables recent suggestions so the user's saved layout becomes authoritative; the setting can be re-enabled explicitly.
- Recent-app suggestions never request Android Usage Access and do not fabricate system-wide activity history. Launcher records only launches performed through Launcher while the setting is enabled.
- Long-press Home edit mode with visible grid/edit affordances and icon management actions.
- Unified drag/drop between Home and Dock, Dock reordering, Home reordering by cell, and App Drawer copy-to-Home/copy-to-Dock placement while preserving Drawer inventory.
- Home icon rename, Android App info, uninstall request, and placement controls; context actions disappear during active drag.
- Optional **Add new apps to Home** behavior in Settings, disabled by default, using a persisted local primary-profile launchable-app baseline so installs missed while Launcher is not running can be detected on the next inventory refresh without a manifest receiver or new Android permission.
- Minimal local app-activity ranking data stores only application workspace keys, aggregate Launcher launch counts, and a bounded most-recently-launched ordering. Recency is represented only by order: no timestamps, dwell time, cross-app history, queries, or network data are collected. Users can disable its suggestion use and clear the local usage data.
- Persisted Home-grid presets and application presentation settings.
- Direct primary-Home drag placement into configured grid cells, including guarded occupied-cell swaps and empty-cell placement.
- Persisted Home layout lock that gates implemented workspace mutation paths while ordinary launching and page selection remain usable.
- Five-second intentional Home hold path for unlocking with progressive feedback, with Settings retained as the deterministic non-gesture path.
- Room-backed authoritative workspace cutover/read foundations.
- Multi-page Home projection, page selection, empty secondary-page creation, guarded secondary-page reordering, and guarded deletion of eligible empty secondary pages while rank-zero primary Home remains protected.
- Compact/lazy page selector with accessibility context.
- Secondary-page app launch and guarded secondary-page movement/cell operations.
- Guarded primary-Home compatibility-to-spatial migration with persistent configured-grid placement.
- Development presentation of unsupported workspace-item counts instead of silently hiding their existence.

### Widgets

- Launcher-owned first-party Home widget catalog includes **Universal Search**, **Quick actions** (Apps, Search, Edit Home, Settings), **Battery**, **Date**, Digital clock, Compact clock, Analog clock, and **Launcher Status**.
- Universal Search and Quick actions route only to existing Launcher-owned surfaces. Battery observes Android's protected battery-state broadcast without polling, network access, location, telemetry, retained history, or a new runtime permission.
- Android third-party widget selection through the platform AppWidget picker with provider configuration before persistence when required.
- AppWidgetHost/AppWidgetHostView lifecycle integration without requesting privileged `BIND_APPWIDGET` authority.
- Host widget-ID cleanup for canceled/failed selection and successful widget removal.
- Room-backed widget type/provider binding, Home cell position, and horizontal/vertical spans.
- Span-aware primary-Home rendering, widget resize/remove controls, and exclusion of widget-covered cells from application drop targets.
- Ordinary Home/Dock application placement writes preserve validated widget rows while the legacy favorites compatibility projection remains application-only.
- Portable widget backup/restore is not claimed because Android `appWidgetId` bindings are not portable identifiers across restore targets.

### Apps surface and profile-aware presentation

- Separate Apps surface with local application filtering and launching.
- Persisted Grid, Compact, List, and Category presentation modes and related density/label/spacing controls.
- User Apps / Work Apps projection from Android `LauncherApps` inventory when non-primary profile inventory exists.
- App drawer header retains profile/layout context without persistent Settings or explicit close action buttons.
- Downward swipe is the drawer's explicit in-surface dismissal path; Android HOME-button return remains normal system navigation.
- Launcher Settings is reached through the dedicated **Settings** action inside **Edit Home**. Empty-space Home long-press is reserved for Edit Home; the Home quick-action row, app-drawer header, configurable gesture picker, and Launcher-owned Search no longer provide direct Settings bypasses. Historical saved direct-Settings gestures and stale Settings Search destinations are redirected to Edit Home.
- Search result identity labels that distinguish User and Work application matches where applicable.
- Removal of the duplicate rendered drawer-search control so the Launcher-owned Universal Search surface remains the primary general Search experience.

### Performance and icon handling

- Shared process-local LRU caching for Android-provided badged launcher icons with package/profile invalidation stamps and single-flight cold-load sharing.
- Bounded background icon warming with a 12 MiB cache budget, up to 128 candidates, and controlled batches of three while visible cold icons retain lazy fallback behavior.
- Latest-snapshot preload ownership from PR #205: a new authoritative `LauncherApps` inventory warm request cancels the superseded preload tail, and complete cache/profile-topology invalidation cancels background preload work before generation reset. Already-started single-flight decodes remain reusable by newer callers, while stamp checks prevent invalidated results from re-entering the cache.
- Transition diagnostics and Android 16 transition-performance emulator coverage remain available for Development validation; representative physical-device performance acceptance remains separate.

### Launcher settings and appearance

- Separate scrollable Launcher Settings surface.
- Persisted Home-grid, Apps-grid, icon-size, label-visibility, appearance, layout-lock, Launcher Universal Search entry-mode, local-usage-suggestion control, and off-by-default new-app-to-Home preferences where currently implemented.
- System / Light / Dark appearance selection and a reachable native Theme Manager path.
- Repository-level GLAZE UI V1.6 source mapping, material/accessibility policy, and validation guard, subject to the still-open downstream acceptance boundaries recorded in `PLANNED-FEATURES.md`.

### Launcher-owned Universal Search foundation

GoreeCloud Launcher owns the user-facing Universal Search experience. Current implemented Development capabilities include:

- A distinct Launcher-owned Universal Search surface that remains usable without GoreeCloud Search or GoreeCloud Index.
- PR #248 Development source reduces the idle Universal Search presentation to one focused Glaze search field with a leading search glyph, **“Find anything on your device…”** prompt, and an in-field settings control; result panels, status messaging, categories, and provider handoffs remain hidden until typing or another explicit action makes them relevant.
- Universal Search source management remains directly reachable from that settings control without reintroducing a persistent management row or weakening the existing opt-in/privacy boundary.
- Installed-app search backed by Android `LauncherApps` inventory.
- Trusted local Launcher actions/settings destinations.
- Deterministic local ranking, aggregation, deduplication, and fail-soft provider behavior.
- Cooperative cancellable asynchronous provider execution under caller/provider lifecycle control.
- Profile-identifying User / Work subtitles for installed-app results.
- Provider contract v1.0 descriptive metadata covering provider identity, provenance, offline behavior, authorization requirement, remote-processing declaration, query-retention declaration, and fail-closed duplicate/version compatibility evaluation.
- Privacy-first provider policy that limits automatic typed-query fan-out to reviewed providers that are local-only, require no authorization, perform no remote processing, and retain no query; network, remote-processing, retaining, authorization-requiring, and third-party providers are classified for explicit user handoff instead.
- UI-facing result-presentation model that keeps already-ranked application matches separate from Launcher action/settings results and can expose enabled explicit-handoff providers descriptively without invoking them or attaching the query payload.
- Versioned fail-closed serialization contract for provider enablement and explicit provider order, preserving absent-versus-explicit-empty semantics while excluding typed queries, results, history, usage/frequency signals, credentials, grants, and provider payloads (PR #198).
- Launcher-local dedicated Preferences DataStore persistence for that provider enable/order snapshot, with explicit `read`, `set`, and `clear` boundaries; absence remains distinct from an explicitly empty enabled-provider selection and malformed/unsupported stored data fails closed (PR #199).
- Persisted provider-control reconciliation and mutation policy: absent storage adopts only privacy-safe automatic-local defaults; loaded snapshots preserve explicit enable/order choices; malformed or unsupported stored values resolve fail-closed to zero automatic providers; and bounded enable/disable plus ordering helpers emit only the existing versioned provider-control snapshot (PR #207).
- Rendered **Sources** management backed by the persisted provider-control store, including provider identity, enabled state, privacy summary, deterministic Earlier/Later ordering, and safe-default reset while automatic execution remains limited to reviewed automatic-local providers (PR #228).
- Local Android application-shortcut Search via `LauncherApps.startShortcut` without a new runtime permission (PR #229).
- Explicit opt-in local Contacts, Call history, and Messages Search sources, each disabled by default and enabled only after the corresponding Android permission is granted; typed queries stay local and no INTERNET permission, telemetry, query-history persistence, or third-party automatic fan-out is added (PR #229).
- Launcher-owned Search no longer exposes a direct **Launcher settings** result; any historical/internal Settings destination resolves to Edit Home, preserving the long-press Home editor as the Settings entry path (PRs #229 and #235).
- Explicit opt-in local file Search over user-selected Android Storage Access Framework roots. Launcher stores only the selected tree URIs/read grants, indexes filename and MIME metadata within bounded depth/index/result limits, does not index file contents, and requests no broad storage permission (PR #238).
- Explicit **Search with…** handoffs for Google Drive, Dropbox, and Brave Search. Google Drive and Dropbox are exposed only when Android resolves the reviewed package-scoped search handoff; Brave Search uses its HTTPS query URL. Connected providers receive the query only after the user taps that provider, and Launcher still has no `INTERNET` permission or automatic third-party query fan-out (PR #238).
- Third-party provider query-retention state can be represented as provider-policy-controlled/unknown rather than inventing a retention guarantee (PR #238).

The PR #199 provider-control store and PR #207 reconciliation/mutation policy remain deliberately separate from the strict portable-preference v1 backup/recovery contract. Portable adoption/migration remains planned work. Rendered provider management, opt-in sensitive local sources, user-selected file-root Search, and the reviewed Google Drive/Dropbox/Brave explicit handoffs are implemented in Development; broader external provider discovery/registration and optional GoreeCloud Search/Index providers remain open.

### Privacy and local-first boundaries

- Core Launcher operation remains offline-capable and does not require a GoreeCloud server or cloud account.
- Current source has no Android `INTERNET` permission for core Launcher behavior.
- No advertising, sponsorship, promoted placement, affiliate ranking, mandatory analytics, attribution, or behavioral-tracking dependency is part of the documented Launcher product model.
- Search provider controls through PRs #188, #198, #199, #207, #228, #229, and #238 do not persist typed queries, results, history, credentials, provider payloads, or usage/frequency signals. Contacts, Call history, and Messages are opt-in local sources whose Android permissions are requested only when the user enables the corresponding source. File Search is limited to user-selected Storage Access Framework roots and indexes metadata rather than file contents. Connected Google Drive/Dropbox/Brave providers receive a query only after an explicit **Search with…** action; Launcher still has no INTERNET or broad-storage permission.

### Branding and asset provenance

- Canonical Launcher visual assets are governed from the GoreeCloud branding-assets repository; the Launcher repository retains traceable Android derivatives required for the application.
- Repository provenance metadata and validation guard prevent Launcher-local derivatives from silently becoming a competing canonical branding source.

### Build and validation foundations

- Repository CI includes privacy, HOME-manifest, identity, GLAZE UI, Room/schema, lint, unit-test, debug-build, and Android 16 managed-emulator coverage for exercised Development paths.
- Evidence from source, CI, APK staging, and managed emulators remains exact-revision-bound and is not treated as physical-device, production, or Stable acceptance.

## Evidence highlights

| Change | Evidence | Implemented result |
| --- | --- | --- |
| PR #238 | Exact head `71be92123352e5d179f48e5e1a892680aadd8987`; Android CI #729 / `35821960552`; merge `426e466f62d8347de720258be8492e518e84c4d5` | User-selected local file Search plus explicit Google Drive/Dropbox/Brave Search handoffs; no automatic third-party fan-out or Launcher INTERNET permission |
| PR #235 | Exact head `75581e01cdfb584b5f4af59377002ca835d0b064`; Android CI #723 / `35820108208`; merge `b68d1e56443a6e8365cf1d440e4ac5c53c868a02` | Settings-only-via-Edit-Home routing, reserved empty-space long-press, legacy direct-Settings gesture/Search compatibility routing |
| PR #229 | Exact head `740f967339de2adf0a4d70c26c9aeef2c2ecff7e`; Android CI #714 / `35818607895`; merge `0d6ccf955ae0884d4b06d9ed1839b377ab2a1af2` | Local app shortcuts plus opt-in Contacts, Call history, and Messages Search; no INTERNET permission |
| PR #228 | Exact head `851bf9007ea57209981b61c1ec11bc746ad0a009`; Android CI #703 / `35816326844`; merge `1e1f72c6a994e8381c0eecf3bd9fc3db17a44dde` | Persisted Search-source controls connected to rendered Sources management |

| PR #180 | Exact head `e2c8e613da320f020bc4f1960147eb03a6012685`; Android CI `35693613631`; merge `92e5c50fc82a2eb369a891e4afc3e1ec1a49dba7` | Launcher label/icon safe-area work, bounded icon warming, browse-order preservation, User/Work Search identity labels |
| PR #181 | Merge `a8d87cc7cd5a9c05bbb3d4bf699ad9ee36fcc15d`; exact-head CI `35695886062` | Rendered Universal Search moved to cancellable asynchronous provider execution |
| PR #182 | Exact head `58fb0430d8c0a6b6f06e01209ac4606a895459df`; CI `35697136234`; merge `78529be0917c072cd18846c2b8c213a1d3bc2ad1` | User/Work Apps projection and removal of duplicate drawer Search UI |
| PR #184 | Exact head `52bb20c19182b1a362561083949acaed3811cf6e`; CI `35699487607`; merge `e7221284f3eb7b9763923c469fb0ea9a555a7739` | Provider contract v1.0 metadata and fail-closed catalog evaluation |
| PR #188 | Exact head `cf2a608fcf6f590f5929ad69fa15279b0c082797`; CI `35700543213`; merge `0b54dffa1cb73d97c8d3a035807bf29b429e73e3` | Privacy-first automatic-local vs explicit-user-handoff provider policy |
| PR #195 | Exact head `f37fa1af628bf272946c79bccf57e3a846ae3ba6`; CI `35702386949`; merge `957f21a7a8904ee455c029e61d8a45466b725ed9` | UI-facing Search grouping and descriptive non-invoking explicit-handoff entries |
| PR #198 | Exact head `74cf28e2cc989e3e88d3cdd3e252dd69be45a71e`; CI `35707597053`; merge `d2600bc3f0b2fce6d3c8d524a8aef536e43cd1cb`; merged-main CI `35708430142` | Versioned provider enable/order serialization contract |
| PR #199 | Exact head `5646ce68d998ec96797d29a8c470df96c6ceac57`; CI `35710385034`; merge `ec6640dda8522244d57a947db083aecb8b9cfe33` | Dedicated DataStore persistence for provider enable/order controls |
| PR #205 | Exact head `f6696f4933b073e355c08e3871bd1f7365b4ceac`; Android CI #643 / `35758277303`; merge `439943d7918e4d04e9f7bf62707dc55d4a2898cd`; merged-main Android CI #644 / `35759394496` | Cancel superseded background icon-preload tails while preserving bounded cache, invalidation, and single-flight behavior |
| PR #207 | Exact head `f823ab9c1cb406116b68fe1f7c20cefef1ad6575`; Android CI #647 / `35794625569`; merge `0af5d6753d1dca98de432f24f8703fe5bae85e2c` | Fail-closed persisted provider-control reconciliation plus bounded enable/order mutation helpers |

## Material limitations

The following remain incomplete and therefore are **not** represented here as fully implemented: mature cross-page drag/drop; complete folders/smart folders; full AppWidgetHost/widget editing; pinned/dynamic shortcuts; complete work/private-profile behavior across all Launcher capabilities; complete Theme Manager/icon-pack behavior; complete portable backup/recovery; rendered provider management; external provider discovery and invocation; provider-specific consent flows; Search recents/history/context; complete Integral Platform System runtime acceptance; representative-device accessibility/performance/power validation; Quickstep/Recents acceptance; protected production signing/distribution; Release Candidate; production; and Stable qualification.

See `PLANNED-FEATURES.md` for the open obligations and acceptance gates.