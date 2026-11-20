# GoreeCloud Launcher for Android

Android 10+ (API 29) native HOME application proof-of-implementation. Build with Android Studio, JDK 17 and Gradle 9.4.1; Android SDK platform 37.1. `gradle :app:assembleDebug`.

Capabilities in source: HOME and LAUNCHER intent filters, LauncherApps current-user discovery, search, app launch, locally saved pinned tiles, 5-item quick-launch dock, Refresh and optional Home-role request.

The canonical GoreeCloud UI Material 3 Expressive source adapter and generated palettes are vendored in `gc-ui-compose/` from `GoreeCloud/gc-ui` MIT source, to keep a standalone source build. Their values remain provisional alpha.

**Not provided**: widgets, icons packs, folders, gesture editor, auto-categorization, work/private profile support, recents integration, remote sync, wallpaper management, legacy Android compatibility, runtime certification. HOME role and default handling vary by OEM. Keep an alternative Home installed; change back through Settings > Default apps > Home app if needed.

No `INTERNET` or `QUERY_ALL_PACKAGES` permission. Apps are enumerated from LauncherApps only for `Process.myUserHandle()`, not work/private profiles.
