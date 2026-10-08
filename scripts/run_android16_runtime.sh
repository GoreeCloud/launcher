#!/usr/bin/env bash
set -euo pipefail

runtime_dir="${GITHUB_WORKSPACE:?}/apps/launcher/ci-runtime"
mkdir -p "$runtime_dir"

set +e
timeout --signal=TERM --kill-after=30s 20m \
  gradle --project-dir "$GITHUB_WORKSPACE/apps/launcher" --no-daemon connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.timeout_msec=60000
status=$?
set -e

printf 'gradle_exit_status=%s\n' "$status" > "$runtime_dir/status.txt"

if [ "$status" -ne 0 ]; then
  app_pid="$(adb shell pidof com.goreecloud.launcher.dev 2>/dev/null | tr -d '\r' | awk '{print $1}')"
  if [ -n "$app_pid" ]; then
    adb shell kill -3 "$app_pid" || true
    sleep 2
  fi
  adb shell uiautomator dump /sdcard/launcher-runtime-ui.xml >/dev/null 2>&1 || true
  adb pull /sdcard/launcher-runtime-ui.xml "$runtime_dir/ui.xml" >/dev/null 2>&1 || true
  adb logcat -d > "$runtime_dir/logcat.txt" || true
  adb shell dumpsys activity activities > "$runtime_dir/activity.txt" || true
  adb shell dumpsys window windows > "$runtime_dir/window.txt" || true
fi

exit "$status"
