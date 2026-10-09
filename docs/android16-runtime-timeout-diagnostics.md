# Android 16 runtime artifact analyzer

**Status:** Development tooling only. This utility does not change Launcher runtime
behavior, Android permissions, test results, or release qualifications.

When an exact-head `Launcher Android CI` instrumentation job fails, obtain its
`launcher-android16-runtime-reports` ZIP artifact from GitHub Actions.

Run the analyzer locally:

```bash
python3 scripts/analyze_android16_runtime.py launcher-android16-runtime-reports.zip
```

The analyzer reads the Gradle exit status, AndroidJUnitRunner progress log, and
runtime logcat captured in that ZIP. It outputs aggregate counts, the unfinished
test name(s), and the last `LauncherRuntimeFixture` stage. Raw logcat is never
printed. Missing archive entries produce an error rather than an inferred pass.

Regression tests:

```bash
python3 -m unittest discover -s scripts -p 'test_*.py'
```

These Python tests are now invoked by the Android CI workflow on PR candidates.
A local Python-test pass is not evidence of Android instrumentation success.

## Documented intermittent blocker

The failure of [Android CI run 37915691839](https://github.com/GoreeCloud/launcher/actions/runs/37915691839)
on drawer [PR #6](https://github.com/GoreeCloud/launcher/pull/6) was exit 124
after two completed tests out of 71. The next started test was
`emptyHomeLongPressAlwaysOpensHomeEditor`, with the last fixture stage
`prepare:awaiting-home-pages`. No assertion failure was observed before the
outer timeout. Other exact-head Android 16 suites have passed; the underlying
Room/Home fixture non-progress remains unresolved.

Canonical tracking: [issue #3](https://github.com/GoreeCloud/launcher/issues/3).
Never skip or suppress the hanging test or treat intermittent green runs as
proof of root-cause resolution. Fix and verify the issue through independent
source review, exact-head full Android CI and protected-main follow-up.
