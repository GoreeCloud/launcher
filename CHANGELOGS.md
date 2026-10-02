# GoreeCloud Launcher — Changelogs


## October 1, 2026 — correct Search top chrome and starter Home defaults from device feedback

Representative-device screenshots from the CI #507 mockup-alignment build exposed three Development defects that automation had not caught. Universal Search still rendered a product title/scope row and privacy chip above the search field, an already-initialized empty starter Home could bypass the Calendar/Quick actions seeding path, and the starter workspace did not explicitly establish the intended second Home page.

This follow-up makes the **Search with GoreeCloud…** field the first and only top chrome on the ordinary Universal Search surface; source management remains available through the settings control inside that field. Fresh starter initialization now creates a deterministic second Home page while retaining `home:0` as the sole protected rank-zero page. The empty-starter repair path is narrowly limited to a Room-authoritative, startup-complete Development workspace with no Favorites, a Dock count that exactly matches the configured starter Dock size, no runtime/test fixture pages, and no Home apps/widgets/folders/unsupported items. In that signature it keeps the protected primary plus one secondary page, removes only excess empty secondary pages, and seeds the real Calendar and Quick actions widgets. User-created Home content prevents this repair path from running.

The migration effect intentionally keys on stable readiness predicates rather than the full rendered-page object, preventing its own page/widget writes from cancelling the coroutine mid-repair.

The API 36 acceptance harness is also updated for the two-page/default-widget contract: editor checks target the editor surface/action tags instead of assuming **Edit Home** is a unique text label (Quick actions legitimately exposes the same label on Home), and paging cases discover/navigate the actual secondary-page index instead of assuming a temporary page is always Page 2.

The Home background now also exposes an explicit accessibility long-click semantic action labeled **Edit Home** in addition to the existing pointer long-press. This keeps the gesture available to assistive/semantic clients and gives runtime acceptance a deterministic editor action even when default widgets visually occupy much of the grid. The API 36 editor/wallpaper cases invoke that semantic action directly, while the legacy swipe-up compatibility case injects its swipe through the center of the Home surface instead of the bottom edge so the externally hosted Dock cannot intercept the gesture in the two-page layout. The unified two-page pager now observes configured vertical Home gestures during the Initial pointer-event pass without consuming them, so Android 16 pager/child arbitration cannot hide swipe-up/down actions before Launcher classifies them.

With more than one Home page active, the unified horizontal pager observes vertically dominant swipes and dispatches the configured Home swipe-up/down actions without consuming the pager's horizontal stream. Single-page Home now uses the same Initial-pass, non-consuming vertical observer instead of `detectVerticalDragGestures`, so seeded widgets, app tiles, Search, or other child content cannot win gesture arbitration before Launcher sees the configured global Home swipe. The externally hosted pager remains the sole vertical observer on multi-page Home, preventing duplicate dispatch.

The full-screen widget gallery now exposes an explicit **Close** action in its own header instead of relying only on Android Back to exit. This gives touch, keyboard, accessibility, and runtime clients a first-party dismissal target and lets lifecycle tests close the dialog through supported Launcher UI before destroying the scenario.

**Acceptance boundary:** source changes require fresh exact-head build/JVM/lint/schema, Android 16 runtime, transition-performance, provenance, required-gate, and protected-promotion validation before integration. Physical-device visual comparison remains required after a new APK is produced. Launcher remains Development.


## October 1, 2026 — realign Home and Universal Search to the owner mockups

This Development candidate treats the two owner-supplied references in `GoreeCloud/Mockups/GoreeCloud Launcher/` — **Launcher Home — Current Mockup** and **Universal Search — Current Mockup** — as the product-specific visual source of truth for the current Launcher redesign.

Home moves away from the oversized dark Glance card toward the mockup's wallpaper-first composition: time/date/weather are presented as open foreground content, the product line becomes **A calmer, more private you**, fixed Search and Dock use light translucent Glaze materials, and fresh/untouched v1 starter layouts replace the reserved starter Glance card with the fixed glance plus real Calendar and Quick actions widgets when grid space permits. Existing user-created layouts are not identified by that reserved starter id and are not rewritten by this migration.

Universal Search keeps the existing local-first provider and permission boundaries but adopts the mockup's lighter hierarchy: a larger product heading, descriptive scope copy, a subtle **A more private you** source-management action, **Search with GoreeCloud…** input copy, a transparent wallpaper-preserving results host, lighter independent result cards, and a three-column app presentation instead of the prior dense four-column grid plus duplicate list tail. Installed-app search also rejects resource-reference strings such as `res/mipmap-...xml` as visible labels and falls back to a human-readable package-derived label rather than leaking diagnostic resource text.

The mockups are presentation authority only. The implementation does not fabricate the sample contact, file, calendar-event, privacy-status, cloud-sync, or other demo content shown in the references; live Launcher data and explicit permission/provider state remain authoritative.

**Acceptance boundary:** source implementation is under exact-head CI/runtime validation. Representative physical-device visual comparison against the canonical mockups, accessibility/large-text/form-factor review, protected distribution, and Stable qualification remain open until their evidence exists. Launcher remains Development.

## October 1, 2026 — add local arithmetic and unit-conversion Quick answers

Universal Search now includes a Launcher-owned **Quick answers** source. Arithmetic uses a deliberately bounded parser with precedence, parentheses, unary signs, and the four basic operators rather than arbitrary expression/code evaluation. Unit conversion uses an explicit local allowlist for common length, mass, time, and temperature units.

Quick answers participate through the existing built-in provider contract as local-only, permission-free, no-network, and no-query-retention behavior. Results are ranked as direct local answers and expose an explicit tap-to-copy action. Android 13+ relies on the platform clipboard confirmation; earlier Android versions receive a short Launcher copy confirmation.

Focused JVM coverage includes arithmetic precedence/parentheses, malformed expressions, division-by-zero rejection, common conversions, incompatible dimensions, below-absolute-zero rejection, bounded query length, copy payloads, and built-in provider metadata.

**Acceptance boundary:** merged PR #149 integrated Quick answers to authoritative `main` as `1f3758eb9ea5722e6557bae56ecc3be240d3c12e` from exact head `a52056f558ef028b844f7a4317500a8602d219ae` after Mandatory app migration provenance #390, Android Development Foundation #850, Migrated Android apps CI #403 including Launcher build/JVM/lint/schema, complete API 36 runtime instrumentation, transition-performance diagnostics and the migrated-app required gate, plus Protected promotion #357 all succeeded. Representative-device Search/clipboard/accessibility/large-text/form-factor/performance acceptance remains open; Launcher remains Development.

## October 1, 2026 — polish Home and Universal Search Glaze composition

This Development candidate turns the supplied Home/Universal Search visual direction into a bounded native Launcher polish pass without changing provider authority, permissions, workspace persistence, or network policy. Universal Search now has a clear product heading, an explicit **Local first · connected sources are opt-in** boundary, a direct Sources action, a broader **Search this device** field, presentation-policy-resolved translucent surfaces, a ranked **Top result** treatment, counted result sections, **Contacts** naming, purpose-specific original vector category glyphs, and preserved explicit Call/Message actions. The product title and result-section labels are also exposed as semantic headings so the stronger visual hierarchy remains useful to assistive technology.

Home keeps the current Room-authoritative layout and configurable presentation model while refining the fixed Search capsule copy to **Search with GoreeCloud…** and giving the fixed Glance hero slightly more optical radius, restrained depth, and the non-authoritative product line **A calmer Home. Your way.** The built-in Glance, Calendar, and Weather cards now resolve their inner presentation through the same Glaze material fallback instead of retaining light-only inner surfaces; the Dock and shared Home page indicator use a more restrained floating-glass treatment. Widget-picker previews no longer show fabricated dates, temperatures, clock times, or battery percentages: previews use current local date/time where authoritative and neutral purpose-specific glyphs where live data is unavailable. The wallpaper Home preview follows the same rule by using the current local time/date plus a neutral permission-gated Weather treatment instead of a fixed sample forecast. Reduced-transparency and performance fallbacks continue to resolve through the accepted GLAZE UI V1.6 presentation policy.

The source intentionally remains pinned to Official Stable GLAZE UI **1.6.0** / revision `a7180679ea851389e0f3004515f9a25f420e716d`. The active Glaze 1.7 Development line is not treated as consumer-eligible or as Launcher conformance evidence.

**Acceptance boundary:** merged PR #147 integrated this Development polish to authoritative `main` as `d667a65ca565ff1512f4065a3462e4b16ca76f49` from exact head `29072f1fe007b5839a08afb4ea69f8c4167060be` after Mandatory app migration provenance #386, Android Development Foundation #846, Migrated Android apps CI #399 including complete API 36 runtime/transition lanes, and Protected promotion #353 all succeeded. Representative-device visual/accessibility/large-text/form-factor/performance acceptance remains open; Launcher remains Development.

## October 1, 2026 — stabilize Drawer runtime setup authority

A later exact-head feature validation reproduced a lifecycle-suite setup race that can occur before any Drawer gesture assertion: the Drawer runtime test observed terminal ROOM authority, then used the compatibility repository snapshot to decide whether its candidate app needed Home placement. The launched Home could still be finishing startup-owned reconciliation at that point, so the guarded test-owned Room write could legitimately return a non-Written result and contaminate subsequent lifecycle cases.

The test now waits for the actual rendered Drawer gesture surface, Compose idle, a ready paged-Home projection, and a ready authoritative placement snapshot before deciding whether it must add the candidate. If a setup write is required, the test verifies the exact write result and then observes authoritative placement until the candidate is present before continuing.

Exact head `3dac0fa9c39b6cd29172a7bd9eea46667beb7000` cleared build/JVM/lint/schema, Android Development Foundation, provenance, and transition-performance, but the complete API 36 run exposed a second teardown-only race in `secondaryHomeRendersMovedBuiltInWidget`. The widget render/move assertions completed; teardown then sent HOME and waited for page-indicator semantics from the scenario-owned Compose hierarchy. HOME can validly replace that LAUNCHER Activity with a fresh HOME Activity, so the old test hierarchy may disappear even though the reset succeeded. Runtime artifact `11157485154` preserves the failed evidence. The repaired test still sends the real HOME reset before deleting its temporary page, but no longer requires post-HOME page-indicator semantics from an ActivityScenario instance that Android may legitimately replace.

No Launcher production Kotlin behavior, permissions, workspace authority, persistence schema, or user-facing interaction is changed.

**Acceptance boundary:** Development test/runtime-stabilization candidate restacked on accepted main `d78ef5625ad14ef62c2405fdb697fe37aba81bec`. Fresh exact-head build and complete API 36 runtime/promotion evidence are required before integration.

## October 1, 2026 — add local App Drawer favorites

Merged PR #144 adds profile-qualified App Drawer favorites without introducing new permissions, network access, or another workspace authority. Long-pressing an application in Apps can now **Pin in Apps** or **Unpin in Apps**. Pins use the existing profile-qualified Launcher workspace key, so a Work-profile package and a same-package User app remain independent.

The App Drawer sort menu gains **Pinned first**. In that mode, pinned applications sort ahead of unpinned applications and folders, while each group retains deterministic normalized-label and stable-key ordering. The selected sort mode is now persisted as device-local Drawer presentation state, so A–Z / Z–A / Most recent / Most frequent / Pinned first survives Drawer re-entry and process restart. Pinned applications also receive a compact visual/accessibility marker, and a ★ header control can temporarily filter the current User/Work page to pinned applications only while still combining with local search. Folder membership, Home placement, Dock placement, and launch history are unchanged.

Pinned state is device-local Launcher presentation metadata stored in DataStore and intentionally remains outside the strict portable-preference v1 contract. Focused JVM coverage verifies profile-qualified persistence and pinned-first ordering.

**Acceptance boundary:** merged to authoritative `main` as `d78ef5625ad14ef62c2405fdb697fe37aba81bec` from exact head `f98fd45d7c7dae86790f24ab4c05c13e4f66eb37` after Mandatory app migration provenance #360, Android Development Foundation #820, Migrated Android apps CI #373 including complete API 36 runtime and transition-performance, and Protected promotion gate #327 all succeeded. Broader categories, tags, collections, custom ordering, portability policy, accessibility, and representative-device acceptance remain open; Launcher remains Development.


## October 1, 2026 — restore monorepo Android 16 Launcher runtime gates

A post-consolidation audit found that the migrated-app workflow still ran Launcher source guards, JVM tests, lint, schema verification, and APK assembly, but no longer executed the source-controlled Launcher Android instrumentation suite or the existing transition-performance instrumentation.

The current Development candidate restores two exact-head API 36 jobs: the complete unfiltered `connectedDebugAndroidTest` suite and the focused `LauncherTransitionPerformanceRuntimeTest` diagnostic. Both use the historically accepted immutable Android Emulator Runner revision, use KVM when available with software-acceleration fallback, upload runtime/test evidence, and are required by both the migrated-app required gate and the protected promotion gate whenever Launcher changes.

The first restoration head exposed a workflow-path defect before any Launcher performance assertion: the emulator action invoked the monorepo root Gradle project and therefore attempted to run Since instrumentation with a Launcher class filter. The repaired candidate binds both emulator commands explicitly to `$GITHUB_WORKSPACE/apps/launcher` via Gradle `--project-dir` and writes transition evidence to an absolute Launcher path. The failed head remains historical evidence and is not accepted.

The first correctly routed unfiltered runtime attempt then remained inside `connectedDebugAndroidTest` far beyond the historical KVM full-suite duration while the focused transition-performance lane succeeded on the same runner class. The candidate therefore bounds the unfiltered Gradle instrumentation command to 20 minutes and captures logcat plus activity/window dumps on timeout or test failure. Two inline diagnostic-wrapper attempts then failed before Gradle because Android Emulator Runner invokes script lines independently and retained embedded line breaks from the workflow scalar. The current candidate removes inline shell composition entirely: `apps/launcher/scripts/run_android16_runtime.sh` owns the timeout/status/diagnostic sequence, the workflow validates it with `bash -n`, and the emulator action invokes that script with one command. The outer job timeout remains 45 minutes for setup, emulator boot, artifact upload, and cleanup; the shorter inner bound is diagnostic fail-fast protection rather than a relaxation of required runtime coverage.

The first successful execution of that checked-in harness reached the complete 64-test suite and exposed a real test-process crash after four tests: `emptyHomeLongPressAlwaysOpensHomeEditor` destroyed `MainActivity` while the full-screen editor Dialog composition was still mounted, racing Compose SlotTable disposal. The current test-only repair dismisses Edit Home through its supported Done action, waits for the editor composition to leave the semantics tree, and waits for Compose idle before `ActivityScenario.close()`. No Launcher production behavior changes in this repair; fresh exact-head API 36 validation is required.

A follow-up teardown audit found the same unsafe Activity-destruction pattern in four additional editor-owned runtime paths: multi-page Edit Home, the wallpaper picker, the widget picker, and the legacy Launcher-settings gesture fallback. The current candidate now sends the standard HOME reset before closing those scenarios and requires the editor, widget picker, and wallpaper picker surfaces to leave the Compose tree before teardown. This remains test-harness stabilization only; it does not change Launcher production behavior or weaken the complete runtime suite.

Exact head `477d31f74d93fd3086787f3d2b7e654be1be070a` then completed all 64 instrumentation cases without a process crash and exposed two independent stale/racy test assumptions. `horizontalSwipeSwitchesHomePagesAndReturns` attempted a direct Room page mutation before the newly launched Home root finished startup-owned reconciliation, producing a legitimate snapshot-conflict setup result before the paging assertion ran. `LauncherStartupWizardRuntimeTest.homeHintCoversExactPlacementLivePageSwitchAndCurrentFolderAddPath` still asserted an older widget-help sentence that the current `LauncherHomeHintCard` no longer renders. Head `46a8cba415d7b06bbc0321d6f6f7bfec1faf9016` waits for the Home surface, Compose idle, and a ready paged-Room projection before the test-owned page mutation, adds the actual mutation result to any setup failure, and aligns the hint assertion with current production copy. No Launcher production source changes are included; fresh exact-head validation remains mandatory.

Exact head `0795c1f25178e5dcc217a0480d9fa8bfaf230613` then reached the real API 36 suite but stalled after 2/64 completed tests. Runtime artifact `11141061487` and its instrumentation log show `secondaryHomeRendersMovedBuiltInWidget` completed successfully, then `swipeUpOpensDrawerAndSwipeDownDismissesWithoutHeaderActions` began and never returned before the checked-in 20-minute harness timeout exited 124. That test had two Compose-injection hazards around the same Drawer path: the opening swipe was coupled to an app tile that also owns drag recognition, and the dismissal swipe remained attached to a Drawer surface that intentionally removes itself from composition as soon as the downward threshold dispatches Home. The runtime harness now applies AndroidJUnitRunner `timeout_msec=60000` per test, while retaining the 20-minute complete-suite bound, and captures a SIGQUIT thread dump plus UI hierarchy, logcat, activity, and window state on nonzero exit.

Head `00552a4d3cb61eb901775e9e84eea3f809ef0b68` completed all 64 instrumentation tests instead of hanging, proving the per-test timeout/diagnostic topology works. It produced seven failures, all inside `ActivatedHomeLifecycleRuntimeTest`; the first failure was again `swipeUpOpensDrawerAndSwipeDownDismissesWithoutHeaderActions`, which timed out at 60 seconds before its cleanup completed. Subsequent lifecycle failures are therefore not yet accepted as independent defects because the first interrupted default-HOME test can contaminate shared role/activity state. Runtime artifact `11141593689` (SHA-256 `1de8b44093dcfc137e648cdb07d2a8175a1967b2ebea9a1db367d58eb3708bf3`) preserves the complete 64-test failure evidence. Transition-performance passed on the same exact head with artifact `11142086088` (SHA-256 `92f811f283712c0088831da46d820e1d173a064af0c9af10d4b51759809c4a96`).

The current test-only repair removes the final Compose touch injector from the Drawer-opening path as well: after verifying the dedicated Home gesture surface is present, the test now injects both Drawer open and Drawer dismissal through Android-level `input swipe`, using empty right-side Home coordinates away from the seeded app tile and bottom system-gesture edge. No Launcher production behavior changes and no runtime coverage is removed or weakened; fresh exact-head validation must determine whether the later lifecycle failures were cascade effects or independent defects.

This correction also tightens the evidence boundary for PR #121: its edge-action source and JVM/build/schema evidence are merged, but the post-consolidation workflow did not execute the Launcher Android suite. The edge-action Android test source becomes current runtime evidence only after the restored lane successfully executes it.

Exact head `504ef79d5a1dfc0a4b66eb626393f3059085c350` completed all 64 API 36 instrumentation methods with two test failures while Launcher build/JVM/lint/schema and transition-performance remained green. Runtime artifact `11143368047` (SHA-256 `5cf5b3727cc514a07059773e05f101971a1597aad2f839910668ce4658aebf21`) preserved the result. `longPressDragMovesPrimaryHomeAppIntoEmptyCellAndPersists` used one Compose-local jump after the long-press hold and never produced the multi-step Android drag stream required by the Home tile recognizer; `horizontalSwipeSwitchesHomePagesAndReturns` returned from the secondary page through the old page-local surface instead of the shared unified pager. The current candidate now injects a real Android-level long-press drag with bounded motion steps and routes the return swipe through `launcher-home-unified-pager`. Runtime coverage is unchanged and no production Launcher source changes are included.

