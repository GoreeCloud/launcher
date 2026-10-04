# GoreeCloud Launcher — Implemented Features

## October 3, 2026 — inset, icon-mask, Search, and widget-gallery correction candidate

Development source now keeps the Launcher widget gallery, full-screen Home editor, and Universal Search inside Android safe-drawing insets so status bars, navigation regions, and display cutouts cannot overlap their primary controls; keyboard-sensitive surfaces additionally retain IME padding. The widget gallery uses an icon-led Android widget-picker handoff and replaces the installed-widget **Add** text pill with a Launcher-owned plus glyph. The Home search capsule likewise uses a first-party vector overflow glyph instead of a font-based ellipsis so its alignment is independent of font metrics. The full-screen Home editor also uses first-party check, add, and trash glyphs for completion, Add Page artwork, and empty-page deletion instead of text-only/literal-symbol controls, while keeping explicit semantics and a stable completion test tag. The shared app-icon mask renderer now places a shape-matched backing plane behind clipped artwork so the default **Rounded square** presentation remains visible for OEM/Android bitmaps that already contain circular transparent masking; Home, Dock, App Drawer, Search, folders, widget-provider rows, hidden apps, and App Lock surfaces share this path.

Universal Search shortcut groups now default to two direct shortcut glyphs plus an explicit overflow control, with expansion retaining access to the complete shortcut set. Voice, camera, image, checklist, drawing, search, note, and document-style shortcut labels map to distinct first-party glyphs, while unknown shortcut labels use a neutral launch/action glyph rather than a repeated Search symbol. Search Sources rows now reserve one consistent trailing toggle/disclosure width across On-device, Your content, and Connected sections; Files setup stays in the detail panel instead of widening only that compact row. The **Icons / Words / Both** presentation selector uses three equal-width 48 dp segments below its section label for stable phone-width alignment.

**Acceptance boundary:** implemented on stacked Development PR #233, not authoritative main. Fresh exact-head protected validation and representative-device visual/inset/icon/accessibility acceptance remain required.

## October 3, 2026 — restacked Theme and Home-page glyph-control candidate

Development source now replaces Theme Manager's text completion action and the current Home-page carousel's **Add page**, **Done**, **Move earlier**, **Move later**, and **Delete empty page** controls with Launcher-owned semantic vector actions. Every compact control keeps a 48 dp target and an explicit accessibility description. Existing page selection, layout lock, primary-page protection, empty-page deletion guard, Room workspace mutation, App Lock, provider, permission, and gesture authority remain unchanged.

**Acceptance boundary:** implemented on a stacked Development candidate, not authoritative main. Fresh exact-head validation and representative-device accessibility/reflow/form-factor acceptance remain required before integration or any stronger lifecycle claim.

## October 3, 2026 — App Lock and owner-feedback UI candidate

Development source now contains device-local, profile-qualified **App Lock** membership, a **Privacy & security** settings destination, app-context Lock/Unlock actions, and Android device-authentication gating for Launcher-originated app and app-shortcut launches. The App Lock manager includes app search, **All / Locked** filtering, per-app switches, Personal/Work plus package context, and empty-state handling. Apps grid, compact, list, and category views expose a small Launcher-owned lock mark and accessible locked-state semantics. Startup setup also surfaces App Lock directly.

The same Development source makes the ordinary Dock background-free, keeps material behind only the intentional Edge/drag-feedback states, removes the wallpaper-backed pill around Home page dots, uses a readable single-column built-in widget catalog on phone widths, reduces setup/help visual weight, replaces the Apps search text close mark with a vector glyph, and refines Settings/Search-source vector geometry. App Lock stores no PIN, password, biometric material, authentication token, telemetry, or network state; Android remains the authentication authority.

**Acceptance boundary:** implemented in the active Development candidate, not yet integrated into authoritative main. App Lock does not claim system-wide blocking: launches that originate outside GoreeCloud Launcher remain outside this feature's authority. Current-head protected validation, representative-device security/interaction acceptance, accessibility/reflow/form-factor validation, protected Development signing/update continuity, Release Candidate, Production Acceptance, Stable, Seal, and Anchor remain open.

## October 3, 2026 — onboarding and Search-source scanability follow-up

Protected PR #224 integrates compact segmented **Home apps**, Grid, and Dock setup choices with a 48 dp interaction floor and explicit radio-button selected-state semantics; active-step progress accenting; shorter setup and Search-mode presentation; direct **Sources** recovery from unavailable-source Search warnings; Launcher-owned Search-source reorder glyphs; shorter source/folder/provider summaries; bounded fallback-result titles; and end-to-end clear-query recovery coverage. The non-clickable gesture guide remains compact at a 44 dp card height without being treated as an actionable touch target. Search actions, including the Search Sources Back control, retain a 48 dp interaction floor.

Exact head `8ee98f3286e7e2e67677393664a905086a8d8581` passed provenance `37140755406`, Android Development Foundation `37140755431`, Migrated Android apps CI `37140755457` including the complete Launcher Android 16 runtime and transition diagnostics, and Protected promotion `37140755404` before expected-head-protected squash merge as authoritative main `06c4be22844c8f97e9c3fdee7757629ab11ccdb9`.

**Acceptance boundary:** Development integration only. Representative-device visual/accessibility/large-text/form-factor acceptance, protected Development signing/update continuity, Release Candidate, Production Acceptance, Stable, Seal, and Anchor remain open.

## October 3, 2026 — reversible profile-qualified Hidden apps

Protected PR #223 integrates device-local Hidden Apps state keyed by exact profile-qualified Launcher workspace identity. **Hide from Apps & Search / Show in Apps & Search** affects only App Drawer and Universal Search discovery; it does not disable or uninstall packages or remove existing Home, Dock, folder, widget, or Room placement. Launcher Settings → App drawer → **Hidden apps** exposes a count and direct **Show** recovery actions, distinguishes User and Work identities, and indexes hidden/hide/visibility terms for Settings search.

Exact head `4ce68c0201cb2779c91c3203e68a8d75fdc07cd6` passed Migrated Android apps CI `37138104658` including complete Launcher Android 16 runtime and transition diagnostics, migration provenance `37138104703`, Android Development Foundation `37138104654`, and Protected promotion `37138104626` before expected-head-protected squash merge as `b433104499372248d03cd0fed89fa729b6db1d77`. The same integration hardens HOME-role runtime-test isolation without changing production HOME authority.

**Acceptance boundary:** Development integration only. Portable Hidden Apps backup/recovery policy, representative personal/Work/Shelter/private-space behavior, accessibility/large-text/form-factor acceptance, protected signing/update continuity, release qualification, Stable, Seal, and Anchor remain open.

## October 3, 2026 — broader Search, widget, onboarding, and starter-Home polish

Protected PR #216 integrates the post-PR #194 polish tranche: an inline Universal Search clear-query control and no-result Search Sources recovery; compact Search Sources Reset/Order glyph actions and enabled/total counts; denser result/contact/handoff presentation; compact built-in widget catalog rows with truthful previews; Launcher-owned vector replacement for remaining battery/search/pin/check pseudo-glyphs; tighter top-anchored onboarding and Home-hint presentation; and bounded cleanup of only the reserved historical starter Calendar/Quick-actions widget identities while preserving user-created widgets.

Exact head `40d9608533936bbea29d73f2ccf33c7a7b0a6c05` passed Mandatory app migration provenance `37130779442`, Android Development Foundation `37130779438`, Migrated Android apps CI `37130779487` including complete Launcher Android 16 runtime and transition diagnostics, and Protected promotion `37130779497` before protected squash merge as `0978c4deaf460ed15bd9ffd37f573bf4d2772168`.

