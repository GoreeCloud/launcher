#!/usr/bin/env bash
set -euo pipefail

runtime_dir="${GITHUB_WORKSPACE:?}/ci-runtime"
mkdir -p "$runtime_dir"

set +e
timeout --signal=TERM --kill-after=30s 20m \
  gradle --project-dir "$GITHUB_WORKSPACE" --no-daemon connectedDebugAndroidTest
status=$?
set -e

printf 'gradle_exit_status=%s\n' "$status" > "$runtime_dir/status.txt"

if [ "$status" -ne 0 ]; then
  # ADB shell and the debug app have different Linux UIDs on Android 16.
  # Signal only the verified Launcher Development process through its own
  # debuggable package identity, never through elevated/root privileges.
  app_pid="$(adb shell pidof com.goreecloud.launcher.dev 2>/dev/null | tr -d '\r' | awk '{print $1}')"
  printf 'app_pid=%s\n' "$app_pid" > "$runtime_dir/stack-request.txt"
  if [[ "$app_pid" =~ ^[0-9]+$ ]]; then
    if adb shell run-as com.goreecloud.launcher.dev kill -3 "$app_pid" >> "$runtime_dir/stack-request.txt" 2>&1; then
      printf 'sigquit=sent-by-app-uid\n' >> "$runtime_dir/stack-request.txt"
      sleep 2
    else
      printf 'sigquit=unavailable\n' >> "$runtime_dir/stack-request.txt"
    fi
  else
    printf 'sigquit=no-valid-app-pid\n' >> "$runtime_dir/stack-request.txt"
  fi
  adb shell uiautomator dump /sdcard/launcher-runtime-ui.xml >/dev/null 2>&1 || true
  adb pull /sdcard/launcher-runtime-ui.xml "$runtime_dir/ui.xml" >/dev/null 2>&1 || true
  adb logcat -d > "$runtime_dir/logcat.txt" || true
  adb shell dumpsys activity activities > "$runtime_dir/activity.txt" || true
  adb shell dumpsys window windows > "$runtime_dir/window.txt" || true
fi

exit "$status"
