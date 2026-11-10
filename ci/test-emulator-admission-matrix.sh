#!/usr/bin/env bash
# Disposable-target preflight across API 26/28 legacy, 35 responsive, 35/36 smoke.
# All ADB calls are mocked; zero real devices or package changes.
set -Eeuo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
cat >"$tmp/adb" <<'SH'
#!/usr/bin/env bash
printf '%s\n' "$*" >>"$GC_MOCK_ADB_LOG"
printf '%s\n' "${ANDROID_SERIAL:-unset}" >>"${GC_MOCK_ADB_LOG}.serials"
if [[ "${ANDROID_SERIAL:-}" != emulator-5554 ]]; then
  echo 'Blocked inherited/physical ADB device selector' >&2; exit 92
fi
case "$*" in
  "-s emulator-5554 get-state") printf '%s\n' "${GC_MOCK_STATE:-device}" ;;
  "-s emulator-5554 shell getprop ro.kernel.qemu") printf '%s\n' "${GC_MOCK_QEMU:-1}" ;;
  "-s emulator-5554 shell getprop ro.build.version.sdk") printf '%s\n' "${GC_MOCK_SDK:-35}" ;;
  *) echo 'Mock forbids install/role/setting changes' >&2; exit 93 ;;
esac
SH
chmod 700 "$tmp/adb"
export PATH="$tmp:$PATH" GC_MOCK_ADB_LOG="$tmp/adb-log"
scripts=(
  "ci/emulator-legacy.sh|26 28|35"
  "ci/emulator-responsive.sh|35|26"
  "ci/emulator-smoke.sh|35 36|26"
)
for entry in "${scripts[@]}"; do
  IFS='|' read -r script accepted rejected <<<"$entry"
  for case_name in offline non_qemu invalid_api wrong_api; do
    export GC_MOCK_STATE=device GC_MOCK_QEMU=1 GC_MOCK_SDK="$rejected" ANDROID_SERIAL=physical-test-device
    case "$case_name" in
      offline) GC_MOCK_STATE=offline ;;
      non_qemu) GC_MOCK_QEMU=0 ;;
      invalid_api) GC_MOCK_SDK=bad ;;
      wrong_api) : ;;
    esac
    : >"$GC_MOCK_ADB_LOG"
    : >"${GC_MOCK_ADB_LOG}.serials"
    if bash "$script" >"$tmp/out" 2>"$tmp/err"; then
      echo "UNSAFE accepted $case_name for $script" >&2; exit 1
    fi
    grep -Fq 'Launcher QA device admission:' "$tmp/err" || {
      echo "Missing fail-closed rejection: $script $case_name" >&2; exit 1;
    }
    if grep -Ev '^-s emulator-5554 (get-state|shell getprop ro.kernel.qemu|shell getprop ro.build.version.sdk)$' "$GC_MOCK_ADB_LOG"; then
      echo "Mutation before admission: $script $case_name" >&2; exit 1
    fi
    if grep -vx emulator-5554 "${GC_MOCK_ADB_LOG}.serials"; then
      echo "Wrong serial used on rejected target" >&2; exit 1
    fi
  done
  for api in $accepted; do
    export GC_MOCK_STATE=device GC_MOCK_QEMU=1 GC_MOCK_SDK="$api" ANDROID_SERIAL=physical-test-device
    : >"$GC_MOCK_ADB_LOG"
    : >"${GC_MOCK_ADB_LOG}.serials"
    if bash "$script" >"$tmp/out" 2>"$tmp/err"; then
      echo "UNSAFE test ran with mock ADB: $script/$api" >&2; exit 1
    fi
    if ! grep -q '^install -r ' "$GC_MOCK_ADB_LOG"; then
      echo "Expected guarded script to reach mock installation: $script/$api" >&2; exit 1
    fi
    if grep -vx emulator-5554 "${GC_MOCK_ADB_LOG}.serials"; then
      echo "Inherited device was not pinned: $script/$api" >&2; exit 1
    fi
  done
done
echo "PASS: legacy (26/28), responsive (35) and smoke (35/36) QA refuses unverified ADB devices before mutation"