Exact head `c5690dab0649cdd04019287675d69953ad48835a` again completed all 64 API 36 methods with the same two lifecycle failures while build/JVM/lint/schema and transition-performance remained green. The Android-level drag stream used Compose-root coordinates as screen coordinates, so it did not reliably land on the target cell; the unified-pager return gesture also remained dependent on a Compose injector attached to a moving pager surface. The current test-harness correction keeps the required real multi-step drag stream but generates it through the source app node's Compose input scope, and injects the secondary-to-Primary return swipe at the Android input layer using the measured unified-pager bounds. No production Launcher behavior or runtime coverage is changed. Fresh exact-head API 36 validation is required.

**Acceptance boundary:** Development CI-governance candidate reconciled to authoritative main `b00ca6f2c44e7c7270d7e4e6d6f2097859ec8d8e`. Fresh exact-head build, full API 36 runtime, transition-performance, migrated-app required-gate, and protected-promotion evidence are required. Emulator runtime/frame timing remain diagnostic; representative physical-device/default-HOME interaction, performance, power, accessibility, form-factor, recovery, signing, and lifecycle qualification remain open.


## October 1, 2026 — restore configured Home pager edge actions

The unified Home `HorizontalPager` now observes outward horizontal gestures at its two outer boundaries without consuming pager input. A gesture that begins on the first page and moves right, or begins on the last page and moves left, dispatches the corresponding configured Launcher gesture action only after the existing 56 dp horizontal-dominance threshold.

The observer snapshots the starting page for the gesture, so a normal follow-finger transition that settles onto an outer page cannot accidentally trigger an edge action during the same swipe. It is disabled while Home app drag routing owns the pager, and interior/vertical/short gestures remain ordinary pager or vertical input.

Focused JVM coverage verifies first/right and last/left dispatch plus rejection of interior, inward, short, vertical, and single-page cases. Android 16 runtime coverage extends the existing real default-HOME multi-page flow by configuring Swipe right to Universal Search, swiping outward from Primary Home, requiring the real Search surface, and then returning HOME before continuing page/editor acceptance.

**Acceptance boundary:** PR #121 is merged as `23b3bc085ef2ae644a71bcea79667f2c3aade8f4`. Accepted source head `18abf1673529c1125e31a12ceb55a433eb505f56` passed migration provenance `36802711649`, migrated-app build/JVM/lint/schema run `36802711618`, Android Development Foundation `36802711630`, and Protected promotion gate `36802711614`. A post-merge audit established that the monorepo migrated-app workflow was not executing Launcher Android instrumentation at that time, so those runs are not Android-runtime evidence. Representative-device gesture, frame-pacing, accessibility, large-text, and form-factor acceptance remain open.


## October 1, 2026 — unify Primary and secondary Home follow-finger paging

The stable Home shell now renders the full Room-ordered Home page list through one Compose `HorizontalPager`. Primary Home and every secondary page participate in the same follow-finger motion path, while the already-shared editable Dock remains stationary below the moving content.

The pager synchronizes with the existing selected Home-page ID instead of becoming a second workspace authority. Stale IDs still fail closed to Primary Home. The former Activity-level threshold handoff is removed, secondary pages render content-only to avoid nesting their older secondary-only pager, and Primary's page-local horizontal recognizer is disabled only while the unified pager owns multi-page motion. Pager scrolling is disabled during an active Home drag so edge/drop routing retains control.

The pager keeps at most one adjacent page warm and Primary page-entry animation is held at its settled key while the outer pager moves, avoiding a second animation layered on top of follow-finger motion.

**Acceptance boundary:** PR #118 is merged after complete exact-head Development validation as `e0c7bc787f8dab5a187127ea0819a5b7d80b5d19`. Representative-device/default-HOME frame pacing, input latency, memory/power, configured edge-action acceptance, accessibility, large text, and form-factor acceptance remain open.


## October 1, 2026 — host one editable Dock across Home pages

The stable Home root now owns one full `EditableHomeDock` below page-specific content. Primary Home suppresses only its internal Dock and bottom navigation inset; secondary Home does the same while retaining its existing `HorizontalPager`, page mutations, vertical gestures, and editor/Search handoffs.

The shared Dock preserves the extracted Primary contract: layout lock, edit mode, drag geometry, Dock bounds/item bounds, local drag lifecycle, launch/manage actions, reorder/drop behavior, and configured swipe gestures. Bottom system-bar padding is owned once by the stable Home shell.

Focused JVM policy coverage verifies that external Dock hosting suppresses page-local Dock/inset ownership while the default local path remains unchanged.

**Acceptance boundary:** PR #117 is merged after exact-head Development validation as `89ff54e485220d8f408a237c2ed5e4b4da5c5098`. Representative-device frame pacing/input latency/memory/power acceptance remains open.


## October 1, 2026 — extract the full editable Home Dock for future pager hosting

The Primary Home Dock path is now factored through a reusable `EditableHomeDock` composable. It forwards the existing `GlazeDock` configuration unchanged: layout lock, edit mode, active-drag geometry, Dock bounds/item bounds, local drag lifecycle, launch/manage actions, reorder/drop behavior, and configured vertical gestures.

This is a behavior-neutral architecture foundation for the eventual shared Home pager. It deliberately preserves the editable Primary Dock contract instead of promoting the simplified read-only secondary Dock.

**Acceptance boundary:** PR #116 is merged after exact-head Development validation as `4cacb1af78bdd03c6df4a00e824983fc061ca411`. Final Primary↔secondary follow-finger paging and representative-device acceptance remain open.


## October 1, 2026 — keep Launcher Home root mounted across Primary and secondary selection

`MainActivity` now supplies the existing secondary Home renderer to `LauncherBetaRoot` as a composable slot. The root resolves the authoritative selected Home-page identity and renders either Primary Home or that secondary slot internally, so crossing the Primary boundary no longer removes and later reconstructs the entire Launcher root.

The secondary renderer itself is unchanged: Room-backed app/folder/widget mutations, cross-page movement, secondary vertical gestures, follow-finger secondary paging, Dock presentation, Search/editor handoffs, and grid-bound reporting remain under their existing authorities. A focused policy test verifies that only a known non-Primary Room page can select the secondary slot.

**Acceptance boundary:** PR #115 is merged after exact-head Development validation as `3b84f0d9023a395abf7612c4d5e6d543ab2462d3`. Primary↔secondary follow-finger motion and representative-device frame pacing remain open.


## October 1, 2026 — thread authoritative selected Home page identity into the Primary root

`MainActivity` now supplies the current selected Home-page ID to `LauncherBetaRoot` / `HomeSurface`. A fail-closed resolver accepts the identity only when it exists in the current Room-rendered page list; stale, missing, or null identities resolve to Primary Home.

Current user-visible behavior is unchanged because the Primary root is still mounted only for Primary Home. The state thread is the next prerequisite for moving Primary and secondary content under one pager without creating a second selection or workspace authority.

**Acceptance boundary:** PR #114 is merged after complete exact-head Development validation as `f770f14d7be4a0ea5b23c6a7061478ae7196efde`. Unified Primary↔secondary paging remains open.


## October 1, 2026 — keep one adjacent secondary Home page warm

The secondary Home `HorizontalPager` now requests one beyond-viewport page only when two or more secondary pages exist. This keeps an adjacent page composed around the active page to reduce swipe-edge composition work while avoiding broad offscreen page retention.

A focused JVM policy keeps the behavior bounded: zero warm pages for zero/one page and exactly one for larger secondary page sets.

**Acceptance boundary:** Development performance candidate reconciled onto merged PR #112 / current main `54b65654075667d48bc4c757357897c128f3337c`; fresh exact-head validation is required before integration. Representative-device frame pacing, input latency, memory, and power acceptance remain open.


## September 30, 2026 — separate Primary Home page content from persistent chrome

`LauncherBetaRoot` now exposes a behavior-neutral Primary Home content-only mode. When that mode is used, the Primary page leaves fixed Top/Bottom Search, page-indicator reserve space, Dock rendering, bottom navigation-bar padding, and horizontal page-gesture ownership to an outer Home shell/pager; the existing full surface remains the default for current callers.

The content-only path also clears stale in-root Dock geometry so future use inside a unified pager cannot accidentally retain an old Dock drop target. Focused JVM policy coverage locks both content-only suppression and the unchanged full-surface behavior.

This creates the Primary-side counterpart to merged PR #108's secondary content boundary and merged PR #109's secondary follow-finger pager without changing Room authority, placement semantics, Search providers, permissions, networking, or telemetry.

**Acceptance boundary:** Development architecture candidate; fresh exact-head CI is required before integration. A unified Primary↔secondary follow-finger pager and representative-device frame pacing remain open.


## September 30, 2026 — follow-finger paging between secondary Home pages

Secondary Home pages now use a single Compose `HorizontalPager` when two or more secondary pages exist. The moving layer contains page content only; the persistent Dock is rendered once outside the pager so it does not slide away with page content.

The selected secondary page identity is synchronized from the pager's current page, keeping the existing Room-backed page model authoritative. The outer Home swipe recognizer is restricted to targets at the Primary boundary, preventing it from racing the new secondary↔secondary pager while preserving the existing bounded Primary↔secondary handoff.

The single-secondary-page path remains unchanged, and app/folder/widget placement, cross-page move authority, Search behavior, permissions, networking, and telemetry are unchanged.

**Acceptance boundary:** PR #109 is merged after complete exact-head Development validation. Primary↔secondary follow-finger paging and representative-device frame pacing remain open.


## September 30, 2026 — separate secondary Home page content from persistent chrome

Secondary Home rendering now has an explicit content-only mode that suppresses the page-local indicator and persistent Dock while also leaving bottom navigation-bar padding to the future outer chrome owner. The existing full-surface behavior remains the default, so current Launcher behavior is unchanged by this foundation.

Focused policy coverage locks both modes: content-only suppresses Dock/page-indicator chrome, while the ordinary full surface retains the requested page indicator and Dock when applicable.

This creates a clean rendering boundary for the next follow-finger paging tranche without adding another workspace authority, changing Room state, or modifying app/folder/widget placement behavior.

**Acceptance boundary:** PR #108 is merged after complete exact-head Development validation. PR #109 now provides secondary↔secondary follow-finger paging; Primary↔secondary unification remains open.


## September 30, 2026 — remove redundant first-composition Home page animation

The multi-page Home path no longer starts a fresh 180–220 ms Slide/Fade/Zoom entrance animation merely because the Activity has switched between the Primary Launcher root and a newly composed secondary Home surface. A newly composed page now starts at its settled visual state.

Configured Home transition styles are preserved for a real page-key change inside an already-composed page surface. Focused JVM policy coverage locks first-composition snap, real key-change animation, and no replay when only the transition preference changes.

This is intentionally a bounded latency mitigation. It does not yet replace the Primary↔secondary whole-subtree swap with a follow-finger pager, and it does not establish representative-device frame-time, jank, input-latency, power, accessibility, or form-factor acceptance.

**Acceptance boundary:** PR #107 is merged after exact-head Development validation. Representative-device follow-finger paging, frame-time/jank/input-latency/power, accessibility, and form-factor acceptance remain open.


## September 30, 2026 — movable Universal Search Home surface

Launcher Settings now exposes four explicit Home Search presentations: **Swipe down**, **Movable**, fixed **Top**, and fixed **Bottom**. Swipe down remains the default and fail-safe behavior.

Movable Search reuses the existing first-party `goreecloud.search` 4 × 1 Room-backed widget instead of introducing a second Search or drag authority. The managed Search widget participates in ordinary Home widget placement, long-press drag, cross-page movement, and widget management. If no 4 × 1 primary-Home area is currently free, Launcher keeps the bottom Search bar available and retries managed placement after primary-Home geometry changes. Switching to a fixed or gesture-only mode removes only the Launcher-managed Search instance; manually added Search widgets remain user-managed.

Fixed Top/Bottom Search continues to use the existing Glass/Clear/Solid bar presentation. The settings surface no longer presents those fixed-bar style controls as if they changed the movable widget.

This tranche builds on merged post-consolidation stabilization: PR #103 drawer/Home interactions, PR #104 exact-profile Work folders, and PR #105 bounded confirmation of transient active-profile inventory losses.

**Acceptance boundary:** PR #106 is merged after exact-head Development validation. Representative-device movable/fixed Search placement, Home-space fallback/retry, accessibility, large text, form factors, gesture coexistence, jank, and power acceptance remain open.


## September 30, 2026 — App Drawer and Home interaction stabilization

The App Drawer now exposes four explicit presentation sorts — **A–Z**, **Z–A**, **Most recent**, and **Most frequent** — using the existing privacy-bounded local Launcher launch history for the two usage-based orders. User/Work profile pages stay precomposed across the two-tab pager to reduce the transient blank-page behavior reported on representative hardware. The Work page heading is corrected from the redundant “Work Apps apps” wording.

Drawer app tiles and list rows now retain a stationary long-press command path even while drag-to-Home/Dock is armed, so the compact app context menu and direct drag placement can coexist. Launcher Settings category rows no longer show the unwanted far-right ASCII arrow.

Home paging reacts as soon as a clear horizontal gesture crosses the existing distance/direction threshold instead of deferring the page selection until finger-up. Secondary Home pages now honor the same configurable vertical Home gestures as primary Home, including the default **swipe down → Universal Search** behavior. Missing/unknown stored Universal Search Home-mode values now also fall back to **Swipe down only**, matching the actual new-install default. HOME re-entry invalidates an open Edit Home/widget-picker generation immediately before the cleanup effect runs, reducing the extra visible editor-dismiss delay.

**Acceptance boundary:** Development candidate. Exact-head CI and representative-device drawer inventory stability, stationary-long-press versus drag arbitration, Home paging jank, secondary-page Search, and HOME-from-Edit-Home acceptance remain required.

## September 30, 2026 — polished Calendar and Weather widgets

Launcher now includes separate 2 × 2 **Calendar** and **Weather** built-ins. Calendar uses a cleaner local date hierarchy, while Weather combines local time with the existing opt-in condition presentation. Starter Glance remains compatible and receives a richer gradient treatment.

Recent successful weather is kept in memory for up to 15 minutes, so ordinary app/settings → Home navigation can reuse current conditions instead of visibly reloading them every time. Explicit Weather taps still refresh.

The supplied launcher screenshot was used as visual inspiration only; no external image or copied asset is bundled. This remains a Development candidate pending exact-head CI and representative-device visual/performance acceptance.

## September 30, 2026 — monorepo Development APK staging restored

The migrated Android workflow now stages and uploads a self-verifying GoreeCloud Launcher Development APK after Launcher validation, unit tests, lint, build, and Room-schema checks pass. CI reads the generated APK back with Android build tools, verifies the Development package identity, versionName, versionCode, and signature, and records exact source/workflow identity plus SHA-256 checksums in the artifact bundle.

Because the monorepo workflow run-number sequence restarted below the retired standalone Launcher workflow, migrated CI assigns Development versionCode `1,000,000 + workflow run number`. This preserves monotonic Android version ordering across the repository cutover instead of producing a numerically older package than previously distributed Development APKs.

The build remains a side-by-side Development artifact with CI-debug signing. It does not establish update-in-place continuity, Release Candidate status, production signing, or Stable qualification. Retained/distributed APKs and their evidence remain subject to canonical GoreeCloud/Artifacts storage and representative-device installation checks.

## September 30, 2026 — compact app context menu and HOME resume stabilization

The primary app long-press flow no longer hands off through **More options** to the large legacy placement dialog. Home, Dock, app-specific Widgets, App info, folder assignment, shortcuts, and Android's user-confirmed Uninstall handoff remain on the compact Glaze context surface. For an app opened from Home, the first quick action is now **Remove**. Persisted Home apps use the existing Room-authoritative Home-placement removal path; presentation-only Recent/Most-used suggestions instead record a bounded device-local suppression so the icon leaves Home without uninstalling the package or hiding it from Apps.

Android `MAIN` + `HOME` re-entry now dismisses the full-screen Edit Home and widget-picker overlays as part of returning to the primary Home surface. Launcher also waits for the first real application, preference, and workspace snapshots instead of briefly rendering default first-run state, keeps those core collectors active while the Activity is backgrounded, and no longer forces a complete application inventory refresh plus workspace reconciliation on every `onResume`. This targets the reported setup-wizard flash and icon/widget reload when returning Home from another application.

**Acceptance boundary:** Development candidate on PR #99. Exact-head CI plus representative-device TikTok/YouTube → Home, Edit Home → Home-button, compact context-menu, automatic-suggestion Remove, accessibility, large-text, and form-factor acceptance remain required. No Release Candidate, production, or Stable claim is made.
## September 29, 2026 — legacy Room 3.0.2 reconciliation candidate

Legacy Launcher PR #79 upgraded the Room 3 Gradle plugin, runtime, and compiler from 3.0.1 to 3.0.2, but that dependency delta was absent from the mandatory cutover tree. This candidate restores only that version change against the current Launcher source.

The change does not alter Room schema authority, migration definitions, workspace behavior, backup/restore scope, or lifecycle state. Exact-head build, unit, lint, schema, and Android runtime validation remain required before integration.


## September 29, 2026 — Home horizontal-swipe arbitration correction

The Home page gesture arbiter no longer permanently abandons a clear horizontal page swipe merely because a child Home surface consumed an earlier movement sample for press or long-press bookkeeping. Page selection still requires the existing horizontal-distance and direction-dominance thresholds, is committed only after pointer release so the Home subtree is not replaced mid-gesture, and is disabled whenever an app drag session is active so drag/drop and paging cannot compete for authority.

This directly addresses the repeatable managed-emulator timeout in the primary-to-secondary-to-primary Home swipe acceptance flow. No page ordering, Room workspace mutation, Dock placement, or drag/drop persistence contract changes.

**Acceptance boundary:** fresh exact-head Android CI, including the room-runtime-emulator page-swipe acceptance, is required before this correction is treated as validated Development evidence. Representative-device gesture/drag coexistence remains open.

## September 29, 2026 — owner-device weather, drawer-icon, and secondary-Home stabilization

Owner-device feedback is now reflected directly in the Development candidate. Glance weather uses a larger Glaze hierarchy with condition artwork for clear/partly-cloudy/cloudy weather, fog, high wind, rain, snow, and thunderstorms; the weather path remains opt-in behind Android foreground location permission, does not request background location, and does not persist coordinates. The App Drawer keeps ordinary personal/work inventories in stable row ownership while scrolling so icon artwork is not repeatedly recycled away, with a bounded virtualization path retained only for unusually large inventories. Secondary Home now exposes the same empty-space Edit Home long-press route as Primary Home, and its compact page indicator is laid out above the persistent Dock rather than sharing Dock space.

The compact App Drawer sort/New folder/Settings icon cluster and the refreshed Universal Search transitions/tab icons were explicitly accepted in the same owner-device review and are retained.

**Acceptance boundary:** Draft PR #248 remains Development-only. Exact-head Android CI, a fresh Development artifact, and representative-device retesting of weather presentation, drawer icon completeness during repeated scrolling, secondary Home editing, and page-indicator/Dock separation remain required before these owner-device defects are considered closed.


## September 29, 2026 — consistent Universal Search tab iconography

The idle Universal Search **Frequent**, **Recent**, and **New/updated** tabs now use one shared outlined icon grammar rather than three unrelated silhouettes. Each 22 dp mark uses the same circular frame, stroke weight, proportions, and optical footprint while retaining a distinct interior symbol for usage frequency, recency, and newly installed/updated apps. Existing accessibility labels remain authoritative, so the visual symbol is never the only source of meaning.

**Acceptance boundary:** Development candidate on Draft PR #248. Fresh exact-head Android CI and representative-device visual, large-text, TalkBack/Switch Access, reduced-motion, and touch-target acceptance remain open.