**Acceptance boundary:** Development integration only. Representative-device visual retest, accessibility/large-text/form-factor acceptance, protected Development signing/update continuity, Release Candidate, Production Acceptance, Stable, Seal, and Anchor remain open.

## October 2, 2026 — compact Universal Search, clean starter Home, and visual onboarding

Protected PR #194 integrates the CI-661 follow-up density and first-run redesign. Universal Search now keeps a distinct Top result while ordinary app matches, app-shortcut groups, handoff rows, and section headers use substantially denser presentation. Search Sources uses compact back/title navigation, keeps Suggestion tabs inside the normal scrolling list, flattens grouped source presentation, keeps the complete registered connected-source catalog visible, and reduces source rows to a concise two-line hierarchy with expanded details for technical/privacy information.

Fresh starter provisioning no longer automatically places built-in Calendar or Quick-actions widgets and sets the starter Home card presentation to Off without globally changing the established preference fallback. The three-step startup wizard uses a visible progress rail, explicit Step n of 3 orientation, visual Search-mode previews, compact Home/Grid/Dock choices, concise feature cards, and Learn more for advanced details. The post-setup Home hint uses the same condensed visual language.

Exact PR head `ef21e27304071c332ec72b2c1497332c6be2dbf9` passed Mandatory app migration provenance #660, Android Development Foundation #1152, Migrated Android apps CI #686, and Protected promotion #640 before squash merge as `a318fd982d0fa7ba437cca1a2a1fad5fcfe16be3`.

**Acceptance boundary:** Development integration only. Representative-device visual acceptance, protected Development signing/update continuity, accessibility/large-text/form-factor acceptance, Release Candidate, Production, Stable, Seal, and Anchor remain open.

## October 2, 2026 — representative-device UI correction after CI-649

Protected PR #192 integrates the first screenshot-driven follow-up after the CI-649 representative-device pass. Launcher-owned Calendar, Weather, and Quick actions Home widgets now use lighter wallpaper-aware Glaze surfaces; Edit Home and New Folder replace visible font/Unicode pseudo-icons with Launcher-owned vector geometry; Edit Home page previews use widget-specific miniature structures instead of generic blue blocks; the App Drawer sort/new-folder/settings artwork and sort popup are visually aligned to the dark Glaze drawer; fixed five-column Drawer labels use bounded single-line ellipsis; and Universal Search Sources keeps the complete registered connected-source catalog visible even when an optional handoff application is unavailable.

Search Sources now uses installed provider artwork when available with Launcher-owned fallback glyphs, and unavailable app-backed handoffs remain represented with explicit readiness state rather than disappearing. Google Drive, Dropbox, and Brave Search remain governed by their existing execution boundaries: Drive is opt-in remote-inline only when authorization is ready; Brave and Dropbox remain explicit handoff-only; unavailable handoffs fail closed. No new permission, account, query-retention, profile, network-fan-out, or workspace authority is introduced.

Exact PR head `982d4d3fb10aba707c7b61a7456e5f676d290492` passed Mandatory app migration provenance #635, Android Development Foundation #1127, Migrated Android apps CI #661, and Protected promotion #615 before squash merge as `f4e5279bd0cdd82786811ca0dd5cbb67fdc51f73`. Launcher validation/JVM/lint/build, Room schema verification, APK staging, the complete Android 16 runtime suite, transition-performance diagnostics, and the migrated-app required gate all passed. Exact-head sidecar artifact `11242758852` contains package `com.goreecloud.launcher.dev`, version `0.1.0-dev`, versionCode `1000661`, APK SHA-256 `8407de91febbdd0bc528a242666b0a0d642efe0714d36ce48059b8dd230a055f`, and declares `ci-debug-installability-only` signing with update-in-place continuity not established.

**Acceptance boundary:** Development integration only. CI-661 is staged for representative-device visual retesting. Broader visual acceptance, TalkBack/Switch Access, large-text/landscape/foldable acceptance, protected Development signing/update continuity, Release Candidate, Production, Stable, Seal, and Anchor remain open.

## October 2, 2026 — canonical Universal Search Sources Glaze presentation

Protected PR #190 rebuilt the Universal Search **Search Sources** manager around the owner-supplied canonical mockup while preserving the existing provider-control model. The integrated presentation now uses a rounded icon back control, stronger title/subtitle hierarchy, a three-way **Icons / Words / Both** segmented selector, a compact **Private by default** strip, grouped **On-device / Your content / Connected** cards, source-specific badges, denser row hierarchy, inline permission/readiness emphasis, disclosure chevrons, and a clearer Files folder action.

The visual change does not alter provider execution, Android permission authority, consent, source ordering, selected file roots, connected-provider authorization, query retention, profile identity, workspace persistence, or network policy. Registered providers remain authoritative even when an individual reference crop does not show every provider.

Exact PR head `e670a9662780c8d257fec4869d639cf252447650` passed Mandatory app migration provenance #621, Android Development Foundation #1113, Migrated Android apps CI #647, and Protected promotion #601 before squash merge as `080e004843ea9119e96cd1446353191e06144228`. The migrated-app matrix included Launcher validation/JVM/lint/build, Room schema verification, Development APK staging, full Android 16 runtime instrumentation, and transition-performance diagnostics.

**Acceptance boundary:** Development integration only. Representative physical-device mockup comparison, accessibility/large-text/form-factor acceptance, protected Development signing/update continuity, release qualification, Production, Stable, Seal, and Anchor remain open.

## October 2, 2026 — manual App Drawer pinned ordering and A–Z reset

Protected PR #185 integrated device-local, profile-qualified manual ordering for pinned App Drawer applications. **Pinned first** honors the persisted manual rank, and pinned Drawer app menus expose **Move pinned earlier / Move pinned later**. Protected PR #186 then added **Reset pinned order A–Z** when more than one Drawer app is pinned. Reset derives deterministic case-normalized label order from currently known profile-qualified app identities, uses the workspace key as a stable tie-break, persists only pinned-key order, and retains existing reconciliation behavior when pin membership later changes.

PR #185 exact head `69a65412f24271ecb627b2eb5aaf96b55de9e570` passed its complete protected exact-head matrix before squash merge as `7902e7bd58a510591e6c8f8778a3872df4c77642`. PR #186 exact head `d7dbd5b6cdb0b478f668aa406be774f1ee036420` then passed Mandatory app migration provenance `36990802823`, Android Development Foundation `36990802838`, Migrated Android apps CI `36990802796`, and Protected promotion `36990802778` before merge as `1bd60b949696d7774058ed19b7a6ecf67126e293`. Pin membership/order remains device-local presentation metadata outside portable preference v1 and does not mutate Home, Dock, folders, package state, or cross-profile authority. Representative-device accessibility, large-text/form-factor, performance/power, protected signing/update continuity, recovery, and release qualification remain open.

## October 1, 2026 — local Universal Search Quick answers

- Added a Launcher-owned **Quick answers** provider for arithmetic using `+`, `-`, `×`/`*`, `÷`/`/`, unary signs, and parentheses.
- Added allowlisted offline unit conversion for common length, mass, time, and Celsius/Fahrenheit/Kelvin values, including aliases such as `10 km to mi`, `5 lb in kg`, and `32 f to c`.
- Quick answers execute in-process through a bounded parser; they do not evaluate arbitrary code, request Android permissions, use the network, or retain typed queries.
- A selected answer copies the rendered result to the Android clipboard. Android 13+ uses the platform copy confirmation; older Android versions receive a short Launcher confirmation.
- The source is registered through the existing Universal Search provider-control contract as **Local only · No query retention**, and focused JVM coverage exercises precedence, parentheses, invalid input, division by zero, cross-dimension rejection, temperature absolute-zero rejection, copy payloads, and provider metadata.

