# GoreeCloud Launcher — Pull-down Universal Search and layout onboarding

## Development scope

- Swipe down on empty Home space opens `GoreeCloudUniversalSearchActivity`, a translucent Glaze card animated downward over the existing Home screen rather than transitioning into All Apps. The native All Apps search remains independently available.
- Three selectable, keyboard-accessible tabs: **Frequent**, **Recent**, **New/Updated**.
- Local app launches with partial-name matching in the pull-down; app labels and Android icons. New/Updated uses installed package first-install / update timestamps. Frequent / Recent uses permitted Android Usage Access, with locally recorded Search launches as a fallback. No telemetry, cloud uploads, or implicit web query transmission. Web submission requires the user pressing Search or selecting the web action. Hidden-app search preferences and personal-profile isolation are honored.
- Home icon/widget vertical gestures retain priority. Back and tap-outside dismiss the overlay. Optional keyboard autofocus is governed by existing Universal Search preference.
- About is GoreeCloud branded, displays the product icon, version, development channel and authorized repo link. Lawnchair team/news/donate/promotional content is not displayed. **Open-source licenses and upstream acknowledgements remain available**.
- Onboarding is six steps: Welcome; Home content; **Layout** (independent Home, Dock, App Drawer 4/5/6 columns); Appearance; Universal Search; Privacy. Default is 5 columns for all three areas. Existing stored choices are preserved on upgrade; setup only writes after Finish. Home's ranked shortcut placement uses the selected Home columns and avoids occupied cells.

## Acceptance remaining

Physical-device regression required before release: swipe direction and top-sheet animation, Android Back, touch exclusion near widgets, IME shown/hidden, hardware keyboard, right-to-left layout, display sizes and landscape, 4/5/6 grid first-run and existing-user migrations, New/Updated timestamp ranking, permission denial, no network side-effects of local searches, TalkBack ordering, source-private/hidden apps, and official icon render. Passing source checks and Android CI do not prove device UX.

## Release boundary

Keep in Development. Do not merge/promote Stable or claim upgrade-safe QA signing without pinned persistent Development certificate. Verified APK must be copied to authorized GoreeCloud Drive for normal delivery.
