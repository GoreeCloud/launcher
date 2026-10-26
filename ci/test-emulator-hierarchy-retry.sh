#!/usr/bin/env bash
set -Eeuo pipefail
# No Android device required. Test real XML validation and failure behavior.
case_dir="$(mktemp -d)"
trap 'rm -r "$case_dir"' EXIT
counter=0
scenario=recovers
adb() {
  if [[ "${1:-}" == shell && "${2:-}" == uiautomator && "${3:-}" == dump ]]; then
    counter=$((counter + 1))
    case "$scenario:$counter" in
      recovers:1|never:*) return 1 ;;
      invalid:1|invalid:2) printf '<hierarchy' > "$case_dir/hierarchy"; return 0 ;;
      *) printf '<hierarchy><node text="Home workspace"/></hierarchy>\n' > "$case_dir/hierarchy"; return 0 ;;
    esac
  fi
  if [[ "${1:-}" == pull ]]; then
    cp "$case_dir/hierarchy" "$3"
    return 0
  fi
  echo "Unexpected adb call" >&2
  return 1
}
sleep() { :; }
. ci/emulator-hierarchy-retry.sh

capture_hierarchy_with_retries test "$case_dir/valid.xml"
test "$counter" -eq 2
grep -q 'Home workspace' "$case_dir/valid.xml"
counter=0
scenario=invalid
capture_hierarchy_with_retries malformed "$case_dir/recovered.xml"
test "$counter" -eq 3
counter=0
scenario=never
if capture_hierarchy_with_retries missing "$case_dir/not-available.xml"; then
  echo "Missing hierarchy was incorrectly accepted" >&2
  exit 1
fi
test "$counter" -eq 3
echo "PASS valid, invalid, and absent UiAutomator hierarchies fail safely"
