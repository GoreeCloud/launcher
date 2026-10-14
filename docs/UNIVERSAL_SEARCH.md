# GoreeCloud Launcher — Universal Search acceptance

Development branch: `feat/lawnchair16-glaze-foundation`

## Implemented in this candidate

- On clean profiles, swiping down on Home activates the existing local-first app drawer search instead of opening notifications. The gesture remains configurable in the existing Gestures settings, and the native notification action is still selectable.
- Gestures respect the Universal Search enabled toggle. When disabled with Universal Search assigned to swipe down, the launcher no longer intercepts that default swipe.
- Automatic keyboard opening for Universal Search defaults on, independent from normal drawer entry. Android Back navigation follows the existing launcher search state.
- Settings displays Universal Search and opens the search-result settings tab by default. Master enable, keyboard-on-open, apps/shortcuts, optional contacts/files/web, calculator, and other local search controls are exposed through existing settings.
- All protected data continues to use the existing source permission checks and opt-in mechanisms; no new endpoint, account requirement, tracking, or Android permissions were added.

## Verification

`scripts/verify_universal_search.py` provides source-level contracts and runs in GoreeCloud CI alongside the existing privacy, launcher, and permission checks.

**Not yet established:** exact-device swipe trigger and latency, widget gesture conflict behavior, keyboard animation smoothness, Android Back and focus restoration, results navigation, screen reader navigation, live permission denial/grant, offline network traces, performance, regression with persisted custom gestures, install/update compatibility and persistent development signing.

## Release boundaries

This is not a stable release. GitHub CI building a QA APK is compilation and packaging evidence, not device-level acceptance. Development QA APKs may be signed with ephemeral debug certificates, which may not update prior installs. The exact owner-facing APK must be copied into the authorized GoreeCloud Drive before delivery. Avoid destructive reinstall recommendations.
