# GoreeCloud Launcher

An original, open-source GoreeCloud application-launching suite for **Android, Linux, and the web**. **Development preview; not release-certified.**

| Platform | Implementation | Actual scope of this repository |
| --- | --- | --- |
| Android | Kotlin, Jetpack Compose, Material 3 Expressive | HOME intent handler, installed-app inventory, searchable drawer, favorites/dock, launch and Home-role request |
| Linux | Qt Quick 6.12.0 via PySide6 | Search and launch approved XDG desktop entries (not a compositor or shell replacement) |
| Web | React + TypeScript | Local bookmark and app-link dashboard with search, import/export and URL validation |

This is **not** a fork of Nova Launcher, Samsung One UI Home, Lawnchair or AOSP Launcher3. Their code is not bundled. Third-party UI libraries and tooling are disclosed in [docs/THIRD_PARTY.md](docs/THIRD_PARTY.md).

## Quick starts

**Android** — Open `apps/android` in Android Studio with Android SDK 37.1, JDK 17 and Gradle 9.4.1; run `gradle :app:assembleDebug` (or use Android Studio's Build action). Install the debug APK, start Launcher, and optionally select it as the default Home application in Android Settings. Always keep an alternative installed Home application during evaluation. The app has no Internet permission.

**Linux** — Requires Python 3.10+, a Linux desktop session, and glibc 2.34+ when using PyPI's PySide6 wheels:

```sh
python3 -m venv .venv
. .venv/bin/activate
python -m pip install -r apps/linux/requirements.txt
python apps/linux/main.py
```

Linux app launching uses the **GLib GIO desktop-entry launch API**, not a shell, to respect freedesktop Exec and DBusActivatable semantics. Requires the distribution's `python3-gi`/PyGObject, `Gio` and `GLib` bindings (often system Python); see [Linux guide](apps/linux/README.md).

**Web** — Requires Node.js 22+:

```sh
cd apps/web
npm install
npm test
npm run build
npm run dev
```

No hosted service, cloud account, device synchronization, analytics, background agent, wallpaper provider or AI capability is configured. These may be added only with explicit trust, consent and integration contracts.

## Scope, safety and standards

- UI authority: [GoreeCloud/gc-ui](https://github.com/GoreeCloud/gc-ui). The Android theme adapter is vendored under its MIT notice; web/Linux use verified semantic token values and platform-native presentation. GC UI is alpha, so token values are provisional.
- GoreeShield is **privacy**. Wardveil is **cybersecurity**. Mesh is **integration**. Identity is **authentication/authorization**; see [MIPS assessment](docs/MIPS.md).
- Android is a genuine Android Home candidate, while Linux and web are *app launchers*, not replacements for platform shells/home screens.
- No `QUERY_ALL_PACKAGES`, storage, location, notification-listener, accessibility-service or Internet permission requested on Android.
- No Android widgets, drag-and-drop layouts, gestures, work/private profile support, icon packs, recents integration, cloud sync or device acceptance yet. See [feature register](docs/FEATURES.md), [roadmap](docs/ROADMAP.md), and [security](docs/SECURITY.md).
- Changes target a feature branch and must pass review and CI before merging. Main branch protection could not be confirmed from the connected GitHub installation; [docs/TESTING.md](docs/TESTING.md) records outstanding gates.

## Repository layout

```text
apps/android/       Android Home app
apps/linux/         Native Qt Quick desktop launcher
apps/web/           React browser launcher
docs/               Architecture, features, objectives, security, MIPS and roadmap
.github/workflows/  Automated verification
```

## License

Original source: [MIT](LICENSE), Copyright (c) 2026 GoreeCloud. Third-party dependencies retain their own licenses, including Qt/PySide6's applicable LGPL/commercial terms. See [NOTICE](NOTICE) and [THIRD_PARTY](docs/THIRD_PARTY.md).
