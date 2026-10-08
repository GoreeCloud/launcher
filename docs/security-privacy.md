# Security and privacy

The launcher is high-trust software because it becomes the HOME surface and can enumerate launchable apps. Its default architecture therefore keeps app inventory, layout, folders, local search data and usage-derived suggestions on-device.

No advertising, attribution, sponsorship or engagement SDK is included. Milestone 0 requested no `INTERNET` permission. The current PR #248 Development candidate now declares `INTERNET` only for explicitly enabled connected Search adapters such as the authorized Google Drive metadata provider. Core Home, Apps, settings, and local Search remain offline-capable; cleartext traffic is disabled and provider opt-in/authorization remains the transmission boundary.

Do not add Accessibility Service or device-admin privileges merely to imitate privileged launcher behavior. Imported themes/backups are untrusted input.

Signing keys and passwords remain outside source control. The Development build can consume a complete externally supplied `GOREECLOUD_DEV_KEYSTORE_*` environment configuration and fails closed on partial configuration. Ordinary PR CI intentionally continues to use the CI debug key and labels its artifact as installability-only; a persistent Development key must not be injected into untrusted PR execution. Protected key provisioning, trusted distribution workflow protection, and representative-device update-in-place verification remain separate security/acceptance gates.
