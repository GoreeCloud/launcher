# GoreeCloud Launcher User Manual

## Current availability

GoreeCloud Launcher is a **Development** Android HOME application. It is not yet a signed production/Stable release. Current source provides a substantially rebuilt daily-launcher shell with a real Home surface, scoped launchable-app discovery, Apps, Launcher Settings, local placement controls, persisted presentation preferences, a Launcher-owned Universal Search foundation, Home layout locking, configurable Home gestures, configurable Universal Search Home presentation, and the guarded terminal-Room multi-page Home foundation.

Features described under **Approved future product direction** are planned/target capabilities and are **not currently available** unless a current-behavior section explicitly says otherwise.

## Make GoreeCloud Launcher your Home app

After installing a Development build, open GoreeCloud Launcher and use the default-Home control when shown. Android remains the authority for which launcher is the default and presents the system chooser.

You can change the default launcher later through Android system settings. Exact labels vary by device and Android version.

## Home screen

The primary Home experience is a launcher-style surface. Android renders the device wallpaper behind the launcher window, and Home presents the persisted application grid and Dock over that surface without requesting wallpaper-storage privileges.

- Tap an app icon to launch it.
- Long-press and drag a primary Home icon to any visible configured Home grid cell. Dropping onto an empty cell places it there; dropping onto an occupied cell swaps the two primary Home positions. Long-press without moving opens placement management.
- Open **Apps** from the Home affordance to browse installed launchable applications.
- Long-press empty Home space to enter **Edit Home**, then use its **Settings** action to change supported Home, Apps, icon, label, appearance, layout-lock, and Launcher Universal Search preferences.
- Swipe one finger downward through the unobstructed Home gesture zone to open Launcher Universal Search by default. This assignment can be changed under **Launcher settings → Gestures**.
- Favorites and Dock are seeded from installed launchable apps on first run when needed.
- The Dock is currently bounded to five items.

The launcher discovers launchable activities through Android `LauncherApps` across available profiles. The manifest uses a scoped `MAIN` + `LAUNCHER` package-visibility query without requesting broad `QUERY_ALL_PACKAGES` access; core search no longer requires the legacy GoreeCloud Index search-action query.

The primary Home page remains the protected HOME rank-zero page. Under terminal Room authority, Launcher can migrate its legacy null-coordinate Favorites rows into authoritative grid coordinates on demand; subsequent primary Home drags persist those coordinates without creating a second workspace authority.

## Launcher Universal Search from Home

**GoreeCloud Launcher owns Universal Search.** Swipe down on the unobstructed Home gesture zone opens the Launcher-owned search surface by default; the assignment can be changed under **Launcher settings → Gestures**. When the Home search bar is enabled, tapping **Search GoreeCloud** always opens Launcher Universal Search.

Launcher Settings provides two Home-entry modes:

### Permanent on Home

Home keeps the **Search GoreeCloud** affordance visible. Swipe down opens Launcher Universal Search by default, but the gesture can be reassigned in **Launcher settings → Gestures**.

### Gesture only

The persistent Search GoreeCloud affordance is hidden. Swipe down still opens Launcher Universal Search by default, but any supported Home gesture can be assigned to search under **Launcher settings → Gestures**.

The current Development search foundation provides installed applications, Android application shortcuts, and user-enabled local Contacts, Call history, Messages, and file-name results. Local file Search is limited to Android Storage Access Framework folders that you explicitly choose; Launcher indexes bounded file-name and MIME metadata only and does not read file contents or request broad storage access.

When Universal Search opens, the idle view is intentionally minimal: one search field with a search icon, **Find anything on your device…**, and a settings icon at the far right. Result groups, status information, and other search controls appear only after you begin typing or explicitly open settings.

Tap the **settings icon** in the Universal Search field to review enabled sources and their privacy behavior. For **Files**, choose one or more folders to make them searchable. Selected folders are shown in the Sources view. Removing a folder requires confirmation, removes it from Launcher Search, and releases the saved Android read grant when possible. You can choose the folder again later. If one selected document-provider root becomes revoked, malformed, or unavailable, Launcher fails that root softly so other selected roots can continue contributing results.

