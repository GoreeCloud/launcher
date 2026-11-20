# Architecture and trust boundaries

Android `apps/android`: native HOME activity, in-process LauncherApps listing for the **current Android user only**, local SharedPreferences favorite IDs. Launch intents are constructed from platform-supplied Components/Users; no shell and no Internet permission. System settings remain available to switch default Home. Work profiles are not implemented; support must be implemented explicitly.

Linux `apps/linux`: PySide6 6.12.0 renders QML Qt Quick; Python GIO enumerates XDG desktop-entry IDs and opens them via GLib's DesktopAppInfo.launch (or DBus activation where requested). No concatenated shell commands, ad hoc `Exec` parsing or third-party scripts. The process operates as the logged-in user. It is a launcher window, not a replacement compositor, session manager or XDG menu daemon.

Web `apps/web`: React renders user-configured bookmarks. Local state is held in the browser's localStorage; imported JSON is parsed with strict bounds and URL protocol checks. Clicking an explicit user-selected URL opens a new tab with noopener/noreferrer. No background requests, external favicon service, auth or data upload.

### MIPS and data flow

- **GC UI**: Android consumes vendored alpha Material 3 Expressive theme source; web and Linux use values checked against GC UI `gc.tokens.json` (provisional, not a versioned SDK integration).
- **GoreeShield**: no analytics or network sync; user data remains local unless user explicitly exports a file.
- **Wardveil**: protocol allowlists, no shell launch interpolation, minimal Android privileges; not a deployed Wardveil agent.
- **Mesh**: no network bridge enabled; future adapters require scoped contracts and consent.
- **Identity**: no account requirement in local mode. Cross-device account/sync remains blocked by unverified Identity implementation.