**Acceptance boundary:** merged PR #149 integrated the feature to authoritative `main` as `1f3758eb9ea5722e6557bae56ecc3be240d3c12e` from exact head `a52056f558ef028b844f7a4317500a8602d219ae`. Mandatory app migration provenance #390, Android Development Foundation #850, Launcher build/JVM/lint/schema, complete API 36 runtime instrumentation, transition-performance diagnostics, migrated-app required gate, and Protected promotion #357 all succeeded before guarded squash merge/readback. Representative-device Search/clipboard/accessibility/large-text/form-factor/performance acceptance remains open; Launcher remains Development.

## October 1, 2026 — Home and Universal Search Glaze polish

The current Development branch adds a presentation-only polish tranche inspired by the approved Home and Universal Search concepts: a stronger Universal Search hierarchy, explicit local/connected-source boundary copy, direct source access, a device-wide search prompt, a ranked Top result, counted source sections, purpose-specific original vector result glyphs, semantic Search headings, refined translucent Glaze surfaces, adaptive Glance/Calendar/Weather cards, a more restrained floating Dock/page-indicator treatment, and updated Home Glance/Search optical treatment. Widget-picker and wallpaper Home previews use current local date/time or neutral purpose-specific glyphs rather than fabricated weather, date, time, or battery values. Existing provider execution, opt-in behavior, Android permissions, profile identity, Room workspace authority, Search actions, and reduced-transparency/performance fallbacks are preserved.

Merged PR #147 integrated this source to authoritative `main` as `d667a65ca565ff1512f4065a3462e4b16ca76f49` from exact head `29072f1fe007b5839a08afb4ea69f8c4167060be` after the complete protected exact-head matrix succeeded. This is Development integration, not release acceptance. Launcher remains mapped to Official Stable GLAZE UI 1.6.0; Glaze 1.7 remains Development and is not claimed as a consumer baseline.

## October 1, 2026 — App Drawer favorites

Merged PR #144 adds device-local, profile-qualified App Drawer pinning. Application long-press actions can pin/unpin an app in Apps, pinned state is surfaced in the app context status and directly on pinned app tiles/rows, the drawer sort menu includes **Pinned first**, and a ★ control can show pinned apps only on the active User/Work page. Drawer sort selection is persisted locally across surface/process recreation. Pin filtering/sorting remains presentation-only and never mutates Home/Dock/folder placement or cross-profile identity.

The feature is integrated into authoritative `main` as `d78ef5625ad14ef62c2405fdb697fe37aba81bec` from exact head `f98fd45d7c7dae86790f24ab4c05c13e4f66eb37`. Exact-head provenance, Android Development Foundation, Launcher build/JVM/lint/schema/APK, complete API 36 runtime, transition-performance, migrated-app required gate, and protected promotion all succeeded before merge. Representative-device accessibility/large-text acceptance and broader drawer organization remain open; Launcher remains Development.


## September 30, 2026 — post-consolidation stabilization and Home paging architecture continuation

- Authoritative Launcher development now lives in `GoreeCloud/android-app-defaults/apps/launcher/`; the standalone Launcher repository is migration history only.
- Merged PR #103 adds explicit App Drawer A–Z/Z–A/Most recent/Most frequent sorting, warm User/Work profile paging, stationary drawer long-press actions, responsive Home page switching, secondary-page vertical gestures, swipe-down Search fallback, and HOME-editor reset stabilization.
- Merged PR #104 adds exact Android-profile folders for Work Apps while keeping legacy folders primary-profile-only and enforcing membership in the persistence layer.
- Merged PR #105 confirms suspicious full-refresh inventory losses before publishing them when the Android profile is still active, reducing transient disappearing apps without delaying explicit package/profile removals.
- Merged PR #106 adds **Movable** Universal Search beside **Swipe down**, fixed **Top**, and fixed **Bottom**. Movable Search reuses the existing 4 × 1 Room-backed Universal Search widget, supports ordinary Home widget movement and cross-page placement, keeps a fixed-bottom fallback when no 4 × 1 primary-Home area is free, and retries managed placement after primary-Home geometry changes.
- Merged PR #107 removes the redundant 180–220 ms first-composition page-entry animation while preserving configured transitions for actual page-key changes.
- Merged PR #108 adds a content-only mode for secondary Home surfaces so page content can be rendered independently from persistent page indicators and Dock chrome.
- Merged PR #109 pages between two or more secondary Home pages with Compose `HorizontalPager`. Secondary page content follows the finger while the persistent Dock stays outside the pager, and the Activity-level threshold recognizer owns only the Primary↔secondary boundary.
- Merged PR #112 adds a Primary Home content-only mode to `LauncherBetaRoot`/Home. Merged PR #113 keeps one adjacent secondary page warm. Merged PR #114 threads authoritative selected-page identity into the root. Merged PR #115 keeps `LauncherBetaRoot` mounted across Primary↔secondary selection. Merged PR #116 extracts the full editable Primary `GlazeDock` contract; merged PR #117 hosts one editable Dock in the stable root; merged PR #118 puts Primary plus all secondary Home content under one Compose `HorizontalPager`; and merged PR #121 restores configured horizontal edge actions without consuming pager motion. Merged PR #127 restores the monorepo API 36 full instrumentation and transition-performance jobs and makes both exact-head promotion requirements whenever Launcher changes.

**Acceptance boundary:** PRs #103–#109, #112–#118, and #121 are merged Development source evidence; PR #121 is integrated as `23b3bc085ef2ae644a71bcea79667f2c3aade8f4`. Its accepted head `18abf1673529c1125e31a12ceb55a433eb505f56` passed the build/JVM/lint/schema and repository gates, but a post-merge audit established that the monorepo workflow was not executing Launcher Android instrumentation. Merged PR #127 supplied exact-head full API 36 Launcher runtime and transition-performance evidence for the restored gate topology. Representative-device/default-HOME multi-page frame pacing, edge gestures, input latency, memory/power, Search placement, package/profile churn, accessibility, large-text/form-factor, and visual acceptance remain open.

## September 30, 2026 — polished Calendar and Weather widgets

- Added first-party 2 × 2 **Calendar** and **Weather** widget entries and gallery previews.
- Calendar stays local-only; Weather combines local time with the existing foreground-location condition surface.
- Starter Glance keeps compatibility while using a richer gradient weather presentation.
- Successful weather snapshots are reused for up to 15 minutes in memory, preventing routine Home re-entry from visibly restarting weather loading.
- JVM coverage now includes the new catalog entries/default spans/search terms and cache-expiration policy.

**Acceptance boundary:** Development source; representative-device visual, accessibility, form-factor, and sustained performance acceptance remain open.

## September 30, 2026 — compact app context menu and HOME resume stabilization

