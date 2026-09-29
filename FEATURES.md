# GoreeCloud Launcher Features

## Status and interpretation

GoreeCloud Launcher is in **Development**. This file distinguishes current implemented Development behavior from the approved product capability scope.

A capability listed under **Approved product scope** is a product requirement or target capability. It is **not** an implementation, runtime-acceptance, release-acceptance, or Stable claim unless it is also listed under the implemented/current sections and supported by repository evidence.

## Implemented in Development source

Current source includes:

- Native Android launcher foundations using Kotlin and Jetpack Compose.
- Android HOME-role onboarding and lifecycle-aware default-HOME state.
- Scoped Android `MAIN` + `LAUNCHER` package visibility rather than broad `QUERY_ALL_PACKAGES` access.
- `LauncherApps` discovery across available profiles, package/profile refresh callbacks, and launchable-activity deduplication.
- A rebuilt wallpaper-backed primary Home surface with application grid, Dock, Apps affordance, and Launcher Settings affordance.
- Native Android system-wallpaper presentation behind the launcher window without wallpaper/storage privileges.
- A separate Apps surface with local label/package filtering and application launching.
- Persisted local Apps presentation modes for Grid, Compact, and List layouts; this presentation-only preference is outside the current strict v1 portable preference snapshot.
- A separate scrollable Launcher Settings surface.
- Persisted Home grid presets covering supported 4–6 column / 4–7 row combinations.
- Persisted Apps-grid density of 4, 5, or 6 columns.
- Persisted Small/Medium/Large icon presentation and app-label visibility.
- Persisted System / Light / Dark appearance selection.
- Persisted Home layout-lock state and Launcher Universal Search Home-entry mode. The strict v1 persistence/backup wire key remains `index_home_mode` only for backward compatibility.
- Locally persisted Favorites and a bounded five-item Dock.
- Long-press app placement management with accessible earlier/later controls, plus direct primary-Home long-press drag placement into occupied or empty configured grid cells.
- Current layout-lock gating for implemented Favorite, Dock, primary/secondary spatial, and Home-page mutation callbacks while ordinary app launching and page selection remain usable.
- A five-second locked-state Home hold control with progressive feedback, with the Launcher Settings switch retained as the deterministic non-gesture unlock path.
- Room-backed authoritative workspace cutover/read foundations.
- Multi-page Home projection from authoritative Room state.
- Home page selection, creation, guarded secondary-page reordering, and guarded deletion of eligible empty secondary pages while the primary Home page remains protected at rank zero.
- Compact/lazy Home page selector with authoritative accessibility context.
- App launch from rendered secondary Home pages.
- Secondary Home pages rendered as ordinary icon grids instead of engineering panels.
- Secondary app movement controls hidden behind long-press management rather than permanently displayed under each app.
- App movement between authoritative secondary Home pages.
- Within-secondary-page nearest-free-cell earlier/later movement.
- Guarded exact one-cell movement left/right/up/down that fails closed on occupied or out-of-bounds targets.
- Guarded primary-Home compatibility-to-spatial migration in terminal Room authority, preserving rank-zero page identity while enabling persistent configured-grid cell placement, empty-cell drops, and occupied-cell swaps.
- Development presentation of unsupported workspace-item counts instead of silently hiding their presence.
- Launcher-owned Universal Search Home entry through the **Search GoreeCloud** affordance in Permanent mode and one-finger downward Home invocation in both supported entry modes. Earlier Index activity-handoff revisions remain historical provenance.
- Scoped `MAIN` + `LAUNCHER` package visibility for app discovery without the legacy Index search-action query, `QUERY_ALL_PACKAGES`, or Launcher Internet permission.
- Canonical Launcher visual-asset provenance from `GoreeCloud/goreecloud-branding-assets`, with this repository limited to traceable Android derivatives.
- Repository-level Glaze UI Adoption Candidate mapping and validation guard.
- Privacy/HOME/identity/Room/schema/lint/test/debug-build validation in CI, with Android 16 emulator runtime coverage for the exercised Development paths.

