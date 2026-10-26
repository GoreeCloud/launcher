#!/usr/bin/env bash
# Opt-in smoke for a personal physical Android device. Never changes the default Home role.
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Usage: GOREELAUNCHER_DEVICE_SMOKE_ACK=YES [ANDROID_SERIAL=<authorized-device>] bash ci/physical-device-smoke.sh /path/to/app-debug.apk
Checks native APK install, cold launch, force-stop/relaunch and unchanged default Home.
Runs only on an explicitly connected physical device. Refuses to overwrite an
existing GoreeCloud Launcher install. Uninstalls its own temporary test package.
No app inventory, screenshots, device identifiers or personal UI data are saved.
This is NOT user-consented default Home acceptance or a release certification.
USAGE
}
if [[ "${1:-}" == "--help" ]]; then usage; exit 0; fi
if [[ $# -ne 1 ]]; then usage >&2; exit 64; fi
if [[ "${GOREELAUNCHER_DEVICE_SMOKE_ACK:-}" != "YES" ]]; then
  echo "REFUSED: set GOREELAUNCHER_DEVICE_SMOKE_ACK=YES for an authorized device." >&2
  exit 65
fi
command -v adb >/dev/null || { echo "REFUSED: adb unavailable" >&2; exit 66; }
apk="$1"
[[ -r "$apk" && "$apk" == *.apk ]] || { echo "REFUSED: supply a readable APK path." >&2; exit 66; }
package="com.goreecloud.launcher"

# Select exactly one currently online and authorized device. Never guess among devices.
mapfile -t active < <(adb devices | awk 'NR>1 && $2=="device" {print $1}')
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  serial="$ANDROID_SERIAL"
  printf '%s\n' "${active[@]}" | grep -Fxq -- "$serial" ||
    { echo "REFUSED: selected device is not connected and authorized." >&2; exit 67; }
elif [[ "${#active[@]}" -eq 1 ]]; then
  serial="${active[0]}"
else
  echo "REFUSED: connect exactly one device or select ANDROID_SERIAL." >&2
  exit 67
fi
adbq() { adb -s "$serial" "$@"; }
[[ "$(adbq get-state)" == "device" ]] ||
  { echo "REFUSED: device not ready." >&2; exit 67; }
if [[ "$(adbq shell getprop ro.kernel.qemu | tr -d '\r')" == "1" ]]; then
  echo "REFUSED: this script is for physical devices; use the emulator CI instead." >&2
  exit 67
fi
if adbq shell pm path "$package" | grep -q '^package:'; then
  echo "REFUSED: Launcher already installed. Existing user data will not be touched." >&2
  exit 68
fi

home_cmd=(shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME)
home_before="$(adbq "${home_cmd[@]}" | tr -d '\r' | tail -n 1)"
[[ -n "$home_before" && "$home_before" != *"No activity found"* ]] ||
  { echo "REFUSED: cannot establish current default Home." >&2; exit 69; }

installed=0
cleanup() {
  local rc=$?
  trap - EXIT
  if [[ "$installed" == "1" ]]; then
    # Only return to the existing Home, never assign or revoke the Home role.
    adbq shell input keyevent 3 >/dev/null 2>&1 || true
    if ! adbq uninstall "$package" >/dev/null 2>&1; then
      echo "CLEANUP REQUIRED: test APK may remain installed. No Home-role command ran." >&2
      rc=1
    elif adbq shell pm path "$package" | grep -q '^package:'; then
      echo "CLEANUP FAILED: temporary test package is still installed." >&2
      rc=1
    fi
  fi
  if (( rc == 0 )); then echo "PASS: native install/start/restart and test APK removal; default Home unchanged."; fi
  exit "$rc"
}
trap cleanup EXIT

echo "Preflight complete; physical Android API $(adbq shell getprop ro.build.version.sdk | tr -d '\r')."
adbq install --no-streaming "$apk" >/dev/null
installed=1
echo "Installed disposable evaluation APK; checking launch."
output="$(adbq shell am start -W -n "$package/.LauncherActivity")"
grep -Eq 'Status:[[:space:]]*ok' <<<"$output" ||
  { echo "FAIL: native cold-start did not report success." >&2; exit 1; }
[[ -n "$(adbq shell pidof "$package" | tr -d '\r')" ]] ||
  { echo "FAIL: app process absent after cold launch." >&2; exit 1; }

adbq shell am force-stop "$package"
output="$(adbq shell am start -W -n "$package/.LauncherActivity")"
grep -Eq 'Status:[[:space:]]*ok' <<<"$output" ||
  { echo "FAIL: native relaunch did not report success." >&2; exit 1; }
[[ -n "$(adbq shell pidof "$package" | tr -d '\r')" ]] ||
  { echo "FAIL: app process absent after relaunch." >&2; exit 1; }

home_after="$(adbq "${home_cmd[@]}" | tr -d '\r' | tail -n 1)"
[[ "$home_after" == "$home_before" ]] ||
  { echo "FAIL: selected Home changed during testing; manual inspection required." >&2; exit 1; }
