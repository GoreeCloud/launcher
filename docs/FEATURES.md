# Implementation feature register

| Capability | Android source | Linux source | Web source |
| --- | --- | --- | --- |
| Native platform graphical app | Implemented in source; build/device test pending | Implemented in source; desktop runtime pending | Browser app implemented in source; browser test pending |
| Launch installed apps | LauncherApps for current user; test pending | GIO desktop-entry launch; test pending | N/A: browser cannot enumerate installed apps |
| Default Home selection | HOME intent filter and RoleManager flow; device test pending | Not supported by design | Not supported by browser |
| Search/filter | On-device app title filter | Local desktop-entry filter | Local bookmark filter |
| Pin/favorites | Local SharedPreferences | Not yet persisted | Local browser bookmark management |
| Dock/home navigation | Compose home and apps destinations | Search list | Dashboard navigation |
| Shortcut import/export | Planned | Planned | Versioned JSON; validation and size limits |
| Android widgets / gestures / icon packs | Planned | N/A | N/A |
| Cross-device sync / Mesh | Proposed; no transport enabled | Proposed; no transport enabled | Proposed; no transport enabled |
| Accessibility acceptance | Pending real TalkBack/hardware tests | Pending real keyboard/AT tests | Pending real screen reader/browser tests |
| Stable release readiness | Not certified | Not certified | Not certified |

Historical Android APKs in Google Drive are not part of this source tree. Avoid treating archived tests as tests of these new files.
