# Launcher Development APK signing and delivery

Status: **Development — signing setup pending**  
Scope: standalone `GoreeCloud/launcher`, Android package `com.goreecloud.launcher.dev`.  
This is a release-engineering guide, **not** evidence that a signed Development APK is ready.

## Non-negotiable safety boundaries

- An APK's versionCode increasing does **not** establish update compatibility. Android must also verify the same trusted signing certificate (or a validated supported signer lineage) and package ID.
- Earlier owner Drive APK `0.1.0-dev-CI1317` uses versionCode **1001317**. Standalone PR #6 CI run **37875889328** uses versionCode **2000018** but was signed with a **different** ephemeral CI key. Do not install the latter as a supposed update over CI1317.
- Never commit an unencrypted keystore, passwords, CI key material, service-account credentials, or their base64 encoding. Never put those values in pull-request logs, issues, build provenance, repository variables, or Google Drive documents.
- Do not advise uninstall as the normal upgrade path. Uninstalling the existing package deletes its local Launcher application data.
- Green Android CI does not prove physical personal+Work profile visibility, device performance, stable readiness, or update continuity.
- Owner-download APKs must be staged **as actual .apk files** in authorized `goreecloud@gmail.com` GoreeCloud Drive before a download URL is offered. A GitHub Actions ZIP artifact alone is insufficient.

## Protected persistent signing (to provision)

The native Gradle build already supports `GOREECLOUD_DEV_KEYSTORE_PATH`, `GOREECLOUD_DEV_KEYSTORE_PASSWORD`, `GOREECLOUD_DEV_KEY_ALIAS`, and `GOREECLOUD_DEV_KEY_PASSWORD`. The manual workflow `.github/workflows/launcher-development-signed-apk.yml` uses these values only after they are provisioned through the dedicated GitHub Environment `launcher-development-signing`.

Configure the following **Environment secrets** through GitHub's protected settings; do not copy their contents into source:

- `LAUNCHER_DEVELOPMENT_KEYSTORE_BASE64`: base64 of the **protected persistent** Development signing keystore.
- `LAUNCHER_DEVELOPMENT_STORE_PASSWORD`: its protected store password.
- `LAUNCHER_DEVELOPMENT_KEY_ALIAS`: the designated Development key alias.
- `LAUNCHER_DEVELOPMENT_KEY_PASSWORD`: the designated key password.

Configure one **Environment variable**:

- `LAUNCHER_DEVELOPMENT_CERT_SHA256`: independently verified SHA-256 **certificate** digest for the intended persistent key, 64 lowercase hex characters. This is a public trust pin, **not** a password.

Prefer an already-authorized persistent Development key if one exists and can be verified. Otherwise generate and back up a new, protected Development key through approved owner-controlled key custody. A new key cannot update an existing install signed with another certificate. Establish the key's secure custody, backup/recovery, and fingerprint before publishing any signed Development artifact.

The dedicated signing workflow is deliberately **manual, main-only, SHA-bound and fail-closed**. It cannot use a pull-request build's unreviewed source with signing secrets. It requires the exact current protected `main` SHA and a new monotonically increasing versionCode greater than **2000018**. It verifies the Android package identity, version name/code, cryptographic APK signing and pinned signer fingerprint; the resulting APK, checksum and provenance remain a candidate until device acceptance and authorized Drive staging.

## Delivery and repeat-install verification

1. Merge the validated implementation through branch protection; check exact-head reviews and applicable CI, and verify `main` readback.
2. Provision the signing environment through authorized settings; keep backup material out of repository, chat, artifacts and plain Drive files.
3. Dispatch the signing workflow on protected `main` with its exact SHA and an unused higher versionCode. Inspect all steps and the recorded artifact SHA-256 and certificate digest.
4. Verify package ID, versionCode, signing cert and APK checksum independently. Confirm that the **installed** package uses the same signer before trying update-in-place. If different, **do not** attempt that as an in-place update; retain the owner's data and plan a documented transition or separate QA package.
5. Check upgrade behavior and App Drawer completeness across repeated opens with the personal and Work/Shelter profiles enabled. Include profile quiet/active transitions and uninstall/disable events, visual accessibility and power/performance sampling. Record unresolved discrepancies in issue #4.
6. Upload the exact approved APK (not merely a ZIP) to the authorized GoreeCloud Drive Launcher delivery folder; verify the Drive file identity, bytes/checksum and accessible owner download URL.
7. Keep the lifecycle label **Development** until all actual device, security, privacy, signing and release acceptance gates pass.

This procedure does **not** permit claiming the new pinned key is compatible with the earlier CI1317 installation unless the two actual signing certificates or supported lineages are independently verified to match.