- The primary app long-press experience now stays on one compact Glaze context menu; the redundant **More options** handoff and legacy `AppPlacementDialog` are removed.
- Home-origin app contexts expose **Remove**. Saved Home apps use the existing Home-placement toggle, while automatic Recent/Most-used suggestions can be suppressed locally without uninstalling the application or removing it from Apps.
- Android HOME re-entry dismisses full-screen Edit Home/widget-picker state and returns to the primary Home surface.
- Core application inventory, Launcher preferences, and authoritative workspace snapshots no longer render first-run placeholder defaults while their stores are still loading, and resume no longer forces full inventory/workspace reinitialization. This removes the intentional sources of the reported onboarding flash and Home icon/widget reload.
- Focused preference coverage verifies that automatic-suggestion suppression persists independently of the configured automatic Home mode.

**Acceptance boundary:** Development source on PR #99. Exact-head CI and representative physical-device return-to-Home/context-menu/accessibility validation remain open; these changes do not establish Release Candidate, production, or Stable acceptance.
## September 28, 2026 — accessible App Drawer page indicators

Paged App Drawer layouts now keep the existing restrained **6/8 dp** visual dots inside explicit **48 dp** interaction surfaces. Each page target exposes a stable test tag plus a page-position accessibility label and selected state while preserving the compact Glaze visual treatment. Focused policy coverage locks the interaction floor separately from the visual-dot geometry.

**Acceptance boundary:** this is Development source/test coverage on Draft PR #248. Exact-head Android CI plus representative-device touch accuracy, TalkBack/Switch Access traversal, large-text, landscape/foldable, and one-handed paging acceptance remain open.


**Record type:** Repository implemented-feature inventory  
**Repository:** `GoreeCloud/android-app-defaults` (`apps/launcher/`)  
**Lifecycle:** Development  
**Migration state:** **Mandatory monorepo consolidation is complete. New Launcher source work belongs only under `apps/launcher/`; legacy standalone history below is retained as dated provenance.**  
**Current authoritative main:** `06c4be22844c8f97e9c3fdee7757629ab11ccdb9` after protected monorepo PR #224 on top of Hidden Apps PR #223. Historical standalone baseline sections below retain their dated provenance and do not override the current monorepo integrations recorded above.  
**Governing standard:** Standard — Repository Feature Tracking and Changelog Governance, version 1.0, effective September 22, 2026.

## Interpretation

This file records capabilities that are implemented in the current GoreeCloud Launcher Development source. It does **not** claim Release Candidate, production, Stable, representative-device, or complete platform-integration acceptance unless those states are separately supported by authoritative evidence.

Partially implemented capabilities remain open obligations in `PLANNED-FEATURES.md`. The same capability may therefore be described here for the portion that exists and in `PLANNED-FEATURES.md` for the portion that remains incomplete.

## Historical standalone verified source baseline (pre-consolidation provenance)

The latest source-bearing runtime in the retained standalone-repository baseline was `03d4c3d2d7e355916412565b531e411d1bba71de`, the guarded squash merge of legacy standalone PR #240, **Stabilize selected file Search roots**. Repository-native feature/changelog records were subsequently reconciled through PR #241 without changing runtime behavior. PR #240 exact head `32c2972d53786d3171ec7f4586bb121fe192a991` passed Android CI run #734 / `35827675978` across repository/privacy/identity/GLAZE/Room-cutover guards, lint/build/unit/schema checks, Development APK staging, Android 16 Room/runtime emulator, and Android 16 transition-performance emulator lanes before merge. Artifact `10735388852`, `goreecloud-launcher-android-dev-sidecar`, is bound to that exact head with archive digest `sha256:1eb903e3f91596065b8d9e1231244dcf3750006d956f1b88d3f352343a70798b`; independent archive inspection matched that digest, its internal `SHA256SUMS` validated `GoreeCloud-Launcher-Dev.apk`, and the APK SHA-256 is `cdd6f5ee01cb32be7f3872a0b44bab7a1b52525fddab89f081430f410d39137d`. The runtime retains PR #238's user-selected local file Search plus explicit Google Drive, Dropbox, and Brave Search handoffs and now adds selected-root visibility/removal, confirmed persisted-read-grant release, and per-root failure isolation while preserving the local-first/no-automatic-third-party-fan-out boundary and PR #235's Home-editor-only Launcher Settings route. This baseline also includes PR #229's opt-in local Contacts/Call history/Messages and local application-shortcut Search sources, legacy standalone-repository PR #228's rendered persisted Search-source controls, PR #226's four built-in Glaze wallpapers, legacy standalone-repository PR #223's gesture-only app-drawer dismissal, PR #221's 5 × 6 starter Home/five-app Dock defaults, PR #219's AppWidgetHost-based Home widgets, PR #215's unified Home/Dock/App Drawer drag-and-edit interactions, and PR #212's source-manifest reconciliation.

This is Development evidence. It does not establish physical-device performance, complete accessibility, personal/work/Shelter/private-space acceptance, Quickstep/Recents compatibility, complete Integral Platform System acceptance, protected production signing/distribution, Release Candidate, production, or Stable qualification.

## Historical standalone PR #248 Development extensions

The retained standalone Draft PR #248 candidate contains additional historical Development source beyond that standalone merged-main runtime baseline. The latest source-bearing held-drag/onboarding revision is `2aef1d336935fa244ff7cd75d722da285375f876`; the live app page-handoff implementation is preserved through `5ae8d1792b3c08fe756216eed11941e10b103a07`, and the Month/widget, secondary-page widget movement, accessibility, owner-feedback/Glance/system-bar, and earlier onboarding work remain preserved in its ancestry. Implemented Development source includes:

