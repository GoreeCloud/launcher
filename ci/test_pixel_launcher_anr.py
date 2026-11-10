#!/usr/bin/env python3
"""Pure XML fixtures only. Never connects to an emulator or personal device."""
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from xml.sax.saxutils import escape

from pixel_launcher_anr import recovery_point

SCRIPT = Path(__file__).with_name("pixel_launcher_anr.py")


def fixture(
    message="Pixel Launcher isn't responding",
    message_package="android",
    button_package="android",
    button_text="Close app",
    button_class="android.widget.Button",
    clickable="true",
    bounds="[70,1170][1010,1296]",
    duplicate_button=False,
):
    def xml(value):
        return escape(value, {'"': '&quot;'})
    button = (
        f'<node package="{xml(button_package)}" text="{xml(button_text)}" '
        f'class="{xml(button_class)}" clickable="{xml(clickable)}" '
        f'bounds="{xml(bounds)}" />'
    )
    return (
        f'<hierarchy rotation="0"><node package="{xml(message_package)}" '
        f'text="{xml(message)}" />{button}'
        f'{"".join([button]) if duplicate_button else ""}</hierarchy>'
    ).encode()


class PixelLauncherAnrTests(unittest.TestCase):
    def test_only_verified_system_pixel_launcher_close_button(self):
        self.assertEqual((540, 1233), recovery_point(fixture()))

    def test_other_alerts_and_apps_are_never_dismissed(self):
        for content in [
            fixture(message="GoreeCloud Launcher isn't responding"),
            fixture(message="Settings isn't responding"),
            fixture(message="Pixel Launcher stopped"),
            fixture(message_package="com.goreecloud.launcher"),
            fixture(button_package="com.goreecloud.launcher"),
            fixture(button_text="Wait"),
            fixture(button_class="android.widget.TextView"),
            fixture(clickable="false"),
            fixture(bounds="[70,1170][70,1296]"),
            fixture(bounds="impossible"),
            fixture(duplicate_button=True),
            b"<not-hierarchy />",
            b"<hierarchy>",
            b"x" * (256 * 1024 + 1),
        ]:
            with self.subTest(data=content[:80]):
                self.assertIsNone(recovery_point(content))

    def test_cli_outputs_only_coordinates_on_exact_match(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "ui.xml"
            path.write_bytes(fixture())
            good = subprocess.run(
                [sys.executable, str(SCRIPT), str(path)],
                text=True, capture_output=True, check=False,
            )
            self.assertEqual(0, good.returncode)
            self.assertEqual("540 1233", good.stdout.strip())
            path.write_bytes(fixture(message="GoreeCloud Launcher isn't responding"))
            denied = subprocess.run(
                [sys.executable, str(SCRIPT), str(path)],
                text=True, capture_output=True, check=False,
            )
            self.assertNotEqual(0, denied.returncode)
            self.assertEqual("", denied.stdout)


if __name__ == "__main__":
    unittest.main()
