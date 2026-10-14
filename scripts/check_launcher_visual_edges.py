#!/usr/bin/env python3
"""Report, not presume, system-bar continuity from emulator screenshots."""
import json
import sys
from pathlib import Path
from PIL import Image, ImageChops

out = Path(sys.argv[1])
for state in ("home", "edit"):
    hierarchy = out / f"{state}-hierarchy.xml"
    if hierarchy.exists():
        xml = hierarchy.read_text(errors="replace").lower()
        if "pixel launcher" in xml and "responding" in xml:
            raise SystemExit(f"Stock Pixel Launcher ANR obscured {state} screenshot")
        if state == "edit" and "drag pages to reorder" in xml:
            raise SystemExit("Edit-mode tutorial obscures the live Home preview")
        if state == "edit" and "default home page 1" not in xml:
            raise SystemExit("The primary Home page indicator is missing from edit navigation")
        if state == "edit" and "add a home screen" not in xml:
            raise SystemExit("The add-page control is missing from edit navigation")
added_hierarchy = out / "edit-added-hierarchy.xml"
if added_hierarchy.exists():
    added_xml = added_hierarchy.read_text(errors="replace").lower()
    if "preview home page 2" not in added_xml and "default home page 2" not in added_xml:
        raise SystemExit("Adding a Home page did not expose its page-navigation indicator")
measurements = {}
for state in ("home", "edit", "edit-added"):
    image = Image.open(out / (state + ".png")).convert("RGB")
    w, h = image.size
    if w < 360 or h < 640:
        raise SystemExit(f"Invalid {state} screenshot dimensions: {w}x{h}")
    # Horizontal quartiles avoid centered gesture bar and most status icons.
    def mean_at(y):
        samples = [image.getpixel((int(w * f), y)) for f in (0.25, 0.4, 0.6, 0.75)]
        return [round(sum(p[channel] for p in samples) / len(samples), 2)
                for channel in range(3)]
    top = mean_at(int(h * 0.018))
    upper = mean_at(int(h * 0.061))
    # Sample below the dock instead of inside its translucent background.
    lower = mean_at(int(h * 0.967))
    navigation = mean_at(int(h * 0.986))
    distance = lambda a, b: round(sum((x - y)**2 for x, y in zip(a, b))**0.5, 2)
    measurements[state] = {
        "width": w, "height": h, "status_edge_rgb": top,
        "upper_wallpaper_rgb": upper, "lower_wallpaper_rgb": lower,
        "navigation_edge_rgb": navigation,
        "status_distance": distance(top, upper),
        "navigation_distance": distance(lower, navigation),
        "warning": "Color-distance measurements are diagnostic; inspect the images visually."
    }
home_img = Image.open(out / "home.png").convert("RGB")
edit_img = Image.open(out / "edit.png").convert("RGB")
if home_img.size != edit_img.size:
    raise SystemExit("Home and edit captures have different dimensions")
delta = ImageChops.difference(home_img, edit_img).convert("L").resize((108, 240))
changed_fraction = round(sum(v > 32 for v in delta.getdata()) / (108 * 240), 4)
measurements["interaction"] = {
    "changed_fraction": changed_fraction,
    "note": "A difference alone does not prove correct edit-mode rendering."
}
(out / "edge-metrics.json").write_text(json.dumps(measurements, indent=2) + "\n")
print(json.dumps(measurements, indent=2))
if changed_fraction < 0.03:
    raise SystemExit("Home and edit screenshots too similar; edit-mode capture unverified")