- ordinary Home page indicators now preserve the restrained 7/9 dp visual-dot treatment inside explicit 48 dp Glaze interaction targets, reuse the Home page context accessibility label, expose selected state, and carry stable test tags; Android 16 runtime coverage verifies clickable semantics, 48 dp minimum target geometry, and selected-state changes across primary↔secondary navigation; exact source head `0503cc8f811e1e637732d30a31b3366b8f50f5c7` passed Android CI #1221 / run `36418903654` with **63/63 runtime tests**, transition-performance, and the full validate/build/lint/JVM/schema/APK lane; artifact `10968461828` has ZIP SHA-256 `8da8d654e8bf5fc5fd19db42dccf6eede1165e5c81b13bbd9fab287ba55a1dc7` and is archived byte-identically under Drive `CI-1221` as file `18hgj_NeaTH7FtBr3CMwIxjF9T4p6YCrg`;
- owner-device stabilization now enables deliberate long-press drag of unlocked Home widgets from normal Home, adds a movable first-party **Glance** 4 × 2 widget with local time/date plus truthful weather-provider status, seeds that movable Glance only during fresh starter-workspace provisioning when placement succeeds, and keeps upgraded layouts free from background Room rearrangement: an older fixed clock/date card converts only after an explicit long-press while Home is unlocked and has room; the tranche also removes the heavy legacy glass-card shadow path associated with the reported rectangular rendering artifact, recomposes Edit Home into one phone viewport with a flexible page carousel plus a single Wallpaper/Widgets/Apps/Folders/Settings action rail, and synchronizes edge-to-edge system-bar contrast/icon appearance with wallpaper-backed versus full-screen material surfaces;
- the Launcher-owned widget catalog now includes a **Month** 4 × 2 card that renders a local calendar month grid and highlights today without requesting Calendar permission or reading event data; it participates in the same catalog search, Room placement, resize, same-page movement, secondary-page rendering, and cross-page widget movement paths as other built-in widgets; focused catalog tests cover membership, naming/default span, and `calendar`/`event` search discovery;
- the Google Drive Sources control now fails closed in ordinary debug CI builds instead of presenting an account flow that cannot complete there; those builds direct users to the existing permission-scoped **Files → Choose folder** path while full inline Drive remains separately gated on an accepted signed Development connection path; focused JVM tests cover the Glance catalog/span, direct-widget-drag threshold, debug-build Drive availability policy, and system-bar light/dark surface policy, while Android runtime guidance covers the new widget interaction model and the widget-gallery **weather** query now correctly resolves Glance rather than expecting no first-party match;
- exact head `92ecf6766e830c82e0641e28192c6a12816d531c` reached Android CI #1208 / run `36389175109`: validate/build/lint/JVM/schema/APK staging and transition-performance passed, but Android 16 Room/runtime finished 61/63 tests with two failures; that head and artifact 10955501711 remain superseded failed evidence. The correction updates the gallery expectation, removes automatic upgrade-time workspace mutation, confines automatic Glance placement to fresh starter provisioning, and makes upgraded legacy-card conversion user-triggered only. Corrected source head `297f92413483a352691fdcf656db200b0eeabc87` then passed Android CI #1219 / run `36392265538` across all configured validation, Android 16 Room/runtime, and transition-performance lanes. Artifact `10956034529` is bound to that exact source head with ZIP SHA-256 `674cb1d9cc6394afbc813df2a3c6c435005c1527cab57d9461d6659b0b2ef654`; embedded APK SHA-256 is `19a59a71d6cfad21c1a933784effb7ab1b5f29676800f63446c1b1b498b4c297`, versionCode 1219, and the byte-identical ZIP is archived under Drive `CI-1219` as file `1znpiwADpLNFpEraYTRQn3yw72t1ZF2GQ`. The record-only reconciliation head `b90054a55dfe5f4abe693e3e10cd26411adf9bc0` subsequently passed Android CI #1220 / run `36415566330`;
- the three-step first-use wizard and replayable Home hint now match the current Home/folder/Search interaction model: the final wizard step explains exact held App Drawer → Home/Dock placement, Edit Home's Wallpaper/Widgets/Pages/Apps/Settings route, paged folders and their grid-integrated **Add apps** path, and the explicit opt-in boundary for connected Search; the Home hint carries the same placement/folder guidance without changing defaults, permissions, provider activation, or workspace authority; source commit `aa1484c30af371216a0af29179e44980e41f2788` implements the UI guidance and `414e85e9a57dcffef40b26934f2b60ea605aa083` adds focused Android runtime assertions; later exact-head Android CI through #1221 includes this onboarding/runtime coverage; representative-device first-use and hint accessibility remain separate acceptance;
- the **Add apps** picker now replaces its dense full-width list with an adaptive Glaze app grid using 82 dp normal, 104 dp large-text, and 116 dp extra-large-text minimum cell widths, rounded 48 dp Search, 46 dp icons, centered two-line labels, and explicit Add/Added state while preserving personal-app-only scope, membership authority, deterministic ordering, already-added disablement, and the 100-app limit; exact corrective source head `118fe42693b943bcb0a84a38f27dfb23c8aa70bd` passed Android CI #1180 / run `36381239898` with 61/61 Android 16 Room/runtime tests and transition-performance diagnostics; the prior `ab22645…` / CI #1179 compile failure remains audit history;
- opened folders now paginate larger collections into bounded three-row horizontal pages using the existing responsive 3/4-column geometry, with a compact current-page indicator, an **Add apps** overflow action that stays reachable during selection mode, and the existing grid-integrated Add tile preserved as the final collection item; focused JVM coverage locks page-capacity/Add-tile behavior and exact source head `4215ec7f67bfc9f5049555525255e1c19a674e50` passed Android CI #1176 / run `36380184073` with 61/61 Android 16 Room/runtime tests plus transition-performance diagnostics;
- opened folders now place the direct **Add apps** action as the final normal grid tile rather than a second title-row icon control, preserving the folder-specific accessible description, responsive cell sizing, empty-folder Add path, and selection-mode clarity; exact source head `669e96f5cbcaf63cb71556717869e4cec9b77c54` passed Android CI #1174 / run `36377931540` with 61/61 Android 16 Room/runtime tests and transition-performance diagnostics;
- the refreshed folder surface now uses responsive 92%-width/capped panel geometry across phone/compact/tablet canvases, gives large/extra-large text more vertical cell room, permits a two-line large-text folder title, and applies the V1.6 policy-resolved interaction target to folder controls; Launcher-owned built-in widgets and the unavailable-widget fallback now share the Home presentation-policy material boundary so reduced-transparency/constrained-performance modes can resolve wallpaper glass to Solid/Raised styling; exact source head `de34e62c19e1b92f65e6402fe703cbc5abe15015` passed Android CI #1172 / run `36376618932` with 61/61 Android 16 Room/runtime tests and transition-performance diagnostics;
- Launcher-owned text Search fields now use V1.6 presentation-context sizing and focus policy: 54 dp normal height grows to 60/64 dp for large/extra-large text, Touch Assistance can raise the effective floor to 56 dp, and policy-required strong focus adds a higher-contrast focus outline without changing query/provider behavior; exact source head `72ee4683ca0fb1266259bc828082630038ea60ab` passed Android CI #1166 / run `36374441916` with 61/61 Android 16 Room/runtime tests and transition-performance diagnostics;
- Home Search, Home quick-action pills, and the Dock now use the shared GLAZE UI V1.6 presentation policy for material fallback and interaction sizing: glass surfaces can resolve to Solid/Raised under reduced-transparency or constrained-performance policy, Search grows to 60/64 dp for large/extra-large text, touch-exploration context raises Search/quick-action/Dock targets to the existing 56 dp Touch Assistance floor, and the adaptive Dock overflows horizontally rather than shrinking below the resolved target floor;
- focused JVM presentation-policy coverage locks Home Search/Dock material-role requests and normal/large/extra-large Home Search height selection; exact source head `a299dd716dd68c015f160f2fa60dbc8acd40b067` passed Android CI #1161 / run `36372586285`, including 61/61 Android 16 Room/runtime tests and transition-performance diagnostics;