## Native Universal Search — approved architecture and current migration state

**GoreeCloud Launcher owns Universal Search.** The approved planned architecture assigns the user-facing search UI, native provider framework, core local indexing required for the Launcher experience, result aggregation/ranking, permission-aware filtering, categories, recents/history, commands, shortcuts, contextual actions, and standardized provider APIs to Launcher.

Core Launcher Universal Search must work when GoreeCloud Index and GoreeCloud Search are absent, disabled, unavailable, or not production-ready.

Current Development source now contains the first Launcher-owned Universal Search foundation: Home search routes directly to the Launcher search surface, the legacy mandatory Index activity handoff/package-visibility query is removed, and installed-app results flow through a native provider contract backed by Android `LauncherApps`. Earlier Index-backed PR/test records remain exact-revision provenance; they do not define current search authority.

Launcher Universal Search is intended to search and act on, where authorized and implemented:

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

Search results may expose direct permission-aware actions such as launching an app, invoking a shortcut, opening/sharing/moving/inspecting a resource, opening a person/profile or approved communication action, navigating to a setting, executing a GoreeCloud command, invoking a registered provider action, or resolving an appropriate natural-language request.

GoreeCloud applications and platform services should register searchable resources/actions through versioned Launcher provider contracts. Provider definitions should cover source identity/provenance, resource types, authorization requirements, schemas, ranking metadata, freshness, offline behavior, privacy, cancellation/timeouts, failure isolation, and compatibility/versioning. Optional provider failure must not disable core Launcher search.

GoreeCloud Search remains a separate system and may later integrate as an optional advanced search/query/discovery backend for full-text, semantic, cross-service, federated, filtered, or current-information retrieval. GoreeCloud Index remains a separate system and may later integrate as an optional scalable indexing/retrieval backend for large-scale metadata/content catalogs, semantic indexing, cross-service indexing, and background pipelines. Neither service owns Launcher Universal Search or is required for the core experience.

The controlling principle is: **GoreeCloud Launcher owns Universal Search. GoreeCloud Search and GoreeCloud Index enhance Launcher once stable, but Launcher must not depend on either for core Universal Search.**

## Official product identity and asset authority

All canonical GoreeCloud Launcher logos, icons, symbols, illustrations, and artwork live in **`GoreeCloud/goreecloud-branding-assets`**. The canonical Launcher asset is `products/launcher/app-icon.svg`.

`GoreeCloud/goreecloud-launcher` is a consumer repository. It may retain only traceable synchronized/generated/platform-specific derivatives needed for the Android application. `branding/provenance.json` records the canonical repository, asset path, and source blob used for the current derivatives, and the repository identity guard rejects a competing Launcher-local canonical source.

The current Development APK contains synchronized adaptive, round, and Android 13+ monochrome derivatives. This establishes derivative provenance and build integration, not production visual-identity acceptance or Stable qualification.

## Development / acceptance work still required

Important incomplete or separately gated work includes:

- Broaden the Launcher-owned Universal Search provider framework beyond the initial installed-app provider to app actions/shortcuts, contacts when authorized, scoped media, files/documents, first-party searchable content, Drive/cloud resources, connected services/devices, settings, commands, contextual workspace resources, and optional third-party providers.
- A fully polished Glaze UI Universal Search/result experience and complete rendered/native/accessibility acceptance.
- Production visual-identity review of the branding-assets-backed Launcher derivatives across required Android/system surfaces.
- Production Room-authority recovery acceptance for the complete intended workspace experience.
- Mature cross-page drag/drop and direct live span editing.
- Primary-to-secondary/secondary-to-primary spatial item movement.
- Populated-page deletion with recovery/undo semantics.
- Complete folders, shortcuts, widgets/AppWidgetHost, folder/widget editing, and richer workspace editing.
- Layout-lock enforcement and interaction acceptance for future placeable item types once folders, shortcuts, and widgets are implemented, plus representative-device five-second-hold acceptance.
- Complete native Theme Manager, icon-pack discovery/application, icon masking, optical normalization/scaling, and broader gesture bindings.
- Complete first-party Glaze Theme Engine behavior.
- Versioned local Launcher backup/restore and later authorized Everkeep/Backup/Drive/Sync transport/continuity integration.
- Complete Privacy Shield, Wardveil Security, Everkeep, GoreeCloud Mesh, GoreeCloud Identity, Sync, Backup, Location, Search, Drive, Mail, Messenger, Maps, Calendar, and other applicable platform integrations.
- Android OS process-death and schema-upgrade recovery acceptance.
- Representative physical-device default-HOME and interaction acceptance.
- Signed release packaging/distribution and Stable qualification.

