#!/usr/bin/env python3
"""Validate actual app icon pixels in disposable Android Edit Home QA."""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image, ImageStat

root = Path(sys.argv[1])
nodes = list(ET.parse(root / "edit-app-added-hierarchy.xml").getroot().iter("node"))
if any(n.get("text") == "Add apps to Home" for n in nodes):
    raise SystemExit("Add apps picker did not close after selection")

events = (root / "add-app-events.txt").read_text()
if "add-app component=" not in events:
    raise SystemExit("No persisted app selection event was recorded")

# Edit-mode icons need not be accessibility nodes. Inspect the first Home
# cell's icon artwork; it must not be an uninitialized monochrome disk.
before = Image.open(root / "edit-added.png").convert("RGB")
after = Image.open(root / "edit-app-added.png").convert("RGB")
if before.size != after.size:
    raise SystemExit("App picker screenshot dimensions changed")
w, h = after.size
region = (int(.218 * w), int(.163 * h), int(.258 * w), int(.183 * h))
previous = ImageStat.Stat(before.crop(region))
current = ImageStat.Stat(after.crop(region))
if max(current.stddev) < 12:
    raise SystemExit("New Home shortcut rendered as a blank, unresolved icon")
if sum(abs(a - b) for a, b in zip(previous.mean, current.mean)) < 45:
    raise SystemExit("New Home shortcut did not change the selected cell")
print("PASS: persisted app selection draws resolved, nonblank Home icon")
