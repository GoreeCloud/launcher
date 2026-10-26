# GoreeCloud Launcher

Native Android home screen and launcher. **Early native Android foundation with limited Android 15 emulator smoke verification; not physically-device verified, released, or Stable.**

The canonical founder-directed product records are in GoreeCloud Google Drive:
- [Planned capabilities](https://drive.google.com/file/d/1ossnWKM42HDieuee5jihvmIky1WkaEUG/view)
- [Verified implemented features](https://drive.google.com/file/d/1aZiSsnF4MhbFwUkXH3v7SHhqgJONcQUz/view)
- [Competitive objectives](https://drive.google.com/file/d/1KNpn-2XEG_CoVqhH7zq8VXRi-Cr5F-kZ/view)

## Foundation in source

- Android Activity declaring the standard Home intent, with a consent-based Android 10+ default Home role request, live role status, and Home-settings fallback for older devices.
- Separate app drawer and local home workspace with up to seven pages and 24 application shortcuts per page. App shortcuts can be added or moved by long-pressing an application in the drawer; pages are saved on device.
- App grid based on launchable activities visible through Android package query APIs.
- Local application search and direct launching.
- Long-press to pin/unpin a limited number of dock apps, move shortcuts earlier/later within a Home page, move them to adjacent pages, or open Android application information.
- Saved dock pins survive temporary application unavailability; hidden/missing applications are skipped without destroying preferences.
- Basic preferences for grid columns and icon labels, using on-device private preferences.
- Manual, versioned local workspace backups through the Android system document picker; restore requires format/integrity checks and explicit replacement confirmation. Contains app identifiers, not passwords, and is not encrypted.

This is not the full planned launcher. Drag-and-drop rearrangement, widgets, folders, full workspace profiles, icon packs, automated/cloud backup, rich gestures, notification badges, work-profile management and feeds are **not** yet implemented. The bounded Android 15 emulator suite has verified basic Home/app launching, workspace shortcut persistence, user-consented default Home selection and cancellation, and manual export/import/restore through the Android Downloads provider. Other versions, physical devices, OEM launchers and accessibility are unverified.

## Build

Requires JDK 17, Android SDK platform 36, SDK Build Tools 36.0.0 and Gradle 9.6.0. Android Gradle Plugin 9.4.0 is declared in the root Gradle script.

Run Gradle tasks: app:assembleDebug and app:testDebugUnitTest.

The repository does not include an unverified Gradle Wrapper binary; automated GitHub Actions installs pinned Gradle. The manual emulator workflows use a separate permission-free QA fixture app with multiple launchable activities to exercise app discovery/search, cross-app launching, workspace placement, shortcut ordering, dock interactions and local persistence. This fixture is not distributed as part of the Launcher APK. Manual backup/restore uses explicitly user-selected documents and requires no broad storage permissions. One Android 15 Pixel 6 emulator workflow completed file creation, import, cancellation, confirmed restore and restart persistence, plus tampered/oversized/unsupported file rejection with the Android Downloads provider. Multi-provider, device and accessibility acceptance remains open. Even passing emulator smoke tests cannot establish broad physical-device acceptance.

## Verified emulator evidence

- [Home role consent and cancellation, API 35](https://github.com/GoreeCloud/launcher/actions/runs/40257203862) — Android system selection, cancellation preserves the previous Home, confirmed selection, selected-state Settings UI and Home-key return.
- [Manual local backup and restore, API 35](https://github.com/GoreeCloud/launcher/actions/runs/40251850733) — actual file export/import, checksum, cancel, confirmation and post-restart persistence.
- [Invalid backup rejection, API 35](https://github.com/GoreeCloud/launcher/actions/runs/40253831973) — stale checksum, 65,537-byte oversized and unsupported-version files rejected without overwriting state.
- [Bounded shortcut grid and backup regression, API 35](https://github.com/GoreeCloud/launcher/actions/runs/40252843342) — sparse Home page geometry and previous backup behaviors.

These are **single-emulator test cases**, not cross-version or hardware certification.

## GoreeCloud UI

The visual foundation uses GC UI [native design principles](https://github.com/GoreeCloud/gc-ui): quiet Canvas, solid Surface, tonal Lens dock and Iris colors, adapted through Android native views. Full GC UI compliance needs dedicated visual, accessibility, and device review.

## Privacy and acceptance

No network, usage-statistics, contacts, notification-listener, accessibility-service, device-admin, or broad QUERY_ALL_PACKAGES permission is requested. Remaining acceptance includes full dock lifecycle/capacity and order, arbitrary device application visibility, permission-denied and storage-provider variations, TalkBack/keyboard access, orientation, different screen sizes, physical devices, OEM restrictions, security/performance checks and distribution signing. Do not label capabilities verified without evidence from build and device execution.