## September 29, 2026 — searchable Launcher Settings category home

Owner-provided Nova Launcher screenshots were used only as an information-architecture reference for making Launcher Settings faster to scan and easier to enter. GoreeCloud Launcher retains original GoreeCloud-owned source, Glaze materials, typography, line iconography, wording, and category structure; no Nova assets, branding, proprietary implementation, or exact screen composition are copied.

The Development candidate now opens Launcher Settings on a searchable category home instead of immediately presenting one very long settings page. A compact default-Home status banner is shown near the top when GoreeCloud Launcher is not the active HOME application. The overview exposes eight original GoreeCloud categories with purpose-built line icons and short summaries: **Home screen**, **App drawer**, **Folders**, **Search**, **Look & feel**, **Gestures & inputs**, **Notification badges**, and **System & setup**. Search matches both category descriptions and detailed-setting keywords such as grid, Dock, page transition, icon pack, notification access, and Universal Search.

Selecting a category reuses the existing detailed controls rather than creating a second preference authority. Home also owns Dock controls; Look & feel combines icon and appearance controls. The newer owner-feedback direction retains the compact **Settings** gear in the App Drawer header alongside the icon-first sort and new-folder actions, while **Edit Home → Settings** remains available. The first-use guidance now teaches both supported routes. This deliberately supersedes the earlier Development navigation preference that treated Edit Home as the only Settings entry point.

The Settings search field and transient controls use bounded Glaze treatment while durable settings content remains solid/near-solid, consistent with the current Glaze UI hierarchy. The Nova-style bottom Settings/Style tab bar, Nova AI/Cards/Feed concepts, and other Nova-specific product surfaces are not adopted.

**Acceptance boundary:** Development candidate on Draft PR #248. Automated exact-head build/lint/unit/runtime/performance validation and representative-device Settings search, navigation, large-text, TalkBack/Switch Access, landscape/foldable, touch-target, and visual acceptance remain required before any broader lifecycle claim.

## September 29, 2026 — compact Home page indicator

The Home page indicator no longer allocates a separate 48 dp interaction surface for every page dot. It now renders as one compact Glaze pill with restrained 5 dp dots and a 14 dp selected marker, substantially reducing the visual width and bottom-of-Home footprint while keeping the current page exposed through accessibility semantics.

Direct page switching remains available through Home horizontal swipes and the existing page-management surface; the indicator is intentionally informational rather than a row of oversized tap targets.

**Acceptance boundary:** Development candidate on Draft PR #248. Fresh exact-head Android CI and representative-device visual, TalkBack/Switch Access, large-text, landscape/foldable, gesture, and performance acceptance remain open.

## September 28, 2026 — accessible App Drawer page indicators

Paged App Drawer layouts now keep the existing restrained **6/8 dp** visual dots inside explicit **48 dp** interaction surfaces. Each page target exposes a stable test tag plus a page-position accessibility label and selected state while preserving the compact Glaze visual treatment. Focused policy coverage locks the interaction floor separately from the visual-dot geometry.

**Acceptance boundary:** this is Development source/test coverage on Draft PR #248. Exact-head Android CI plus representative-device touch accuracy, TalkBack/Switch Access traversal, large-text, landscape/foldable, and one-handed paging acceptance remain open.


## 2026-09-28 — drawer ordering and search-count candidate

- Added an A–Z/Z–A app-drawer presentation control without mutating Home positions or folder membership.
- Preserved deterministic Unicode-equivalent label ordering with explicit tests.
- Corrected drawer search result counts so matching folders are included for the personal profile.
- Kept the work on Draft PR #248; no Stable, production, or representative-device acceptance is claimed.

