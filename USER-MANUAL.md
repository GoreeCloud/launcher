# GoreeCloud Launcher User Manual

## Current availability

GoreeCloud Launcher is a **Development** Android HOME application. It is not yet a signed production/Stable release. Current source provides a substantially rebuilt daily-launcher shell with a real Home surface, scoped launchable-app discovery, Apps, Launcher Settings, local placement controls, persisted presentation preferences, a Launcher-owned Universal Search foundation, Home layout locking, configurable Home gestures, configurable Universal Search Home presentation, and the guarded terminal-Room multi-page Home foundation.

Features described under **Approved future product direction** are planned/target capabilities and are **not currently available** unless a current-behavior section explicitly says otherwise.

### Development installation and updates

The side-by-side Development package is `com.goreecloud.launcher.dev`. Current source supports an externally supplied positive Development `versionCode`, and GitHub Actions uses its run number for newly built CI artifacts so later CI builds do not reuse a fixed code.

Version progression alone does not establish update-in-place support. Ordinary PR artifacts still use the CI debug signing identity. A protected persistent Development signing identity has not yet been provisioned into a trusted distribution workflow, so current CI APKs are installability artifacts rather than update-compatible Development releases. A future protected Development channel must preserve one signing identity, keep version codes increasing, and pass representative-device update-in-place verification.

## Make GoreeCloud Launcher your Home app

On a genuinely new Launcher setup, GoreeCloud Launcher opens into a three-step startup wizard before the ordinary Home, Apps, and Universal Search surfaces. The wizard can request Android's Home role and lets you choose the initial Home/app behavior, grid, labels, Universal Search Home entry, optional new-app placement, and whether built-in hints remain enabled. Its final step also explains the current direct-placement and folder model: hold an app in Apps and drag it to an exact Home or Dock position; use **Add apps** at the end of a folder grid or from the folder menu; swipe larger folders between compact pages; and treat connected Search as opt-in rather than automatic query transmission.

Existing upgraded workspaces whose historical starter setup was already applied are not intentionally forced back through onboarding.

Android remains the authority for which launcher is the default and presents the system chooser. You can change the default launcher later through Android system settings. Exact labels vary by device and Android version.

### Automatic Home apps

The startup wizard and Launcher Settings expose three automatic Home modes:

- **No automatic apps** — Launcher adds no transient automatic app suggestions to the Home grid.
- **10 most recent apps** — Launcher shows up to ten apps most recently launched through GoreeCloud Launcher.
- **10 most used apps** — Launcher shows up to ten apps with the highest local Launcher launch counts.

Recent/most-used suggestions are presentation-only. They fill otherwise-empty Home cells, exclude persisted Home Favorites and Dock duplicates, and do not rewrite saved Room-authoritative placement. Long-press an automatic suggestion on Home and choose **Remove** to suppress that suggestion on this device; the app remains installed and remains available in Apps. Manual Home apps remain available in every mode: press and hold an app in Apps, keep holding and move to drag a copy onto the exact Home cell or Dock position you want, or use the compact app context actions.

The ranking store is local and privacy-bounded. It retains only application workspace keys, aggregate Launcher launch counts, and a bounded recency ordering. It does not request Android Usage Access or retain launch timestamps, dwell time, Search queries, or network telemetry for this feature.

## Home screen

The primary Home experience is a launcher-style surface. Android renders the device wallpaper behind the launcher window, and Home presents the persisted application grid and Dock over that surface without requesting wallpaper-storage privileges.

