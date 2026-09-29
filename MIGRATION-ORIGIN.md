# Launcher migration origin

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
