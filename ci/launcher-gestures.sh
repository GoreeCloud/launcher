#!/usr/bin/env bash
# UI-only gestures for disposable emulator acceptance. No production-device writes.
launcher_swipe_up() {
    local w h x
    read -r w h < <(adb shell wm size | sed -n 's/.*: \([0-9]*\)x\([0-9]*\).*/\1 \2/p' | head -1)
    test "${w:-0}" -gt 0 && test "${h:-0}" -gt 0
    x=$((w/2))
    adb shell input swipe "$x" "$((h*70/100))" "$x" "$((h*41/100))" 440
    sleep 1
}
launcher_swipe_down() {
    local w h x
    read -r w h < <(adb shell wm size | sed -n 's/.*: \([0-9]*\)x\([0-9]*\).*/\1 \2/p' | head -1)
    test "${w:-0}" -gt 0 && test "${h:-0}" -gt 0
    x=$((w/2))
    adb shell input swipe "$x" "$((h*44/100))" "$x" "$((h*70/100))" 440
    sleep 1
}
