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
- Local calculator results are enabled by default in app search without new permissions or network requests. Online query suggestions are opt-in on **new installations**; the settings switch discloses that enabling them transmits typed terms to the selected provider. Previously saved preferences remain unchanged and require a migration audit. Custom providers require valid HTTPS query-template URLs; invalid templates never silently redirect to Google. Inherited Startpage partner identifiers and the default app-market search placement were removed. CI checks source-level privacy invariants, but network observation and regression/device tests remain outstanding.
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