# Approved product scope

The following capability inventory is the approved Launcher product direction. These are target capabilities unless separately evidenced as implemented.

## Home Screen

- Multiple customizable home-screen pages.
- Adjustable home-screen grid sizes with independent row and column configuration.
- Adjustable application icon sizes.
- Drag-and-drop application placement and widget placement.
- Precise sub-grid positioning.
- Custom screen margins and padding.
- Application folders with custom names and intelligent folder-name suggestions.
- Home-screen application shortcuts and contextual shortcuts.
- Resizable widgets and a searchable widget picker.
- Favorite-application dock with customizable layouts, multiple dock pages, and scrolling.
- Home-screen page scrolling with optional infinite scrolling.
- Wallpaper scrolling and page indicators.
- Automatic creation of additional home-screen pages where appropriate.
- Remove applications from Home without uninstalling them.
- Lock the home-screen layout, with Settings unlock and an optional intentional five-second one-finger Home hold while preserving accessible alternatives.
- Hide application labels and adjust label size/positioning.
- Overlapping widgets and supported interface elements.
- Adaptive layouts based on device size, posture, orientation, and display configuration.

## Application Drawer

- Swipe-up application drawer.
- Alphabetically organized application library.
- Independent/custom application-drawer grids.
- Application-drawer folders and tabs.
- Automatic categorization, intelligent groups, and smart folders.
- Suggested, frequently used, recently installed, and recently used applications.
- Application-drawer search.
- Hide applications from the drawer.
- Custom drawer organization, backgrounds, transparency, and vertical scrolling.
- Context-sensitive application ordering.

## Launcher Universal Search with optional GoreeCloud Search and GoreeCloud Index

- GoreeCloud Launcher as the canonical owner of the user-facing Universal Search experience, provider framework, core local search/index path, aggregation/ranking, actions, commands, and shortcuts.
- Versioned Launcher provider APIs for GoreeCloud applications/services to register searchable resources, actions, shortcuts, commands, and metadata.
- Default one-finger swipe-down access from an unobstructed Home area.
- User choice between **Permanent on Home** and **Swipe down only** Launcher entry modes.
- An original Glaze UI Launcher Universal Search/result experience with immediate query focus, clear hierarchy, provider/source provenance, grouped results, direct actions, responsive motion, and accessible reduced-motion/reduced-transparency behavior.
- Offline-capable core local results independent of GoreeCloud Search and GoreeCloud Index availability.
- Installed application and application-content search through authoritative Android/Launcher provider contracts.
- App shortcuts and direct actions.
- Launcher settings/actions and supported device-setting search.
- Contact search when explicitly enabled and permissioned.
- File and document search through supported Android document/provider access.
- Photo, screenshot, video, and supported media search through scoped Android media access.
- First-party GoreeCloud application searchable-content providers.
- Authorized GoreeCloud Drive search.
- Web/current-information results through optional GoreeCloud Search provider integration.
- Application-store and GoreeCloud-service search.
- Search across compatible connected GoreeCloud devices when authorized.
- Recently accessed content suggestions.
- Recently used, usage-based, routine-based, and contextual local recommendations where enabled.
- Context-aware results with user-configurable result categories.
- Search-bar customization, themes, shortcuts, and animations.
- Direct actions from search results.
- Local history that is disableable and independently clearable where implemented.
- Per-source privacy/permission controls for sensitive categories.
- Clear/rebuild controls for Launcher-owned local search/index state.
- No sponsored, promoted, affiliate, or advertising ranking.

