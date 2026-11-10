#!/usr/bin/env bash
# Source-only admission guard for disposable GitHub-hosted AVD QA.
# This does not attest host ownership: CI workflow isolation is still required.
# Must be called BEFORE any APK install, HOME role change or shell mutation.
assert_disposable_launcher_emulator() {
  local allowed="${1:-}" state qemu sdk
  case "$allowed" in
    "35"|"26 28"|"35 36") ;;
    *) printf 'Invalid Launcher QA emulator API allowlist\n' >&2; return 2 ;;
  esac
  # This export also pins subsequent child Python subprocess calls to adb.
  export ANDROID_SERIAL=emulator-5554
  state="$(command adb -s "$ANDROID_SERIAL" get-state 2>/dev/null | tr -d '\r')" || {
    printf 'Launcher QA device admission: emulator unavailable\n' >&2
    return 2
  }
  qemu="$(command adb -s "$ANDROID_SERIAL" shell getprop ro.kernel.qemu 2>/dev/null | tr -d '\r')" || {
    printf 'Launcher QA device admission: emulator property unavailable\n' >&2
    return 2
  }
  sdk="$(command adb -s "$ANDROID_SERIAL" shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')" || {
    printf 'Launcher QA device admission: SDK property unavailable\n' >&2
    return 2
  }
  if [[ "$state" != device || "$qemu" != 1 || ! "$sdk" =~ ^[0-9]{2}$ || " $allowed " != *" $sdk "* ]]; then
    printf 'Launcher QA device admission: non-disposable or wrong-API ADB target refused\n' >&2
    return 2
  fi
}