- Tap an app icon to launch it.
- Long-press and drag a saved Home app to reposition it. On primary Home, dropping onto an empty visible cell places it there and dropping onto an occupied primary cell swaps the two primary positions. While dragging a saved Home app on any Home page, keep holding at a valid left or right page edge briefly to switch to the adjacent page without changing the workspace yet; once that page appears, move to the exact destination cell and release. Invalid, spacing-gap, occupied, or out-of-bounds cross-page destinations fail closed and preserve the original placement. A quick edge release still uses the deterministic adjacent-page edge landing behavior. Long-press without moving opens the compact app context menu. On Home, its first quick action is **Remove**; this removes the Home placement (or suppresses a presentation-only automatic suggestion) without uninstalling the app. **Uninstall** remains a separate Android-confirmed action. When an app is dragged from Apps, the authoritative add is committed directly to the requested primary Home cell or Dock position; Drawer drags do not imply live cross-page placement.
- Long-press a Home widget, keep holding, and drag it to a free grid area. On primary or secondary Home, releasing a widget inside a valid left/right page edge moves it to the adjacent Home page at a span-aware opposite-edge position; the landing row is clamped so the complete widget remains in bounds. If that exact destination overlaps another item or cannot fit, the move is rejected and the original placement is preserved. A stationary long-press opens widget management, where resize, remove, exact same-page movement, and **Move to another Home page** remain available even after the widget is on a secondary page. Fresh starter workspaces place the Launcher-owned **Glance** widget in the Home grid when room is available. Upgraded installs may retain the older fixed clock/date card; while Home is unlocked, long-press that fixed card once to request conversion to movable Glance. The conversion is explicit and fails without changing the existing card if the grid has no room. Glance combines local time/date with opt-in local weather. Tap the weather area to grant foreground location access; Launcher then shows current temperature and a condition icon for clear/cloudy weather, fog, wind, rain, snow, or thunderstorms. Coordinates are not persisted, and weather remains unavailable rather than fabricated when permission or the weather request is unavailable.
- Open **Edit Home → Widgets** to browse Launcher-owned cards and Android widgets. The built-in catalog now leads with separate **Calendar** and **Weather** 2 × 2 cards, followed by Glance, Universal Search, Quick actions, Battery, Date, **Month**, Digital/Compact/Analog clocks, and Launcher Status. Calendar is local-only. Weather includes local time plus current conditions after foreground location is allowed; recent successful conditions are reused briefly in memory so normal Home returns do not visibly reload them. **Month** remains a 4 × 2 local calendar overview that highlights today without reading calendar events.
- Long-press a Home folder while the layout is unlocked. On primary or secondary Home, release it over a free grid cell to move it within the current page. Releasing the folder inside a valid left/right page edge moves it to the adjacent Home page at the corresponding opposite-edge column and release row. If an exact same-page or cross-page destination is occupied or invalid, the move is rejected and the folder keeps its existing placement. The folder menu's **Move to another Home page** action remains available and uses the first free destination cell instead of an exact drag target.
- Open **Apps** from the Home affordance to browse installed launchable applications.
- Long-press empty Home space to enter **Edit Home**, then use its **Settings** action to change supported Home, Apps, icon, label, appearance, layout-lock, and Launcher Universal Search preferences. Pressing Android **Home** while Edit Home is open exits the editor and returns to the ordinary primary Home surface. You can also open the same Launcher Settings surface from the gear at the top of **Apps**.
- Swipe one finger downward through the unobstructed Home gesture zone to open Launcher Universal Search by default. This assignment can be changed under **Launcher settings → Gestures**.
- The first-run setup can seed the five-item Dock from common app roles when available. Automatic Home apps are controlled separately by the selected No automatic apps / 10 most recent apps / 10 most used apps mode.
- Five apps remain the first-run Dock default, not a capacity limit. You can add more apps; the Dock tightens item presentation while preserving at least a 48 dp touch slot, then scrolls horizontally when more items are present than fit safely at once.

The launcher discovers launchable activities through Android `LauncherApps` across available profiles. The manifest uses a scoped `MAIN` + `LAUNCHER` package-visibility query without requesting broad `QUERY_ALL_PACKAGES` access; core search no longer requires the legacy GoreeCloud Index search-action query.

The primary Home page remains the protected HOME rank-zero page. Under terminal Room authority, Launcher can migrate its legacy null-coordinate Favorites rows into authoritative grid coordinates on demand; subsequent primary Home drags persist those coordinates without creating a second workspace authority.

### Built-in Launcher hints

After startup, Launcher can show a short dismissible Home hint covering the main interactions: **swipe up for Apps**, **swipe down for Universal Search**, **long-press empty Home space for Edit Home**, **hold an app in Apps and drag it to an exact primary Home or Dock position**, **keep holding a saved Home app at a left/right page edge briefly to switch pages and release on the exact target cell**, **long-press and drag Home widgets to a free area or adjacent page edge**, and **long-press a Home folder to move it to a free cell or adjacent page edge while using Add apps inside opened folders**.

You can disable hints during startup, dismiss the Home hint with **Got it**, and later turn **Launcher hints** back on from Launcher Settings. Use **Review Launcher setup → Open** to replay the three-step first-use guidance without clearing the Home layout, Search, appearance, or hint choices. Hint and setup-progress state stay local and are not telemetry.

## Launcher Universal Search from Home

**GoreeCloud Launcher owns Universal Search.** Swipe down on the unobstructed gesture zone of any Home page opens Launcher Universal Search by default; the gesture assignment can be changed under **Launcher Settings → Gestures & inputs**.

Under **Launcher Settings → Search**, Home Search now has four Development presentation choices:

### Swipe down

This is the default and fail-safe mode. No persistent Search object is required on Home. Swipe down opens Universal Search unless you reassign that gesture.

### Movable

Launcher uses the existing first-party **Universal Search** 4 × 1 Home widget as the persistent Search surface. It participates in the Room-authoritative Home grid, so you can long-press and drag it like other Home widgets, move it between Home pages, and use normal widget management. Selecting Movable does not create a second Search implementation.

