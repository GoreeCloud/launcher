"""Focused unit tests for the read-only Android 16 artifact analyzer."""

import tempfile
import unittest
import zipfile
from pathlib import Path

from analyze_android16_runtime import analyze_archive


def write_report(path, exit_code, progress, fixture=""):
    with zipfile.ZipFile(path, "w") as z:
        z.writestr("ci-runtime/status.txt", f"gradle_exit_status={exit_code}\n")
        z.writestr(
            "app/build/outputs/androidTest-results/connected/debug/device - 16/testlog/test-results.log",
            progress,
        )
        z.writestr("ci-runtime/logcat.txt", fixture)


def step(name, outcome):
    return (
        "INSTRUMENTATION_STATUS: test=" + name + "\n"
        + "INSTRUMENTATION_STATUS_CODE: " + str(outcome) + "\n"
    )


class RuntimeArtifactAnalyzerTests(unittest.TestCase):
    def test_timeout_preserves_partial_progress_and_last_fixture_stage(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "runtime.zip"
            progress = (
                "INSTRUMENTATION_STATUS: numtests=71\n"
                + step("first", 1) + step("first", 0)
                + step("second", 1) + step("second", 0)
                + step("emptyHomeLongPressAlwaysOpensHomeEditor", 1)
            )
            fixture = (
                "TestRunner: started: emptyHomeLongPressAlwaysOpensHomeEditor(Test)\n"
                "LauncherRuntimeFixture: prepare:awaiting-home-pages\n"
            )
            write_report(path, 124, progress, fixture)
            result = analyze_archive(path)
            self.assertTrue(result["outer_timeout"])
            self.assertFalse(result["automated_pass"])
            self.assertEqual(result["tests_expected"], 71)
            self.assertEqual(result["tests_completed"], 2)
            self.assertEqual(
                result["incomplete_test_names"], ["emptyHomeLongPressAlwaysOpensHomeEditor"]
            )
            self.assertEqual(result["last_fixture_stage"], "prepare:awaiting-home-pages")

    def test_auxiliary_watchdog_logs_do_not_replace_actual_fixture_phase(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "runtime.zip"
            progress = (
                "INSTRUMENTATION_STATUS: numtests=71\n"
                + step("previous", 1) + step("previous", 0)
                + step("emptyHomeLongPressAlwaysOpensHomeEditor", 1)
            )
            fixture = (
                "TestRunner: started: emptyHomeLongPressAlwaysOpensHomeEditor(Test)\n"
                "LauncherRuntimeFixture: prepare:room-authority-ready\n"
                "LauncherRuntimeFixture: prepare:awaiting-home-pages\n"
                "LauncherRuntimeFixture: prepare:home-pages-watchdog:state=TIMED_WAITING\n"
                "LauncherRuntimeFixture: prepare:home-pages-worker:DefaultDispatcher\n"
                "LauncherRuntimeFixture: prepare:home-page-state:waiting-for-room\n"
            )
            write_report(path, 124, progress, fixture)
            result = analyze_archive(path)
            self.assertEqual(result["last_fixture_stage"], "prepare:awaiting-home-pages")
            self.assertFalse(result["automated_pass"])

    def test_success_requires_all_tests_complete(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "runtime.zip"
            progress = "INSTRUMENTATION_STATUS: numtests=1\n" + step("healthy", 1) + step("healthy", 0)
            write_report(path, 0, progress)
            self.assertTrue(analyze_archive(path)["automated_pass"])

    def test_assertion_failure_cannot_be_classified_as_success(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "runtime.zip"
            progress = "INSTRUMENTATION_STATUS: numtests=1\n" + step("broken", 1) + step("broken", -2)
            write_report(path, 1, progress)
            result = analyze_archive(path)
            self.assertFalse(result["automated_pass"])
            self.assertEqual(result["tests_failed"], 1)

    def test_missing_diagnostics_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "runtime.zip"
            with zipfile.ZipFile(path, "w") as z:
                z.writestr("ci-runtime/status.txt", "gradle_exit_status=0\n")
            with self.assertRaises(ValueError):
                analyze_archive(path)


if __name__ == "__main__":
    unittest.main()
