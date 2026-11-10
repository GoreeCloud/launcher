#!/usr/bin/env bash
# All ADB operations below are synthetic function calls; no device is contacted.
set -Eeuo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
EVIDENCE="$(mktemp -d)"
trap 'rm -rf "$EVIDENCE"' EXIT
scenario=transient
attempts=0
screens=0
adb() {
  case "$*" in
    "shell uiautomator dump /sdcard/launcher-hierarchy.xml")
      attempts=$((attempts + 1)); return 0 ;;
    "pull /sdcard/launcher-hierarchy.xml "*)
      local dest="${*: -1}"
      printf '<hierarchy/>\n' > "$dest"; return 0 ;;
    "shell screencap -p /sdcard/launcher-screen.png")
      screens=$((screens + 1))
      if [[ "$scenario" == never || ( "$scenario" == transient && "$screens" -lt 3 ) ]]; then
        return 1
      fi
      return 0 ;;
    "pull /sdcard/launcher-screen.png "*)
      local dest="${*: -1}"
      printf 'synthetic-png-evidence\n' > "$dest"; return 0 ;;
    *)
      echo "Unexpected ADB invocation in synthetic test: $*" >&2; return 99 ;;
  esac
}
sleep() { :; }
source ci/emulator-home-role-visibility.sh
dump_disposable_home_snapshot retry-pair
[[ "$attempts" -eq 3 && "$screens" -eq 3 ]] || {
  echo 'Snapshot did not retry two transient failures' >&2; exit 1;
}
[[ -s "$EVIDENCE/retry-pair.xml" && -s "$EVIDENCE/retry-pair.png" ]] || {
  echo 'Complete snapshot pair absent' >&2; exit 1;
}
scenario=never; attempts=0; screens=0
if dump_disposable_home_snapshot never-pair; then
  echo 'Offline emulator was accepted without screenshot evidence' >&2
  exit 1
fi
[[ "$attempts" -eq 4 && "$screens" -eq 4 ]] || {
  echo 'Snapshot did not respect four-attempt deadline' >&2; exit 1;
}
[[ ! -e "$EVIDENCE/never-pair.png" ]] || {
  echo 'Unexpected completed screenshot on failed operation' >&2; exit 1;
}
echo 'PASS: transient screenshot recovery is bounded and persistent ADB failure remains denied'
