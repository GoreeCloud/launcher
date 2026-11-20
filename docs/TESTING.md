# Verification guide and evidence boundaries

CI should run isolated unit/type/build checks:
- Web: `cd apps/web && npm install && npm test && npm run build`.
- Linux: `python -m compileall -q apps/linux && python -m unittest discover -s apps/linux/tests`. Qt runtime tests require a native display session; these checks are intentionally limited.
- Android: `cd apps/android && gradle :app:assembleDebug :app:testDebugUnitTest --no-daemon`, with JDK 17, Gradle 9.4.1 and Android SDK platform 36.

CI/build success would **not** establish on-device HOME role, icon, settings, thermal, accessibility, USB, work/private profile, uninstall fallback, or OEM navigation behavior.

Repository review on 2026-11-20:
- `main` initially had only `README.md`.
- API `/rulesets` returned `[]`; `/branches/main/protection` returned 403 (integration lacks admin endpoint). Do **not** call this proof that every branch protection control is absent.
- Create a reviewable pull request first. Do not push directly to main. A principal with verified settings access must establish the founder-required protected-main, CI checks, no force push/delete and no bypass controls before merging.