Launcher manages one dedicated movable Search widget for this setting. If an existing Universal Search widget is already present on Home, Launcher reuses the visible Search-widget path rather than adding another one. If the primary Home has no free 4 × 1 area yet, Launcher keeps a fixed bottom Search bar visible and retries managed placement after primary-Home geometry changes. Manually added Search widgets remain ordinary user-managed widgets.

### Top

A persistent **Search GoreeCloud** bar is pinned above the Home grid. It does not consume Home-grid cells. Fixed-bar presentation can use the available Glass, Clear, or Solid styles.

### Bottom

A persistent **Search GoreeCloud** bar is pinned below the Home grid. It does not consume Home-grid cells. Fixed-bar presentation can use the available Glass, Clear, or Solid styles.

Tapping either a fixed Search bar or a movable Universal Search widget opens the same Launcher-owned Universal Search surface.

The current Development search foundation provides installed applications, Android application shortcuts, and user-enabled local Contacts, Call history, Messages, and file-name results. Local file Search is limited to Android Storage Access Framework folders that you explicitly choose; Launcher indexes bounded file-name and MIME metadata only and does not read file contents or request broad storage access.

When Universal Search opens, the idle view is intentionally minimal: one search field with a search icon, **Find anything on your device…**, and a settings icon at the far right. Result groups, status information, and other search controls appear only after you begin typing or explicitly open settings.

Tap the **settings icon** in the Universal Search field to review enabled sources and their privacy behavior. For **Files**, choose one or more folders to make them searchable. Selected folders are shown in the Sources view. Removing a folder requires confirmation, removes it from Launcher Search, and releases the saved Android read grant when possible. You can choose the folder again later. If one selected document-provider root becomes revoked, malformed, or unavailable, Launcher fails that root softly so other selected roots can continue contributing results.

Enabled connected sources share the same Universal Search result panel. **Google Drive** can participate inline after authorization; providers that remain handoff-only, including Brave Search and Dropbox in the current Development source, appear under **Search online** as full result rows instead of behind a separate provider strip.

**Files** and **Google Drive** are intentionally different sources. **Files** searches folders you explicitly choose through Android's folder picker, including document-provider folders. Enabling **Google Drive** instead starts Google account authorization for the metadata-only `drive.metadata.readonly` scope. After authorization, Launcher sends the typed query to the Google Drive API over HTTPS only while Drive is enabled, returns a bounded set of matching Drive files/folders inline, and opens the selected Drive item through its Google-provided web link. The short-lived access token is kept in process memory only. If authorization is unavailable, expires, or is rejected, Drive stops receiving queries and its source control becomes reconnectable.

The Development source also attempts to reacquire a previously granted Drive access token silently after process restart when the persisted Drive source is enabled. A user-facing account prompt is not opened automatically at startup. Ordinary CI APKs use Android debug identity and fail closed for the account flow when that identity is not registered as the authorized Android OAuth client. In those builds, use **Files → Choose folder** for permission-scoped Drive folders. Full inline Google Drive remains gated on a protected signed Development build whose Android package/signing fingerprint is registered with Google, followed by representative-device consent, restart, expiry, shared-drive, and result-opening acceptance.

Brave live suggestions are not yet active. The official Brave Autosuggest API requires a confidential subscription token, so Launcher will not embed that key in the APK or scrape Brave pages. Until a governed credential path exists, Brave remains a truthful explicit online handoff.

Broader provider discovery/registration, portable recovery of provider controls and file-root grants, Google Drive representative-device/OAuth-configuration acceptance, Brave Autosuggest, additional connected adapters, recents/history/context, optional GoreeCloud Search/Index backends, and complete accessibility/profile/performance acceptance remain separately gated.

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

If you long-press an app while locked, the compact app context menu can still open, but placement-changing actions are disabled while the layout lock is active.

### Unlock from Settings

Open **Launcher settings → Home screen → Lock Home screen layout** and turn the switch off. This is the deterministic non-gesture unlock path.

### Unlock by holding on Home

When the layout is locked, Home shows a **Layout locked** control. Press and continuously hold that control for **5 seconds**. Launcher shows progress while you hold. Releasing before the five seconds completes cancels the unlock. Completing the hold turns the persisted layout lock off.

The five-second interaction remains subject to representative physical-device and accessibility acceptance before release/Stable qualification. Future folders, shortcuts, widgets, and other placeable item types must join the same lock policy when those features are implemented; they are not current runtime behavior merely because the target scope mentions them.

## Apps

Open **Apps** from Home to browse the launchable application inventory exposed to the launcher. The Apps surface is separate from Home and Launcher Settings; Home page-management controls are not rendered over it.

Use the **Search apps** field to search the installed-application inventory locally. This Apps view is a specialized Launcher-owned view backed by the same installed-app provider foundation used for Universal Search. It does not require Internet access.

