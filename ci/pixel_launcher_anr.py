#!/usr/bin/env python3
"""Return a bounded tap point ONLY for the disposable Android system's
specific Pixel Launcher ANR modal. No device access, permissions or actions.
Other alerts, including a GoreeCloud Launcher ANR, MUST fail closed.
"""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

BOUNDS = re.compile(r"^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$")
MESSAGE = "Pixel Launcher isn't responding"


def recovery_point(xml_bytes: bytes):
    if not 0 < len(xml_bytes) <= 256 * 1024:
        return None
    try:
        root = ET.fromstring(xml_bytes)
    except ET.ParseError:
        return None
    if root.tag != "hierarchy":
        return None
    nodes = [node.attrib for node in root.iter("node")]
    if sum(
        1 for node in nodes
        if node.get("package") == "android" and node.get("text") == MESSAGE
    ) != 1:
        return None
    buttons = [
        node for node in nodes
        if node.get("package") == "android"
        and node.get("text") == "Close app"
        and node.get("class") == "android.widget.Button"
        and node.get("clickable") == "true"
    ]
    if len(buttons) != 1:
        return None
    m = BOUNDS.fullmatch(buttons[0].get("bounds", ""))
    if m is None:
        return None
    x1, y1, x2, y2 = map(int, m.groups())
    if not (0 <= x1 < x2 <= 10000 and 0 <= y1 < y2 <= 10000):
        return None
    return ((x1 + x2) // 2, (y1 + y2) // 2)


def main():
    if len(sys.argv) != 2:
        return 2
    try:
        data = Path(sys.argv[1]).read_bytes()
    except OSError:
        return 2
    result = recovery_point(data)
    if result is None:
        return 2
    print(result[0], result[1])
    return 0


if __name__ == "__main__":
    sys.exit(main())
