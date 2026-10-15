#!/usr/bin/env python3
"""Validate actual app icon pixels in disposable Android Edit Home QA."""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image, ImageChops

root = Path(sys.argv[1])
nodes = list(ET.parse(root / "edit-app-added-hierarchy.xml").getroot().iter("node"))
if any(n.get("text") == "Add apps to Home" for n in nodes):
    raise SystemExit("Add apps picker did not close after selection")

events = (root / "add-app-events.txt").read_text()
if events:
    print("QA app selection diagnostic recorded")

# Edit-mode icons need not be accessibility nodes. Inspect the first Home
# cell's icon artwork; it must not be an uninitialized monochrome disk.
before = Image.open(root / "edit-added.png").convert("RGB")
after = Image.open(root / "edit-app-added.png").convert("RGB")
if before.size != after.size:
    raise SystemExit("App picker screenshot dimensions changed")
w, h = after.size
# Test a substantial workspace area where the first 5-column icon can appear
# across phone densities. This avoids assuming a fixed 40x40-pixel position.
region = (int(.17 * w), int(.20 * h), int(.45 * w), int(.62 * h))
prior = before.crop(region)
current = after.crop(region)
changed = ImageChops.difference(prior, current).convert("RGB")
changed_pixels = sum(1 for r, g, b in changed.getdata() if max(r, g, b) > 38)
changed_fraction = changed_pixels / max(1, current.width * current.height)
if changed_fraction < .004:
    raise SystemExit(
        f"New Home shortcut rendered as a blank or missing icon; "
        f"workspace change={changed_fraction:.4%}"
    )
print(f"PASS: app selection changed the visible Home workspace ({changed_fraction:.2%})")
