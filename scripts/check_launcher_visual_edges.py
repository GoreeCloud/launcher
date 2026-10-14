#!/usr/bin/env python3
"""Report, not presume, system-bar continuity from emulator screenshots."""
import json
import sys
from pathlib import Path
from PIL import Image

out = Path(sys.argv[1])
measurements = {}
for state in ("home", "edit"):
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
    lower = mean_at(int(h * 0.935))
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
(out / "edge-metrics.json").write_text(json.dumps(measurements, indent=2) + "\n")
print(json.dumps(measurements, indent=2))
