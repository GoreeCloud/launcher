#!/usr/bin/env bash
# Offline synthetic check. Must never touch a real device or invoke Android role UI.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
cat > "$tmp/adb" <<'SH'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "$GC_HOME_QA_TRACE"
case "$*" in
  "-s emulator-5554 get-state") printf '%s\n' "${GC_STATE:-device}" ;;
  "-s emulator-5554 shell getprop ro.kernel.qemu")
    printf '%s\n' "${GC_QEMU:-1}" ;;
  "-s emulator-5554 shell getprop ro.build.version.sdk")
    printf '%s\n' "${GC_API:-35}" ;;
  *) echo 'UNSAFE: Home-role QA executed non-preflight ADB call' >&2; exit 90 ;;
esac
SH
chmod 700 "$tmp/adb"
export PATH="$tmp:$PATH" GC_HOME_QA_TRACE="$tmp/trace"
for mode in physical wrong-api offline; do
  : > "$GC_HOME_QA_TRACE"
  export GC_STATE=device GC_QEMU=1 GC_API=35
  case "$mode" in
    physical) GC_QEMU=0 ;;
    wrong-api) GC_API=36 ;;
    offline) GC_STATE=offline ;;
  esac
  if bash ci/emulator-home-role.sh > "$tmp/out" 2>&1; then
    echo "Home-role QA accepted an unsafe $mode device" >&2
    exit 1
  fi
  grep -Fq 'Refusing Home-role QA outside the verified disposable Android 35 emulator' "$tmp/out"
  if grep -Ev '^-s emulator-5554 (get-state|shell getprop ro.kernel.qemu|shell getprop ro.build.version.sdk)$' "$GC_HOME_QA_TRACE"; then
    echo "Unsafe ADB action before Home-role test preflight" >&2
    exit 1
  fi
  [[ -s "$GC_HOME_QA_TRACE" ]]
done
echo 'PASS: Home-role QA rejects personal, offline and wrong-API Android targets'
