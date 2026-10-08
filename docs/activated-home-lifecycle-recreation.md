# Activated Home lifecycle recreation

This Milestone 1 acceptance slice validates Android activity recreation after the production workspace authority cutover merged in PR #19.

## Scope

The test launches the real `MainActivity` through `ActivityScenario` on Android 16 / API 36. Before launch, it seeds the production Preferences DataStore workspace with a real launchable application from the emulator. `MainActivity` then follows the accepted production runtime path and performs the guarded one-way cutover to Room.

The test requires terminal `ROOM` authority, verifies the seeded application label is rendered by the real Compose Home, calls `ActivityScenario.recreate()`, and requires the same Room-authoritative Home placement to render again. It then creates the same production runtime coordinator contract against the application persistence clients, adds a second real launchable application to Favorites under terminal Room authority, and requires the recreated Home to render that second application reactively.

This demonstrates that the activated Home observation path reconnects after activity destruction/recreation and continues consuming Room-backed placement mutations rather than returning to legacy DataStore placement callbacks.

## Test dependencies

The Android test configuration explicitly includes AndroidX Test Core KTX plus Compose UI test JUnit support and the debug test manifest. These dependencies are test-only and do not add runtime application permissions, network behavior, analytics, or remote services.

## Acceptance boundary

Activity recreation destroys and recreates `MainActivity` within the existing application process. This is stronger evidence than merely closing and reopening persistence clients, but it is not Android OS process-death acceptance. True process termination/relaunch, schema-version upgrade recovery, and representative physical-device/default-HOME acceptance remain separate gates.

## Privacy and security

The lifecycle test queries the emulator's local launcher inventory only to select two real launchable applications whose labels can be asserted in the Home UI. It does not export, log, upload, or persist an installed-application inventory outside the local test device. GoreeCloud Launcher still requests no INTERNET permission and introduces no advertising, sponsorship, analytics, attribution, or tracking dependency.
