#!/usr/bin/env bash
# Pure synthetic ADB: prove Settings QA refuses non-QEMU/incorrect API/offline
# BEFORE any installation, display edits, tap, or filesystem modification.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
cat > "$tmp/adb" <<'SH'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "$GC_SETTINGS_QA_TRACE"
case "$*" in
  "-s emulator-5554 get-state") printf '%s\n' "${GC_STATE:-device}" ;;
  "-s emulator-5554 shell getprop ro.kernel.qemu")
    printf '%s\n' "${GC_QEMU:-1}" ;;
  "-s emulator-5554 shell getprop ro.build.version.sdk")
    printf '%s\n' "${GC_API:-35}" ;;
  *) echo 'UNSAFE: attempted non-preflight ADB action' >&2; exit 99 ;;
esac
SH
chmod 700 "$tmp/adb"
export PATH="$tmp:$PATH"
export GC_SETTINGS_QA_TRACE="$tmp/calls"
for mode in physical wrong-api offline; do
  : > "$GC_SETTINGS_QA_TRACE"
  export GC_STATE=device GC_QEMU=1 GC_API=35
  case "$mode" in
    physical) GC_QEMU=0 ;;
    wrong-api) GC_API=36 ;;
    offline) GC_STATE=offline ;;
  esac
  if bash ci/emulator-settings-font.sh > "$tmp/output" 2>&1; then
    echo "Settings QA improperly accepted $mode" >&2
    exit 1
  fi
  grep -Fq 'Refusing Launcher Settings QA outside the verified Android 35 emulator' "$tmp/output"
  if grep -Ev '^-s emulator-5554 (get-state|shell getprop ro.kernel.qemu|shell getprop ro.build.version.sdk)$' "$GC_SETTINGS_QA_TRACE"; then
    echo "Unsafe Settings ADB operation before emulator preflight" >&2
    exit 1
  fi
  [[ -s "$GC_SETTINGS_QA_TRACE" ]]
done
echo 'PASS: Settings QA refused non-emulator, wrong Android API and offline target without device changes'