Google Drive, Dropbox, and Brave Search are exposed only as explicit **Search with…** handoffs. Launcher does not automatically send typed queries to those providers. Core Search remains Launcher-owned, local-first, and does not require GoreeCloud Index, GoreeCloud Search, Launcher Internet permission, or telemetry.

Broader provider discovery/registration, portable recovery of provider controls and file-root grants, recents/history/context, optional GoreeCloud Search/Index backends, and complete representative-device/accessibility/profile/performance acceptance remain separately gated.

## Home layout lock

Launcher Settings includes **Lock Home screen layout**.

When the lock is enabled, current placement-changing operations are blocked for the item/page types that Launcher currently implements. This includes Favorite and Dock membership/order changes, primary Home cell placement, Home-page creation/deletion/reordering, moving supported applications between secondary Home pages, and current within-secondary-page movement controls.

The following normal actions remain available while Home is locked:

- launching applications;
- selecting Home pages;
- opening Apps;
- opening Launcher Settings;
- invoking Launcher Universal Search; and
- changing non-placement presentation preferences.

If you long-press an app while locked, the placement dialog can still open so it can explain the locked state, but its current placement-changing controls are disabled.

### Unlock from Settings

Open **Launcher settings → Home screen → Lock Home screen layout** and turn the switch off. This is the deterministic non-gesture unlock path.

### Unlock by holding on Home

When the layout is locked, Home shows a **Layout locked** control. Press and continuously hold that control for **5 seconds**. Launcher shows progress while you hold. Releasing before the five seconds completes cancels the unlock. Completing the hold turns the persisted layout lock off.

The five-second interaction remains subject to representative physical-device and accessibility acceptance before release/Stable qualification. Future folders, shortcuts, widgets, and other placeable item types must join the same lock policy when those features are implemented; they are not current runtime behavior merely because the target scope mentions them.

## Apps

Open **Apps** from Home to browse the launchable application inventory exposed to the launcher. The Apps surface is separate from Home and Launcher Settings; Home page-management controls are not rendered over it.

Use the **Search apps** field to search the installed-application inventory locally. This Apps view is a specialized Launcher-owned view backed by the same installed-app provider foundation used for Universal Search. It does not require Internet access.

Long-press an app to open its current placement dialog. When the Home layout is unlocked, you can add/remove it from Home or the Dock and use accessible earlier/later ordering controls. For a Home app, the dialog can also save a Launcher-local Home label override or reset it to the application label. **Uninstall app** delegates to Android's system uninstall confirmation; Launcher does not silently remove packages. When the layout is locked, the dialog explains the lock and disables current placement changes.

## Launcher settings

The current Development settings surface is scrollable and persists supported choices locally.

### Home screen grid

Current presets cover Home grids from 4 to 6 columns and 4 to 7 rows through the supported preset combinations in the UI. Once primary Home spatial placement is active, a grid-size change is applied only after the current primary placements can be validated or safely reflowed into the requested grid; a change that cannot preserve all current Home apps fails closed.

The Home screen settings card also contains the **Lock Home screen layout** switch described above.

### Universal Search

Choose **Permanent on Home** or **Gesture only**. Permanent mode keeps the Search GoreeCloud affordance visible; Gesture only removes the persistent Home bar. Search can still be assigned to any supported Home gesture under **Launcher settings → Gestures**.

### Apps screen

You can choose 4, 5, or 6 columns for the Apps grid.

### Icons and labels

You can choose Small, Medium, or Large icon presentation and turn app labels on or off. These settings apply to the rebuilt primary surface and are also used by the current secondary-page presentation where applicable.

A full Theme Manager, third-party icon-pack selection, icon masking, and richer optical icon normalization are approved future capabilities and are not implemented by this Development slice.

### Appearance

The launcher supports persisted **System**, **Light**, and **Dark** appearance selection. Launcher retains evidence-backed Glaze UI Adoption Candidate mapping. The approved current product target is Glaze UI 2.1.0 Stable, but complete rendered/native/accessibility/device acceptance against that release remains separately gated.