- an anchored Glaze app context command surface with Home/Dock/App-info controls, primary-profile app-specific widget access, optional content-free notification-count status, and up to four Android manifest/dynamic/pinned shortcuts when shortcut-host authority is available;
- app-specific widget selection from long-press plus a Launcher-owned multi-widget chooser, provider app artwork where available, Glaze fallback widget glyphs, minimum-size summaries, and direct Add affordances;
- widget persistence and management are now page-aware across the complete Home snapshot: primary or secondary widgets retain descriptor identity, spans, resize/remove/same-page movement, and non-drag **Move to another Home page** controls; direct adjacent-page widget edge release is span-aware so opposite-edge landing coordinates keep the full widget in bounds, while occupied/invalid exact targets fail closed; Android 16 runtime coverage verifies a built-in Battery widget moved to and rendered on secondary Home;
- a revamped, scrollable Glaze Edit Home workspace plus a Room-backed visual Home-page manager using centered horizontal page carousels: the dedicated manager keeps the selected real-layout preview prominent, adjacent page edges visible, direct swipe/select navigation, a compact header Add action, and guarded secondary-page reorder/delete controls;
- the long-press Edit Home overview uses a centered swipeable page-preview carousel whose swipe state remains local to the editor instead of changing the Activity-level selected Home page; a final **+ Add Page** card creates a blank Room-authoritative page without dismissing Edit Home and immediately yields the new page preview at that carousel position, while Wallpaper/Widgets/Settings/Apps/Folders remain pinned in the bottom editing toolbar, layout lock disables creation, eligible empty secondary pages retain confirmation-gated deletion, and the dedicated manager retains full advanced reorder/add controls;
- app page management now exposes a bidirectional non-drag **Move to another Home page** route: secondary-page apps can target primary Home or other secondary pages, and primary-Home detailed app controls can target every secondary page; the protected rank-zero move destination is labeled **Primary Home** while retaining its app/other-item context, and primary-boundary moves pass the current Home grid through the authoritative Room mover so protected spatial constraints are validated in either direction;
- Home apps now also support direct adjacent-page edge drop with exact drag-only landing-cell intent: a right-edge release targets the adjacent page's leftmost column, a left-edge release targets its rightmost column, vertical release position selects the row, collision/out-of-bounds validation fails closed, primary Home preserves existing exact same-page cell/Dock drag geometry, and stationary long-press management remains available;
- saved Home apps now use Android's platform drag transport for a live held-page handoff: keeping the drag at a valid left/right grid edge briefly switches the visible page without mutating Room, the Activity-level drag target survives the source page leaving composition, and release on the newly measured page maps to an exact destination cell before calling the existing snapshot-checked Room mover; empty secondary pages retain a visually silent but measured destination grid, invalid/gap/collision targets fail closed, primary same-page/Dock routing remains on its established path, and this live-switch tranche is app-only;
- Home folders now support deterministic adjacent-page edge drop through the same measured edge policy: primary Home preserves free-cell same-page folder movement, while a valid left/right edge release from primary or secondary Home carries an exact opposite-edge destination cell through the Room-authoritative folder transaction; secondary Home additionally supports exact measured same-page free-cell drops; occupied/out-of-bounds exact targets fail closed, folder identity/name/membership are preserved, and non-drag page moves continue choosing the first free target cell;
- secondary Home pages now use one persisted spatial grid for apps, folders, and multi-cell widgets, preserving Room coordinates, empty-cell gaps, and widget spans instead of compacting content; held secondary-page apps and folders can drop onto exact measured same-page cells through snapshot-checked Room mutations that reject collisions/out-of-bounds targets, and widget rendering/actions remain available after a widget leaves primary Home;
- opened folders now use a larger Glaze floating panel with stronger folder-name hierarchy, a direct 48 dp Add control, adaptive four-column phone layout with three-column narrow/large-text fallback, transparent default app cells instead of nested card chrome, and bounded presentation-policy-aware opening/closing motion; Launcher-owned widget containers and the Glass/Edge Dock share the same more generous rounded/depth language while preserving accessibility floors and existing workspace behavior;
- Home and Dock cells now use one adaptive icon-slot geometry before label placement, keeping app and folder labels aligned across Compact/Balanced/Airy density and icon/folder-size customization; constrained cells scale artwork and folder preview geometry down within the cell instead of allowing large previews to crowd neighboring rows;
- the optional Home at-a-glance time/date block now uses a rounded presentation-policy-aware Glaze surface that can resolve from functional glass to a more solid material under reduced-transparency/performance constraints, and optional Home quick actions enforce the common 48 dp minimum target while retaining their existing Apps/Search behavior;
- clean ordinary secondary/new Home presentation with no persistent numbered top switcher, no empty-page message, and page position communicated only by horizontal navigation state plus the bottom dots;
- direct Home page navigation from ordinary horizontal swipes: primary Home routes left/right gestures to the adjacent Room-backed page before any configured fallback action, secondary pages use the shared page-swipe surface, and Android 16 runtime coverage creates a secondary page and verifies left-to-secondary then right-to-primary navigation;
- dedicated page-indicator clearance on primary and secondary Home layouts so page dots do not share visual space with the last app row or labels;
- a first-class optional app-drawer Search field that reuses the local app matcher, honors top/bottom placement, filters only the active profile, matches folder names locally, and remains off by default;
- additional representative-device app-icon hardening: the current drawer profile receives an explicit cache warm pass and visible icon loads use bounded retry/backoff for transient OEM resource windows while retaining the profile-aware Android-owned fallback chain;
- explicit notification-badge recovery UX for sideloaded Development builds: Launcher links to App info and Android notification access, explains the OS-owned Restricted Settings gate, and offers a Check again action without attempting to bypass Android security;
- a compact grouped Glaze Search Sources manager with dense source rows, inline status, connected-provider artwork/glyphs, progressive row disclosure, Files-only folder selection, and no repeated always-visible Info controls; **Google Drive** is now a distinct online account source whose enable action invokes Google Identity authorization for the least-privilege `drive.metadata.readonly` scope instead of opening Android’s folder picker;
- an opt-in inline Google Drive provider that keeps its short-lived OAuth access token in process memory only, issues bounded HTTPS Drive API v3 `files.list` metadata queries only while the source is enabled/authorized, maps returned files/folders into the normal `CONNECTED_SOURCE` result stream, restores previously granted authorization silently on process restart when possible, and fails closed back to a reconnectable source state when authorization is unavailable or rejected;
- Development update-continuity build plumbing that accepts a positive externally supplied Development `versionCode`, binds ordinary GitHub Actions builds to the workflow run number, accepts a complete externally supplied Development keystore configuration while rejecting partial configuration, keeps signing material outside source control, and records effective version code plus signer-certificate SHA-256 in CI provenance while ordinary pull-request artifacts remain CI-debug-signed;
- enabled providers that remain handoff-only are rendered as full **Search online** rows inside the unified Universal Search result panel instead of the old **No local matches** card plus compact provider-button strip; a ready inline provider such as authorized Drive is removed from that duplicate handoff section. Brave Autosuggest remains fail-closed until a governed server-side confidential-token path exists; and
- empty Universal Search dismissal from unused backdrop space plus a focus/IME-safe Back path that clears Search focus and hides the keyboard before returning Home, with Android runtime coverage for empty-space tap dismissal and post-Back Home gesture recovery;
- Drawer→primary-Home placement now supplies the requested target cell to the first authoritative Room write, validating bounds/collisions and committing the newly copied app directly in its dropped cell instead of first committing it at the first free cell and then moving it in a second transaction; Android runtime coverage verifies a new drawer app is persisted directly at the requested coordinate.

The current owner-feedback source-bearing head `43b49407527132e97ea687951525de52cabe2f82` passed Android CI #1142 / run `36359121152` across validate/build/lint/JVM/schema/APK staging, Android 16 Room/runtime, and Android 16 transition-performance lanes. The complete Android 16 runtime suite finished 60/60 tests with zero failures, including the Edit Home preview-swipe/direct-add regression. Earlier artifact evidence remains bound to its own exact source revisions; a later record-only head still requires fresh exact-head CI before it can become the next distributed Development candidate.

These are Development source/build/runtime claims only. Protected persistent Development signing, trusted key-bearing distribution, update-in-place acceptance on a representative device, and the broader issue #80 acceptance gates remain separate; PR #248 remains Draft/unmerged Development source.

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
- Shared app-icon loading keeps a bounded stale-while-revalidate bitmap during package/profile cache invalidation and uses a five-stage Android-owned fallback chain: badged activity icon; activity icon re-badged when PackageManager is available; PackageManager activity icon; package-level application icon; and, only when every app-owned resource is unreadable, Android's default activity icon. PackageManager fallbacks are re-badged for the app's profile, visible loads and bounded background warming share the same chain, and Android runtime coverage verifies visible launcher activities resolve non-null artwork without persisting third-party icon data.

### Home, workspace, pages, and Dock

