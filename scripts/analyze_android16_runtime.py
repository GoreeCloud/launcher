#!/usr/bin/env python3
"""Report bounded, non-sensitive test progress from a Launcher Android 16 CI ZIP.

Read-only; no ADB access, permissions, network calls, or test-result overrides.
"""

from __future__ import annotations

import argparse
import json
import re
import zipfile
from pathlib import Path


TEST_LINE = re.compile(r"INSTRUMENTATION_STATUS: test=([^\r\n]+)")
STATUS_CODE = re.compile(r"INSTRUMENTATION_STATUS_CODE: (-?\d+)")
TEST_RUNNER_START = re.compile(r"TestRunner:\s+started:\s+(\w+)\(")
FIXTURE_STAGE = re.compile(r"LauncherRuntimeFixture:\s+(prepare:[a-z0-9:-]+)")


def _member(z: zipfile.ZipFile, suffix: str) -> str:
    names = [name for name in z.namelist() if name.endswith(suffix)]
    if len(names) != 1:
        raise ValueError(f"Expected exactly one {suffix!r} in archive; found {len(names)}")
    return z.read(names[0]).decode("utf-8", errors="replace")


def analyze_archive(path: str | Path) -> dict[str, object]:
    """Identify incomplete tests and fixture state without exposing raw device logs."""
    with zipfile.ZipFile(path) as z:
        status_text = _member(z, "ci-runtime/status.txt")
        test_log = _member(z, "/testlog/test-results.log")
        logcat = _member(z, "ci-runtime/logcat.txt")

    match = re.search(r"^gradle_exit_status=(\d+)\s*$", status_text, re.MULTILINE)
    if match is None:
        raise ValueError("Missing Gradle exit status in diagnostic archive")
    code = int(match.group(1))
    expected_counts = [int(n) for n in re.findall(r"INSTRUMENTATION_STATUS: numtests=(\d+)", test_log)]
    expected_tests = max(expected_counts) if expected_counts else None
    started: list[str] = []
    completed: list[str] = []
    failed: list[str] = []
    active_name: str | None = None

    for line in test_log.splitlines():
        test_match = TEST_LINE.search(line)
        if test_match:
            active_name = test_match.group(1).strip()
        state_match = STATUS_CODE.search(line)
        if state_match and active_name:
            state = int(state_match.group(1))
            if state == 1:
                started.append(active_name)
            elif state == 0:
                completed.append(active_name)
            elif state < 0:
                failed.append(active_name)

    completed_set = set(completed) | set(failed)
    incomplete = [name for name in started if name not in completed_set]

    last_started_runner: str | None = None
    last_fixture_stage: str | None = None
    for line in logcat.splitlines():
        begin = TEST_RUNNER_START.search(line)
        if begin:
            last_started_runner = begin.group(1)
            last_fixture_stage = None
        stage = FIXTURE_STAGE.search(line)
        if stage:
            last_fixture_stage = stage.group(1)

    return {
        "gradle_exit_status": code,
        "outer_timeout": code == 124,
        "tests_expected": expected_tests,
        "tests_started": len(started),
        "tests_completed": len(completed),
        "tests_failed": len(failed),
        "incomplete_test_names": incomplete,
        "last_started_runner_test": last_started_runner,
        "last_fixture_stage": last_fixture_stage,
        "automated_pass": (
            code == 0
            and expected_tests is not None
            and expected_tests > 0
            and len(started) == expected_tests
            and len(completed) == expected_tests
            and not failed
            and not incomplete
        ),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("artifact_zip", type=Path, help="Launcher Android 16 reports ZIP")
    args = parser.parse_args()
    try:
        report = analyze_archive(args.artifact_zip)
    except (OSError, ValueError, zipfile.BadZipFile) as exc:
        parser.exit(2, f"Diagnostic error: {exc}\n")
    print(json.dumps(report, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