**Record type:** Repository change history  
**Repository:** `GoreeCloud/launcher`  
**Lifecycle:** Development  
**Migration state:** **Authoritative on `main` after PR #201 merged as `009371938ac3cab041cfb0893ede68e66e211a4f` and default-branch readback verified this record and its imported history. PR #203 reconciled the post-migration authority records, and legacy Launcher Drive roadmap/changelog retirement was subsequently verified.**  
**Runtime source baseline:** `03d4c3d2d7e355916412565b531e411d1bba71de` (PR #240). Repository-native change records were reconciled after that runtime merge through PR #241.  
**Governing standard:** Standard — Repository Feature Tracking and Changelog Governance, version 1.0, effective September 22, 2026.

## Migration control

This file is the authoritative repository-local human-readable change history for GoreeCloud Launcher. The retired historical Drive migration source was `GoreeCloud/Changelogs/Change Log — Launcher.docx`.

The legacy Drive changelog was migrated into seven linked repository-local historical segments covering the retained chronology from August 21 through September 22, 2026. The migration is a normalized evidence-preserving Markdown import, not a byte-for-byte transcription. Event dates, material implementation state, PR/commit/CI/artifact evidence, lifecycle boundaries, corrections, and material architecture/privacy/security/governance context were preserved where available. Obsolete Drive-as-canonical maintenance instructions were not carried forward as current authority.

Historical entries preserve their contemporaneous claims. Later architecture, terminology, or lifecycle state does not rewrite what an earlier entry established at its exact revision.

After PR #203 reconciled the repository-native records and authoritative `main` readback was complete, the legacy Launcher Drive changelog and roadmap files were deleted. Both former file IDs now return not found, no Launcher changelog remains in the GoreeCloud Changelogs folder, and the dedicated `Feature Roadmap/GoreeCloud Launcher` folder is empty. Git history and the repository-local records are now the durable feature/changelog recovery and authority path.

## September 28, 2026 — Compose-owned Home app drag-start correction

**Change type:** Android runtime reliability; direct-manipulation transport correction; Development candidate.

Android CI #1290 exposed a worker-thread `Handler` crash in the newly added Home app drag path and a downstream primary-Home drag persistence timeout. The primary and secondary saved-app drag sources now use Compose 1.8's transfer-data callback so Compose owns drag-start detection instead of manually calling the deprecated `startTransfer` overload from a pointer coroutine. Existing Activity/root drop targets, Room-authoritative exact placement, collision rejection, and cross-page routing remain unchanged.

**Acceptance boundary:** Fresh exact-head Android CI is required. This correction does not establish representative-device drag continuity, stationary-hold behavior, accessibility acceptance, protected signing, Release Candidate, Production, or Stable qualification.

## September 28, 2026 — held-drag live Home page switching for apps

**Change type:** Workspace direct manipulation; live cross-page handoff; Room-authoritative exact placement; Development candidate.

Saved Home applications now use Android's platform drag transport so a held drag can survive the source page leaving composition. While an unlocked saved app is held inside a valid left or right grid edge, Launcher keeps the workspace unchanged, waits for a brief dwell, and switches the visible Home page. The Activity owns that drag session across the page swap; after the destination page is measured, release over an exact cell routes through the existing snapshot-checked Room mutation. Primary same-page/Dock routing remains on its established path, and a quick edge release retains the deterministic opposite-edge landing behavior.

The destination mapping rejects spacing gaps, invalid geometry, missing pages, collisions, and out-of-bounds cells rather than silently choosing a different location. Empty secondary pages remain visually silent but now measure the same transparent destination grid, so a genuinely empty page can accept a held cross-page app drop. The drag source carries its source page identity explicitly; no Room row moves during the hover/page-switch phase.

Implementation source is carried by commits `7668a18b45541205ad2e2d9403e83bf8f6718a63`, `16fb7984c64e2056dbba2238d4218766b269291a`, `c6b4765a5ede1fef4b6964802576438014c132c9`, `fd1dd3fe00bc5aa0e7e6f5f07c7592bdaaae310c`, `a3e08e3de5c0bf20972d594550d653c1e99ea388`, and `5ae8d1792b3c08fe756216eed11941e10b103a07`; onboarding source/test follow in `2aef1d336935fa244ff7cd75d722da285375f876` and `b483187ba319f0b2826a2a2a9617a766ff476957`. CI #1283 exposed a missing Compose experimental-API opt-in and stopped before emulator execution; the correction at `a3e08e3de5c0bf20972d594550d653c1e99ea388` then passed the full validate/build/lint/JVM lane in superseded CI #1284 before its emulator jobs were cancelled by newer commits. Final exact-head Android CI remains required.

**Acceptance boundary:** This tranche implements live held-page switching for saved Home **apps only**. Folder/widget live held-page switching, representative-device continuity across page swaps, TalkBack/Switch Access, large-text/landscape/foldable behavior, physical-device performance/power, durable edit undo/recovery, protected Development signing, Release Candidate, Production, and Stable qualification remain separate gates. PR #248 remains Draft/unmerged.

## September 28, 2026 — local Month Home widget

**Change type:** First-party Home widget; local calendar presentation; privacy-bounded utility; Development candidate.

The Launcher-owned widget catalog now includes a **Month** 4 × 2 card. It renders the current calendar month, localizes weekday headings, and highlights today while reusing the existing Glaze widget material policy and Room-authoritative widget placement/movement paths. The widget deliberately does **not** request Calendar permission, read event data, contact a remote service, or imply that event/task integration is already available.

The canonical built-in catalog, widget picker preview, Home renderer, user manual, implemented-feature record, and roadmap are reconciled to the new card. Focused JVM catalog coverage verifies membership, naming/default span, and discovery through both **calendar** and **event** search terms. Source commits `607f67f2efb1b91dd5c9604084483a3fb159cc8f`, `98522364bdc0f73e143c78d04f10cd4910c8eec0`, and `b6217c520e1652a50907a994e315bda3e2a8bc04` carry the catalog, renderer, and focused test changes.

**Acceptance boundary:** This remains Development source on Draft PR #248. The final reconciled head must pass the configured Android CI matrix, and representative-device widget visual/accessibility/large-text/form-factor acceptance remains open. Event-aware calendar data, tasks, favorite contacts, media controls, widget stacks, and other richer provider-backed cards remain separate roadmap work.

## September 28, 2026 — secondary Home widgets and span-aware cross-page movement

**Change type:** Workspace direct manipulation; widget rendering/persistence; secondary Home parity; onboarding continuity; Development candidate.

Secondary Home pages now use the same persisted spatial model for apps, folders, and widgets instead of treating widgets as primary-Home-only. Multi-cell built-in and Android-hosted widgets render at their persisted Room coordinates and spans on secondary pages; their stationary long-press management keeps resize, remove, same-page move, and a non-drag **Move to another Home page** path available. Widget mutation authority is page-aware across the complete Home snapshot, preserving descriptor identity while validating target geometry, spans, collisions, ranks, and concurrent workspace state.

Held widgets can also use deterministic adjacent-page edge release from primary or secondary Home. Edge targeting is span-aware: a right-edge release lands at the destination page's left edge, a left-edge release uses the rightmost origin that still fits the widget span, and the vertical release row is clamped so the full widget remains in bounds. Occupied or invalid exact targets fail closed rather than silently selecting another position. Secondary Home folders also gain exact same-page free-cell movement through the same Room collision authority, completing the app/folder direct-placement parity there.

Android 16 runtime coverage creates a secondary Home page, adds a Launcher-owned Battery widget, moves it across pages through Room authority, navigates through the real page indicator, and verifies that the secondary widget is actually rendered. The existing primary-app drag runtime test was corrected to calculate occupied cells across complete widget spans instead of considering only each item's top-left origin; this prevents the test from selecting a visually occupied widget cell as an allegedly empty drop target. Source head `55da0a1eb59bb6d3567fd7905fa7ce43f78f37e7` passed Android CI **#1266 / run `36432814515`** across validation/build/lint/JVM/schema/APK staging, Android 16 Room/runtime, and Android 16 transition-performance.

First-run guidance and the dismissible Home hint now teach widget free-cell movement, adjacent-page edge release, and the stationary-hold page-move alternative. No new Android permission or remote dependency is introduced.

**Acceptance boundary:** This is Development source/emulator evidence, not Stable acceptance. Pointer-held hover-to-switch, arbitrary destination-cell selection after a live page switch, advanced resize ergonomics/stacks, representative physical-device widget drag/host-provider behavior, TalkBack/Switch Access, large-text/landscape/foldable behavior, physical-device performance/power, protected Development update signing, repository protection/independent review, Release Candidate, Production, and Stable qualification remain separate gates. PR #248 remains Draft/unmerged.

## September 28, 2026 — exact cross-page Home-folder edge drag

**Change type:** Workspace direct manipulation; folder placement; onboarding continuity; Development candidate.

Home folders can now use the same deterministic adjacent-page edge-drop model already established for Home apps. On primary Home, the existing long-press folder drag still moves to a free same-page cell, but a release inside a valid left/right page edge now takes precedence and transfers the folder to the adjacent Home page. Secondary Home folders gain the same cross-page edge route. The drag-only transfer carries an exact destination cell: right-edge releases target the destination page's leftmost column, left-edge releases target its rightmost column, and the vertical release position selects the destination row.

The Room-authoritative folder transaction now accepts an optional exact destination cell while retaining the existing first-free-cell behavior for non-drag **Move to another Home page** actions. Exact targets are validated against the complete target-page spatial snapshot; occupied or out-of-bounds targets fail closed without cloning the folder, changing its membership/name, or overwriting another Home item. The complete HOME snapshot remains comparison-gated for the atomic cross-page write. Android runtime coverage now verifies occupied exact-cell rejection plus exact target persistence.

Launcher Settings, first-run guidance, the dismissible Home hint, and the user manual are being reconciled to teach the new folder page-edge gesture without implying that widgets have the same cross-page behavior. Source implementation head `6592562dc72a445a12736de4717076a25d8b44ab` passed Android CI **#1228 / run `36424397300`** across validation/build/lint/JVM/schema/APK staging, Android 16 Room/runtime, and Android 16 transition-performance.

**Acceptance boundary:** Folder edge-drop is Development source behavior, not Stable acceptance. Pointer-held hover-to-switch between pages, arbitrary destination-cell selection after a live page switch, widget cross-page drag/rendering on secondary pages, representative-device drag ergonomics, TalkBack/Switch Access, large-text/landscape/foldable behavior, physical-device performance/power, protected Development update signing, repository protection/independent review, Release Candidate, Production, and Stable qualification remain separate gates. PR #248 remains Draft/unmerged.

## September 28, 2026 — accessible ordinary-Home page indicators

**Change type:** Home navigation accessibility; touch-target correction; Development candidate.

The normal Home page indicator keeps its restrained 7/9 dp visual dots, but the visible dot is no longer the interaction boundary. Each page now receives a **48 dp Glaze interaction target** with a stable test tag, the same page-context accessibility label used by the explicit Home page switcher, and an explicit selected/not-selected semantic state. This preserves the low-chrome wallpaper presentation while making direct page selection practical for touch and assistive navigation.

Android runtime coverage extends the existing primary↔secondary Home navigation scenario to require clickable page-dot semantics, verify both dot targets measure at least 48 dp in each dimension, and confirm selected state follows the active page in both directions. Exact source head `0503cc8f811e1e637732d30a31b3366b8f50f5c7` passed Android CI **#1221 / run `36418903654`** across validation/build/lint/JVM/schema/APK staging, Android 16 Room/runtime (**63/63 tests, 0 failures**), and Android 16 transition-performance. Diagnostic emulator transition metrics were Home→Apps 63 frames, median 43.07 ms, p95 134.99 ms; Apps→Home 72 frames, median 38.54 ms, p95 77.64 ms; they remain diagnostic rather than release thresholds.

Sidecar artifact `10968461828` is bound to that exact source head with ZIP SHA-256 `8da8d654e8bf5fc5fd19db42dccf6eede1165e5c81b13bbd9fab287ba55a1dc7`, **12,275,803 bytes**. Internal `SHA256SUMS` verifies `GoreeCloud-Launcher-Dev.apk` at SHA-256 `0942ccab980880755e2667c1bed773f2f977f10811640e1d464ffa3ac1aa40bb`; provenance records package `com.goreecloud.launcher.dev`, versionName `0.1.0-dev`, versionCode **1221**, CI-debug signer certificate SHA-256 `dea0747986e09be55c1ca2aa88f599e3485d0c671c46f8e9d0b870e3a0ad2ad1`, and no Stable release authority. The exact ZIP is archived at `GoreeCloud/Artifacts/GoreeCloud Launcher/0.1.0-dev/CI-1221/GoreeCloud-Launcher-Dev-CI-1221.zip` as Drive file `18hgj_NeaTH7FtBr3CMwIxjF9T4p6YCrg`; Drive raw-byte readback reproduced the exact size/digest and ZIP integrity passed.

**Acceptance boundary:** automated source/emulator accessibility evidence only. Representative-device TalkBack/Switch Access traversal, touch accuracy, large-text/landscape/foldable behavior, one-handed ergonomics, broader Home-page management, and the remaining issue #80 gates are still open. PR #248 remains Draft/unmerged.

## September 28, 2026 — owner-device Home and Search stabilization

**Change type:** Owner-device feedback; Home editing; widget direct manipulation; Search-source truthfulness; Development candidate.

Owner screenshots from the CI #1188 Development APK exposed four concrete defects: normal-Home widget movement was incomplete, the default clock/date card was fixed outside the Home grid, Edit Home required unnecessary vertical scrolling on a phone viewport, and the Google Drive source offered a connection control that could not complete in the ordinary CI build.

The active PR #248 candidate now supports deliberate long-press widget dragging from normal unlocked Home while retaining the existing Room collision and bounds checks. A new first-party **Glance** built-in combines local time/date with explicit weather-provider status and participates in the normal widget grid. Fresh starter workspaces attempt to place Glance as the default movable time/date surface; upgraded layouts are never rearranged in the background and may retain the older fixed clock/date card until the user explicitly long-presses that card while Home is unlocked to request conversion. Failed/no-space conversion leaves the existing card intact. No weather condition or temperature is invented when no provider is configured. The legacy clock surface also removes the heavy shadow path associated with the reported rectangular rendering artifact.

Edit Home is recomposed around one phone viewport: a compact header, flexible-height page carousel, and one five-action rail share the system-inset-safe space. The page preview can yield its fixed phone aspect ratio inside this editor while retaining existing preview behavior elsewhere.

The Activity now also owns an explicit system-bar presentation policy instead of inheriting an inconsistent edge-to-edge default. Status/navigation contrast enforcement is disabled on supported Android versions, wallpaper-backed Home/Apps/Search use light system icons, and light full-screen setup/Edit Home/Settings/Theme Manager surfaces switch to dark system icons while dark theme retains light icons. This directly targets the owner screenshot where Home showed a dark top band with dark status icons and Edit Home showed light icons over a light surface.

For connected Search, ordinary debug CI builds now fail closed instead of exposing the known-broken Google Drive connection switch. The Sources UI points users to **Files → Choose folder** for permission-scoped Drive folders until the separately gated signed Development connection path is available and accepted.

Focused JVM coverage checks the Glance catalog/default span, deliberate widget-drag threshold, Drive CI availability policy, and the system-bar surface policy. Android onboarding coverage teaches direct widget movement and the movable Glance model. USER-MANUAL.md is reconciled to the same behavior.

Validation chronology remains explicit. Exact head `92ecf6766e830c82e0641e28192c6a12816d531c` reached Android CI **#1208 / run `36389175109`**: validation/build/lint/JVM/schema/APK staging and transition-performance passed, but Android 16 Room/runtime finished **61/63 tests with 2 failures**. One failure was a stale widget-gallery assertion that still expected the query **weather** to have no first-party match after Glance became weather-provider-aware. The other timed out during primary-Home app drag while the candidate was also auto-promoting the legacy fixed clock into Room, exposing an unacceptable background workspace-mutation race. The correction updates the gallery expectation, removes background legacy-card conversion, makes upgrade conversion user-triggered only, and confines automatic Glance placement to the fresh starter-workspace path. CI #1208 and artifact 10955501711 remain failed/superseded evidence and are not accepted or distributed.

Corrected source head `297f92413483a352691fdcf656db200b0eeabc87` passed Android CI **#1219 / run `36392265538`** across validation/build/lint/JVM/schema/APK staging, Android 16 Room/runtime, and Android 16 transition-performance. The exact sidecar artifact is `10956034529`, ZIP SHA-256 `674cb1d9cc6394afbc813df2a3c6c435005c1527cab57d9461d6659b0b2ef654`, **12,274,374 bytes**; embedded `GoreeCloud-Launcher-Dev.apk` is SHA-256 `19a59a71d6cfad21c1a933784effb7ab1b5f29676800f63446c1b1b498b4c297`, package `com.goreecloud.launcher.dev`, versionName `0.1.0-dev`, versionCode **1219**, with CI-debug signer certificate SHA-256 `e8e25fe55c17ad698e1462c4b1baeeb57467fce841b13ca3acbc91fd5ca24f73`. Independent ZIP/readback validation passed, and the byte-identical archive is retained at `GoreeCloud/Artifacts/GoreeCloud Launcher/0.1.0-dev/CI-1219/GoreeCloud-Launcher-Dev-CI-1219.zip` as Drive file `1znpiwADpLNFpEraYTRQn3yw72t1ZF2GQ`.

**Validation boundary:** record-only reconciliation head `b90054a55dfe5f4abe693e3e10cd26411adf9bc0` subsequently passed Android CI #1220 / run `36415566330` across all configured lanes. This remains Development-only on Draft PR #248. Physical-device confirmation of widget dragging, explicit legacy-card conversion, the reported rendering/top-system-bar artifact, icon contrast against representative wallpapers and light/dark editor surfaces, no-scroll Edit Home across supported font scales/form factors, starter Glance placement, and the full Google Drive connection flow remain open under issue #80 / issue #252.

## September 28, 2026 — onboarding continuity for current Home, folder, and Search flows

**Change type:** First-use guidance; contextual hints; privacy communication; Development candidate.

The three-step startup wizard now teaches the current Launcher interactions introduced during the active stabilization cycle without adding another setup page. The final step explains exact App Drawer → Home/Dock drag placement, keeps Edit Home's Wallpaper/Widgets/Pages/Apps/Settings route visible in the guidance, describes the paged folder surface and its grid-integrated **Add apps** path, and states that connected Search remains explicitly opt-in. The dismissible Home hint is similarly refreshed so its short post-setup guidance matches the current direct-placement and folder interaction model.

This is an educational-only change. It does not enable a connected provider, request a new permission, change a default, alter workspace persistence, or broaden Search authority. Android runtime coverage now asserts the refreshed wizard sections and the current Home-hint placement/folder guidance. Source commit `aa1484c30af371216a0af29179e44980e41f2788` carries the onboarding UI update and `414e85e9a57dcffef40b26934f2b60ea605aa083` adds focused runtime assertions. `SOURCE_MANIFEST.txt` now explicitly tracks both onboarding files so this maintained source/test surface is represented in the repository inventory.

**Validation boundary:** the implementation and tests are committed to Draft PR #248, but the changed candidate requires fresh exact-head Android CI after repository-record reconciliation. Prior CI #1180 validates the earlier folder-picker source only and is not transferred to this onboarding revision. Representative-device first-use/resume, large-text scrolling, TalkBack/Switch Access, hint dismissal/replay, and visual continuity remain open under issue #80.

## September 28, 2026 — adaptive folder app-picker composition

**Change type:** Folder content management; responsive visual hierarchy; large-text ergonomics; Development candidate.

The **Add apps** flow now uses an adaptive icon-led grid instead of the prior dense full-width row list. Normal text uses an 82 dp minimum grid-cell width so phone layouts can present a spacious multi-column catalog; large and extra-large text raise that minimum to 104/116 dp to reduce label crowding and naturally lower the column count. App artwork is presented at 46 dp with centered two-line labels and explicit **+ Add** / **✓ Added** state. The picker Search field now uses rounded Glaze capsule geometry, a 48 dp minimum interaction floor, and a clear installed-app placeholder.

The change preserves the existing personal-app-only picker scope, deterministic label ordering, folder membership authority, direct add callback, already-added disablement, and 100-app folder limit. No permission, network, profile, storage, or search-provider authority changes are introduced.

Superseded head `ab22645bda47eec32686f75a0a79863be83b24d9` and Android CI #1179 / run `36381097747` remain failed audit provenance: validation stopped at Kotlin compilation because the list-specific `lazyItems` alias was mistakenly retained after converting the picker to `LazyVerticalGrid`. Exact corrective source head `118fe42693b943bcb0a84a38f27dfb23c8aa70bd` replaces that call with the grid `items` scope and passed Android CI #1180 / run `36381239898` across validation/build/lint/JVM/schema/APK staging, Android 16 Room/runtime (**61/61 tests, 0 failures**), and Android 16 transition-performance.

Diagnostic emulator measurements on the accepted exact source were Home→Apps 64 frames, median 32.79 ms, p95 133.53 ms; Apps→Home 71 frames, median 27.77 ms, p95 67.05 ms. Sidecar artifact `10952981990` is bound to that source head with ZIP SHA-256 `1c94e614d4f102c4e768111fd86cae0260a6e9cbed2079fa1193f603009748fa` and size **12,260,440 bytes**. Internal `SHA256SUMS` verifies `GoreeCloud-Launcher-Dev.apk` at SHA-256 `459cda63d1f4ceb41f72c6dc39f081aea3ec51ebca8f34d32ae56d1f9f7d5cb3`; provenance records `com.goreecloud.launcher.dev`, versionName `0.1.0-dev`, versionCode **1180**, and CI-debug signer certificate SHA-256 `73df0c83981c5be3f7d6324c6b23616ad8e78dbb519e3812f149a2701b806de7`. The exact archive is stored at `GoreeCloud/Artifacts/GoreeCloud Launcher/0.1.0-dev/CI-1180/GoreeCloud-Launcher-Dev-CI-1180.zip` as Drive file `1LqFsEIHBJ5mYfM2nu_TCr-hek36yah5a`; Drive raw-byte readback reproduced the expected size and SHA-256 and ZIP integrity passed.

**Acceptance boundary:** Development source/CI/emulator evidence only. Representative-device picker density, Add/Added state clarity, keyboard/IME interaction, large text, landscape/foldables, TalkBack/Switch Access traversal, one-handed reachability, and visual continuity with the opened-folder pager remain open, together with protected Development signing/update-in-place, repository protection/independent review, Release Candidate, Production, and Stable qualification. PR #248 remains Draft/unmerged.

## September 28, 2026 — paged floating-folder app surface

**Change type:** Folder navigation; visual organization; accessibility-aware interaction; Development candidate.

Larger opened folders now use a bounded three-row horizontal pager instead of turning the app collection into one generic vertical scroller. Page capacity follows the existing responsive 3/4-column folder geometry, swiping moves between coherent app pages, and a compact page indicator exposes the current position without competing with app labels or the folder title. Up to five pages use restrained Glaze dots; larger collections switch to an explicit current/total page label.

The grid-integrated **Add apps** tile remains the final collection item, so it naturally lands after the last app even when that requires another page. **Add apps** is also available from the folder overflow menu, keeping content management reachable while multi-select hides the grid tile. Empty folders retain their direct Add button. Folder membership persistence, launch behavior, selection/removal semantics, Room authority, and Home placement behavior are unchanged.

Focused JVM coverage locks the three-row pagination policy, Add-tile capacity behavior, and fail-closed column handling. Exact source head `4215ec7f67bfc9f5049555525255e1c19a674e50` passed Android CI #1176 / run `36380184073` across validation/build/lint/JVM/schema/APK staging, Android 16 Room/runtime (**61/61 tests, 0 failures**), and Android 16 transition-performance. Diagnostic emulator measurements were Home→Apps 67 frames, median 40.95 ms, p95 133.48 ms; Apps→Home 72 frames, median 38.03 ms, p95 66.89 ms. Sidecar artifact `10952461756` is bound to that exact source head with ZIP SHA-256 `6f62d3aee7015e81c77cc11d3b7e75b12a3457bd40d048276bac398e1f256350` and size **12,259,694 bytes**; embedded `GoreeCloud-Launcher-Dev.apk` SHA-256 is `ca7015f11b010acc127202ea5aa296c2bb67c628072dee71afe656f72308d6bb`. The staged APK identifies as `com.goreecloud.launcher.dev`, versionName `0.1.0-dev`, versionCode **1176**, using CI-debug signer certificate SHA-256 `7285a8c9b4d326df788b8e4e86f433e5c6b45167602897e9b106497b8a309f15`.

**Acceptance boundary:** Development source/CI/emulator evidence only. Representative-device page-swipe ergonomics, page-indicator clarity, Add-tile discoverability, TalkBack/Switch Access traversal, large-text/landscape/foldable behavior, wallpaper balance, measured physical-device performance/power, protected Development signing/update-in-place, repository protection/independent review, Release Candidate, Production, and Stable qualification remain separate gates. PR #248 remains Draft/unmerged.

## September 28, 2026 — grid-integrated folder Add action

**Change type:** Folder visual hierarchy; interaction simplification; Development candidate.

The refreshed floating folder now presents **Add apps** as the final tile in the normal app grid instead of consuming a separate circular control in the title row. This keeps the folder name and management overflow visually dominant, makes adding content read as part of the folder’s app collection, and more closely matches the owner-requested spacious folder composition without copying third-party assets, code, or product identity.

The Add tile preserves the existing stable semantics/test tag, accessible folder-specific description, responsive cell-height policy, rounded Glaze geometry, and direct app-picker handoff. It is hidden while multi-select is active so selection remains unambiguous. Empty folders retain the direct **Add apps** button in their empty state, so no capability is removed.

Exact source head `669e96f5cbcaf63cb71556717869e4cec9b77c54` passed Android CI #1174 / run `36377931540` across validation/build/lint/JVM/schema/APK staging, Android 16 Room/runtime (**61/61 tests, 0 failures**), and Android 16 transition-performance. Emulator diagnostics were Home→Apps 67 frames, median 26.79 ms, p95 100.17 ms; Apps→Home 76 frames, median 33.43 ms, p95 50.14 ms. Sidecar artifact `10951822359` is bound to that exact source head with ZIP SHA-256 `4db094f38a4c000a63a1d4a9b0ba9b2e91c9d6aa9f8bc7872fbee94243ad24a3` and size **12,253,542 bytes**. The staged APK identifies as `com.goreecloud.launcher.dev`, versionName `0.1.0-dev`, versionCode **1174**, using the CI-debug signer certificate SHA-256 `1c10b93948f6391110b49e830f94a6894755d93977e890a599c99e55fb495458`.

**Acceptance boundary:** Development source/CI/emulator evidence only. Representative-device folder composition, Add-tile discoverability, TalkBack/Switch Access, large-text/landscape/foldable behavior, wallpaper balance, measured physical-device performance/power, protected Development signing/update-in-place, repository protection/independent review, Release Candidate, Production, and Stable qualification remain separate gates. PR #248 remains Draft/unmerged.

## September 28, 2026 — responsive folder and widget material continuity

**Change type:** Folder responsiveness; widget material continuity; accessibility sizing; Development candidate.

The refreshed floating folder now adapts its panel width to the available canvas while preserving wallpaper context: phone-width surfaces remain at 92% with a 520 dp cap, larger compact/tablet widths can expand to 560 dp, and wide canvases cap at 600 dp. Folder app cells yield additional vertical space at large and extra-large text scales (108/118 dp versus the normal 98 dp), folder names can wrap to two lines when text scaling needs it, and Add/actions/Done/remove controls consume the V1.6 policy-resolved interaction target instead of a fixed 48 dp value.

Launcher-owned built-in widgets and the unavailable-widget fallback now consume the same GLAZE UI V1.6 presentation policy used by the Home glance, Search, quick actions, folder surface, and Dock. Normal presentation keeps the intended restrained wallpaper-glass treatment; reduced-transparency or constrained-performance policy can resolve those widget surfaces to Solid/Raised materials with matching foreground, outline, and depth rather than leaving white-on-dark glass styling disconnected from the rest of Home.

Focused JVM coverage locks folder width/cell-height behavior and the wallpaper-glass versus Solid/Raised semantic boundary. Exact source head `de34e62c19e1b92f65e6402fe703cbc5abe15015` passed Android CI #1172 / run `36376618932` across validation/build/lint/JVM/schema/APK staging, Android 16 Room/runtime (**61/61 tests, 0 failures**), and Android 16 transition-performance. Diagnostic emulator measurements were Home→Apps 69 frames, median 25.63 ms, p95 91.07 ms; Apps→Home 75 frames, median 25.16 ms, p95 50.11 ms. Sidecar artifact `10951581183` is bound to that exact source head with ZIP SHA-256 `4fa2a43cbe6e692e4c378e313c780505c22d199150073d9264a188dcb3647e40` and size **12,250,890 bytes**. The staged APK identifies as `com.goreecloud.launcher.dev`, versionName `0.1.0-dev`, versionCode **1172**, using CI-debug signer certificate SHA-256 `c8ee41ea03cd5fddf7c0eef1fca32aefc48c3ba7b033329a9082ef2b0d4222da`.

**Acceptance boundary:** Development source/CI/emulator evidence only. Representative-device folder/widget visual balance, large-text reflow, reduced-transparency behavior, TalkBack/Switch Access, landscape/foldables, third-party AppWidget behavior, measured physical-device performance/power, protected Development signing/update-in-place, repository protection/independent review, Release Candidate, Production, and Stable qualification remain separate gates. PR #248 remains Draft/unmerged.

## September 28, 2026 — adaptive Search-field accessibility presentation

**Change type:** Search accessibility; large-text reflow; visible focus; Development candidate.

Launcher-owned text Search fields now consume the shared GLAZE UI V1.6 presentation context instead of keeping one fixed 54 dp geometry. Normal text retains the existing 54 dp field, large text expands to 60 dp, extra-large text expands to 64 dp, and the resolved interaction target can raise the field further when Touch Assistance requires the existing 56 dp floor.

When the V1.6 presentation policy requires strong visible focus, a focused Search field now receives the policy focus-ring width and a high-contrast primary/white outline appropriate to its light or dark surface. Ordinary presentation keeps the existing restrained one-pixel outline. Search behavior, provider authority, query privacy, focus requests, and keyboard invocation are unchanged.

Focused JVM coverage locks the normal/large/extra-large field-height policy. Exact source head `72ee4683ca0fb1266259bc828082630038ea60ab` passed Android CI #1166 / run `36374441916` across validation/build/lint/JVM/schema/APK staging, Android 16 Room/runtime (**61/61 tests, 0 failures**), and Android 16 transition-performance. Diagnostic emulator measurements were Home→Apps 69 frames, median 26.58 ms, p95 95.99 ms; Apps→Home 75 frames, median 24.96 ms, p95 50.15 ms. Sidecar artifact `10950442994` is bound to that exact source head with ZIP SHA-256 `f4e97077b3179ceb76cffb5255a80f62eb006d75a8d13f21d8e74a843003443d` and size **12,244,681 bytes**. The staged APK identifies as `com.goreecloud.launcher.dev`, versionName `0.1.0-dev`, versionCode **1166**, using CI-debug signing.

**Acceptance boundary:** Development source/CI/emulator evidence only. Representative-device Search/IME layout, focus visibility, large-text behavior, TalkBack/Switch Access, keyboard navigation, landscape/foldables, measured physical-device performance/power, protected Development signing/update-in-place, repository protection/independent review, Release Candidate, Production, and Stable qualification remain separate gates. PR #248 remains Draft/unmerged.

## September 28, 2026 — unified Home interaction-surface presentation policy

**Change type:** Home visual continuity; accessibility-aware sizing; Glaze material fallback; Development candidate.

The persistent Home Search capsule, optional Home quick actions, and Dock now share the existing GLAZE UI V1.6 presentation-policy boundary instead of independently assuming wallpaper glass. Glass and Clear modes still retain their intended lightweight wallpaper treatment under ordinary conditions, while reduced-transparency and constrained-performance contexts can resolve those Launcher-owned surfaces to Solid or Raised materials with matching foreground, outline, and depth treatment.

Home Search now yields additional vertical space at large and extra-large text scales while retaining its existing normal-size Clear/Glass geometry. Search, quick-action, and Dock interaction floors now consume the policy-resolved target size, so Android touch-exploration context raises these controls from the Launcher 48 dp normal floor to the existing 56 dp Touch Assistance floor. The adaptive Dock preserves that floor by using horizontal overflow when needed rather than shrinking app slots below the resolved target size.

Focused JVM coverage locks Home Search/Dock material-role requests and normal/large/extra-large Search heights. Exact source head `a299dd716dd68c015f160f2fa60dbc8acd40b067` passed Android CI #1161 / run `36372586285` across validation/build/lint/JVM/schema/APK staging, Android 16 Room/runtime (**61/61 tests, 0 failures**), and Android 16 transition-performance. Diagnostic emulator measurements were Home→Apps 68 frames, median 26.92 ms, p95 100.13 ms; Apps→Home 75 frames, median 24.74 ms, p95 50.16 ms. Sidecar artifact `10949359243` is bound to that exact source head with ZIP SHA-256 `c96489fd086f33c5648a5af99480cdbd021a7adbf007f7a57ffb2715042a433f` and size **12,243,095 bytes**. The staged APK identifies as `com.goreecloud.launcher.dev`, versionName `0.1.0-dev`, versionCode **1161**, using the CI-debug signer; this remains automation evidence rather than protected Development update-continuity signing.

**Acceptance boundary:** Development source/CI/emulator evidence only. Representative-device Home/Search/Dock visual balance, large-text reflow, TalkBack/Switch Access behavior, reduced-transparency/device accessibility behavior, one-handed interaction, measured physical-device performance/power, protected Development signing/update-in-place, repository protection/independent review, Release Candidate, Production, and Stable qualification remain separate gates. PR #248 remains Draft/unmerged.

## September 28, 2026 — rounded Home glance and accessible quick-action polish

**Change type:** Home visual hierarchy; Glaze material continuity; accessibility target sizing; Development candidate.

The optional Home at-a-glance time/date block now participates in the same rounded layered Glaze composition as Launcher-owned widgets, Search, folders, and Dock instead of rendering as uncontained wallpaper text. It requests functional-glass presentation through the existing GLAZE UI V1.6 policy, retains wallpaper context with a restrained dark glass treatment in standard presentation, and resolves to more solid Material surfaces when reduced-transparency/performance policy requires that fallback. Existing compact/full sizing and left/center alignment behavior remain intact.

Optional Home quick-action pills now enforce the shared 48 dp minimum interaction target and use the same restrained outline/depth language. Their Apps/Search behavior is unchanged.

Exact source head `f501ae8f396a8bed14c478690a59044a8a3b0f9e` passed Android CI #1157 / run `36369644239` across validation/build/lint/JVM/schema/APK staging, Android 16 Room/runtime (**61/61 tests, 0 failures**), and Android 16 transition-performance. Emulator diagnostics only: Home→Apps 68 frames, median 30.47 ms, p95 116.8 ms; Apps→Home 76 frames, median 26.65 ms, p95 50.17 ms. Sidecar artifact `10948977276` is bound to that exact source head with ZIP SHA-256 `b10cd3b3ae337a8dec22e8a9f1f06a0ade1a90a372aacab164de45c905d60a09` and size **12,240,948 bytes**. Embedded `GoreeCloud-Launcher-Dev.apk` SHA-256 is `40ae06136cc51d88481f830caf304f75f968f857b448653e5afe65ce51f65095`, size **34,770,497 bytes**, package `com.goreecloud.launcher.dev`, versionName `0.1.0-dev`, versionCode **1157**, CI-debug signer certificate SHA-256 `68d51759b12156a5fce98cfa13efe5b458f483ec3111150bf82b66a19cb4db9e`.

**Acceptance boundary:** Development presentation work only. Representative-device visual balance on varied wallpapers, reduced-transparency/large-text/landscape/foldable behavior, assistive-technology acceptance, measured physical-device performance/power, protected Development signing/update-in-place, repository protection/review, Release Candidate, Production, and Stable qualification remain separate gates. PR #248 remains Draft/unmerged.

## September 28, 2026 — Home icon and label geometry normalization

**Change type:** Home visual alignment; adaptive density; folder-preview scaling; Development candidate.

The Home and Dock tile renderer now reserve one shared adaptive icon slot before labels are laid out. App artwork, Small/Medium/Large folder previews, badge marks, icon scaling, and Compact/Balanced/Airy Home density therefore share the same vertical geometry instead of allowing each item type to push its label to a different baseline. Compact cells reduce oversized artwork to the space actually available rather than painting into the next row; airy cells retain the larger configured artwork. Folder preview glyph positions and sizes scale with the rendered folder surface so Grid/Radial/Stack/Fan/Line previews remain centered when density constrains a Large folder.

Focused JVM coverage locks compact, balanced, airy, Dock, and fail-closed slot sizing. Exact source head `1a6d537e6e787b4d0ecf3c5253891af713847f35` passed Android CI #1155 / run `36367747487` across validation/build/lint/JVM/schema/APK staging, Android 16 Room/runtime (**61/61 tests, 0 failures**), and Android 16 transition-performance. Sidecar artifact `10947184814` is bound to that exact source head with GitHub archive SHA-256 `42486c2986f7c851c7dac1bc26637b8cc3a7f495d67dafd74644bedff38b39a4` and size **12,240,891 bytes**. Representative-device visual/accessibility acceptance remains required.

**Acceptance boundary:** Development presentation correction only. It does not establish representative-device Home/Dock/folder alignment, large-text/landscape/foldable acceptance, final Human Visual Excellence, protected Development signing, Release Candidate, Production, or Stable qualification. PR #248 remains Draft/unmerged.

## September 28, 2026 — Glaze folder and Home composition polish

**Change type:** Visual hierarchy; folder interaction; Home composition; accessibility-aware motion; Development candidate.

Owner-provided Samsung launcher screenshots were used strictly as interaction and visual-quality references, not copied assets or a competing design authority. PR #248 now gives opened folders a more spacious GoreeCloud Glaze presentation: one dominant rounded floating panel keeps wallpaper context visible around it, promotes the folder name and count, exposes a direct 48 dp Add control, moves selection into compact management chrome, removes the default per-app card-of-cards treatment, and adapts the app grid to four columns on sufficiently wide phone surfaces while retaining three columns for narrower or large-text layouts.

Folder entrance/exit motion is bounded and follows the existing GLAZE UI V1.6 presentation policy: Standard mode uses a short fade/scale continuity cue, Reduced mode shortens it, and Minimal mode removes scale travel. Reduced-transparency/performance policy can resolve the requested functional-glass material to a more solid presentation without changing folder semantics. Existing rename, selection/removal, Home placement, page movement, ordering view, delete confirmation, app launch, and add-app flows remain available.

The Home composition also receives a bounded material-harmony pass: Launcher-owned built-in widget containers use the larger rounded geometry and restrained depth used by the refreshed folder surface, while Glass/Edge Dock treatments receive slightly stronger layered separation, larger optical curvature, and more internal breathing room without changing the 48 dp interaction floor, adaptive capacity, horizontal overflow, exact insertion/reorder behavior, or layout authority.

Focused JVM coverage locks the adaptive folder-column policy. Exact source head `c097bb3f326d3a7cef4effc541c46fa0a3ab39f7` passed Android CI #1153 / run `36366150025` across validation/build/lint/JVM/schema/APK staging, Android 16 Room/runtime (**61/61 tests, 0 failures**), and Android 16 transition-performance. Sidecar artifact `10946833909` is bound to that exact source head with GitHub archive SHA-256 `e12179802899902a960f0d52f47e6e28d6610a1529833adcaf106009cf84a800` and size **12,240,025 bytes**. Representative-device visual/accessibility acceptance remains required.

**Acceptance boundary:** This is Development presentation work. It does not claim copied Samsung/One UI source or assets, true backdrop blur on every device, rendered Glaze conformance, representative-device folder/Dock/widget acceptance, pointer-held cross-page hover switching, folder/widget cross-page drag, protected Development update signing, Release Candidate, Production, or Stable qualification. PR #248 remains Draft/unmerged.

## September 27, 2026 — preserve exact spatial placement on secondary Home pages

**Change type:** Workspace spatial correctness; direct manipulation; Room-authoritative placement; Development candidate.

Secondary Home pages already persisted application coordinates in Room, but their rendered surface compacted visible apps and folders into sequential grid positions. Empty cells therefore collapsed on screen even when the authoritative workspace retained those gaps. PR #248 now renders secondary pages from their saved Home-grid coordinates, preserving empty spatial slots and keeping the visual page consistent with Room placement.

Held app drags on a secondary page can now resolve an exact measured same-page destination cell after the long-press threshold. The new secondary exact-cell mover validates the configured Home grid, rejects the protected primary page, rejects collisions and out-of-bounds coordinates, and commits through the existing Room snapshot-checked mutation path. Adjacent-page edge release still takes precedence at a valid page edge, so the established deterministic cross-page landing-cell behavior remains unchanged. Lazy-grid cell hit targets are removed when their cells leave composition so a scrolled-off cell cannot remain as a stale drag destination.

Focused JVM coverage verifies spatial slot mapping and invalid coordinates. Android runtime coverage verifies exact same-page Room movement, collision rejection, bounds rejection, primary-page protection, and post-mutation workspace health. Source head `50f5beaa6ec4c6b3eae7fed8a5ab0c6a0031d77c` passed Android CI #1150 / run `36362962895`: validation/build/lint/JVM/schema/APK staging, Android 16 Room/runtime (**61/61 tests, 0 failures**), and Android 16 transition-performance all succeeded.

**Acceptance boundary:** This closes the secondary-page render/persistence mismatch and exact same-page app-cell drag in Development source. It does not implement pointer-held hover-to-switch between pages, arbitrary destination-cell selection after a live page switch, folder/widget cross-page drag, representative-device drag/accessibility/form-factor acceptance, protected Development update signing, repository protection/review, or Release Candidate/Production/Stable qualification. PR #248 remains Draft/unmerged.

## September 27, 2026 — Edit Home direct page creation and crash-safe preview navigation

**Change type:** Home-page editing; representative-device defect correction; Room-backed page creation; Development candidate.

Representative-device testing of CI #1139 exposed two Edit Home gaps: creating a page still required the separate **Manage** sheet, and swiping from primary Home to a secondary preview caused the editor to disappear.

The dismissal path was caused by preview swipes mutating the Activity-level selected Home page. That replaced the primary `LauncherBetaRoot` composition with the secondary-page surface and disposed the full-screen editor. Edit Home swipe position is now local to the editor, so browsing previews no longer switches the underlying Home composition.

Edit Home now also appends a dedicated **+ Add Page** card to the preview carousel. It creates a blank page through the existing Room-authoritative mutation without leaving Edit Home; the new page then occupies the former add-card slot and the add card moves to the end. Layout lock disables direct creation, while **Manage** remains available for advanced page management.

Android runtime coverage swipes the Edit Home carousel, verifies the editor remains displayed, creates a page from **+ Add Page**, verifies the Room-backed preview appears, and confirms the bottom editor actions remain mounted. Android CI #1141 / run `36358582943` completed with validate and transition-performance successful but the Room/runtime job failed because the expression-bodied regression inferred a non-`Unit` JUnit return type and the runner rejected that test class; a follow-up test-only commit made the regression explicitly terminate in `Unit` without changing Launcher production behavior. Android CI #1142 / run `36359121152` then passed validate, Android 16 Room/runtime (60/60), and transition-performance on source head `43b49407527132e97ea687951525de52cabe2f82`.

**Acceptance boundary:** Representative-device retesting of multi-page swiping, repeated direct page creation, layout lock, large text, landscape/foldables, TalkBack/Switch Access, and longer editing sessions remains required. PR #248 remains Draft/unmerged.

## September 27, 2026 — direct adjacent-page Home app edge drop

**Change type:** Workspace direct manipulation; cross-page movement; accessible fallback preservation; Development candidate.

PR #248 Development source now adds a direct drag gesture for moving Home applications to an adjacent Home page without opening the app-management dialog. The established primary-Home spatial drag engine remains intact: same-page cell placement and Home↔Dock routing still use their existing geometry, while a Home app released inside the outer 36 dp of a valid left/right page edge routes through the existing Room-authoritative `moveHomeAppToPage` transaction. Secondary Home app tiles provide the same held-drag edge transfer; a stationary hold continues to open the existing management controls. The drag-only route now carries deterministic edge landing-cell intent: a right-edge release targets the destination page's leftmost column, a left-edge release targets its rightmost column, and the vertical release position selects the destination row.

The shared edge policy fails closed unless primary Home is first, page ranks are canonical, the current page exists, the measured grid geometry is valid, and an adjacent destination exists. The authoritative Room mover accepts the requested cell only when the configured Home grid contains it and collision validation succeeds; occupied or out-of-bounds cells are rejected rather than silently falling back to a different placement. Ordinary non-drag **Move to another Home page** continues using the first available valid cell. Focused JVM coverage verifies page targeting plus deterministic edge-column/row mapping and invalid geometry, while Android runtime coverage exercises requested-cell persistence and collision rejection across the primary boundary.

The non-drag page-management surface also distinguishes the protected rank-zero destination as **Primary Home** instead of presenting it as an ordinary numbered page. The label keeps the existing app/other-item context and applies only to move-target controls; ordinary page switcher numbering remains unchanged.

**Acceptance boundary:** This remains a bounded cross-page drag tranche rather than the final live-page handoff model. Exact-head CI and representative-device acceptance remain required. Hover-to-switch while the pointer is still held, arbitrary destination-cell selection after a live page switch, folder/widget cross-page drag, and broader multi-step edit recovery remain open under issue #80.

## September 27, 2026 — reversible bidirectional Home app page movement

**Change type:** Workspace editing; page organization; accessible non-drag movement; recovery-safe navigation.

PR #248 now makes the existing non-drag **Move to another Home page** action reversible across the primary/secondary page boundary in both directions. Apps managed from a secondary Home page can target protected primary Home as well as other secondary pages, and the primary Home detailed app controls now expose every secondary page as a destination. Both routes pass the current configured Home grid dimensions through the existing Room-authoritative primary-boundary mover so the protected primary spatial grid is validated before the transaction is accepted.

The current page is excluded from its own destination list, layout lock remains authoritative, successful moves switch to the destination page, and the existing snapshot/readback protections remain in force. Focused JVM coverage verifies both target-list directions, while the Android runtime suite already verifies a primary→secondary→primary round trip with canonical ranks, spatial coordinates, and Room readback.

Validation chronology remains fail-closed: Android CI #1132 rejected the first UI-wiring head at Kotlin compilation because the new page-label call was missing the existing `WorkspaceRenderedHomePage.context` extension import. The import was corrected without changing the movement policy or weakening validation; that failed head remains diagnostic evidence only.

**Acceptance boundary:** This closes the missing bidirectional non-drag app page-management route in Development source. The newer adjacent-page edge-drop tranche adds a direct gesture path, while hover-to-switch/exact destination-cell cross-page drag, representative-device page-move interaction/accessibility acceptance, populated-page recovery semantics, and broader issue #80 release gates remain open.

## September 27, 2026 — Development update-continuity build foundation

**Change type:** Development packaging; versioning; signing boundary; CI provenance.

PR #248 source now removes the fixed Development `versionCode = 1` assumption. The Android build accepts an explicitly supplied positive Development version code and falls back to the GitHub Actions run number in CI, so newly built Development artifacts can progress monotonically rather than reusing one package version code.

The build also accepts a complete externally supplied Development signing configuration and fails closed if only part of that configuration is present. Keystore material and signing passwords remain outside source control. Ordinary pull-request CI intentionally continues to use the CI debug signing identity; it does not receive or claim a protected persistent Development key. CI provenance now records the effective version code and signer-certificate SHA-256 digest for the staged APK.

The artifact-staging gate now also reads the assembled APK back through Android build tools and requires its package ID, versionName, and versionCode to match the expected Development identity before upload. Those read-back values are written into BUILD-PROVENANCE alongside the signer-certificate digest, so a mispackaged or stale-version APK fails before it can become a CI sidecar artifact.

**Acceptance boundary:** This is build/distribution plumbing, not protected signing acceptance. The exact persistent Development signing identity has not been provisioned, a trusted key-bearing distribution workflow is not active, and update-in-place has not been verified on a representative device. The current installed CI/debug-signed Development package cannot be assumed compatible with a future protected Development key. Repository protection under issue #250 and human security review of the signing trust boundary remain prerequisites before protected signing credentials are exposed to automation.

## September 27, 2026 — owner-device unified Search, Home-page carousel, and stable drawer drop correction

**Change type:** Representative-device Search semantics; connected-provider truthfulness; Home-page editing; drag/drop visual stability; Development candidate.

Representative-device testing of the CI #1070 Development build confirmed the adaptive Dock can visibly hold more than the five starter applications, but exposed three additional interaction gaps: Google Drive enablement was still presented as a selected Android folder rather than an online account connection, Universal Search reduced enabled online providers to a separate **No local matches / Search with** fallback instead of one unified result surface, and the Home-page editor still looked like a settings list rather than a centered swipeable overview. Drawer→Home placement also showed a brief wrong-cell frame before settling at the requested drop target.

PR #248 Development source now:

- removes the Google Drive toggle's Android folder-picker coupling and stops registering selected Drive DocumentsProvider/SAF trees as Google Drive account Search; folder-scoped search remains the separate **Files** capability;
- adds a Development Google Drive account adapter using Google Identity authorization with the least-privilege `drive.metadata.readonly` scope; authorized Drive queries run through bounded HTTPS Drive API v3 metadata search, access tokens stay process-local, and a rejected/expired authorization returns the source to a reconnectable fail-closed state;
- renders enabled explicit online destinations as full **Search online** rows inside the same Universal Search result panel instead of the old compact provider-button strip and avoids the misleading **No local matches** provider-only state;
- keeps Brave as an explicit browser handoff until the official Autosuggest service has a governed credential path, while preserving the opt-in remote-inline contract required for future live completions rather than synthesizing suggestions;
- replaces both Edit Home's page overview and the dedicated Home-page manager with centered horizontal carousels whose selected saved-layout preview is prominent and whose neighboring page edges remain visible; direct selection, swipe navigation, compact add, guarded reorder, and confirmation-gated empty-page deletion remain available according to the authoritative Room policy; and
- changes Drawer→primary-Home insertion so the requested cell is validated and written in the first authoritative Room transaction. The old two-commit path (first free cell, then target cell) is no longer used for a new drawer copy, removing the persistence-level cause of the observed intermediate wrong-cell frame. Android runtime coverage verifies direct persistence at the requested coordinate.

**Lifecycle boundary:** Development only. Source/build/runtime validation is revision-bound, and representative-device retesting is still required for the new carousel behavior, exact drag visual stability, large-text/landscape/foldable/accessibility behavior, final connected-provider presentation, Google account authorization, Drive API result/open behavior, OAuth-client/signing configuration, and authorization recovery after restart/expiry. Live Brave Autosuggest remains open under issue #252 because Brave requires a confidential subscription token that must not be embedded in the APK. PR #248 remains Draft/unmerged; protected Development signing continuity, Release Candidate, production, and Stable acceptance are not established.

## September 27, 2026 — owner-device direct App Drawer drag and adaptive Dock correction

**Change type:** Direct manipulation; Dock capacity; Glaze UI mobile ergonomics; Development implementation.

Representative-device feedback showed two concrete failures in the current Development build: App Drawer long-press exposed contextual buttons instead of letting the held app continue directly into exact Home/Dock placement, and adding another app to the five-item Dock produced **Dock is full.**

The Development correction retains five apps only as the starter default and removes that number as a product capacity ceiling. DataStore/Room compatibility paths inherit the practical-unbounded Dock policy; Home→Dock and App Drawer→Dock placement can grow beyond five while exact insertion and Dock reorder remain intact. The rendered Dock compresses item slots only down to the Glaze 48 dp interaction floor and then becomes horizontally scrollable rather than rejecting another app.

App Drawer grid and list interactions now arm drag on long-press but defer the cross-surface transfer until the held pointer actually moves. Moving after the hold enters the existing Drawer→Home/Dock copy route; a stationary hold retains contextual actions.

**Lifecycle boundary:** Development only. Exact-head automated validation and representative-device retest remain required for stationary context actions, hold-then-drag transition, exact Home/Dock placement, Dock growth beyond five, horizontal overflow, Dock reorder, large text/landscape, assistive technology, and touch ergonomics. PR #248 stays Draft/unmerged; this does not establish production or Stable acceptance.

## Imported historical chronology

The migrated historical record is stored in these repository-local segments:

1. [August 21–22, 2026 — project establishment through Room mirror verification](docs/changelog-history/2026-08-21-to-2026-08-22-a.md)
2. [August 22, 2026 — Room runtime, authority, reconciliation, and promotion rehearsal](docs/changelog-history/2026-08-22-b.md)
3. [August 22–24, 2026 — production authority activation through grid-placement foundation](docs/changelog-history/2026-08-22-to-2026-08-24.md)
4. [August 24–31, 2026 — Glaze adoption, multi-page workspace, placement, and page-navigation work](docs/changelog-history/2026-08-24-to-2026-08-31.md)
5. [August 31–September 2, 2026 — primary Home protections, beta shell, branding, layout lock, and Theme Manager](docs/changelog-history/2026-08-31-to-2026-09-02-a.md)
6. [September 2–9, 2026 — accessibility, portability, HOME stabilization, privacy, and local-first Search](docs/changelog-history/2026-09-02-to-2026-09-09.md)
7. [September 16–22, 2026 — Glaze/Platform stabilization through Universal Search presentation structure](docs/changelog-history/2026-09-16-to-2026-09-22.md)

The source parser identified 71 meaningful dated or titled historical sections/entries in the legacy changelog material. Those sections were accounted for through the seven normalized segments, including historical roadmap-synchronization events as provenance rather than current governance. PR #198 and later source/governance changes that extend the imported retained chronology are recorded directly below.

## September 27, 2026 — PR #248 integrates long-press page overview and clean normal Home pages

**Change type:** Representative-device Home UX refinement; long-press editing; multi-page overview; clean empty pages; guarded page deletion; Development candidate.

Fresh owner-device feedback confirmed that Home page swiping and the page manager existed but normal secondary pages still carried launcher-management chrome, empty pages displayed an unnecessary empty-state card, and long-press Edit Home showed only a single large preview instead of the requested page overview while retaining the Launcher action controls.

Runtime source through `93d88de6e4fde81bf374d24e1086dde9e41f29a9` now:

- removes the persistent numbered/top `HomePageSwitcher` from ordinary Home pages; normal Home page position is communicated by horizontal swipe state and the bottom page dots only;
- renders empty/new secondary Home pages as clean wallpaper space with no **This Home page is empty** card and no numbered page label;
- keeps empty mini-previews visually quiet instead of stamping them with an **Empty** label;
- replaces the old single-page Edit Home preview with a horizontally browsable Glaze Home-page overview inside the same long-press sheet;
- pins Wallpaper, Widgets, Settings, Apps, and Folders in a persistent bottom editing toolbar so page browsing does not push Launcher controls off-screen;
- makes each page preview selectable as a direct page switcher; choosing a different preview returns to that Room-backed Home page;
- shows saved Room-backed page geometry through GoreeCloud-owned spatial thumbnails and the shared Dock preview without copying third-party launcher artwork;
- keeps full switching/reorder/add management behind the dedicated **Manage / Add or manage** affordances while exposing confirmation-gated **Delete** directly on eligible empty secondary-page overview cards;
- reuses the authoritative `canDeleteHomePage` and transactional `deleteEmptyHomePage` boundaries, so primary Home, the last page, non-empty pages, layout-locked state, and invalid primary ordering remain fail-closed; and
- extends Android 16 runtime coverage to require clean empty secondary Home rendering, absence of numbered page chrome, presence of the long-press page overview, pinned Edit Home actions, the direct guarded delete affordance for a real empty secondary page, and preview-driven switching back to that secondary page.

The earlier PR #248 representative-device corrections remain in this candidate: ordinary left/right Home page swiping, dedicated page-dot clearance, active-profile icon warming plus transient OEM icon retry, Android Restricted Settings badge-recovery guidance, and the compact grouped Search Sources presentation.

**Lifecycle boundary:** Development only. Fresh exact-head CI is required after this repository-record reconciliation. Representative-device acceptance of the integrated page overview, direct empty-page deletion, large-text/landscape/foldable composition, secondary-page long-press parity, and final visual quality remains open under issue #80. PR #248 remains Draft/unmerged; protected signing/distribution, Release Candidate, production, and Stable acceptance are not established.

## September 27, 2026 — PR #248 follows up on live Home paging, icon, badge, and Search Sources feedback

**Change type:** Representative-device regression correction; Home paging; indicator layout; app-icon reliability; notification-access recovery; Universal Search source presentation; Development candidate.

Fresh owner-device testing of the PR #248 side-by-side Development build showed that the multi-page model and page manager were present but ordinary Home left/right swipes did not change pages, page dots could overlap the final app row, some drawer artwork could still fail intermittently, Android greyed out notification-listener access behind **Controlled by Restricted Setting**, and Search Sources remained too visually heavy.

Runtime source through `f04f1b21707c94a0b5233e30f2c7bf62f45dd49f` now:

- routes primary Home horizontal gestures to adjacent Room-backed pages before configured left/right fallback actions and retains the shared horizontal page-swipe handler for secondary pages;
- adds JVM gesture-policy coverage and Android 16 runtime coverage that creates a real secondary page, swipes left to it, then swipes right back to primary Home;
- reserves a dedicated page-indicator strip on primary Home and additional bottom grid inset on secondary pages so app icons and labels do not share the page-dot overlay region;
- explicitly warms the current drawer profile through the shared profile-aware icon cache and retries transient OEM icon-resource failures with bounded backoff while retaining stale artwork and the existing Android-owned fallback chain;
- replaces passive notification-badge warning text with a direct two-step recovery surface: **App info** for the user-controlled **Allow restricted settings** action, then Android **Notification access**, plus **Check again**. Launcher does not and cannot bypass the Android Restricted Settings security gate;
- rebuilds Search Sources into compact Glaze section surfaces with dense rows, inline state, connected-provider glyph/artwork, progressive row disclosure, and explicit file-folder actions instead of repeated tall source cards and always-visible Info controls; and
- preserves the existing privacy model: local sources remain local, connected sources remain explicit opt-in, Google Drive inline access remains bounded to user-selected SAF roots, and Brave/Dropbox remain handoff-only until their governed authorization adapters exist.

Exact runtime head `f04f1b21707c94a0b5233e30f2c7bf62f45dd49f` passed Android CI #1048 / run `36299461392`: validate/build/lint/JVM/Debug APK, Android 16 Room/runtime, and Android 16 transition-performance all succeeded. The runtime lane includes the new real Home paging gesture regression.

**Lifecycle boundary:** Development only. The owner must still retest left/right paging, page-dot clearance, drawer icon completeness, the manual Restricted Settings → notification-access path, and the redesigned Search Sources surface on the representative device. PR #248 remains Draft/unmerged; protected signing/distribution, Release Candidate, production, and Stable acceptance are not established.

## September 27, 2026 — PR #248 revamps Edit Home with visual Glaze page management

**Change type:** Home editing; multi-page workspace; Glaze UI presentation; guarded page deletion; Development candidate.

Additional owner-supplied launcher references emphasized a visual page canvas, direct page switching, compact editing actions, and easy page deletion. PR #248 uses those references only for interaction/layout study and implements the resulting behavior with GoreeCloud-owned Glaze UI and the existing Room-authoritative workspace model.

Runtime source through `72469ef6708772a9a28bc7cc6cfa3345c52f3df9` now:

- reframes **Edit Home** as a scrollable Glaze composition workspace with a live Home preview, concise editing description, and a dedicated **Home pages** management card;
- reorganizes the remaining edit commands around Wallpaper, Widgets, Settings, Apps, and Folders so page management is no longer buried among generic actions;
- opens a Glaze bottom-sheet Home-page manager with horizontally browsable visual page cards, selected/current state, primary-Home indication, page item counts, and direct preview-based page switching;
- renders lightweight GoreeCloud-owned spatial thumbnails for saved app, widget, and folder placements without copying third-party launcher artwork;
- exposes guarded earlier/later ordering for eligible secondary pages and a dedicated **Add Home page** affordance;
- exposes **Delete empty page** directly on eligible secondary-page cards, followed by explicit confirmation;
- keeps rank-zero primary Home, the last Home page, non-empty pages, locked layouts, and invalid primary-page ordering fail-closed, matching the authoritative Room mutation layer;
- preserves transactional deletion safety: `deleteEmptyHomePage` rechecks the stored page and item snapshot inside Room before any page is removed, preventing page deletion from cascading user content after a concurrent insertion;
- adds focused JVM policy coverage for empty-secondary deletion plus primary, non-empty, layout-locked, single-page, and invalid-order protections; and
- updates drawer lifecycle runtime tests to target the stable `launcher-app-drawer` surface tag after the drawer header was visually simplified, rather than depending on the removed **User Apps** heading.

The redesigned Edit Home sheet is vertically scrollable so its preview, page manager entry, and edit commands remain reachable on shorter screens and under larger text settings.

No new Android permission, network authority, telemetry, workspace authority, or destructive database primitive was added. Populated-page deletion remains intentionally open work until explicit item-move/recovery semantics are implemented and accepted.

**Validation boundary:** this entry records implemented Development source. Fresh exact-head Android CI is required after the documentation reconciliation, and representative-device page switching, add/delete/reorder behavior, large-text, landscape/foldable, touch-target, visual-quality, recovery, protected signing/distribution, Release Candidate, production, and Stable acceptance remain open under issue #80.

## September 27, 2026 — PR #248 stabilizes empty Universal Search dismissal and Back gesture recovery

**Change type:** Representative-device regression correction; Universal Search dismissal; IME/focus lifecycle; gesture responsiveness; Development candidate.

Owner testing of the CI #1006 side-by-side Development build found that an empty Universal Search surface did not dismiss when unused space was tapped and that leaving Search with Back could stall the Launcher and delay subsequent Home gestures.

PR #248 now:

- makes the unused region below an empty Universal Search field an explicit accessible dismissal target, so tapping/clicking the empty Search surface returns directly to Home;
- routes empty-space dismissal and Search Back through the same lightweight path: clear Compose focus, hide the software keyboard, then switch the Launcher root surface back to Home;
- avoids carrying a still-focused Search text field through `AnimatedContent` disposal while the IME is leaving, preventing the stale focus/IME ownership pattern associated with the reported post-Back gesture delay;
- adds Android runtime coverage that opens empty Universal Search with the IME active, injects a touchscreen tap into the empty dismissal region, and requires Home to become active;
- adds a Back regression that exercises the Activity/Compose Back dispatcher, requires Search → Home completion, then immediately requires Home → Apps → Home gestures to remain responsive; and
- preserves all Search provider, query-retention, permission, network, and privacy boundaries.

Runtime checkpoint `3efe99b92ae80e107fabeab8127bb799fe3b79cb` passed Android CI #1010 / run `36293591765`: validate/build/lint/JVM/Debug APK, the Android 16 connected runtime suite, and the Android 16 transition-performance lane all succeeded with the new regression tests. Historical CI #1009 remains failed because its first version of the Back test used low-level emulator key injection that Android rejected before Launcher received the event; that harness defect was corrected rather than reclassified.

**Lifecycle boundary:** This is Development evidence. Representative-device retest of the exact empty-space dismissal and Back/gesture behavior is still required under issue #80, along with the previously open physical-device, accessibility, profile, recovery, protected signing/distribution, Release Candidate, production, and Stable gates.

## September 26, 2026 — PR #248 adopts richer Launcher interaction references for context menus, widgets, wallpaper preview, and Home editing

**Change type:** Representative-device UX refinement; icon context commands; application shortcuts; widgets; wallpaper preview; Home editor; Development candidate.

Additional owner-supplied launcher references emphasized compact long-press command surfaces, app-specific shortcut rows, direct widget access, visual widget galleries, Home-composition wallpaper previews, and a dedicated Home editing toolbar. PR #248 applies those interaction lessons through GoreeCloud-owned Glaze UI without copying third-party visual assets.

The runtime source through `8c350c9b55f41c75d543e22284575fe3e394730a` now:

- turns the anchored app long-press popup into a compact Glaze command surface with Home, Dock, Widgets, and App-info quick actions;
- queries Android manifest/dynamic/pinned shortcuts only when Launcher has shortcut-host authority and exposes up to four app-specific shortcut actions using the existing profile-aware `LauncherApps.startShortcut` launch path;
- shows only the already-governed content-free per-app notification count in the context header when badge access is both enabled and granted; notification titles, senders, messages, images, and history are not added;
- adds an app-specific widget entry point. Multiple widgets use a Launcher-owned chooser; widget actions are deliberately unavailable for non-primary-profile apps until widget-provider profile identity is explicit;
- improves installed-widget rows with provider application artwork when Android exposes it, a Glaze widget glyph instead of an initials placeholder, clearer minimum-size information, and a direct Add affordance;
- normalizes Edit Home actions into a balanced 3 × 2 command layout for Wallpaper, Widgets, Pages, Apps, Folders, and Settings; and
- expands the built-in wallpaper preview into a fuller Home composition with Glaze widget cards, apps, page indicator, Universal Search treatment, and Dock before the existing explicit Apply action.

No new Android permission, network authority, provider credential, telemetry, notification-content retention, or automatic wallpaper mutation was introduced. Android/system-provider authorization boundaries remain unchanged.

Android CI #1003 / run `36290677647` was started for the exact runtime head above. This entry does not claim that run succeeded until live workflow evidence says so. Representative-device visual/touch/accessibility, shortcut availability, widget-provider/profile behavior, wallpaper rendering, and Home-editor acceptance remain open under issue #80.

## September 26, 2026 — PR #248 follows up on live Dev feedback for icons, badges, and connected Search

**Change type:** Representative-device stabilization; app-icon reliability; notification access UX; Universal Search Glaze refinement; bounded connected-source inline Search; Development candidate.

Owner testing of the current side-by-side Development APK confirmed that the new onboarding and progressive Search direction was working while exposing five remaining issues: app icons could still disappear transiently in the drawer; notification counts/dots were inaccessible on the sideloaded Dev build because Android reported **Controlled by Restricted Setting**; the Search Sources manager remained too dense; Google Drive/Brave/other connected providers still did not produce inline results; and Brave fell back to a **BS** initials placeholder when installed provider artwork was unavailable.

The PR #248 correction pass now:

- prevents an icon invalidation race from replacing already-rendered app artwork with a transient null; a stale in-flight decode is rejected, the icon path retries against the newest package/profile cache generation, and already-visible artwork remains until a fresh decode succeeds;
- keeps the existing Android-owned five-stage icon fallback, stale-while-revalidate cache, profile rebadging, single-flight loading, and bounded memory behavior;
- explains Android's Restricted Settings gate inside Launcher notification-badge settings, provides an App-info handoff for **Allow restricted settings**, and then a separate notification-listener access handoff; Launcher does not attempt to bypass Android's decision;
- restructures Universal Search Sources into **On-device**, **Your content**, and **Connected** groups with compact state summaries and progressive per-source Details, preserving explicit enablement, ordering, permissions, and privacy boundaries while reducing always-visible explanatory text;
- replaces letter-initial connected-provider fallbacks with local Glaze semantic glyphs when installed provider artwork is unavailable, so Brave no longer renders as **BS**;
- implements a bounded opt-in Google Drive inline adapter over Drive folders explicitly selected through Android's Storage Access Framework. Launcher accepts only a tree whose DocumentsProvider resolves to Google Drive, searches bounded filename/MIME metadata from that granted tree, normalizes the matches as attributed connected-source results, and retains the explicit Drive **Search with** handoff as a fallback;
- matches the typed Drive query inside Launcher rather than transmitting it through a new Launcher network client; Launcher does not receive the Google account credential and still declares no `INTERNET` permission;
- releases a persisted URI grant if the user attempted to enable Drive inline Search but selected a non-Drive document tree; and
- keeps Brave Search and Dropbox handoff-only because their true inline paths still require governed provider authorization: Brave Autosuggest needs an approved subscription-token/proxy or user-owned credential mechanism, and Dropbox needs reviewed OAuth. No embedded secret, authenticated scraping, cookie/session reuse, or weakened privacy guard was introduced.

Runtime checkpoint `55ec855cb7e321b29f375e95b4a7880322a330d3` passed Android CI #995 / `36288344233` across repository/privacy/identity/Glaze/Room guards, lint, JVM tests, Debug APK assembly, Android 16 runtime, and Android 16 Home/Apps transition performance. Development artifact `10921850750` has archive digest `sha256:35f292d817be29d1ea85bd726c6d00a699315adfe43e59a8eea51cd31561c060`; the contained APK SHA-256 is `9551b6a343d0bde67ec95e1024abb24f22782918e0b2cc481fa1c39504695888`.

**Lifecycle boundary:** The implementation checkpoint is green but representative-device acceptance remains open. The owner still needs to retest random drawer icon completeness, the revised Search Sources composition, Drive inline behavior, and the Android Restricted Settings → notification access flow. Brave/Dropbox/Gmail and broader account-wide Drive inline adapters remain open under issue #252. PR #248 remains Draft/unmerged Development source; Release Candidate, production, protected signing/distribution, and Stable qualification are not established.

## September 26, 2026 — PR #248 compacts Search handoffs and adds remote-inline provider control

**Change type:** Universal Search presentation; provider-control architecture; privacy boundary; Development candidate.

Representative-device feedback found that the existing **Search with** row consumed too much horizontal space and that a terminal **No results found** message understated the availability of enabled connected handoffs.

The current PR #248 candidate now:

- renders Google Drive, Dropbox, Brave Search, and future explicit handoffs as compact 48 dp icon controls instead of full provider-name buttons;
- reuses installed provider application artwork when Android LauncherApps inventory exposes it, with accessible provider labels and a compact initials fallback when artwork is unavailable;
- reports **No local matches** when local providers return nothing but an enabled handoff remains available, reserving **No results found** for a genuinely empty local/handoff state;
- introduces a distinct **opt-in remote-inline** provider invocation mode and marker contract so a reviewed asynchronous remote provider can participate in the unified result stream only after explicit source enablement;
- requires an opted-in remote-inline implementation to report its governed credential/authorization/configuration path ready before it is admitted to live execution, so enablement alone cannot cause an unconfigured provider to receive typed queries;
- gives any future reviewed `CONNECTED_SOURCE` result explicit **From <provider>** attribution derived from the active provider-control display name, with focused unit coverage that local results never receive that remote-source label; and
- keeps current Google Drive, Dropbox, and Brave Search registrations in explicit-handoff mode because they do not yet have governed inline adapters.

Issue #252 remains the implementation authority for actual inline connected-source results. Brave Autosuggest still requires a governed credential/proxy or user-owned credential path rather than a long-lived secret embedded in the APK; private Google Drive, Gmail, and Dropbox results require provider-approved account authorization/OAuth and least-privilege scopes. No shortcut around those boundaries was added.

**Lifecycle boundary:** This remains unmerged Development source. A fresh exact-head Android CI pass is required for the changed candidate, and representative-device Search layout, accessibility, provider-icon rendering, IME behavior, connected-source privacy, and issue #80 acceptance remain open.

## September 26, 2026 — PR #248 adds first-run setup, automatic Home modes, and Launcher hints

**Change type:** First-run experience; Home personalization; local usage ranking; discoverability; Development candidate.

PR #248 now includes a first-run GoreeCloud Launcher startup wizard. Ordinary Launcher surfaces remain behind the setup flow until the user completes the initial choices. The wizard includes Android default-launcher role setup plus Home automatic-app mode, Home grid, Home app-label visibility, optional newly-installed-app placement, Universal Search Home-entry mode, and whether built-in Launcher hints remain enabled.

Automatic Home apps are now modeled explicitly rather than through the earlier boolean suggestion switch:

- **No apps** adds no automatic app suggestions to Home;
- **10 most recent** presents up to ten apps most recently launched through GoreeCloud Launcher;
- **10 most used** presents up to ten apps with the highest Launcher-local aggregate launch counts;
- automatic suggestions exclude persisted Home Favorites and Dock duplicates and are placed only into currently empty Home cells;
- persisted/manual Home apps keep their Room-authoritative spatial coordinates and ordinary drag/drop behavior; automatic suggestions do not rewrite the saved workspace;
- manual apps can still be added from Apps at any time, regardless of the selected automatic mode; and
- ranking remains privacy-bounded to Launcher-initiated launches, retaining no timestamps, dwell time, Android Usage Access, query history, or network telemetry.

A dismissible Home hint now explains the default swipe-up Apps gesture, swipe-down Universal Search gesture, empty-space long-press editing, and drag/drop. The wizard can disable hints, Home can dismiss them, and Launcher Settings can show them again.

Test architecture separates onboarding from established-runtime acceptance: existing Home/Drawer lifecycle and transition-performance tests explicitly complete startup first, while a dedicated startup-wizard instrumentation test verifies the three automatic Home choices and final configuration handoff.

**Lifecycle boundary:** This remains unmerged Development source. Fresh exact-head CI plus representative-device first-run, Home-role, automatic-mode, manual-placement coexistence, hint accessibility, large-text/landscape, recovery, signing, Release Candidate, production, and Stable acceptance remain open under issue #80.

## September 26, 2026 — PR #248 incorporates representative-device Launcher feedback

**Change type:** Home suggestions; app-icon reliability; Launcher identity derivative; Universal Search follow-up; Development candidate.

Representative-device testing after Android CI #892 confirmed that the simplified Universal Search entry is materially cleaner while exposing additional Launcher defects and follow-up requirements.

Implemented in the current PR #248 candidate:

- extends the existing local-only usage store with a bounded most-recently-launched app ordering in addition to aggregate launch counts;
- represents recency only by ordering, without timestamps, dwell time, Android Usage Access, cross-application history, query history, or network telemetry;
- evolves the earlier recent-app prototype into the current explicit automatic Home model: No apps, 10 most recent, or 10 most used. Automatic suggestions are transient, exclude persisted Home/Dock entries, occupy only otherwise-empty Home cells, and never rewrite Room-authoritative manual placement; manual drag/drop remains available in every mode;
- keeps a bounded process-local stale-while-revalidate icon fallback during package/profile cache invalidation so a previously decoded official icon can remain visible while its replacement is decoded;
- retries Android's authoritative activity icon when the badged-icon decode path fails and now re-badges that fallback for the app's Android profile when PackageManager is available;
- adds PackageManager activity-icon resolution, package-level application-icon recovery, and an Android-owned default activity icon only as a final fail-soft fallback when app-owned resources are unreadable; visible loads and bounded background warming share the same chain;
- adds focused JVM coverage for the full fallback ordering plus Android runtime coverage that enumerates visible launcher activities and verifies the shared cache resolves non-null artwork; and
- recenters/refines the Android adaptive Launcher foreground while preserving the canonical four-tile plus center-accent identity contract and required GoreeCloud color/opacity tokens.

Validation correction:

- intermediate icon revisions were intentionally rejected by `scripts/check_identity.py` when they removed the center accent or canonical identity tokens; those failures remain audit evidence and the guard was not weakened;
- the identity-valid Search/icon checkpoint `9c18d1af4996b9ebac9bdbf15aabbff62708d8b6` passed Android CI #902 / `36255771493` across all configured lanes before the later live recent-Home integration;
- later superseded runtime runs exposed two related boundaries: persisted-workspace lifecycle tests were sharing automatic Home state, and suggested Home apps had lost global vertical Home gestures because drag eligibility and swipe eligibility were coupled. The lifecycle suite now preserves/restores the exact Home app mode around persisted-layout tests, while Home tiles keep swipe-up/swipe-down gesture handling regardless of whether they are draggable. Suggested apps remain non-draggable until pinned. Exact-head validation for the active PR is tracked in PR #248 and GitHub workflow history; representative-device acceptance remains separate.

Still open:

- issue #252 tracks true inline connected-source results and predictions for Brave Search, Google Drive, Dropbox, Gmail, and other approved providers. The current explicit-handoff model is retained until real provider authorization/API adapters exist; Launcher must not silently transmit typed queries or scrape authenticated provider sessions to imitate inline integration.
- representative-device verification is still required for all three automatic Home modes, coexistence with manual drag/drop placement, disappearance of random icon placeholders, adaptive-icon visual centering across masks/themed icons, startup wizard/hint behavior, and the broader issue #80 acceptance matrix.

**Lifecycle boundary:** PR #248 remains unmerged Development source. This work does not establish Release Candidate, production, Stable, or representative-device acceptance.

## September 26, 2026 — PR #248 simplifies the Universal Search entry state

**Change type:** Universal Search presentation; Glaze UI progressive disclosure; owner-reported UX refinement; Development candidate.

PR #248 now opens Universal Search in a deliberately minimal idle state. Before typing, the user sees only one refined Glaze search field with the existing leading search glyph, the prompt **“Find anything on your device…”**, and a trailing settings control for Universal Search sources and related controls. The previous idle-state explanatory/status content and separate **Manage sources** row are not rendered.

After the user begins typing, the existing result panel, grouped categories, source-status feedback, and explicit connected-provider handoffs may appear as relevant. Source management remains available through the in-field settings control. Android/system Back returns from source management to Search and closes Search from the primary search state.

The change does not alter provider execution, source enablement, Android permission authority, local-first processing, profile isolation, query retention, or explicit third-party handoff policy. Android runtime coverage now verifies the minimal idle state, the source-settings entry path, and transition to the result panel after typing.

**Lifecycle boundary:** This remains unmerged Development source on draft PR #248. Fresh exact-head CI and representative-device visual, keyboard/IME, TalkBack/Switch Access, large-text, landscape, touch-target, gesture, latency, and privacy acceptance remain required under issue #80.

## September 24, 2026 — PR #248 restores the long-press Uninstall handoff

**Change type:** App actions; Android package management handoff; owner-reported Development defect.

PR #248 restores the long-press **Uninstall** action by declaring Android's normal `REQUEST_DELETE_PACKAGES` capability and routing the action to the package-specific `UNINSTALL_PACKAGE` system flow instead of the generic data-deletion intent. The request still targets only the current Android profile. For Work or other secondary-profile apps, the same action opens Android's profile-specific App Info surface with a clear instruction to use that profile's Uninstall control. If an OEM cannot open the direct uninstall confirmation for a same-profile app, Launcher now also fails soft to Android App Info instead of leaving the action as a dead end.

The Launcher does not silently delete packages. Android remains authoritative for the uninstall confirmation and final package-removal decision. The change adds a testable uninstall-request contract, JVM regression coverage for the action/package URI contract and blank-package rejection, and a manifest validation requirement so the uninstall capability cannot silently disappear.

**Lifecycle boundary:** This remains Development source. Exact-head CI and representative-device confirmation that Android's uninstall confirmation opens, cancel preserves the app, confirm removes an ordinary uninstallable app, and restricted/system/profile cases fail safely remain required under issue #80.

## September 24, 2026 — PR #248 wallpaper-picker accessibility semantics

**Change type:** Accessibility; wallpaper selection; Glaze UI interaction semantics; Development stabilization.

PR #248 now exposes each built-in wallpaper choice as an explicit single-selection radio option instead of a generic clickable surface. The picker and collection titles are also exposed as accessibility headings, while the visual cards remain the same Launcher-owned preview and apply flow.

This is a presentation/accessibility correction only. It adds no Android permission, network behavior, telemetry, wallpaper mutation before the existing explicit **Apply** action, or new product authority.

**Lifecycle boundary:** This source remains Development. Exact-head CI, representative-device TalkBack/Switch Access, large-text/landscape input, visual contrast, wallpaper application/recovery, signing, Release Candidate, production, and Stable acceptance remain open under issue #80.

## September 23, 2026 — PR #248 broadened Launcher-owned Home widgets

**Change type:** Home widgets; local-first utilities; user navigation; battery-state presentation; Development candidate.

PR #248 expands the first-party widget catalog beyond clock-centric choices:

- adds a **Universal Search** 4 × 1 widget that opens Launcher-owned Universal Search;
- adds a **Quick actions** 4 × 2 widget for Apps, Search, Edit Home, and Launcher Settings;
- adds a **Battery** 2 × 1 widget for local percentage and charging state;
- retains Date, Digital clock, Compact clock, Analog clock, and Launcher Status;
- derives the picker from the canonical built-in catalog instead of maintaining a second hardcoded list; and
- adds JVM regression coverage for utility widget identity, naming, descriptions, and default spans;
- marks the widget-gallery title and catalog sections as accessibility headings, hides decorative preview glyphs from assistive semantics, and adds stable UI semantics for the unified picker search; and
- adds Android runtime coverage that opens the widget picker from Edit Home, exercises unified filtering, and verifies that a built-in widget remains an actionable accessibility node without mutating Home.

Privacy/performance boundary:

- no new Android runtime permission, location access, network request, telemetry, query persistence, or third-party data authority is added;
- Battery listens only to Android's protected battery-state broadcast and unregisters with the widget lifecycle instead of polling; and
- Search/Quick actions invoke existing Launcher-owned surfaces only.

**Lifecycle boundary:** PR #248 remains Development until its exact final head passes the configured CI lanes and the still-open issue #80 representative-device widget visual/touch/accessibility/resizing, profile/platform, performance, recovery, signing, and release gates are satisfied. This entry does not claim Release Candidate, production, Stable, or physical-device acceptance.

## September 23, 2026 — PR #240 stabilized selected file Search roots

**Change type:** Universal Search; Storage Access Framework lifecycle; failure isolation; Development stabilization.

PR #240, **Stabilize selected file Search roots**, was guarded-squash merged to `main` as `03d4c3d2d7e355916412565b531e411d1bba71de`.

Implemented:

- exposed selected local File Search folders in the rendered **Sources** manager;
- added explicit per-folder removal with a confirmation that explains Search removal and persisted Android read-access release;
- removes the Launcher-local root record and then attempts to release the matching persisted read grant, while allowing the user to select the folder again later;
- isolated file-index construction per selected root so a revoked, malformed, or broken document-provider tree fails soft without suppressing results from healthy selected roots;
- preserved the global 1,500-file index bound and existing depth/result limits;
- added JVM regression coverage for failed-root isolation and aggregate-bound enforcement;
- updated `SOURCE_MANIFEST.txt`; and
- corrected `USER-MANUAL.md` for the implemented File Search source controls, Home-editor-only Settings route, and existing AppWidgetHost behavior.

Privacy/trust boundary:

- no Launcher `INTERNET`, broad-storage, `QUERY_ALL_PACKAGES`, telemetry, query-history persistence, file-content indexing, or automatic third-party typed-query fan-out was added;
- File Search remains restricted to user-selected Storage Access Framework roots; and
- root removal reduces retained access instead of creating new authority.

Validation:

- exact PR head `32c2972d53786d3171ec7f4586bb121fe192a991` passed Android CI run #734 / `35827675978` across validate/build/unit/schema/APK staging, Android 16 Room/runtime emulator, and Android 16 transition-performance emulator lanes;
- artifact `10735388852`, `goreecloud-launcher-android-dev-sidecar`, was produced for that exact head with GitHub digest `sha256:1eb903e3f91596065b8d9e1231244dcf3750006d956f1b88d3f352343a70798b`;
- independent archive verification matched the GitHub digest; internal `SHA256SUMS` validated `GoreeCloud-Launcher-Dev.apk`; APK SHA-256 is `cdd6f5ee01cb32be7f3872a0b44bab7a1b52525fddab89f081430f410d39137d`; APK ZIP validation reported no errors; and
- guarded squash merge commit: `03d4c3d2d7e355916412565b531e411d1bba71de`.

**Lifecycle boundary:** Development only. Representative-device folder-picker/removal behavior, document-provider compatibility, large-tree latency/memory/power, accessibility, profile isolation, portable file-root recovery, release qualification, production, and Stable acceptance remain open under issue #80.

## September 23, 2026 — PR #238 added local file Search and explicit connected Search handoffs

**Change type:** Universal Search; Storage Access Framework; explicit third-party handoff; privacy boundary; Development implementation.

PR #238, **Add file and connected Universal Search sources**, was guarded-squash merged to `main` as `426e466f62d8347de720258be8492e518e84c4d5`.

Implemented:

- added opt-in local file Search over user-selected Android Storage Access Framework tree roots;
- persists only the selected tree URIs/read grants in a dedicated local DataStore;
- indexes filename and MIME metadata only, with bounded depth/index/result limits and no file-content indexing;
- opens file results through Android document intents;
- added explicit **Search with…** handoffs for Google Drive, Dropbox, and Brave Search;
- exposes Google Drive/Dropbox only when Android resolves the reviewed package-scoped search handoff and opens Brave Search through its HTTPS query URL;
- connected third-party providers receive the query only after the user explicitly taps the named provider;
- added provider-policy-controlled/unknown query-retention representation instead of inventing third-party retention guarantees;
- restricted new package visibility to Google Drive and Dropbox and enforced the allowlist in `scripts/check_manifest.py`;
- preserved PR #235's Home-editor-only Settings route; and
- updated the Android runtime regression to expect the file-aware Universal Search placeholder.

Privacy/trust boundary:

- no Launcher `INTERNET`, broad-storage, or `QUERY_ALL_PACKAGES` permission was added;
- file Search is limited to explicitly selected SAF roots and does not persist query/result history;
- Google Drive/Dropbox/Brave queries are not automatically fanned out while the user types; and
- provider-side account, network processing, and retention behavior remains governed by the selected provider.

Validation:

- initial exact-head source/build and transition-performance evidence was green, but Android runtime exposed a stale UI assertion after the file-search placeholder changed;
- the runtime assertion was corrected without changing the provider architecture;
- final exact PR head `71be92123352e5d179f48e5e1a892680aadd8987` passed Android CI run #729 / `35821960552` across validate/build/unit/schema/APK staging, Android 16 Room/runtime emulator, and Android 16 transition-performance emulator lanes; and
- guarded squash merge commit: `426e466f62d8347de720258be8492e518e84c4d5`.

**Lifecycle boundary:** Development only. Representative-device large-tree performance, connected-provider compatibility, accessibility, profile isolation, sensitive-permission distribution-policy review, broader external provider discovery/registration, release qualification, production, and Stable acceptance remain open under issue #80.

## September 23, 2026 — PR #235 reserved Launcher Settings for Edit Home

**Change type:** Settings navigation; Home gesture authority; compatibility routing; Development implementation.

PR #235, **Keep Launcher Settings in the Home editor**, was guarded-squash merged to `main` as `b68d1e56443a6e8365cf1d440e4ac5c53c868a02`.

Implemented:

- removed the Home quick-action **Customize** shortcut that directly opened Launcher Settings while retaining Apps and Search quick actions;
- reserved empty-space Home long-press for **Edit Home** so the Settings entry path cannot be remapped away;
- removed Tap and hold from the configurable gesture list and presents it as the read-only **Edit Home** action;
- removed direct **Launcher settings** from the configurable gesture picker;
- retained historical stored `LAUNCHER_SETTINGS` and `TAP_AND_HOLD` values for compatibility while routing legacy direct-Settings gestures to Edit Home and ignoring stored tap-and-hold remaps for the reserved long-press behavior;
- routes stale/internal Universal Search Settings destinations to Edit Home rather than directly entering Settings; and
- leaves the Launcher-owned Settings surface reachable through **long-press empty Home → Edit Home → Settings**.

Validation:

- candidate head `6447ee1ddfbfa3c5983512bfb8a958badb1e13a0` passed source/build validation but its Android 16 runtime lane exposed an overly strict test assertion that expected one Settings semantics node where the Home editor rendered two matches; the same run also contained a separate Compose-hierarchy emulator flake;
- no runtime implementation change was required for that assertion issue; final exact head `75581e01cdfb584b5f4af59377002ca835d0b064` relaxed only the test assertion;
- final Android CI run #723 / `35820108208` passed validate/build/unit/schema/APK staging, Android 16 Room/runtime emulator, and Android 16 transition-performance emulator lanes; and
- guarded squash merge commit: `b68d1e56443a6e8365cf1d440e4ac5c53c868a02`.

**Lifecycle boundary:** Development only. Representative physical-device long-press discoverability, gesture ergonomics, accessibility, one-handed behavior, sustained performance, release qualification, production, and Stable acceptance remain open under issue #80.

## September 22, 2026 — PR #229 added opt-in local Universal Search sources

**Change type:** Universal Search; local sensitive sources; application shortcuts; permission gating; Development implementation.

PR #229, **Add opt-in local Universal Search sources on current main**, was guarded-squash merged to `main` as `0d6ccf955ae0884d4b06d9ed1839b377ab2a1af2`.

Implemented:

- added Android application shortcuts as a local/default Launcher Search source and launches them through `LauncherApps.startShortcut`;
- added Contacts, Call history, and Messages as explicit opt-in local Search sources;
- keeps those sensitive sources disabled by default and requests the matching Android permission only when the user enables the source;
- persists source enablement only after permission grant;
- separates automatic-local, opt-in-local, and explicit-handoff provider modes;
- opens contact/dialer/messaging results through Android intent actions;
- removed the direct **Launcher settings** Universal Search result so Settings remains behind the Home editor policy; and
- preserves local query processing with no INTERNET permission, telemetry, query-history persistence, or automatic third-party query fan-out.

Permissions/trust boundary:

- added only `READ_CONTACTS`, `READ_CALL_LOG`, and `READ_SMS` for their explicitly enabled local sources;
- telephony hardware is declared optional; and
- Android/Play distribution-policy eligibility for sensitive permissions remains a separate release obligation.

Validation:

- exact PR head `740f967339de2adf0a4d70c26c9aeef2c2ecff7e` passed Android CI run #714 / `35818607895`;
- guarded squash merge commit: `0d6ccf955ae0884d4b06d9ed1839b377ab2a1af2`.

**Lifecycle boundary:** Development only. Representative-device permission behavior, profile isolation, accessibility, latency, distribution-policy review, release qualification, production, and Stable acceptance remain open under issue #80.

## September 22, 2026 — PR #228 connected persisted Universal Search source controls

**Change type:** Universal Search provider controls; rendered Sources management; Development implementation.

PR #228, **Connect persisted Universal Search source controls on current main**, was guarded-squash merged to `main` as `1e1f72c6a994e8381c0eecf3bd9fc3db17a44dde`.

Implemented:

- loads the existing versioned Search-provider preference store into activity-owned runtime state;
- keeps automatic Search providers disabled until persisted control state has loaded;
- adds an isolated Glaze Search surface with a **Sources** view for provider identity, enable/disable state, privacy summary, deterministic Earlier/Later ordering, and safe-default reset;
- limits automatic execution to providers classified by the existing privacy policy as automatic-local; and
- continues to exclude typed queries, results, history, credentials, usage signals, and provider payloads from the persisted provider-control store.

Validation:

- exact PR head `851bf9007ea57209981b61c1ec11bc746ad0a009` passed Android CI run #703 / `35816326844`;
- guarded squash merge commit: `1e1f72c6a994e8381c0eecf3bd9fc3db17a44dde`.

**Lifecycle boundary:** Development only. Portable provider-control recovery, file/connected-source Search, provider-specific external consent/handoff, representative-device Search latency/accessibility/profile behavior, release qualification, production, and Stable acceptance remain open under issue #80.

## September 22, 2026 — PR #226 added four Launcher-owned Glaze wallpapers

**Change type:** Wallpaper personalization; local rendering; Android system integration; Development implementation.

PR #226, **Restack four built-in GoreeCloud wallpapers on current main**, was guarded-squash merged to `main` as `29badaae9e4b1ce0c6a697501ed5cc969cc79eaa`.

Implemented:

- added four Launcher-owned Glaze wallpaper designs: Glaze Aurora, Glaze Horizon, Glaze Nocturne, and Glaze Cascade;
- renders the wallpaper images locally from inspectable GoreeCloud source instead of downloading artwork;
- reuses the existing Home long-press **Wallpaper** action and Universal Search Wallpaper action;
- presents the four built-ins with names/descriptions and retains an explicit Android system-wallpaper-picker fallback;
- applies selected built-ins through Android `WallpaperManager`; and
- added focused catalog coverage and source-manifest tracking.

Privacy/security boundary:

- added only `android.permission.SET_WALLPAPER`, guarded by the repository manifest allowlist;
- no INTERNET, storage, advertising, analytics, remote asset, or executable remote-code dependency was added.

Validation:

- exact PR head `3d535cfee09a6995790cbc46371f7a55f8d4cdd8` passed Android CI run #698 / `35815073478` across validate, Android 16 Room/runtime emulator, and Android 16 transition-performance emulator lanes;
- guarded squash merge commit: `29badaae9e4b1ce0c6a697501ed5cc969cc79eaa`.

**Lifecycle boundary:** Development only. Representative-device visual quality, resolution/orientation rendering, picker accessibility, sustained performance/power, Human Visual Excellence, release qualification, production, and Stable acceptance remain open under issue #80.

## September 22, 2026 — PR #223 removed persistent app-drawer header actions

**Change type:** App drawer interaction; navigation cleanup; Development implementation.

PR #223, **Use gesture-only app drawer dismissal**, was guarded-squash merged to `main` as `c09e2d581f300271f762422eef206631769f73b1`.

Implemented:

- removed the persistent Settings action from the app drawer header;
- removed the explicit app drawer close action;
- retained profile/layout context in the drawer header without a right-side action cluster;
- preserved downward swipe as the drawer's explicit in-surface dismissal gesture;
- preserved Android HOME-button return as normal system navigation rather than a rendered drawer control;
- retained Launcher Settings in the Home long-press editor sheet through its dedicated **Settings** action; and
- added a stable `launcher-app-drawer` test tag plus Android runtime coverage for the revised drawer interaction.

Validation:

- initial candidate head `5a908d8b5f0801cbe706cb297e1f50ab7eb13107` passed source/build and transition-performance validation but failed the Room/runtime lane because the newly added test injected dismissal through a parent semantics node; that failed head remains audit provenance and no source acceptance was taken from it;
- final exact PR head `2fe8ed2046daca56202ef07873a806a9b09a7c30` passed Android CI run #693 / `35813157287` across validate/build/unit/schema/APK staging, Android 16 Room/runtime emulator, and Android 16 transition-performance emulator lanes; and
- guarded squash merge commit: `c09e2d581f300271f762422eef206631769f73b1`.

**Lifecycle boundary:** Development only. Representative physical-device/default-HOME gesture ergonomics, accessibility, one-handed behavior, sustained performance, profile behavior, Quickstep/Recents, release qualification, production, and Stable acceptance remain open under issue #80.

## September 22, 2026 — PR #221 established the 5×6 starter Home and five-app Dock defaults

**Change type:** Home defaults; Dock defaults; local-only usage ranking; install-to-Home behavior; Development implementation.

PR #221, **Restack 5x6 starter Home and five-app Dock defaults**, was guarded-squash merged to `main` as `384568dda7abcf0a7e2c0942773efa540b70af2c`.

Implemented:

- changed a new Launcher preference store's default Home grid to 5 × 6 while retaining supported configurable grid presets;
- expanded the one-time starter Dock policy to prefer Phone, Messages, Email/Mail, Browser, and Camera when matching launchable applications are available;
- expanded the starter Home policy to up to 10 applications and places them in the bottom two Home rows directly above the Dock after Room spatial activation;
- added minimal Launcher-local aggregate launch-count ranking for starter suggestions when such local history already exists, with deterministic common/GoreeCloud role fallback when it does not;
- added Settings controls to disable local usage-based suggestions, clear the local aggregate counts, and enable **Add new apps to Home**;
- kept automatic new-app Home placement disabled by default;
- added a persisted local primary-profile launchable-app inventory baseline so installs that occur while the Launcher process is not running can be detected on the next inventory refresh;
- made the first inventory observation initialization-only so enabling the feature never treats the whole existing application inventory as newly installed; and
- preserved user layout authority after the one-time starter rather than continuously re-ranking or reshuffling Home.

Privacy/trust boundary:

- no Android Usage Access, manifest package receiver, new Android permission, INTERNET authority, analytics SDK, or remote ranking service was added;
- the local usage store keeps only an application workspace key and aggregate Launcher launch count, with no timestamps, dwell time, search queries, destinations, or network data; and
- the install baseline stores only currently visible primary-profile launchable-application workspace keys needed for local new-install detection.

Validation:

- exact PR head `d464224bbd8d975dcad8e6320a0adf117bafa318` passed Android CI run #688 / `35811191027`, including repository/privacy/identity/GLAZE/Room-cutover guards, lint/build/unit/schema checks, Development APK staging, Android 16 Room/runtime emulator, and Android 16 transition-performance emulator lanes;
- guarded squash merge commit: `384568dda7abcf0a7e2c0942773efa540b70af2c`.

**Lifecycle boundary:** Development only. Representative-device first-install behavior, package-inventory timing, accessibility, performance, work/private-profile automatic-placement policy, release qualification, production, and Stable acceptance remain open under issue #80.

## September 22, 2026 — PR #219 added GoreeCloud and Android Home widgets

**Change type:** Home widgets; AppWidgetHost; Room workspace; Development implementation.

PR #219, **Restack GoreeCloud and Android Home widgets**, was guarded-squash merged to `main` as `42513fb80ec2caea8431437664a96abb1a605f1f`.

Implemented:

- added Launcher-owned GoreeCloud Clock and Launcher Status Home widgets;
- added Android third-party widget selection through the platform AppWidget picker and provider configuration activities;
- added AppWidgetHost/AppWidgetHostView lifecycle handling, including host-ID cleanup after canceled/failed selection and successful removal;
- persisted widget identity/provider binding, Home cells, and spans in the Room workspace;
- added span-aware primary-Home rendering, resize/remove controls, and widget-covered-cell exclusion from application drop targets;
- preserved widget rows across ordinary Home/Dock application placement writes while keeping the legacy favorites compatibility projection application-only; and
- added focused JVM and Android runtime regression coverage for widget key/spatial validation, paged rendering, authoritative compatibility reads, and widget preservation across application placement writes.

Privacy/trust boundary:

- no privileged `BIND_APPWIDGET` authority, network permission, advertising, analytics, or remote widget service was added by Launcher;
- Android widget provider behavior remains governed by the selected provider; and
- portable widget backup/restore is not claimed because Android `appWidgetId` bindings are not portable across restore targets.

Validation:

- exact PR head `c188884d22a5a90bed9074a4695ecfb2a30c99a8` passed Android CI run #679 / `35809355554`;
- guarded squash merge commit: `42513fb80ec2caea8431437664a96abb1a605f1f`.

**Lifecycle boundary:** Development only. Representative-device widget picker/configuration/provider behavior, accessibility, rotation/form-factor behavior, performance, portable widget recovery, release qualification, production, and Stable acceptance remain open under issue #80.

## September 22, 2026 — PR #215 unified Home, Dock, and App Drawer drag/edit interactions

**Change type:** Workspace interaction; drag/drop; edit mode; Development implementation.

PR #215, **Restack unified Launcher drag and edit interactions**, was guarded-squash merged to `main` as `12634e7388f6c997debb255874136f80cc720830`.

Implemented:

- added long-press edit mode with visible Home grid/edit affordances;
- added Home-to-Dock and Dock-to-Home drag movement;
- added Dock reordering and primary-Home cell reordering by drag;
- added App Drawer copy-to-Home and copy-to-Dock placement without removing the application from Drawer inventory;
- hid icon context actions during active drag;
- added Home rename and Android App info alongside existing uninstall and placement controls;
- preserved layout lock, Home capacity, and five-item Dock limits; and
- retained accessible non-drag placement/order controls.

Validation:

- exact PR head `c5ec82a5850b75d3442b5a457a8eac9d0ec44d7d` passed Android CI run #671 / `35803617800`;
- guarded squash merge commit: `12634e7388f6c997debb255874136f80cc720830`.

**Lifecycle boundary:** Development only. Representative-device drag ergonomics, accessibility, sustained performance, release qualification, production, and Stable acceptance remain open under issue #80.

## September 22, 2026 — PR #212 reconciled the Launcher source manifest

**Change type:** Repository integrity; source tracking; Development governance.

PR #212, **Reconcile Launcher source manifest**, was guarded-squash merged to `main` as `53e79befaf7f76b1abf27acecf8a3c838515f34b`.

Implemented:

- reconciled `SOURCE_MANIFEST.txt` with the authoritative tracked repository blobs after earlier source growth; and
- restored exact source-manifest parity required by Launcher validation before subsequent interaction/widget/default-layout tranches were integrated.

Validation:

- exact PR head `d333cfb69a3064bad2cded99b29af28a2576f496` passed Android CI run #660 / `35802293870`;
- post-merge readback verified 213 manifest entries matched the 213 intended tracked blobs at that checkpoint.

**Lifecycle boundary:** This was repository-integrity work only and did not change the Launcher Development lifecycle or release-acceptance state.

## September 22, 2026 — PR #207 stabilized persisted Universal Search provider controls

**Change type:** Universal Search; privacy controls; local persistence reconciliation; Development implementation.

PR #207, **Fail closed on unreadable Universal Search provider preferences**, was guarded-squash merged to `main` as `0af5d6753d1dca98de432f24f8703fe5bae85e2c`.

Implemented:

- added policy-level reconciliation from the persisted provider-preference decode result into executable provider-control state;
- preserved privacy-safe automatic-local defaults only when provider-control storage is genuinely absent;
- preserved explicit loaded enablement and provider order;
- made malformed or unsupported persisted state fail closed to no enabled automatic providers instead of silently restoring defaults;
- added bounded provider enable/disable and ordering mutation helpers that emit the existing versioned provider-control snapshot;
- ignored stale or unknown provider IDs during mutation and preserved enablement independently from ordering; and
- added focused JVM regression coverage for absent, loaded, invalid, unsupported, stale-ID, enable/disable, and ordering semantics.

Privacy/trust boundary:

- no typed queries, results, history, usage/frequency signals, credentials, authorization grants, or provider payloads were added to persistence;
- no networking, external provider discovery/invocation, Android permission, telemetry, or cross-profile authority was added; and
- rendered provider management, runtime Compose/DataStore collection, external handoff execution, provider-specific consent, and portable backup adoption remain separate open work.

Validation:

- exact PR head `f823ab9c1cb406116b68fe1f7c20cefef1ad6575` passed Android CI run #647 / `35794625569` across validate/build/unit/schema/APK staging, Android 16 Room/runtime emulator, and Android 16 transition-performance emulator lanes;
- guarded squash merge commit: `0af5d6753d1dca98de432f24f8703fe5bae85e2c`.

**Lifecycle boundary:** Development only. This tranche hardens persisted provider-control semantics but does not render the provider manager, connect the persisted state to live Search UI/provider fan-out, establish representative-device Search acceptance, or satisfy Release Candidate, production, or Stable gates. Issue #80 remains open.

## September 22, 2026 — PR #205 cancelled superseded Launcher icon preload work

**Change type:** Performance stabilization; application inventory; icon caching; Development implementation.

PR #205, **Cancel superseded Launcher icon preload work**, was guarded-squash merged to `main` as `439943d7918e4d04e9f7bf62707dc55d4a2898cd`.

Implemented:

- added a single replaceable background preload runner for Launcher icon warming;
- a new authoritative `LauncherApps` inventory warm request now cancels the superseded preload tail before it can continue scheduling lower-priority icon loads;
- complete icon-cache/profile-topology invalidation now cancels background preload work before the cache generation is advanced and entries are evicted;
- preserved the existing 12 MiB LRU cache, 128-candidate warm bound, batches-of-three parallelism, package/profile invalidation stamps, single-flight decode sharing, LauncherApps inventory authority, and visible-icon lazy fallback;
- preserved safe reuse of already-started single-flight decodes by newer callers while stale stamp checks continue to prevent invalidated results from entering the cache;
- added focused JVM regression coverage proving replacement preload work cancels the superseded tail; and
- added the directly affected icon-cache source and regression test to `SOURCE_MANIFEST.txt`.

Validation:

- exact PR head `f6696f4933b073e355c08e3871bd1f7365b4ceac` passed Android CI run #643 / `35758277303` across validate/build/unit/schema/APK staging, Android 16 Room/runtime, and Android 16 transition-performance emulator lanes;
- guarded squash merge commit: `439943d7918e4d04e9f7bf62707dc55d4a2898cd`;
- exact merged `main` passed push Android CI run #644 / `35759394496` across the same configured validation and Android 16 emulator lanes.

**Lifecycle boundary:** Development only. This tranche reduces superseded background preload scheduling but does not itself establish representative physical-device/default-HOME latency, jank, memory, power, package/profile churn acceptance, Quickstep/Recents compatibility, Release Candidate, production, or Stable qualification. Issue #80 remains open.

## September 22, 2026 — PR #203 finalized repository authority and Drive retirement was verified

**Change type:** Governance; migration completion; source-of-truth retirement; documentation correction.

PR #203, **Finalize repository record authority after migration**, reconciled the three repository-native governance records against the verified post-PR #201 state before Drive retirement.

Repository reconciliation:

- confirmed `IMPLEMENTED-FEATURES.md`, `PLANNED-FEATURES.md`, and `CHANGELOGS.md` as authoritative repository records after PR #201 merge/readback;
- distinguished repository authority state from the latest source-bearing Launcher runtime so the documentation-only migration could not be mistaken for a runtime promotion;
- preserved Launcher **Development** lifecycle status and all remaining acceptance gates; and
- recorded PR #201 as the repository-side migration event without rewriting historical evidence.

Validation and promotion:

- exact PR #203 head `3bc23b85629b51cef1a7762a93d6191907e59f3a` passed Android CI run #638 / `35721409203` across the repository's validate/build/unit/schema/APK-staging and Android 16 emulator lanes;
- PR #203 merged to `main` as `25d53b5b213aa6ddaf99bb09e4ec9edeebbec7e9`;
- authoritative readback confirmed the three repository-native records, README navigation, seven imported history segments, and absence of root `FEATURE-ROADMAP.md`.

Drive retirement verification after authoritative readback:

- legacy `GoreeCloud/Changelogs/Change Log — Launcher.docx` file ID `1NInGthOUuofym6TbA1ffAVONT0_BRG3i` returns not found;
- legacy `GoreeCloud/Feature Roadmap/GoreeCloud Launcher/FEATURE-ROADMAP.docx` file ID `1Y9eFLv1583ffP1k3ra_smZZ0UpFMfRau` returns not found;
- no remaining Launcher-named changelog exists in the canonical GoreeCloud Changelogs folder; and
- the dedicated `GoreeCloud Launcher` feature-roadmap folder remains present but empty pending the broader estate-wide retirement of obsolete roadmap/changelog directory structures.

**Lifecycle boundary:** This completed the Launcher repository-native feature/changelog migration only. It did not promote Launcher beyond Development and did not complete the broader GoreeCloud estate migration.

## September 22, 2026 — PR #201 established repository-native feature and changelog authority

**Change type:** Governance; documentation architecture; source-of-truth migration.

PR #201, **Migrate feature tracking and changelog governance**, completed the repository-side migration required by **Standard — Repository Feature Tracking and Changelog Governance v1.0**.

Implemented and reconciled:

- added root-level `IMPLEMENTED-FEATURES.md`, `PLANNED-FEATURES.md`, and `CHANGELOGS.md`;
- migrated and dispositioned legacy roadmap obligations without promoting partial work to complete status;
- imported the meaningful historical Launcher changelog chronology into the seven repository-local history segments above;
- classified the former Drive-synchronization obligation as superseded rather than silently dropping it;
- updated README authority to the repository-native records and prohibited feature/changelog synchronization back to Drive;
- recorded the governance supersession on issue #80 without rewriting its historical evidence; and
- retired root `FEATURE-ROADMAP.md` from authoritative `main` after its replacement records were verified on the migration branch.

Validation and promotion:

- exact PR head `bb9f5f7d2a91b771875b2aa4222d98b012bb7bda` passed Android CI run #635 / `35719798536` across validate/build/unit/schema/APK staging, Android 16 Room/runtime emulator, and Android 16 transition-performance emulator lanes;
- PR #201 was squash-merged to `main` as `009371938ac3cab041cfb0893ede68e66e211a4f`;
- default-branch readback verified the three required root records and imported changelog history; and
- `FEATURE-ROADMAP.md` is retired from the reviewed migration result.

**Remaining migration cleanup at that point:** The former Drive `FEATURE-ROADMAP.docx` and `Change Log — Launcher.docx` records still required deletion and removal verification after repository authority was established. PR #203 and the subsequent Drive audit completed that cleanup; this sentence preserves the PR #201 contemporaneous boundary rather than implying the files had already been retired at that earlier stage.

## September 22, 2026 — PR #199 persisted Universal Search provider preferences

**Change type:** Universal Search; local persistence; privacy controls; Development implementation.

PR #199, **Persist Universal Search provider preferences**, was merged to `main` as `ec6640dda8522244d57a947db083aecb8b9cfe33`.

Implemented:

- added `LauncherSearchProviderPreferencesRepository` backed by a dedicated Launcher-owned Preferences DataStore;
- persisted only the versioned provider enable/order snapshot introduced by PR #198;
- preserved the semantic distinction between no stored provider preference and an explicitly stored empty enabled-provider set;
- surfaced malformed or unsupported persisted values through the existing fail-closed decode result instead of silently manufacturing defaults;
- exposed explicit `read`, `set`, and `clear` boundaries; and
- added focused JVM coverage for absent-vs-explicit-empty behavior, enable/order round trips, malformed persisted data, and the single-key storage boundary.

Privacy/recovery boundary:

- no typed queries, results, history, usage/frequency signals, credentials, authorization grants, or provider payloads are stored by this persistence layer;
- no networking, external provider discovery/invocation, Android permission, telemetry, or cross-profile authority was added; and
- the provider-control DataStore remains deliberately separate from the strict seven-field portable-preference v1 backup/recovery contract.

Validation:

- exact PR head `5646ce68d998ec96797d29a8c470df96c6ceac57` passed Android CI run #620 / `35710385034` across validation, build, unit/schema checks, Development APK staging, Android 16 transition-performance emulator, and Android 16 Room/runtime emulator lanes;
- merge commit: `ec6640dda8522244d57a947db083aecb8b9cfe33`.

Lifecycle boundary: Development only. Rendered provider management, provider-specific consent, explicit `Search with` invocation, external provider discovery/registration, portable-backup adoption/migration, representative-device Search acceptance, release, production, and Stable gates remain open.

## September 22, 2026 — PR #198 added versioned provider preference serialization

**Change type:** Universal Search; provider controls; serialization; privacy architecture; Development implementation.

PR #198 established the versioned, fail-closed serialization contract used by later provider-control persistence.

Implemented:

- versioned serialization of provider enablement and explicit provider order;
- preservation of absent-versus-explicit-empty semantics; and
- exclusion of typed queries, results, history, usage/frequency signals, credentials, authorization grants, and provider payloads from the serialized control boundary.

Validation:

- exact PR head `74cf28e2cc989e3e88d3cdd3e252dd69be45a71e` passed Android CI run #618 / `35707597053` across validate/build/unit/schema/APK staging, Android 16 Room/runtime, and Android 16 transition-performance emulator lanes;
- guarded merge commit: `d2600bc3f0b2fce6d3c8d524a8aef536e43cd1cb`;
- exact merged `main` then passed Android CI run #619 / `35708430142`.

Lifecycle boundary at the time of PR #198: the serialization contract did not yet write its payload to DataStore or portable backup/recovery state. PR #199 subsequently added Launcher-local DataStore persistence, while portable backup/recovery adoption remains open.

## Historical integrity rule

Historical sections may retain terminology, authority assumptions, or governance practices that were true for their exact revision but later superseded. For example, older entries referring to Drive roadmap synchronization or GoreeCloud Index-oriented Search authority remain historical provenance; they do not override the current repository-native feature/changelog standard or the current Launcher-owned Universal Search architecture.

Corrections must be additive and traceable. Do not silently rewrite older evidence to resemble current state.

## Changelog maintenance rule

Meaningful Launcher changes must be recorded in this repository-local `CHANGELOGS.md`, with supporting history under `docs/changelog-history/` when needed for volume or historical preservation. Google Drive must not receive a synchronized, mirrored, backup, convenience, or canonical changelog copy.

A repository commit, pull request, CI run, or artifact alone is not proof of production deployment or runtime acceptance. Each entry must describe the evidence-backed lifecycle state actually established.