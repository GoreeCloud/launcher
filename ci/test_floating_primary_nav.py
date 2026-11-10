#!/usr/bin/env python3
"""Fixtures test the parser only, not an Android emulator or end-user device."""
from pathlib import Path
import struct
import tempfile
import unittest
import xml.etree.ElementTree as ET

import check_floating_primary_nav as module


def hierarchy(home="[20,620][160,680]", apps="[170,620][300,680]",
              home_sel=True, apps_sel=False, shelf="[40,560][280,585]"):
    nodes = [
        f'<node content-desc="Home tab{", selected" if home_sel else ""}" '
        f'bounds="{home}" clickable="true"/>',
        f'<node content-desc="All apps tab{", selected" if apps_sel else ""}" '
        f'bounds="{apps}" clickable="true"/>',
        f'<node text="Pinned apps" bounds="{shelf}" clickable="false"/>',
    ]
    return ET.fromstring("<hierarchy>" + "".join(nodes) + "</hierarchy>")


class FloatingNavGeometryTests(unittest.TestCase):
    def test_valid_floating_bar(self):
        a = module.check(hierarchy(), 320, 760)
        self.assertEqual(a["width"], 320)

    def test_rejects_top_tabs(self):
        with self.assertRaisesRegex(ValueError, "top/content"):
            module.check(hierarchy(home="[20,100][160,160]",
                                   apps="[170,100][300,160]",
                                   shelf="[40,30][280,60]"), 320, 760)

    def test_rtl_mirrored_order_is_accepted(self):
        result = module.check(hierarchy(home="[170,620][300,680]",
                                        apps="[20,620][160,680]"), 320, 760)
        self.assertGreater(result["home"][0], result["apps"][0])

    def test_rejects_overlapping_buttons(self):
        with self.assertRaisesRegex(ValueError, "overlap"):
            module.check(hierarchy(apps="[150,620][300,680]"), 320, 760)

    def test_rejects_two_selected_buttons(self):
        with self.assertRaisesRegex(ValueError, "Exactly one"):
            module.check(hierarchy(apps_sel=True), 320, 760)

    def test_rejects_partially_offscreen_button(self):
        with self.assertRaisesRegex(ValueError, "system navigation edge"):
            module.check(hierarchy(apps="[170,620][300,759]"), 320, 760)

    def test_rejects_misaligned_buttons(self):
        with self.assertRaisesRegex(ValueError, "aligned"):
            module.check(hierarchy(apps="[170,610][300,672]"), 320, 760)

    def test_rejects_content_covered_by_nav(self):
        with self.assertRaisesRegex(ValueError, "obscured"):
            module.check(hierarchy(shelf="[40,602][280,642]"), 320, 760)

    def test_landscape_scroll_avoids_both_floating_tabs(self):
        n = module.check(hierarchy(
            home="[643,835][1189,961]", apps="[1210,835][1756,961]",
            shelf="[50,700][300,760]"), 2400, 1080)
        x, y_start, y_end = module.landscape_scroll_gesture(n)
        self.assertLess(x, min(n["home"][0], n["apps"][0]))
        self.assertEqual((y_start, y_end), (777, 162))
        rtl = module.check(hierarchy(
            home="[1210,835][1756,961]", apps="[643,835][1189,961]",
            shelf="[50,700][300,760]"), 2400, 1080)
        self.assertEqual(module.landscape_scroll_gesture(rtl), (x, y_start, y_end))

    def test_landscape_scroll_fails_closed_without_safe_side_lane(self):
        n = module.check(hierarchy(
            home="[8,620][150,680]", apps="[160,620][312,680]",
            shelf="[40,560][280,585]"), 320, 760)
        with self.assertRaisesRegex(ValueError, "No unobstructed"):
            module.landscape_scroll_gesture(n)

    def test_rejects_missing_real_actions(self):
        with self.assertRaisesRegex(ValueError, "exactly one"):
            module.check(ET.Element("hierarchy"), 320, 760)

    def test_rejects_outside_safe_horizontal_edge(self):
        with self.assertRaisesRegex(ValueError, "horizontal edge"):
            module.check(hierarchy(home="[1,620][160,680]"), 320, 760)

    def test_checks_fresh_png_file_and_real_xml(self):
        with tempfile.TemporaryDirectory() as tmp:
            xml = Path(tmp, "hierarchy.xml")
            png = Path(tmp, "screen.png")
            xml.write_text(ET.tostring(hierarchy(), encoding="unicode"))
            png.write_bytes(b"\x89PNG\r\n\x1a\n" + b"\0" * 8 +
                            struct.pack(">II", 320, 760))
            self.assertEqual(module.check_files(xml, png)["height"], 760)
            png.write_bytes(b"NOT_PNG")
            with self.assertRaisesRegex(ValueError, "PNG"):
                module.check_files(xml, png)


if __name__ == "__main__":
    unittest.main()
