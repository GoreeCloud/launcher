# Third-party technology audit — initial selection (2026-11-20)

| Technology | Purpose | License / action |
| --- | --- | --- |
| [GoreeCloud UI](https://github.com/GoreeCloud/gc-ui) | Canonical theme and semantically named tokens; Android adapter vendored from MIT project | MIT; attribution in NOTICE, retain upstream notices |
| [AndroidX Compose Material 3](https://developer.android.com/jetpack/androidx/releases/compose-material3) | Native Android rendering and Expressive motion | AndroidX generally Apache-2.0; confirm transitive artifacts |
| [Android platform LauncherApps](https://developer.android.com/reference/android/content/pm/LauncherApps) | Enumerate/launch apps for current user | Android OS API; no third-party code copied |
| [React](https://react.dev/) | Browser UI | MIT; dependency from npm |
| [Vite](https://vite.dev/) | Web build/dev tooling | MIT; dependency from npm |
| [Qt for Python PySide6 6.12.0](https://pypi.org/project/PySide6/6.12.0/) | Native Linux Qt Quick rendering | LGPLv3/GPL/commercial; review dynamic linkage, source distribution and packaging obligations |
| [PyGObject / GLib GIO](https://pygobject.gnome.org/) | Correct freedesktop desktop-entry launch | LGPL family; distro package dependencies; no custom Exec evaluation |
| [Desktop Entry Specification](https://specifications.freedesktop.org/desktop-entry/latest-single/) | Desktop file semantics | Specification, not imported source |

**Not adopted:** Lawnchair/Launcher3 GPL source (large fork and integration/redistribution burden), Nova proprietary code, Samsung proprietary code, wallpaper/icon scrape services, privileged QuickSwitch modules, broad Android package visibility permissions.

This is a dependency *selection and provenance note*, not a full transitive SBOM, vulnerability certification, license legal opinion or package audit. Before shipping, lock dependency graphs, scan artifacts, inspect transitive licenses and test on target systems.
