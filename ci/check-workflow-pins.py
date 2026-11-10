#!/usr/bin/env python3
"""Offline supply-chain regression for the Launcher UI/emulator CI subset.

Only immutable reviewed workflow references are admitted. This script does
not download or execute actions and does not claim full dependency auditing.
"""
from pathlib import Path
import re

BASE = Path(__file__).resolve().parents[1] / ".github" / "workflows"
PINS = {
    "actions/checkout": "3d3c42e5aac5ba805825da76410c181273ba90b1",
    "actions/setup-java": "b843ebf9b913146b38da2eaa7aeddf76074ae1ad",
    "gradle/actions/setup-gradle": "8da46b539cd8fe6ab27ca75128919a18d757604c",
    "actions/upload-artifact": "cf430e030ddbb5b0abf93d22962f4752f3646cd9",
    "reactivecircus/android-emulator-runner": "a421e43855164a8197daf9d8d40fe71c6996bb0d",
}
WORKFLOWS = (
    "android.yml",
    "android-home-role.yml",
    "android-dock.yml",
    "android-responsive.yml",
    "android-settings-accessibility.yml",
)
USES = re.compile(r"(?m)^\s*(?:-\s*)?uses:\s*([^\s#]+)")
CHECKOUT = re.compile(
    r"(?m)^\s*-\s*uses:\s*actions/checkout@[a-f0-9]{40}\n"
    r"\s+with:\n\s+persist-credentials:\s*false\s*$"
)


def validate(name: str, content: str) -> None:
    actual = USES.findall(content)
    expected = set(PINS) - (
        {"reactivecircus/android-emulator-runner"} if name == "android.yml" else set()
    )
    if len(actual) != len(expected):
        raise ValueError(f"{name}: missing, duplicate, or extra action reference")
    observed = set()
    for reference in actual:
        action, sep, sha = reference.partition("@")
        if not sep or action not in expected or sha != PINS.get(action):
            raise ValueError(f"{name}: unreviewed or movable workflow action")
        observed.add(action)
    if observed != expected:
        raise ValueError(f"{name}: required workflow action missing")
    if len(CHECKOUT.findall(content)) != 1:
        raise ValueError(f"{name}: checkout must disable persisted credentials")


def main() -> None:
    for name in WORKFLOWS:
        validate(name, (BASE / name).read_text(encoding="utf-8"))
    sample = (BASE / "android-dock.yml").read_text(encoding="utf-8")
    for bad in (
        sample.replace(PINS["actions/checkout"], "v4", 1),
        sample.replace("persist-credentials: false", "persist-credentials: true", 1),
    ):
        try:
            validate("android-dock.yml", bad)
        except ValueError:
            continue
        raise SystemExit("FAIL: action policy accepted an unsafe regression")
    print("PASS: exact action pins and ephemeral checkout credentials on 5 Launcher workflows")


if __name__ == "__main__":
    main()
