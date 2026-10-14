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
        if state == "edit":
            # Functional control accessibility doubles as a screenshot acceptance gate.
            # These must all be present in the visible UI, not just static source.
            controls = ("edit home screen", "done editing home screen",
                        "add apps", "remove", "wallpapers", "themes",
                        "widgets", "home screen settings")
            for label in controls:
                if label not in xml:
                    raise SystemExit(f"Mockup editor control missing from UI hierarchy: {label}")
added_hierarchy = out / "edit-added-hierarchy.xml"
if added_hierarchy.exists():
    added_xml = added_hierarchy.read_text(errors="replace").lower()
    if "preview home page 2" not in added_xml and "default home page 2" not in added_xml:
        raise SystemExit("Adding a Home page did not expose its page-navigation indicator")
    from xml.etree import ElementTree
    nodes = ElementTree.parse(added_hierarchy).getroot().iter("node")
    first_home_selected = any(
        n.get("content-desc") == "Default Home page 1" and n.get("selected") == "true"
        for n in nodes
    )
    if not first_home_selected:
        raise SystemExit("Adding a page shifted the selected preview off-screen")
# Ensure this is a usable editor, not only a set of labels in a screenshot.
# Compare relative screen positions so taller phones do not require hard-coded
# reference pixels from the owner's differently proportioned mockup.
edit_hierarchy = out / "edit-hierarchy.xml"
if edit_hierarchy.exists():
    import re
    from xml.etree import ElementTree

    nodes = [n.attrib for n in ElementTree.parse(edit_hierarchy).getroot().iter("node")]
    def controls_named(name):
        return [n for n in nodes if n.get("content-desc") == name
                and n.get("clickable") == "true"]

    def bounds(node):
        coords = [int(v) for v in re.findall(r"\\d+", node["bounds"])]
        if len(coords) != 4 or coords[2] <= coords[0] or coords[3] <= coords[1]:
            raise SystemExit(f"Invalid editor control bounds: {node.get('content-desc')}")
        return coords

    required = ("Edit Home Screen. Tap to set this page as Home",
                "Done editing Home Screen", "Add apps", "Remove",
                "Wallpapers", "Themes", "Widgets", "Home screen settings",
                "Default Home page 1", "Add a Home screen")
    selected = {}
    for label in required:
        found = controls_named(label)
        if len(found) != 1:
            raise SystemExit(f"Editor requires exactly one accessible {label}: {len(found)}")
        selected[label] = bounds(found[0])

    w, h = Image.open(out / "edit.png").size
    title = selected[required[0]]
    done = selected[required[1]]
    add = selected[required[2]]
    remove = selected[required[3]]
    title_center = (title[0] + title[2]) / 2
    if abs(title_center - w / 2) > w * .12 or not (title[1] < add[1]):
        raise SystemExit("Edit Home heading is not centered above page actions")
    if not (done[0] > w * .60 and done[2] <= w and done[1] < add[1]):
        raise SystemExit("Done button is not in the editor's top-right")
    if not (add[0] < remove[0] and add[3] < h * .38):
        raise SystemExit("Page actions are not side by side in the top part of the preview")

    dock_badges = [bounds(n) for n in nodes
                   if n.get("content-desc", "").startswith("Remove ")
                   and n.get("content-desc", "").endswith(" from dock")
                   and n.get("clickable") == "true"]
    if len(dock_badges) != 5:
        raise SystemExit(f"Expected exactly five removable starter dock icons, found {len(dock_badges)}")
    dock_centers = sorted((r[0] + r[2]) / 2 for r in dock_badges)
    if min(b - a for a, b in zip(dock_centers, dock_centers[1:])) < w * .055:
        raise SystemExit("Dock badges overlap or are not arranged in five columns")

    nav = selected["Default Home page 1"]
    footer = [selected[x] for x in ("Wallpapers", "Themes", "Widgets",
                                  "Home screen settings")]
    if not (max(r[3] for r in dock_badges) < nav[1] < nav[3] < min(r[1] for r in footer)):
        raise SystemExit("Page dots must sit between the dock and bottom action tray")
    if max(r[1] for r in footer) - min(r[1] for r in footer) > h * .02:
        raise SystemExit("Bottom action tiles must share a common row")
    if any(a[2] > b[0] for a, b in zip(footer, footer[1:])):
        raise SystemExit("Bottom action tiles overlap")

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
    if state in ("home", "edit"):
        # Detect narrow vertical status glyphs as a possible clipping symptom.
        # This is diagnostic, not a generic threshold for all Android devices.
        bands = [(int(w * .05), int(w * .24)), (int(w * .78), int(w * .96))]
        ink_rows = []
        for y in range(int(h * .008), int(h * .075)):
            bright = sum(1 for left, right in bands for x in range(left, right)
                         if min(image.getpixel((x, y))) > 190)
            if bright >= 8:
                ink_rows.append(y)
        icon_span = max(ink_rows) - min(ink_rows) + 1 if ink_rows else 0
        measurements[state]["status_glyph_span_px"] = icon_span
        measurements[state]["status_possible_clipping"] = icon_span < h * .009
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
