#!/usr/bin/env python3
"""Source-only inventory of Launcher QA scripts which may operate on an AVD.

Every standalone state-changing QA entrypoint must enforce a verified target
before any ADB mutation. This inventory complements (does not replace) fake-ADB
negative tests and actual disposable hosted emulator acceptance. It cannot
attest ownership of an arbitrary ADB server or a physical device.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SHARED = {
    "emulator-backup.sh": "35",
    "emulator-dock.sh": "35",
    "emulator-dock-accessibility.sh": "35",
    "emulator-icon-refresh.sh": "35",
    "emulator-keyboard.sh": "35",
    "emulator-legacy.sh": "26 28",
    "emulator-responsive.sh": "35",
    "emulator-smoke.sh": "35 36",
}
CUSTOM = {
    "emulator-app-availability.sh",
    "emulator-home-role.sh",
    "emulator-settings-font.sh",
}
SOURCED_ONLY = {
    "emulator-admission.sh",
    "emulator-hierarchy-retry.sh",
    "emulator-home-role-visibility.sh",
    "emulator-menu-retry.sh",
    "emulator-screenshot-retry.sh",
}
KNOWN = set(SHARED) | CUSTOM | SOURCED_ONLY


def verify(source: dict[str, str]) -> None:
    if set(source) != KNOWN:
        raise ValueError("new, missing or unreviewed emulator QA script")
    for name, api in SHARED.items():
        prefix = (
            "#!/usr/bin/env bash\n"
            "set -Eeuo pipefail\n"
            "source ci/emulator-admission.sh\n"
            f'assert_disposable_launcher_emulator "{api}"\n'
        )
        if not source[name].startswith(prefix):
            raise ValueError(f"{name}: shared admission absent or runs too late")
    for name in CUSTOM:
        script = source[name]
        install = "adb_target install -r" if name == "emulator-settings-font.sh" else "adb install -r"
        first_install = script.find(install)
        if first_install == -1:
            raise ValueError(f"{name}: install boundary changed; review required")
        prelude = script[:first_install]
        required = (
            'serial="emulator-5554"' if name == "emulator-settings-font.sh"
            else "export ANDROID_SERIAL=emulator-5554"
        )
        for token in (required, "get-state", "ro.kernel.qemu",
                      "ro.build.version.sdk", "exit 2"):
            if token not in prelude:
                raise ValueError(f"{name}: missing pre-install device refusal")
        if name == "emulator-settings-font.sh" and (
                'adb_target() { command adb -s "$serial" "$@"; }' not in prelude):
            raise ValueError("settings QA has no explicit serial-pinned ADB wrapper")
    # Helpers have no top-level device mutation. They are sourced into an
    # already admitted entrypoint; new helper files need an explicit review.
    for name in SOURCED_ONLY:
        script = source[name]
        if not script.startswith("#!/usr/bin/env bash\n"):
            raise ValueError(f"{name}: helper entrypoint changed")


def main() -> None:
    source = {path.name: path.read_text(encoding="utf-8")
              for path in ROOT.glob("emulator-*.sh")}
    verify(source)
    sample = "emulator-legacy.sh"
    for name, mutation in (
        (sample, source[sample].replace(
            'assert_disposable_launcher_emulator "26 28"',
            'echo "skip guard"', 1)),
        ("emulator-home-role.sh", source["emulator-home-role.sh"].replace(
            "ro.kernel.qemu", "other.property", 1)),
        ("emulator-settings-font.sh", source["emulator-settings-font.sh"].replace(
            'serial="emulator-5554"', 'serial="physical"', 1)),
        ("emulator-app-availability.sh",
         source["emulator-app-availability.sh"].replace(
             "ro.build.version.sdk", "unverified.sdk", 1)),
    ):
        candidate = dict(source)
        candidate[name] = mutation
        try:
            verify(candidate)
        except ValueError:
            pass
        else:
            raise AssertionError(f"{name}: invalid admission mutation accepted")
    candidate = dict(source)
    candidate["emulator-new-unguarded.sh"] = "#!/usr/bin/env bash\nadb install /tmp/app.apk\n"
    try:
        verify(candidate)
    except ValueError:
        pass
    else:
        raise AssertionError("new unreviewed emulator entrypoint accepted")
    print("PASS: 11 Launcher emulator QA entrypoints and five helpers have reviewed admission provenance")


if __name__ == "__main__":
    main()
