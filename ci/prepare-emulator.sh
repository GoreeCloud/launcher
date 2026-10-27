#!/usr/bin/env bash
set -Eeuo pipefail

# GitHub-hosted Ubuntu may lack PulseAudio runtime; recent Android emulator
# builds require libpulse.so.0 even when audio playback is disabled.
sudo apt-get update -qq
sudo apt-get install -y -qq libpulse0

SDKMANAGER="$(find "$ANDROID_HOME/cmdline-tools" -path '*/bin/sdkmanager' | sort -V | tail -n 1)"
test -x "$SDKMANAGER"
for attempt in 1 2 3; do
  echo "Preparing Android emulator package (attempt $attempt/3)"
  if "$SDKMANAGER" --install emulator --channel=0; then
    if "$ANDROID_HOME/emulator/emulator" -version >/dev/null 2>&1; then
      echo "PASS Android emulator binary runs with installed host libraries"
      exit 0
    fi
  fi
  echo "::warning::Android Emulator install/host dependencies unavailable ($attempt/3)"
  sleep "$((attempt * 12))"
done
echo "FAIL Android emulator preparation after three bounded attempts" >&2
if [ -f "$ANDROID_HOME/emulator/qemu/linux-x86_64/qemu-system-x86_64" ]; then
  ldd "$ANDROID_HOME/emulator/qemu/linux-x86_64/qemu-system-x86_64" | grep 'not found' || true
fi
exit 1
