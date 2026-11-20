# Privacy, security and limitations

## Threat boundaries

Android: app inventory includes installed launchable applications and is not transmitted. Only Android standard HOME/LAUNCHER intent semantics and LauncherApps current-user API are requested. Favorites are stored in private SharedPreferences. A third-party launcher can expose Home surfaces, therefore test lock-screen privacy before using it as a daily driver. Hidden apps are not a security feature. Keep a trusted alternative launcher installed.

Linux: GIO parses desktop file semantics and launches a user-selected installed application. This is **not a sandbox**; the selected application runs with its own permissions. Desktop entry directories may contain user-controlled content. No extra authentication or arbitrary command field is offered.

Web: supports only absolute http/https URLs, blocks javascript/data/file protocols, never fetches remote icon previews, never silently imports shortcuts, enforces JSON size and count limits, uses safe DOM text and noopener/noreferrer links. localStorage is readable by scripts on this origin; do not store private tokens or credentials. Exported backups are unencrypted; protect the file.

## Release blockers

- Full static analysis, dependency vulnerability/license scans and binary provenance.
- Real Android launcher-role, work-profile and recovery testing; third-party launcher OS/OEM restrictions.
- Browser CSP review and production hosting; Linux AppImage/Flatpak packaging and update security.
- Real accessibility, contrast, keyboard, physical hardware and power tests.
- GitHub-native main branch protection and required status checks.
- Identity/Mesh/GoreeShield/Wardveil integration contracts as applicable.

Report suspected vulnerabilities privately to the repository maintainers; do not include exploit data or secrets in public issues.