## Icons and Visual Appearance

- Unique official GoreeCloud Launcher product identity whose canonical source is maintained in `GoreeCloud/goreecloud-branding-assets`.
- Traceable Android adaptive foreground/background icon resources derived from the canonical branding asset.
- Traceable monochrome/themed Launcher icon derivative.
- Native Theme Manager integrated with applicable Glaze Theme Engine behavior.
- Theme previews and safe reset/fallback behavior.
- Icon-pack support for other applications.
- Icon masking and bounded icon scaling/optical normalization.
- GoreeCloud-native themed icons and adaptive themed icons.
- Custom icon shapes and sizes.
- Individual application-icon customization and custom folder icons.
- Wallpaper-based icon styling and wallpaper-derived palettes.
- Automatic interface color extraction.
- Custom interface and accent colors.
- Light, Dark, and automatic appearance switching.
- Transparent interface elements.
- Custom folder, dock, drawer, and Home appearance.
- Adaptive visual themes and custom text styling.
- Dynamic interface styling based on device context.

## Gestures and Interaction

- Custom swipe, horizontal swipe, double-tap, pinch, and two-finger gestures.
- Swipe down opens Launcher Universal Search by default on an unobstructed Home area.
- Configurable swipe-up and other gesture actions without removing accessible non-gesture alternatives.
- Custom gesture assignments.
- Gesture-based application, shortcut, GoreeCloud-action, search, notification, and feed access.
- Double-tap screen locking where supported.
- Long-press contextual menus and application shortcuts.
- Drag-and-drop organization and application grouping.
- Configurable interaction sensitivity.

## Smart and Contextual Experiences

- Context-aware Home and drawer application suggestions.
- Pinnable suggested applications and per-application suggestion controls.
- Recent-usage, long-term usage, routine, and time-of-day recommendations.
- Location-aware recommendations when permitted.
- Device-state-aware and connected-device-aware recommendations.
- Context-sensitive launcher content.
- Calendar, weather, upcoming-event, package-delivery, travel, flight, navigation, and destination information.
- Media recommendations and controls.
- Relevant file/document suggestions.
- Contextual information cards and personalized information surfaces.
- Suggested GoreeCloud actions.
- Adaptive launcher layouts based on context.

## GoreeCloud Feed and Cards

- Optional side-mounted personalized information feed.
- Customizable information cards.
- Application, contact, widget, media, calendar, weather, travel, and device-status cards.
- Conditional, pinnable, and context-triggered cards.
- Connected-device-triggered cards and launcher layouts.
- Launcher content recommendations and GoreeCloud service cards.
- Privacy/security status cards.
- Backup/synchronization status cards.
- Cross-device activity cards.
- User-configurable card ordering and visibility.

## Notifications

- Notification dots and numeric/custom notification badges.
- Unread-status indicators.
- Custom badge appearance and application-specific badge controls.
- Long-press notification previews and notification access from application icons.
- Notification-aware contextual shortcuts.
- Privacy-aware notification visibility.

## Folders

- Home-screen and application-drawer folders.
- Automatic and smart folders.
- Custom/suggested folder names and custom folder icons.
- Custom folder grids, backgrounds, transparency, and opening animations.
- Folder swipe actions and organization controls.
- Adaptive folder layouts.
- Automatic and manual application grouping.
- Nested organization where supported.

## Widgets

