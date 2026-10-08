# Launcher migration origin

> **Verified October 8, 2026:** Standalone Launcher PR #1 merged as `a159412507532c0d3314057fab24d138dff131ac` after successful Android 16/runtime/build/contract validation. The old monorepo directory was retired by protected PR #291 at `068a958074431fabecff8748c9b630ea9dfdf6b9`. Historical standalone deletion requirements below are superseded. All previous code remains in Git history; lifecycle is Development.


> **Historical migration record:** This document describes September 29, 2026 consolidation. The October 8, 2026 owner directive restores `GoreeCloud/launcher` as the independent development repository and supersedes the old retirement instruction below. Current standalone source is extracted from monorepo `apps/launcher/` at commit `9097c8cb7adecc3c969f162fc33a9e8fdde20cc4`, with exact source-tree SHA `d6cafd0af17b2cf9bc69aabbc56b5a08588b403c`.


GoreeCloud Launcher is mandatorily migrated into `GoreeCloud/android-app-defaults`.

- Source repository: `GoreeCloud/launcher`
- Imported Development revision: `1eb8dd6d8178f6d730b100559e8f8d149501d18c`
- Legacy cutover pull request: `GoreeCloud/launcher#248` (closed as superseded; branch retained temporarily pending reconciliation and repository deletion)
- Destination: `apps/launcher/`
- Import exclusions: repository-scoped `.github/` metadata and generated `artifacts/` output.
- Source validation at cutover: exact-head Android CI was still running when the cutover was frozen. The immediately preceding head had a failing Android 16 Room/runtime test and was not used as the final cutover revision.
- Lifecycle boundary: this is a Development source migration. It does not establish Release Candidate, Stable, production, representative-device, or other unverified acceptance.
- Retirement boundary: the standalone repository is a temporary migration source only. After all unique required branch/workflow/reference material is reconciled, the standalone repository must be deleted and deletion verified in GitHub.

Repository-scoped legacy workflows are not nested here because GitHub would not execute them from an application subdirectory. Destination-root CI validates the migrated application from this path.
