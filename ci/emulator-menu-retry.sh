#!/usr/bin/env bash
# Source into emulator QA scripts after their dump_screen/longpress_label/assert_text helpers.
# A missed gesture is retried with a FRESH hierarchy; a missing real action fails.
retry_longpress_menu() {
  local snapshot="$1" tile="$2" option="$3" attempt
  for attempt in 1 2 3; do
    dump_screen "${snapshot}-before-${attempt}"
    if ! longpress_label "${snapshot}-before-${attempt}" "$tile"; then
      echo "Context target $tile unavailable before long-press (attempt $attempt of 3)" >&2
      sleep 2
      continue
    fi
    sleep 1
    dump_screen "$snapshot"
    if grep -Fq "text=\"$option\"" "$EVIDENCE/$snapshot.xml"; then
      assert_text "$snapshot" "$option"
      echo "PASS context menu $option for $tile (attempt $attempt)"
      return 0
    fi
    echo "Context menu $option missing after long-press; attempt $attempt of 3" >&2
  done
  echo "FAIL context menu $option for $tile after 3 real attempts" >&2
  return 1
}
