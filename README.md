# GoreeCloud Launcher

**Development — Lawnchair 16-derived Android launcher rebuild**

GoreeCloud Launcher is a privacy-oriented Android HOME launcher built on a pinned [Lawnchair 16](https://github.com/LawnchairLauncher/lawnchair) and AOSP Launcher3 source foundation. This work is a substantial adaptation in progress, **not** a completed GoreeCloud redesign, Stable release or verified APK.

## Current branch scope

- Imported Lawnchair `16-dev` source pinned to a reproducible upstream commit.
- New GoreeCloud Android package identities and short user-facing app name `Launcher`.
- Default search is local app search; search-provider selection excludes revenue-sharing providers and inherited sponsorship UI is removed. Onboarding strings are rebranded across source locales.\n- Disabled the inherited Gradle build-scan uploader and did not import executable upstream GitHub release workflows.
- Introduced an Android-native Glaze 1.7.0 semantic/metrics mapping in the Compose theme and settings grouping surfaces.
- Added repository-level policy verification and an evidence-gated rebuild roadmap.

## Building and source

The source is Android/Gradle, with the pinned `platform_frameworks_libs_systemui` submodule. Clone recursively:

```bash
git clone --recurse-submodules https://github.com/GoreeCloud/launcher.git
```

Use the Development branch while integration is underway; do not treat `main` as the rebuilt launcher unless it is separately accepted and merged. Reproduce the upstream debug build only after checking local SDK, Gradle, manifest, feature variants and dependency trust. No production APK or protected signing configuration is provided by this source import.

## Governance and status

See [planned features and capabilities](docs/PLANNED-FEATURES.md), [rebuild plan](docs/REBUILD_PLAN.md), [Glaze mapping](docs/GLAZE_ANDROID_CONTRACT.md), and [upstream provenance/licenses](docs/UPSTREAM_PROVENANCE.md). The canonical design authority is [GoreeCloud/glaze](https://github.com/GoreeCloud/glaze). The original upstream notices and `LICENSE.txt` are retained. No endorsement by upstream Lawnchair contributors is implied.

GoreeCloud privacy, security, accessibility, testing, package and release standards govern future modifications. Imported behavior remains subject to audit before the product may be shipped.
