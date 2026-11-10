#!/usr/bin/env bash
# Safety regression: synthetic ADB never connects to or modifies an Android device.
set -Eeuo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
cat > "$tmp/adb" <<'SH'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "$GC_FAKE_ADB_LOG"
case "$*" in
  "-s emulator-5554 get-state") printf '%s\n' "${GC_FAKE_ADB_STATE:-device}" ;;
  "-s emulator-5554 shell getprop ro.kernel.qemu") printf '%s\n' "${GC_FAKE_ADB_QEMU:-1}" ;;
  "-s emulator-5554 shell getprop ro.build.version.sdk") printf '%s\n' "${GC_FAKE_ADB_API:-35}" ;;
  *) echo 'UNSAFE: unexpected ADB command' >&2; exit 99 ;;
esac
SH
chmod 700 "$tmp/adb"
export GC_FAKE_ADB_LOG="$tmp/commands" PATH="$tmp:$PATH"
for case_name in offline not_emulator wrong_api; do
  export GC_FAKE_ADB_STATE=device GC_FAKE_ADB_QEMU=1 GC_FAKE_ADB_API=35
  case "$case_name" in
    offline) GC_FAKE_ADB_STATE=offline ;;
    not_emulator) GC_FAKE_ADB_QEMU=0 ;;
    wrong_api) GC_FAKE_ADB_API=36 ;;
  esac
  : > "$GC_FAKE_ADB_LOG"
  if bash ci/emulator-app-availability.sh >"$tmp/stdout" 2>"$tmp/stderr"; then
    echo "Unsafe target accepted: $case_name" >&2
    exit 1
  fi
  grep -Fq 'Refusing app availability QA outside the disposable Android 35 emulator' "$tmp/stderr"
  if grep -Ev '^-s emulator-5554 (get-state|shell getprop ro.kernel.qemu|shell getprop ro.build.version.sdk)$' "$GC_FAKE_ADB_LOG"; then
    echo "Unsafe ADB operation before target admission: $case_name" >&2; exit 1
  fi
  [[ -s "$GC_FAKE_ADB_LOG" ]] || { echo "No target guard probes" >&2; exit 1; }
done
echo 'PASS: app availability QA refuses offline, non-emulator and wrong-API targets before ADB mutation'
