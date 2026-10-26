# GoreeCloud Launcher — Seal Readiness Gate

| Control | Value |
| --- | --- |
| Product | GoreeCloud Launcher |
| Maintainer | GoreeCloud founder |
| Snapshot date | October 26, 2026 |
| Source authority | [GoreeCloud/launcher](https://github.com/GoreeCloud/launcher) and its actual CI/device evidence |
| Feature scope authority | [Planned features](https://drive.google.com/file/d/1ossnWKM42HDieuee5jihvmIky1WkaEUG/view) |
| Implemented-feature authority | [Verified feature register](https://drive.google.com/file/d/1aZiSsnF4MhbFwUkXH3v7SHhqgJONcQUz/view) |
| Open acceptance work | [Native runtime, UI, accessibility #1](https://github.com/GoreeCloud/launcher/issues/1) and [Workspace/widget implementation #2](https://github.com/GoreeCloud/launcher/issues/2) |
| Decision | **Not Sealed.** Bounded engineering acceptance has passed; production-level native acceptance remains incomplete. |

## Meaning of Seal

A Seal decision would certify a specifically identified Android package, supported Android releases/devices, requirements and release scope against recorded evidence. It is not a synonym for a green compile, merge, smoke test, or available debug APK. No currently observed artifact is authorized to claim Stable, a production release, or universal Android support.

A Seal requires **all applicable release-critical checks to pass** against the identified release candidate, including hardware, accessibility, security, update/recovery and distribution. A future decision must record the package/build SHA, supported matrix, tests and evidence links. No gate may be waived solely to expedite launch. This file is a decision aid, not a substitute for provider-native CI results or the canonical Drive registers.

## Verified engineering evidence

The runnable source tested at `56434bc59d01dc858a8598367cd23dc195b121d8` passed the following `main` GitHub Actions runs:

| Gate | Evidence | Classification |
| --- | --- | --- |
| Android build, unit tests and evaluation APK | [Native CI 40395642539](https://github.com/GoreeCloud/launcher/actions/runs/40395642539) | Passed, build-only |
| Responsive Home and font/orientation QA | [Responsive 40395642546](https://github.com/GoreeCloud/launcher/actions/runs/40395642546) | Passed, bounded emulator |
| API 26/28 Home fallback and compatibility | [Legacy 40395642514](https://github.com/GoreeCloud/launcher/actions/runs/40395642514) | Passed, bounded emulator |
| App temporary unavailability/recovery | [Availability 40395642584](https://github.com/GoreeCloud/launcher/actions/runs/40395642584) | Passed, bounded emulator |
| Same-component live icon refresh | [Icon refresh 40395642457](https://github.com/GoreeCloud/launcher/actions/runs/40395642457) | Passed, bounded emulator |
| Dock move, accessibility-node actions and persistence | [Dock 40395642449](https://github.com/GoreeCloud/launcher/actions/runs/40395642449) | Passed, bounded emulator |
| External keyboard behavior | [Keyboard 40395642443](https://github.com/GoreeCloud/launcher/actions/runs/40395642443) | Passed, bounded emulator |

The subsequent `main` commit `9cfd87476ee4bb64395f0701e8ba71efea4e90ee` changes **only README.md** relative to that tested source, as confirmed by GitHub compare, and [CI 40396016855](https://github.com/GoreeCloud/launcher/actions/runs/40396016855) passed. Earlier failed runs were not erased; later successful runs supersede their *specific* test regressions, not every untested platform risk.

Other bounded evidence includes [user-consented Home selection/cancellation](https://github.com/GoreeCloud/launcher/actions/runs/40257203862), [real Android document-picker backup/restore](https://github.com/GoreeCloud/launcher/actions/runs/40251850733), [invalid backup rejection](https://github.com/GoreeCloud/launcher/actions/runs/40253831973), and [Settings 200% font validation](https://github.com/GoreeCloud/launcher/actions/runs/40328855610). These historical runs are **not** asserted to be fresh on the current source revision.

## Open blockers before Seal

- [ ] **Actual hardware:** Install and test the identified candidate on a representative physical Android device (including explicit consent and cancel for default Home selection) and at least one relevant OEM variation. Do not silently change a user's current default Home.
- [ ] **Accessibility:** Observe real TalkBack and switch-access navigation, focus announcements, action execution and recovery. UIAutomator node metadata and automated keyboard checks are necessary but do not replace assistive-technology sessions.
- [ ] **Device matrix:** Confirm applicable Android versions beyond API 26, 28 and 35, and evaluate real display sizes, rotation, OEM restrictions, background restarts, and performance/battery impact. Declare only supported combinations that pass.
- [ ] **Application recovery:** Verify system-home fallback, process-death/crash recovery, package uninstall/reinstall/update and unavailable-app handling on representative devices without losing valid local state.
- [ ] **Backup/data safety:** Validate additional Android document providers, denied/low-space writes, non-destructive cancellation, backup compatibility, destructive restore warnings and failure recovery on actual hardware.
- [ ] **Visual/interaction acceptance:** Capture real full-screen native UI evidence; audit Canvas/Surface/Lens consistency, scaled text, contrast, tap targets, overflow and system-bar insets across required screens/states.
- [ ] **Security and release engineering:** Review Android permissions, component exposure, local data handling and third-party dependencies; produce the intended signed, installable release package, distribution/upgrade path, and demonstrated rollback or recovery. The current CI artifact is explicitly a debug evaluation APK.
- [ ] **Release decision evidence:** Record release candidate version/commit/package digest, target support matrix, all current test results, acceptance exceptions, and explicit Seal decision in the authoritative issue and implemented-feature register.

## Guardrails and operating procedure

1. Keep `main` and existing tests truthful; investigate red runs instead of suppressing their assertions or reducing coverage.
2. Build and run the named candidate through tests appropriate to code changes and device support claims.
3. Use real-device approval for consequential settings changes (especially setting the user's default Home), backup replacement, and other personal-device mutations.
4. Reconcile outcomes in [issue #1](https://github.com/GoreeCloud/launcher/issues/1) and the [implemented-feature register](https://drive.google.com/file/d/1aZiSsnF4MhbFwUkXH3v7SHhqgJONcQUz/view). Preserve issue #2 for unimplemented product features; not every future planned feature is implicitly part of the first release, but any declared release scope must be complete.
5. Mark **Sealed** only once every mandatory check for the defined release scope is independently evidenced and outstanding blocker records are resolved. Until then use **Seal candidate / not released**, and do not imply unattended device verification.

**Recheck note:** This is a dated evidence snapshot. Consult current Actions runs, actual repository heads, devices, and canonical records before making any later status claim.
