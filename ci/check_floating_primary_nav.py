#!/usr/bin/env python3
"""Fail-closed geometry check from a fresh Android UIAutomator XML and screenshot.

Does not infer native accessibility compliance from a screenshot or claim
physical-device acceptance. Both matching action nodes must be real & clickable.
"""
import re
import struct
import sys
import xml.etree.ElementTree as ET

BOUNDS = re.compile(r"^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$")


def read_bounds(node):
    raw = node.attrib.get("bounds", "")
    match = BOUNDS.fullmatch(raw)
    if not match:
        raise ValueError("Primary nav action missing bounds: " + raw)
    x1, y1, x2, y2 = map(int, match.groups())
    if x2 <= x1 or y2 <= y1:
        raise ValueError("Primary nav action has empty/inverted bounds: " + raw)
    return x1, y1, x2, y2


def check(tree, width, height):
    nodes = list(tree.iter())

    def find_one(description):
        matches = [node for node in nodes
                   if node.attrib.get("content-desc") == description]
        if len(matches) != 1:
            raise ValueError("Expected exactly one primary nav action "
                             + description + ", found " + str(len(matches)))
        node = matches[0]
        if node.attrib.get("clickable") != "true":
            raise ValueError("Primary nav action is not clickable: " + description)
        return read_bounds(node)

    home_matches = [n for n in nodes if n.attrib.get("content-desc")
                    in ("Home tab", "Home tab, selected")]
    apps_matches = [n for n in nodes if n.attrib.get("content-desc")
                    in ("All apps tab", "All apps tab, selected")]
    if len(home_matches) != 1 or len(apps_matches) != 1:
        raise ValueError("Expected exactly one Home and All apps navigation tab")
    home = find_one(home_matches[0].attrib["content-desc"])
    apps = find_one(apps_matches[0].attrib["content-desc"])
    if home_matches[0].attrib["content-desc"].endswith("selected") == (
            apps_matches[0].attrib["content-desc"].endswith("selected")):
        raise ValueError("Exactly one primary tab must be selected")

    hleft, htop, hright, hbottom = home
    aleft, atop, aright, abottom = apps
    if any(x < 0 for x in (hleft, htop, aleft, atop)):
        raise ValueError("Primary nav outside screen")
    if hright >= width - 4 or aright >= width - 4 or hleft <= 4:
        raise ValueError("Primary nav is flush with/overflows viewport horizontal edge")
    if max(hbottom, abottom) > height - 4:
        raise ValueError("Primary nav touches the Android system navigation edge")
    if min(htop, atop) < height * 0.52:
        raise ValueError("Primary tab row is in top/content region, not floating near bottom")
    if abs(htop - atop) > 4 or abs(hbottom - abottom) > 4:
        raise ValueError("Primary tabs are not aligned in the same bottom row")
    # Visual order follows the device locale. Both LTR and RTL must stay
    # nonoverlapping; a mirrored row is not a navigation failure.
    if not (hright < aleft or aright < hleft):
        raise ValueError("Primary tabs overlap")
    if hbottom - htop < 44 or abottom - atop < 44:
        raise ValueError("Primary tab has undersized touch target")
    shelf = [n for n in nodes if n.attrib.get("text") == "Pinned apps"]
    if shelf and read_bounds(shelf[0])[3] > min(htop, atop):
        raise ValueError("Pinned apps shelf label is obscured by primary nav")
    return {"home": home, "apps": apps, "width": width, "height": height}



def landscape_scroll_gesture(nav):
    """Choose a swipe lane outside the floating primary nav from actual UI bounds.

    Fixed center-screen swipes can start on Home/All apps and never reach the
    underlying ScrollView. Fail closed when the real nav has no usable side lane.
    """
    width, height = nav["width"], nav["height"]
    left = min(nav["home"][0], nav["apps"][0])
    right = max(nav["home"][2], nav["apps"][2])
    margin = max(12, int(width * 0.03))
    if left >= margin * 2 + 24:
        x = left // 2
    elif width - right >= margin * 2 + 24:
        x = (right + width) // 2
    else:
        raise ValueError("No unobstructed landscape scroll lane beside floating nav")
    start = int(height * 0.72)
    end = int(height * 0.15)
    if not (margin <= x <= width - margin and start > end > 0):
        raise ValueError("Invalid landscape scroll geometry")
    return x, start, end


def check_files(xml_path, png_path):
    tree = ET.parse(xml_path).getroot()
    with open(png_path, "rb") as source:
        header = source.read(24)
    if len(header) != 24 or header[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("Screenshot lacks valid PNG signature")
    width, height = struct.unpack(">II", header[16:24])
    if not width or not height:
        raise ValueError("Invalid screenshot dimensions")
    return check(tree, width, height)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: check-floating-primary-nav.py hierarchy.xml screenshot.png")
    result = check_files(sys.argv[1], sys.argv[2])
    print("PASS bottom-floating Home/All apps native nav", result)
