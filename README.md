# GoreeCloud Launcher

Native Android home screen and launcher. **Early native Android foundation with limited Android 15 emulator smoke verification; not physically-device verified, released, or Stable.**

The canonical founder-directed product records are in GoreeCloud Google Drive:
- [Planned capabilities](https://drive.google.com/file/d/1ossnWKM42HDieuee5jihvmIky1WkaEUG/view)
- [Verified implemented features](https://drive.google.com/file/d/1aZiSsnF4MhbFwUkXH3v7SHhqgJONcQUz/view)
- [Competitive objectives](https://drive.google.com/file/d/1KNpn-2XEG_CoVqhH7zq8VXRi-Cr5F-kZ/view)

## Foundation in source

- Android Activity declaring the standard Home intent; user may select it as default home.
- Separate app drawer and local home workspace with up to seven pages and 24 application shortcuts per page. App shortcuts can be added or moved by long-pressing an application in the drawer; pages are saved on device.
- App grid based on launchable activities visible through Android package query APIs.
- Local application search and direct launching.
- Long-press to pin/unpin a limited number of dock apps or open Android application information.
- Basic preferences for grid columns and icon labels, using on-device private preferences.

This is not the full planned launcher. Drag-and-drop rearrangement, widgets, folders, full workspace profiles, icon packs, backup/restore, rich gestures, notification badges, work-profile management and feeds are **not** yet implemented. Initial Home/All apps navigation and selected page persistence passed a limited API 35 emulator smoke test; additional behaviors and real hardware remain unverified.

## Build

Requires JDK 17, Android SDK platform 36, SDK Build Tools 36.0.0 and Gradle 9.6.0. Android Gradle Plugin 9.4.0 is declared in the root Gradle script.

Run Gradle tasks: app:assembleDebug and app:testDebugUnitTest.

The repository does not include an unverified Gradle Wrapper binary; automated GitHub Actions installs pinned Gradle. The manual emulator workflow uses a separate test-only QA fixture app to exercise HOME role resolution, app discovery/launch, shortcut placement, dock pinning and local persistence. This fixture is not distributed as part of the Launcher APK. Even passing emulator smoke tests cannot establish broad physical-device acceptance.

## GoreeCloud UI

The visual foundation uses GC UI [native design principles](https://github.com/GoreeCloud/gc-ui): quiet Canvas, solid Surface, tonal Lens dock and Iris colors, adapted through Android native views. Full GC UI compliance needs dedicated visual, accessibility, and device review.

## Privacy and acceptance

No network, usage-statistics, contacts, notification-listener, accessibility-service, device-admin, or broad QUERY_ALL_PACKAGES permission is requested. Initial testing must include Home selection, application visibility, launching, app changes, home-page persistence and navigation, pin persistence, accessibility, orientation, different screen sizes and OEM-specific restrictions. Do not label capabilities verified without evidence from build and device execution.
