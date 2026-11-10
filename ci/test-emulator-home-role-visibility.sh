#!/usr/bin/env bash
# Synthetic regression: no ADB binary, emulator, device, role change or network.
set -Eeuo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
EVIDENCE="$(mktemp -d)"
trap 'rm -rf "$EVIDENCE"' EXIT
scenario=immediate
dumps=0
swipes=0

adb() {
  if [[ "$*" != "shell input swipe 445 1690 445 580 520" ]]; then
    echo "Unexpected Android command: $*" >&2
    return 98
  fi
  swipes=$((swipes + 1))
}
sleep() { :; }
dump_screen() {
  local name="$1"
  dumps=$((dumps + 1))
  local controls=false
  if [[ "$scenario" == immediate || ( "$scenario" == delayed && "$dumps" -ge 4 ) ]]; then
    controls=true
  fi
  if [[ "$controls" == true ]]; then
    cat > "$EVIDENCE/$name.xml" <<'XML'
<hierarchy>
  <node text="APP DOCK" bounds="[20,20][400,80]" />
  <node text="GoreeCloud is not the current default Home" bounds="[30,150][900,250]" />
  <node text="Select default Home app" bounds="[30,300][900,400]" />
</hierarchy>
XML
  else
    cat > "$EVIDENCE/$name.xml" <<'XML'
<hierarchy>
  <node text="APP DOCK" bounds="[20,20][400,80]" />
  <node text="Select default Home app" bounds="[30,300][900,400]" />
</hierarchy>
XML
  fi
}
source ci/emulator-home-role-visibility.sh
find_settings_role_controls test-status "GoreeCloud is not the current default Home" "Select default Home app"
[[ "$dumps" -eq 1 && "$swipes" -eq 0 ]] || {
  echo 'Visible controls should require no scrolling' >&2; exit 1;
}
scenario=delayed; dumps=0; swipes=0
find_settings_role_controls test-delayed "GoreeCloud is not the current default Home" "Select default Home app"
[[ "$dumps" -eq 4 && "$swipes" -eq 3 ]] || {
  echo 'Expected exactly three bounded synthetic swipes' >&2; exit 1;
}
scenario=never; dumps=0; swipes=0
if find_settings_role_controls test-missing "GoreeCloud is not the current default Home" "Select default Home app"; then
  echo 'Missing status control was incorrectly accepted' >&2; exit 1
fi
[[ "$dumps" -eq 8 && "$swipes" -eq 7 ]] || {
  echo 'Missing control did not fail within configured budget' >&2; exit 1;
}
# Both labels must be visible with positive bounds, not just present in XML.
cat > "$EVIDENCE/invalid.xml" <<'XML'
<hierarchy>
  <node text="GoreeCloud is not the current default Home" bounds="[0,0][0,0]" />
  <node text="Select default Home app" bounds="[30,300][900,400]" />
</hierarchy>
XML
if role_controls_visible invalid "GoreeCloud is not the current default Home" "Select default Home app"; then
  echo 'Zero-area control was incorrectly accepted' >&2; exit 1
fi
echo 'PASS: Home-role Settings visibility uses real bounded control pairs and refuses missing UI'