- Home-screen widgets with resizing, previews, grouping, padding controls, and precise positioning.
- Searchable widget library.
- Overlapping widgets where supported.
- Information, dynamic, interactive, and context-aware widgets.
- GoreeCloud service widgets.
- Smart widget recommendations and adaptive widget layouts.
- Cross-device information widgets.
- Privacy-aware widget content.
- Widget refresh/update controls.

## Dock

- Custom dock size and adjustable application count.
- Multiple dock pages and scrolling.
- Dock background, transparency, and padding customization.
- Search placement within the dock.
- Dock widgets.
- Optional dock removal.
- Suggested/context-aware dock applications.
- Pinned GoreeCloud actions.
- Adaptive dock layouts.

## Layout Customization

- Custom row/column counts.
- Independent Home, application-drawer, and folder grids.
- Icon-label positioning, size adjustment, and visibility controls.
- Custom screen margins and interface padding.
- Sub-grid and precise widget positioning.
- Overlapping supported interface elements.
- Home-layout locking across implemented and future supported placeable types.
- Per-page customization.
- Different layouts for device modes, portrait/landscape, foldables, multi-display devices, tablets, and desktop-style launcher experiences where appropriate.

## Navigation and Animation

- Custom Home page transitions, drawer animations, folder animations, and search animations.
- Smooth scrolling and spring-based motion.
- Gesture-responsive and wallpaper transition effects.
- Application-launch/return and contextual transition animations.
- Reduced-motion behavior.
- Accessibility-conscious animation behavior.
- Device-performance-aware animation scaling.

## Application Management

- Application uninstall and information shortcuts.
- Hide applications from the drawer.
- Rename application display labels and replace individual icons.
- Organize applications into folders/categories and pin applications.
- Search installed applications and quickly access recently installed/updated apps.
- Integrated access to application permissions, storage, notification, privacy, and security controls.
- Application-specific launcher actions.

## Personalization

- Wallpaper integration and dynamic color extraction.
- Automatic color matching and custom accent colors.
- Adaptive themes, icon styling, text styling, search appearance, drawer styling, folder styling, and dock styling.
- Custom animation preferences.
- Per-device and per-display personalization.
- Context-based and time-based appearance changes.
- Wallpaper-driven launcher styling.
- Accessibility personalization, reduced-motion preferences, and high-contrast options.

## Backup, Sync, and Configuration

- Explicit **Backup Launcher configuration** and **Restore Launcher configuration** settings using a versioned, validated, local/offline-capable format.
- Multiple saved launcher configurations.
- Restore supported Home layouts, folders, dock configuration, preferences, icon customization, and widget placement with safe widget rebinding/reconfiguration rather than stale AppWidget IDs.
- Import compatible launcher layouts through validated migration rules.
- Migrate launcher layouts between devices.
- Synchronize supported Launcher preferences and compatible layouts across GoreeCloud devices when separately authorized and accepted.
- Device-specific configuration overrides.
- Configuration version history where supported.
- Safe recovery after device reset or replacement.

# GoreeCloud platform integration scope

Launcher is intended to act as a unified entry point into compatible GoreeCloud applications, services, devices, and system capabilities. Integration must remain substantive, permission-aware, privacy-aware, security-aware, and subject to each participating service's implementation and authorization boundaries.

## GoreeCloud Drive

- Surface recently accessed files/folders.
- Search Drive/cloud resources through Launcher provider contracts when authorized; an optional future GoreeCloud Index backend may accelerate retrieval without owning Launcher search authority.
- Pin files/folders to Home.
- File/folder shortcut widgets.
- Contextual document recommendations.
- Quick access to synchronized content.

## GoreeCloud Sync

- Synchronize supported Launcher settings.
- Synchronize compatible Home layouts, folders, application organization, and personalization preferences.
- Maintain continuity across GoreeCloud devices.

## GoreeCloud Backups

- Automatic Launcher-configuration backup using the accepted Launcher configuration format when implemented.
- Home-layout, widget, folder, and preference restoration.
- Device-replacement restoration workflows.

## Everkeep