## Multi-page Home navigation

When the guarded workspace has reached terminal Room authority, the Development build can expose authoritative HOME pages through a compact horizontal page selector.

Each page selector entry conveys page identity plus authoritative app/unsupported-item context through its accessibility semantics. The selected page is automatically brought into view as page selection/order/count changes.

### Page controls

When Home layout is unlocked, the current selector can expose guarded controls to:

- **Add page**;
- move eligible secondary pages earlier or later without crossing the protected primary page; and
- **Delete empty page** when the selected secondary page is eligible.

The protected primary compatibility Home page remains first and cannot be moved later or deleted. A secondary page cannot be moved ahead of it. Page mutations continue through the authoritative Room mutation boundary; the switcher is not a second workspace source of truth.

When Home layout is locked, Launcher blocks current page mutation callbacks. Page selection remains available because it does not modify placement.

### Apps on secondary pages

Secondary authoritative Room pages render as ordinary icon grids. Tap an icon to launch the app. Long-press a supported secondary-page icon to open its management dialog.

When Home layout is unlocked, current secondary management actions can request:

- move to another authoritative secondary Home page;
- move earlier/later to the nearest permitted free cell; and
- exact one-cell moves left/right/up/down.

These controls are intentionally behind long-press rather than permanently displayed under every icon. Current mutation callbacks are blocked while layout lock is enabled.

The primary Home page supports authoritative within-page cell placement while remaining protected at HOME rank zero. Primary-to-secondary and secondary-to-primary page transfer remain separately gated.

Exact-cell requests fail closed if the target is occupied or outside the authoritative grid. Secondary spatial mutations also fail closed when authority/placement health is invalid or when the workspace changes during the transaction. Unsupported item types are reported rather than falsely rendered as applications.

If authoritative paged Room state is unavailable, Launcher does not fabricate secondary-page state.

## Official Launcher identity and artwork

All canonical GoreeCloud Launcher logos, icons, symbols, illustrations, and artwork are maintained in **`GoreeCloud/goreecloud-branding-assets`**. The canonical Launcher source is `products/launcher/app-icon.svg`.

The Launcher repository is only a consumer. It contains traceable Android adaptive/round/monochrome derivatives plus provenance metadata required to build the Development APK. A consumer derivative is not an independent canonical artwork source.

The current derivatives have source/build/runtime validation, but that does not by itself establish production visual-identity acceptance. Representative icon-mask, themed-icon, small-size, system-chooser, device, and release review remain separate gates.

## Privacy and network behavior

The current launcher has no Android `INTERNET` permission and core Home/App operation remains offline-capable.

- No broad `QUERY_ALL_PACKAGES` permission is used for launcher discovery.
- Core Launcher search uses the Launcher-owned provider path and does not require the legacy Index activity handoff.
- No wallpaper/storage permission is required to show the system wallpaper behind Home.
- Launcher presentation, layout-lock, and Universal Search entry preferences remain local.
- Launcher owns core Universal Search aggregation/ranking and must preserve source/authorization/privacy boundaries.
- Privacy Shield governs applicable privacy/user-control surfaces.
- Wardveil Security governs applicable security/trust surfaces.
- Everkeep governs accepted backup/restore, continuity, preservation, and portability.
- GoreeCloud Identity governs account/profile-backed authorization where applicable.
- GoreeCloud Mesh governs authenticated/authorized cross-service and cross-device integration.
- Glaze UI governs interface/design-system conformance.

Naming a platform system does not mean every integration is currently implemented or accepted.

## Current limitations

Still incomplete or separately gated include mature cross-page drag/drop editing; primary↔secondary spatial movement; folders and smart folders; pinned/dynamic shortcut placement beyond current Search support; advanced widget resizing/stacking and portable widget rebinding/recovery; complete Theme Manager/icon-pack/masking behavior; additional gesture types and registered-command/provider targets beyond the initial configurable Home-gesture set; broader Launcher Universal Search providers for device/GoreeCloud/third-party content; optional GoreeCloud Search/Index provider-backend integration; fully polished Glaze UI Universal Search presentation and complete Launcher Glaze UI 2.1 acceptance; layout-lock coverage for future placeable item types plus representative-device five-second-hold acceptance; production visual-identity acceptance; full Glaze Theme Engine behavior; versioned backup/restore; cross-device continuity; complete platform-system integration acceptance; Android OS process-death/schema-upgrade recovery acceptance; representative physical-device default-HOME acceptance; signed release packaging; and Stable qualification.

