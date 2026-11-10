#!/usr/bin/env bash
# Synthetic ADB only: never connect to a device or invoke a real launcher.
set -Eeuo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
cat >"$tmp/adb" <<'SH'
#!/usr/bin/env bash
printf '%s\n' "$*" >>"$GC_ADB_FAKE_LOG"
printf '%s\n' "${ANDROID_SERIAL:-unset}" >>"${GC_ADB_FAKE_LOG}.serials"
if [[ "${ANDROID_SERIAL:-}" != emulator-5554 ]]; then
  echo "Unsafe inherited ADB selector" >&2
  exit 94
fi
case "$*" in
  "-s emulator-5554 get-state") printf '%s\n' "${GC_ADB_STATE:-device}" ;;
  "-s emulator-5554 shell getprop ro.kernel.qemu") printf '%s\n' "${GC_ADB_QEMU:-1}" ;;
  "-s emulator-5554 shell getprop ro.build.version.sdk") printf '%s\n' "${GC_ADB_SDK:-35}" ;;
  *) printf 'Fixture blocks all non-admission ADB calls\n' >&2; exit 93 ;;
esac
SH
chmod 700 "$tmp/adb"
export PATH="$tmp:$PATH" GC_ADB_FAKE_LOG="$tmp/adb-log"
scripts=(
  ci/emulator-backup.sh ci/emulator-dock.sh
  ci/emulator-dock-accessibility.sh ci/emulator-icon-refresh.sh
  ci/emulator-keyboard.sh
)
for script in "${scripts[@]}"; do
  for scenario in offline physical wrong_api invalid_sdk; do
    export GC_ADB_STATE=device GC_ADB_QEMU=1 GC_ADB_SDK=35 ANDROID_SERIAL=physical-serial
    case "$scenario" in
      offline) GC_ADB_STATE=offline ;;
      physical) GC_ADB_QEMU=0 ;;
      wrong_api) GC_ADB_SDK=36 ;;
      invalid_sdk) GC_ADB_SDK='35\n36' ;;
    esac
    : >"$GC_ADB_FAKE_LOG"
    if bash "$script" >"$tmp/stdout" 2>"$tmp/stderr"; then
      echo "UNSAFE: $script admitted $scenario" >&2; exit 1
    fi
    if ! grep -Fq 'Launcher QA device admission:' "$tmp/stderr"; then
      echo "Missing fail-closed admission evidence: $script $scenario" >&2; exit 1
    fi
    if grep -Ev '^-s emulator-5554 (get-state|shell getprop ro.kernel.qemu|shell getprop ro.build.version.sdk)$' "$GC_ADB_FAKE_LOG"; then
      echo "UNSAFE: $script mutated a rejected $scenario device" >&2; exit 1
    fi
  done
  # On a simulated valid hosted Android 35 AVD, the next script operation must
  # reach the fake ADB installation, proving the guard did not silently block
  # every script. The fake ADB refuses the install, so no mutation occurs.
  export GC_ADB_STATE=device GC_ADB_QEMU=1 GC_ADB_SDK=35 ANDROID_SERIAL=physical-serial
  : >"$GC_ADB_FAKE_LOG"
  if bash "$script" >"$tmp/stdout" 2>"$tmp/stderr"; then
    echo "UNSAFE: synthetic ADB executed the entire $script" >&2; exit 1
  fi
  if ! grep -q '^install -r ' "$GC_ADB_FAKE_LOG"; then
    echo "Guard failed to admit authorized synthetic Android 35 target: $script" >&2; exit 1
  fi
  if grep -vx 'emulator-5554' "${GC_ADB_FAKE_LOG}.serials"; then
    echo "Inherited real-device selector was not overridden: $script" >&2; exit 1
  fi
done
echo "PASS: five Android 35 Launcher scripts reject offline, non-QEMU, invalid-API and inherited device selectors before mutation"