- Wallpaper-backed primary Home surface using Android system wallpaper presentation.
- Four Launcher-owned built-in Glaze wallpapers — Glaze Aurora, Glaze Horizon, Glaze Nocturne, and Glaze Cascade — rendered locally from inspectable source and selectable from the existing Home/Universal Search Wallpaper action.
- Built-in wallpaper application uses the narrowly reviewed `SET_WALLPAPER` permission; the Android system wallpaper picker remains available as an explicit fallback, with no INTERNET, storage, analytics, ads, or remote-art dependency.
- Persisted Favorites and an adaptive Dock with no product-level item cap. Dock slots contract only to the Glaze 48 dp interaction floor; larger collections become horizontally scrollable instead of rejecting placement.
- Default 5 × 6 Home grid for a new Launcher preference store, while the existing supported grid presets remain configurable.
- First-run startup wizard blocks ordinary Launcher surfaces until initial setup is completed and provides default-launcher role setup plus Home app mode, Home grid, Home-label, add-new-apps, Universal Search entry, and hint preferences. Existing upgraded workspaces whose historical starter layout is already applied are treated as already onboarded.
- One-time starter Dock setup prefers Phone, Messages, Email/Mail, Browser, and Camera for the five Dock positions when matching apps are available; the wizard-selected automatic Home mode is not persisted as fake Favorites.
- Automatic Home app mode is explicitly configurable as **No apps**, **10 most recent**, or **10 most used**. Recent/most-used suggestions are transient presentation layered only into currently empty Home cells and exclude persisted Home Favorites and Dock duplicates.
- Manual Home apps remain Room-authoritative Favorites with their saved spatial coordinates and normal drag/drop behavior regardless of automatic Home mode. Automatic suggestions do not rewrite those coordinates and can be promoted through ordinary app-management/add-to-Home actions.
- Recent/most-used ranking uses only launches initiated through GoreeCloud Launcher. The local store retains application workspace keys, aggregate launch counts, and a bounded recency ordering with no timestamps, dwell time, Android Usage Access, query history, or network telemetry.
- A dismissible built-in Home hint explains swipe-up Apps, swipe-down Universal Search, empty-Home long-press editing, and drag/drop. Hints can be disabled during startup, dismissed from Home, and shown again from Launcher Settings.
- Long-press Home edit mode with visible grid/edit affordances and icon management actions.
- Unified drag/drop between Home and Dock, Dock reordering, Home reordering by cell, and App Drawer copy-to-Home/copy-to-Dock placement while preserving Drawer inventory. In Apps, moving after the long-press hold starts direct placement; a stationary hold retains contextual actions.
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
- App drawer header retains profile/layout context and uses compact icon-first actions for sort, new-folder creation, and the owner-requested Launcher Settings shortcut; the drawer still has no explicit close button.
- Downward swipe is the drawer's explicit in-surface dismissal path; Android HOME-button return remains normal system navigation.
- Launcher Settings is reachable from **Edit Home → Settings** and the compact App Drawer gear. Empty-space Home long-press remains reserved for Edit Home, and Launcher-owned Universal Search does not add a duplicate direct Settings result. Historical stored gesture/Search compatibility values continue to fail safely through their supported routing.
- Search result identity labels that distinguish User and Work application matches where applicable.
- Removal of the duplicate rendered drawer-search control so the Launcher-owned Universal Search surface remains the primary general Search experience.

### Performance and icon handling

- Shared process-local LRU caching for Android-provided badged launcher icons with package/profile invalidation stamps and single-flight cold-load sharing.
- Bounded background icon warming with a 12 MiB cache budget, up to 128 candidates, and controlled batches of three while visible cold icons retain lazy fallback behavior.
- Latest-snapshot preload ownership from PR #205: a new authoritative `LauncherApps` inventory warm request cancels the superseded preload tail, and complete cache/profile-topology invalidation cancels background preload work before generation reset. Already-started single-flight decodes remain reusable by newer callers, while stamp checks prevent invalidated results from re-entering the cache.
- Transition diagnostics and Android 16 transition-performance emulator coverage remain available for Development validation; representative physical-device performance acceptance remains separate.

### Launcher settings and appearance

- Searchable Launcher Settings category home inspired by mature launcher information architecture while retaining original GoreeCloud/Glaze implementation. The overview exposes **Home screen**, **App drawer**, **Folders**, **Search**, **Look & feel**, **Gestures & inputs**, **Notification badges**, and **System & setup**, each with an original Launcher-owned line icon and short summary.
- Settings search matches category descriptions plus detailed-control keywords so users can find controls such as grid, Dock, page transition, icon pack, notification access, and Universal Search without scanning the full configuration surface.
- When GoreeCloud Launcher is not the active HOME application, Settings shows a direct status/action banner near the top rather than burying the default-launcher state.
- Category selection reuses the existing persisted preferences and detailed controls; there is no parallel settings authority. Home owns Dock controls, while Look & feel groups icon and appearance controls.
- Persisted Home-grid, Apps-grid, icon-size, label-visibility, appearance, layout-lock, Launcher Universal Search entry-mode, local-usage-suggestion control, and off-by-default new-app-to-Home preferences where currently implemented.
- System / Light / Dark appearance selection and a reachable native Theme Manager path.
- Repository-level GLAZE UI V1.6 source mapping, material/accessibility policy, and validation guard, subject to the still-open downstream acceptance boundaries recorded in `PLANNED-FEATURES.md`.

### Launcher-owned Universal Search foundation

GoreeCloud Launcher owns the user-facing Universal Search experience. Current implemented Development capabilities include:

- Representative-device feedback correction checkpoint: runtime source `55ec855cb7e321b29f375e95b4a7880322a330d3` passed Android CI #995 / `36288344233` across validation/build/lint/JVM/Debug APK, Android 16 runtime, and Android 16 transition-performance lanes. This checkpoint adds icon invalidation retry/preservation, Android Restricted Settings guidance for Development notification-badge access, a regrouped progressive-disclosure Search Sources Glaze surface, semantic connected-provider fallback glyphs, bounded selected-folder Google Drive inline results, and rejected Drive-root permission-grant cleanup. Representative-device acceptance remains open.