# Approved future product direction — not currently available

The long-term Launcher product scope is substantially broader than the current Development build.

## Home and organization

Future Launcher releases are intended to support deeply customizable Home pages and grids, margins/padding, folders, shortcuts, widgets, multiple dock pages, page indicators, wallpaper behavior, precise placement, lock enforcement across all supported placeable item types, overlapping supported elements, and adaptive layouts for different form factors.

## Application drawer

The intended Apps/application-drawer experience includes folders/tabs, categories, smart groups, suggested/recent/frequent applications, hiding, richer visual customization, and context-sensitive ordering in addition to the current local Apps filter.

## Launcher Universal Search, GoreeCloud Search, and GoreeCloud Index

The approved direction expands Launcher Universal Search across applications, application content, people, device/GoreeCloud settings, files/documents/media, shortcuts/actions, GoreeCloud services, connected resources, and other authorized providers. Launcher owns the user-facing search experience, provider framework, aggregation/ranking, commands, shortcuts, and actions.

GoreeCloud Search may later provide optional advanced search, semantic/query-processing, federation, filters/operators, or Web/current-information capabilities. GoreeCloud Index may later provide optional scalable indexing, catalogs, background indexing pipelines, and high-performance retrieval. Neither is required for core Launcher Universal Search.

## Appearance and gestures

Launcher settings includes a **Gestures** section for Home-surface assignments. Swipe up, Swipe down, Swipe left, Swipe right, Double-tap, and Tap and hold can each be mapped independently to None, Apps, Launcher Universal Search, Launcher settings, Home editor, Wallpaper, Theme Manager, or a currently launchable app. Defaults preserve Swipe up → Apps, Swipe down → Universal Search, and Tap and hold → Home editor. App targets resolve through Android `LauncherApps`; if a selected app is removed or unavailable in its profile, the gesture fails safely without dispatching an unvalidated intent. Swipe left/right, Double-tap, and Tap and hold apply to empty Home space so long-press drag/reorder and app placement controls remain authoritative.

The broader personalization direction still includes icon packs, icon masking, bounded icon scaling/normalization, GoreeCloud/adaptive themed icons, icon shapes, wallpaper-derived palettes, custom colors/transparency, custom Home/Apps/folder/dock styling, additional gesture and registered-command targets, reduced-motion behavior, and high-contrast/accessibility preferences.

## Smart information and cards

Future optional contextual experiences may include application suggestions, calendar/weather/event/delivery/travel/flight/navigation/media/file/device information, privacy/security status, backup/sync state, and other GoreeCloud service cards. These surfaces must remain configurable and privacy-aware.

## Backup and continuity

The approved direction includes explicit **Backup Launcher configuration** and **Restore Launcher configuration** actions using a versioned, validated local/offline-capable format, configuration history, safe widget rebinding/reconfiguration, device migration, supported Sync continuity, Everkeep preservation, and safe device-replacement recovery. These actions are not implemented in the current Development source.

## GoreeCloud integration

The intended product can integrate, where implemented and authorized, with GoreeCloud Drive, Sync, Backups, Everkeep, Identity, Privacy Shield, Wardveil Security, Mesh, Location, Mail, Messenger, Maps, Calendar, Search, Index, Glaze UI, and other compatible GoreeCloud services.

Personalization and contextual intelligence should remain transparent and user-controlled. Privacy, security, identity, continuity, and cross-device features require substantive implementation and acceptance rather than being inferred from names or visuals.

Refer to `README.md`, `SPECIFICATIONS.md`, `FEATURES.md`, `BENEFITS.md`, `COMPETITIVE-OBJECTIVES.md`, and the `docs/` directory for scope, implementation state, architecture, and acceptance details.