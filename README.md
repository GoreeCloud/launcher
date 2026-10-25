# GoreeCloud Launcher

Native Android home screen and launcher. **Early source foundation, not yet device verified, released, or Stable.**

The canonical founder-directed product records are in GoreeCloud Google Drive:
- [Planned capabilities](https://drive.google.com/file/d/1ossnWKM42HDieuee5jihvmIky1WkaEUG/view)
- [Verified implemented features](https://drive.google.com/file/d/1aZiSsnF4MhbFwUkXH3v7SHhqgJONcQUz/view)
- [Competitive objectives](https://drive.google.com/file/d/1KNpn-2XEG_CoVqhH7zq8VXRi-Cr5F-kZ/view)

## Foundation in source

- Android Activity declaring the standard Home intent; user may select it as default home.
- App grid based on launchable activities visible through Android package query APIs.
- Local application search and direct launching.
- Long-press to pin/unpin a limited number of dock apps or open Android application information.
- Basic preferences for grid columns and icon labels, using on-device private preferences.

This is not the full planned launcher. Advanced workspace pages, folders, widget hosting, arbitrary icon packs, backup/restore, rich gestures, notification badges, work-profile management and feeds are **not** yet implemented.

## Build

Requires JDK 17, Android SDK platform 36, SDK Build Tools 36.0.0 and Gradle 9.6.0. Android Gradle Plugin 9.4.0 is declared in the root Gradle script.

Run Gradle tasks: app:assembleDebug and app:testDebugUnitTest.

The repository does not include an unverified Gradle Wrapper binary; automated GitHub Actions installs pinned Gradle. A CI build cannot stand in for installation or runtime testing.

## GoreeCloud UI

The visual foundation uses GC UI [native design principles](https://github.com/GoreeCloud/gc-ui): quiet Canvas, solid Surface, tonal Lens dock and Iris colors, adapted through Android native views. Full GC UI compliance needs dedicated visual, accessibility, and device review.

## Privacy and acceptance

No network, usage-statistics, contacts, notification-listener, accessibility-service, device-admin, or broad QUERY_ALL_PACKAGES permission is requested. Initial testing must include Home selection, application visibility, launching, app changes, pin persistence, accessibility, orientation, different screen sizes and OEM-specific restrictions. Do not label capabilities verified without evidence from build and device execution.