- A distinct Launcher-owned Universal Search surface that remains usable without GoreeCloud Search or GoreeCloud Index.
- PR #248 Development source reduces the idle Universal Search presentation to one focused Glaze search field with a leading search glyph, **“Find anything on your device…”** prompt, and an in-field settings control; result panels, status messaging, categories, and provider handoffs remain hidden until typing or another explicit action makes them relevant.
- Universal Search source management remains directly reachable from that settings control without reintroducing a persistent management row or weakening the existing opt-in/privacy boundary.
- Installed-app search backed by Android `LauncherApps` inventory.
- Trusted local Launcher actions/settings destinations.
- Deterministic local ranking, aggregation, deduplication, and fail-soft provider behavior.
- Cooperative cancellable asynchronous provider execution under caller/provider lifecycle control.
- Profile-identifying User / Work subtitles for installed-app results.
- Provider contract v1.0 descriptive metadata covering provider identity, provenance, offline behavior, authorization requirement, remote-processing declaration, query-retention declaration, and fail-closed duplicate/version compatibility evaluation.
- Privacy-first provider policy that keeps automatic-local execution limited to reviewed local-only/no-retention providers, adds a distinct **opt-in remote-inline** mode that only reviewed async provider implementations can adopt, requires an opted-in remote provider to report its governed credential/authorization/configuration path ready before it can enter live execution, and keeps all other network, retaining, authorization-requiring, or third-party sources behind explicit user handoff. PR #248 now includes a bounded Google Drive remote-inline adapter over Google Identity authorization: enablement requests the least-privilege `drive.metadata.readonly` scope, the short-lived access token remains process-local, typed queries are sent only while Drive is explicitly enabled and authorized, and bounded Drive API v3 metadata results return as attributed `CONNECTED_SOURCE` rows. Android Storage Access Framework roots remain exclusively under the separate **Files** source. Dropbox remains handoff-only; Brave remains handoff-only until a governed server-side credential path exists.
- UI-facing result-presentation model that keeps already-ranked application matches separate from Launcher action/settings results and can expose enabled explicit-handoff providers descriptively without invoking them or attaching the query payload. If a reviewed inline provider later contributes a `CONNECTED_SOURCE` result, the rendered row resolves its provider-control display name and shows explicit **From <provider>** source attribution instead of a generic connected label.
- Versioned fail-closed serialization contract for provider enablement and explicit provider order, preserving absent-versus-explicit-empty semantics while excluding typed queries, results, history, usage/frequency signals, credentials, grants, and provider payloads (PR #198).
- Launcher-local dedicated Preferences DataStore persistence for that provider enable/order snapshot, with explicit `read`, `set`, and `clear` boundaries; absence remains distinct from an explicitly empty enabled-provider selection and malformed/unsupported stored data fails closed (PR #199).
- Persisted provider-control reconciliation and mutation policy: absent storage adopts only privacy-safe automatic-local defaults; loaded snapshots preserve explicit enable/order choices; malformed or unsupported stored values resolve fail-closed to zero automatic providers; and bounded enable/disable plus ordering helpers emit only the existing versioned provider-control snapshot (PR #207).
- Rendered **Sources** management backed by the persisted provider-control store, including provider identity, enabled state, privacy summary, deterministic Earlier/Later ordering, and safe-default reset while automatic execution remains limited to reviewed automatic-local providers (legacy standalone-repository PR #228).
- Local Android application-shortcut Search via `LauncherApps.startShortcut` without a new runtime permission (PR #229).
- Explicit opt-in local Contacts, Call history, and Messages Search sources, each disabled by default and enabled only after the corresponding Android permission is granted; typed queries stay local and no INTERNET permission, telemetry, query-history persistence, or third-party automatic fan-out is added (PR #229).
- Launcher-owned Search no longer exposes a direct **Launcher settings** result; any historical/internal Settings destination resolves to Edit Home, preserving the long-press Home editor as the Settings entry path (PRs #229 and #235).
- Explicit opt-in local file Search over user-selected Android Storage Access Framework roots. Launcher stores only the selected tree URIs/read grants, indexes filename and MIME metadata within bounded depth/index/result limits, does not index file contents, and requests no broad storage permission (PR #238).
- Handoff-only connected destinations such as Dropbox and Brave Search render as full **Search online** rows in the same Universal Search panel rather than a separate compact fallback strip. Authorized Google Drive participates inline through the implemented Google Identity/Drive API adapter and is removed from duplicate handoff presentation while ready. The PR #248 candidate declares Android `INTERNET` only for explicitly enabled connected Search adapters, keeps cleartext traffic disabled, and does not permit unrestricted automatic third-party query fan-out.
- Third-party provider query-retention state can be represented as provider-policy-controlled/unknown rather than inventing a retention guarantee (PR #238).

The PR #199 provider-control store and PR #207 reconciliation/mutation policy remain deliberately separate from the strict portable-preference v1 backup/recovery contract. Portable adoption/migration remains planned work. Rendered provider management, opt-in sensitive local sources, user-selected **Files** roots, unified online handoff rows, the fail-closed remote-inline provider-control contract, and the Development Google Drive OAuth/Drive API metadata-search adapter are implemented in the PR #248 candidate. The former selected-folder Google Drive pseudo-adapter is no longer registered as Drive account search. Representative-device Google authorization/API acceptance, OAuth-client/signing configuration verification, live Brave Autosuggest predictions, Dropbox, Gmail, and other connected-provider adapters remain open.

### Privacy and local-first boundaries

- Core Launcher operation remains offline-capable and does not require a GoreeCloud server or cloud account.
- Core Home/Apps/Settings behavior remains offline-capable. The PR #248 Development candidate now declares Android `INTERNET` solely to support explicitly enabled connected Search adapters; cleartext traffic remains disabled and local providers do not require network access.
- No advertising, sponsorship, promoted placement, affiliate ranking, mandatory analytics, attribution, or behavioral-tracking dependency is part of the documented Launcher product model.
- Search provider controls through PRs #188, #198, #199, #207, #228, #229, #238, and the PR #248 Development candidate do not persist typed queries, results, history, credentials, provider payloads, or usage/frequency signals. Contacts, Call history, and Messages are opt-in local sources whose Android permissions are requested only when the user enables the corresponding source. **Files** Search is limited to user-selected Storage Access Framework roots and indexes metadata rather than file contents. Google Drive account search is not represented by those roots; the current PR #248 candidate uses the explicit Google Identity/Drive API adapter only when Drive is enabled and authorized, while Dropbox and Brave remain explicit handoffs. Launcher declares `INTERNET` for reviewed connected Search adapters but still requests no broad-storage permission.

### Branding and asset provenance

- Canonical Launcher visual assets are governed from the GoreeCloud branding-assets repository; the Launcher repository retains traceable Android derivatives required for the application.
- Repository provenance metadata and validation guard prevent Launcher-local derivatives from silently becoming a competing canonical branding source.

### Build and validation foundations

- Repository CI includes privacy, HOME-manifest, identity, GLAZE UI, Room/schema, lint, unit-test, debug-build, and Android 16 managed-emulator coverage for exercised Development paths.
- Development artifact staging reads the assembled APK identity back through Android build tools and fails if package ID, versionName, or versionCode differs from the expected side-by-side Development identity; staging also verifies the APK signature, records the signer-certificate SHA-256, and emits self-checking APK SHA-256 provenance.
- Evidence from source, CI, APK staging, and managed emulators remains exact-revision-bound and is not treated as physical-device, production, or Stable acceptance.

## Evidence highlights

| Change | Evidence | Implemented result |
| --- | --- | --- |
| PR #238 | Exact head `71be92123352e5d179f48e5e1a892680aadd8987`; Android CI #729 / `35821960552`; merge `426e466f62d8347de720258be8492e518e84c4d5` | User-selected local file Search plus explicit Google Drive/Dropbox/Brave Search handoffs; no automatic third-party fan-out or Launcher INTERNET permission |
| PR #235 | Exact head `75581e01cdfb584b5f4af59377002ca835d0b064`; Android CI #723 / `35820108208`; merge `b68d1e56443a6e8365cf1d440e4ac5c53c868a02` | Settings-only-via-Edit-Home routing, reserved empty-space long-press, legacy direct-Settings gesture/Search compatibility routing |
| PR #229 | Exact head `740f967339de2adf0a4d70c26c9aeef2c2ecff7e`; Android CI #714 / `35818607895`; merge `0d6ccf955ae0884d4b06d9ed1839b377ab2a1af2` | Local app shortcuts plus opt-in Contacts, Call history, and Messages Search; no INTERNET permission |
| Legacy standalone PR #228 | Exact head `851bf9007ea57209981b61c1ec11bc746ad0a009`; Android CI #703 / `35816326844`; merge `1e1f72c6a994e8381c0eecf3bd9fc3db17a44dde` | Persisted Search-source controls connected to rendered Sources management |

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