# GoreeCloud Launcher — Lawnchair-derived rebuild

**State: Development. Not released, Glaze-conformant, representative-device-accepted, or Stable.**

## Working baseline

- Android 16 Launcher3/Lawnchair-based HOME functionality, icon/workspace management, search and widgets enter the GoreeCloud development branch as upstream source, **not verified product features**.
- GoreeCloud app identity and native Glaze 1.7.0 semantic entry points are the first integration increment.
- Settings deep links and external shortcut/icon-pack intent actions now use unique GoreeCloud Launcher identities rather than Lawnchair's inherited intent namespace. External icon-pack app-to-launcher shortcuts must be compatibility-tested separately; users can continue choosing icon packs within Launcher preferences.
- Initial privacy defaults have now been tightened: local on-device QSB provider on a new installation, sponsored provider exclusion, removal of sponsorship surfaces and upstream release workflow isolation. This does **not** certify the broader network/permission behavior.
- Added a 5x6 Home grid preset with a dedicated clean five-role local-app dock, preserving 5x5 and smaller-device choices. Removed inherited vendor-folder, marketplace and example-website starter shortcuts; structural layout tests now run in CI. Source-level Glaze integration also applies the 48dp minimum touch target and card-radius metric to shared settings components. Device acceptance and migration testing are still pending.
- GoreeCloud policies require no ads, no sponsored placement, no silent analytics, offline-capable core launcher functions, user-controlled online search, user privacy and data control.
- Search provider fallback remains on-device when the configured provider is unusable; the launcher does not silently substitute another installed external provider. Two obsolete browser-history permissions were removed from the application manifest.
- Local calculator results are enabled by default in app search without new permissions or network requests. The tap-to-copy result also has a screen-reader label, pending TalkBack acceptance. Opt-in built-in suggestion endpoints also reject redirects, prevent double-encoding Retrofit query parameters, and omit exception details from diagnostics. Custom HTTPS search templates now reject credentials, URL fragments and query placeholders inside URI authority/hostname; custom remote suggestion responses are capped at 64 KiB, including streaming responses with no length header. Built-in suggestion providers now also cap streamed payloads at 64 KiB and clamp persisted suggestion-count values to the supported 0–10 safety range. Custom suggestion HTTP redirects are blocked to avoid silently transmitting entered terms to another endpoint; network/provider failures produce empty suggestions without logging query-bearing exception details. This is source behavior only, awaiting network-observed verification. Online query suggestions are opt-in on **new installations**; the settings switch discloses that enabling them transmits typed terms to the selected provider. Previously saved preferences remain unchanged and require a migration audit. Custom providers require valid HTTPS query-template URLs; invalid templates never silently redirect to Google. Inherited Startpage partner identifiers and the default app-market search placement were removed. CI checks source-level privacy invariants, but network observation and regression/device tests remain outstanding.
- The old original GoreeCloud Launcher spec and task evidence describe an earlier GitHub history no longer present in the live target repository. Treat those assertions as historical until evaluated against the newly imported code.

## Delivery gates

1. **Provenance and privacy:** audit every dependency, manifest permission, network call, usage access, analytics hook, deep link, exported component, content-provider integration and user-data store. Remove, default-off or gate anything incompatible with GoreeCloud policy.
2. **Identity and native Glaze:** theme semantic mapping, typography, spacing, minimum 48 dp touch target, menus, Home, drawer, dock, folders, widgets, settings, onboarding, light/dark and reduced-motion behavior. Validate visual contrast and screen reader interactions.
3. **Launcher product parity:** deterministic 5×6 default grid, 5-app starter dock with overflow, Edit Home, local app discovery/search, drag/drop, shortcuts, AppWidgetHost, multi-user/work profile identity and keyboard accessibility. Check upstream behavior, then replace or adapt where necessary.
4. **GoreeCloud differentiation:** Glaze widgets and wallpapers, user-controlled advanced local search, privacy controls, bounded backup/restore, resilient migration and optional integrations. Online providers must not receive queries without explicit user action and authorization.
5. **Build and compatibility:** Android SDK/Gradle dependency audit, reproducible debug APK, unit/lint/instrumentation on Android 16, upgrade and restore paths, representative phone/tablet/managed-profile tests, performance and power budgets.
6. **Protected delivery:** protected Development signing, monotonically increasing versionCode, exact-head CI, proper review, rollback plan, known-issue assessment, then separate candidate and release acceptance.

## Rollback and upgrade

The existing `main` initial commit remains untouched by this import branch. Revert the branch/PR to roll back source changes. This new package identity does not guarantee upgrade compatibility with the earlier GoreeCloud Launcher or upstream Lawnchair app: storage migration and signing continuity require separate explicit validation.

## Open blockers

- Build, real Android device, accessibility and security runtime acceptance are **unverified**.
- Full visual and interaction redesign, search/privacy safeguards and the complete GoreeCloud feature contract are **not implemented** merely by importing upstream.
- Licensing/dependency compliance and Glaze consumer acceptance require separate evidence.


### 2026-10-12 — Offline calculator typing improvements (development candidate)
Launcher search's local calculator now accepts mathematical multiplication (×), division (÷), Unicode minus (−), full-width plus/parentheses, and optional trailing equals while retaining the original expression in the result. An input-length guard rejects unreasonably long expressions. The new CI source-policy assertion checks the route and keeps calculator evaluation local, behind its existing enabled preference. Android compilation and interactive text-entry/clipboard usability acceptance are separate pending gates.


### 2026-10-12 — Useful recent search history (development candidate)
On-device recent-search suggestions now filter blank entries and deduplicate case-insensitively after newest-first ordering, keeping the latest usable instance of each query. Results are capped even if the stored maximum becomes corrupted. This does not send search history to a remote provider and does not change the user's existing history-retention controls. CI static regression and Android compilation must pass; interactive history and search-action behavior require device acceptance.


### 2026-10-12 — Offline percent-of arithmetic (development candidate)
Typing a simple expression such as 15% of 80 into Launcher search can now evaluate directly to 12 without network requests. This is intentionally distinct from the expression engine's existing binary percent modulo operator, which remains unchanged. The implementation supports signed integer and dot-decimal operands and a trailing equals sign; it does not claim locale-specific percentages or arbitrary nested percent expressions. Source regression is included; Android compilation and interactive results require acceptance.


### 2026-10-12 — Bounded recent-search buffer
The on-device recent-history adapter now keeps no more than 512 recent provider rows in memory before applying its existing newest-first ordering, blank filtering, deduplication and visible result count cap. A zero/negative configured result count skips provider access. Source-policy checks cover the buffer and skip behavior; Android CI, interactive search history and real-device privacy acceptance remain separate requirements.

### 2026-10-12 — Stable chronological history cursor order
The on-device recent-search provider now receives an explicit ascending date and row-id sort, matching Android's generic suggestions URI behavior. The bounded in-memory deque retains the latest rows before the UI reverses them newest-first. Without this, a provider's unspecified SQLite row order could retain stale rows and omit recent searches. Regression source policy enforces the ordering, pending Android/device acceptance.

### 2026-10-12 — Legacy sensitive permission regression guard
Read-only testing on the connected Android 16 Development device found that its older installed Launcher variant requests SMS, call log, location and contacts permissions. On that device, SMS/call-log/location access is not granted and contacts access is user-granted. The current rebuild source has no SMS, call-log or location manifest declarations and requires runtime permission for contacts search; source-policy checks now reject reintroduction of those legacy permission declarations. This is **source evidence**, not a merged-manifest or user-device upgrade pass. We did not modify device permissions.