- Long-term preservation of selected Launcher configurations.
- Recovery of important personalization state.
- Configuration portability and device-transition continuity.
- Integration with continuity/recovery experiences.

## GoreeCloud Identity

- Identity-aware personalization and user-specific Home configurations.
- Secure profile switching.
- Managed application visibility.
- Profile-specific recommendations.
- Identity-aware cross-device continuity.

## Privacy Shield

- Privacy-aware Launcher Universal Search results and contextual recommendations.
- Per-provider controls for permissioned/sensitive Universal Search categories.
- User control over personalization, location, and usage-derived signals.
- Sensitive-content visibility controls.
- Privacy-status surfaces and direct Privacy Center access.

## Wardveil Security

- Security-aware launcher experiences and security-state indicators.
- Protected application access where supported.
- Suspicious-application warnings where supported.
- Direct Security Center access.
- Security-sensitive contextual actions.
- Protection of Launcher configuration/personalization and applicable search-integration state.

## GoreeCloud Mesh

- Compatible-device awareness.
- Cross-device application handoff and nearby-device actions.
- Connected-device contextual cards and device-triggered layouts.
- Cross-device content suggestions and authorized connected-device search through applicable Launcher provider contracts.
- Coordinated Launcher state across supported endpoints.

## GoreeCloud Location

- Location-aware application suggestions when explicitly enabled.
- Destination/travel information and location-sensitive cards.
- Nearby-action suggestions and GoreeCloud Maps shortcuts.
- User-controlled location personalization.

## GoreeCloud Mail

- Unread-mail widgets and important-message cards.
- Mail results through explicit first-party Launcher Universal Search provider integration where implemented and authorized.
- Contact communication shortcuts and contextual email actions.

## GoreeCloud Messenger

- Recent-conversation and contact communication shortcuts.
- Unread-message indicators.
- Conversation widgets and contextual messaging actions.
- Searchable first-party conversation/content contracts where implemented and authorized.

## GoreeCloud Maps

- Destination shortcuts, commute information, upcoming-trip information, and navigation suggestions.
- Location-aware cards and destination search from Launcher.

## GoreeCloud Calendar

- Upcoming-event cards, Calendar widgets, meeting shortcuts, event-based application suggestions, context-aware schedule information, and explicit searchable event integration where implemented.

## GoreeCloud Search

- Optional advanced search/retrieval, Web/current-information, query-processing, semantic, federated, filter/operator, and cross-service discovery provider/backend for Launcher Universal Search once separately stable and authorized.
- Not required for core Launcher Universal Search.
- No authority over Launcher-owned local search/index state.
- No implicit upload of local files, photos, contacts, app inventory, Launcher history, or unrelated local result payloads.

## GoreeCloud Index

- Optional scalable indexing/retrieval provider/backend for Launcher Universal Search once separately stable and authorized.
- May supply large-scale metadata/content catalogs, semantic indexing, cross-service indexing, high-performance retrieval, and background indexing pipelines.
- Does not own the user-facing Launcher Universal Search experience or core provider framework.
- Must fail independently so Index unavailability does not disable core Launcher search.

## Glaze UI

Launcher is a native Glaze UI experience. The intended product scope includes consistent GoreeCloud visual language, adaptive layouts across supported device classes, dynamic materials/interface depth, responsive motion, wallpaper-aware appearance, unified accessibility behavior, consistent components, device-appropriate interaction patterns, platform theme integration, and seamless visual continuity with other GoreeCloud applications. The current approved target is the latest applicable Stable Glaze UI release; full application-specific Glaze UI 2.1 acceptance remains separately evidence-gated.

## Core product principle

GoreeCloud Launcher is intended to be more than an application grid. It is the personalized front door to GoreeCloud: bringing applications, files, people, devices, search, information, services, privacy controls, security state, continuity features, and contextual actions together in one adaptive interface.

Launcher intelligence and personalization should remain transparent and user-controlled.