Long-press an app to open the compact Glaze context menu. A stationary hold opens that menu; keep holding and move to drag the app directly toward Home or the Dock when the layout is unlocked. The same context surface provides Home/Remove, Dock/Undock, app-specific Widgets when available, App info, folder assignment, supported app shortcuts, and **Uninstall**. There is no second **More options** placement dialog. **Remove** only removes or suppresses the Home icon; it does not uninstall the application. **Uninstall** delegates to Android's system confirmation, and Launcher never silently removes packages. Placement-changing actions are disabled while the Home layout is locked.

The App Drawer sort control provides **A–Z**, **Z–A**, **Most recent**, and **Most frequent**. The two usage-based sorts use only the Launcher's local privacy-bounded launch history described above; they do not request Android Usage Access. User Apps and Work Apps remain separate profile views.

## Launcher settings

The current Development Settings experience opens on a searchable category home and persists supported choices locally. It is available from **Edit Home → Settings** and from the gear at the top of **Apps**. If GoreeCloud Launcher is not the active HOME application, the overview shows a direct default-Home status/action banner near the top.

### Settings categories

The searchable overview groups current controls into **Home screen**, **App drawer**, **Folders**, **Search**, **Look & feel**, **Gestures & inputs**, **Notification badges**, and **System & setup**. Search recognizes category descriptions and detailed-control terms such as grid, Dock, page transition, icon pack, notification access, and Universal Search. Opening a category reveals the existing detailed controls; the overview is not a second preference store.

### Home screen

Launcher Settings includes the same **Automatic Home apps** choices used by the startup wizard: **No automatic apps**, **10 most recent apps**, or **10 most used apps**. Changing this mode affects only transient automatic suggestions; it does not remove or reposition manually saved Home apps. Dock material/capacity information is grouped with Home screen settings.

Current grid presets cover Home grids from 4 to 6 columns and 4 to 7 rows through the supported preset combinations in the UI. Once primary Home spatial placement is active, a grid-size change is applied only after the current primary placements can be validated or safely reflowed into the requested grid; a change that cannot preserve all current Home apps fails closed.

The Home settings area also includes local app-activity clearing, **Launcher hints → Show again**, optional **Add new apps to Home**, and the **Lock Home screen layout** switch described above.

### Universal Search

Choose **Permanent on Home** or **Gesture only**. Permanent mode keeps the Search GoreeCloud affordance visible; Gesture only removes the persistent Home bar. Search can still be assigned to any supported Home gesture under **Launcher settings → Gestures**.

### Apps screen

You can choose 4, 5, or 6 columns for the Apps grid.

### Icons and labels

You can choose Small, Medium, or Large icon presentation and turn app labels on or off. These settings apply to the rebuilt primary surface and are also used by the current secondary-page presentation where applicable.

A full Theme Manager, third-party icon-pack selection, icon masking, and richer optical icon normalization are approved future capabilities and are not implemented by this Development slice.

### Appearance

The launcher supports persisted **System**, **Light**, and **Dark** appearance selection. Launcher retains evidence-backed Glaze UI Adoption Candidate mapping. The current repository mapping target is GLAZE UI V1.6 / 1.6.0 at the accepted source revision recorded by Launcher; complete rendered/native/accessibility/device acceptance remains separately gated.

## Multi-page Home navigation

When the guarded workspace has reached terminal Room authority, ordinary Home pages can be swiped horizontally. A clear horizontal gesture switches pages as soon as it crosses the Launcher's distance/direction threshold rather than waiting for finger-up. Page dots communicate position without permanently overlaying page-management controls on the wallpaper.

Long-press empty Home space to open **Edit Home**. On phone layouts, its header, adaptive **Home pages** carousel, and five primary actions are composed into one viewport without requiring ordinary vertical scrolling; the action rail contains Wallpaper, Widgets, Apps, Folders, and Settings. The selected page remains prominent while neighboring page edges stay visible for horizontal navigation. Swipe the preview carousel or tap a page preview to select it. Page creation/deletion and the available page-management controls stay within the Edit Home/page-management experience rather than relying on the removed redundant **Manage** button.

### Page controls

When Home layout is unlocked, the dedicated page manager can:

- **+ Add** a Home page from its compact header action;
- swipe/tap between accurate saved-layout previews;
- move eligible secondary pages earlier or later without crossing the protected primary page; and
- **Delete empty page** when the selected secondary page is eligible, after confirmation.

The protected primary compatibility Home page remains first and cannot be moved later or deleted. A secondary page cannot be moved ahead of it. Page mutations continue through the authoritative Room mutation boundary; the carousel is not a second workspace source of truth.

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

Core Home/App operation remains offline-capable. The PR #248 Development candidate declares Android `INTERNET` only so explicitly enabled connected Search adapters can use HTTPS; local Search sources, Home, Apps, and Launcher settings do not require network access, and cleartext traffic remains disabled.

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