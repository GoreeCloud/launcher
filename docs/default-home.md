# GoreeCloud Launcher — default Home selection

GoreeCloud Launcher is a native Android HOME activity. Its Settings screen offers explicit, user-directed default-Home selection; the app never silently changes the device's default launcher.

## Android behavior

On Android 10 (API 29) or newer, Settings checks `RoleManager.ROLE_HOME` availability and whether GoreeCloud holds the role. If available and not held, a tap on **Select default Home app** launches `RoleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)`. Android—not the app—presents qualified Home apps and requires a user selection and confirmation. If already held, Settings shows **Current default Home: GoreeCloud**, changes the action to **Manage default Home apps**, and opens the device's Home-app settings. When the role is unavailable or the device runs API 26–28, Settings falls back to `Settings.ACTION_HOME_SETTINGS`. A role response or return to Settings refreshes the displayed status.

The ordinary Launcher APK requires no system/privileged permissions for this flow; Android manages role grants and revocation. `SettingsActivity` remains **not exported**, so only the app itself can open that private screen.

## Verification and limitations

[Native CI 40256236019](https://github.com/GoreeCloud/launcher/actions/runs/40256236019) passed the initial consent implementation. [Android 15 emulator run 40256236011](https://github.com/GoreeCloud/launcher/actions/runs/40256236011) completed successfully on a Pixel 6 AVD: it confirmed GoreeCloud was not initially selected, entered the real Android consent dialog, selected GoreeCloud and **Set as default**, read back `com.goreecloud.launcher` as the Home role holder, returned Home, and observed the updated Settings indicator. The AVD's Pixel Launcher initially displayed an unrelated ANR; the QA script dismisses that test-environment dialog without shell-assignment of HOME. Test screenshots and role-holder outputs are kept in the workflow's QA artifact.

This is single-emulator validation, not evidence of compatibility with every OEM Home implementation or every Android version. A device owner must choose and can later revoke or change the default Home app. Do not interpret this evidence as Stable/release acceptance.

Platform reference: https://developer.android.com/reference/android/app/role/RoleManager
