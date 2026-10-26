#!/usr/bin/env bash
set -Eeuo pipefail
# Dependency-free contract check for bounded emulator context-menu recovery.
EVIDENCE="$(mktemp -d)"
trap 'rm -rf "$EVIDENCE"' EXIT
scenario=eventual
presses=0
dump_screen() {
  local name="$1"
  if [[ "$name" == menu && "$scenario" == eventual && "$presses" -ge 2 ]]; then
    printf '<node text="Pin to dock"/>\n' > "$EVIDENCE/$name.xml"
  else
    printf '<node text="All applications"/>\n' > "$EVIDENCE/$name.xml"
  fi
}
longpress_label() {
  test "$2" = "Fixture"
  presses=$((presses + 1))
}
assert_text() { grep -Fq "text=\"$2\"" "$EVIDENCE/$1.xml"; }
sleep() { :; }
. ci/emulator-menu-retry.sh

retry_longpress_menu menu Fixture "Pin to dock"
test "$presses" -eq 2 || { echo "Expected second-attempt success" >&2; exit 1; }

scenario=never
presses=0
if retry_longpress_menu menu Fixture "Pin to dock"; then
  echo "Missing real menu was incorrectly accepted" >&2
  exit 1
fi
test "$presses" -eq 3 || { echo "Retry count must be exactly three" >&2; exit 1; }
echo "PASS context-menu bounded retry and fail-closed behavior"
