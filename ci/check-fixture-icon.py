#!/usr/bin/env python3
"""Verify a synthetic fixture icon's real rendered RGB pixels on a CI emulator.

Only accepts 8-bit, non-interlaced RGB/RGBA PNGs, avoiding external packages.
"""
from pathlib import Path
import re
import struct
import sys
import xml.etree.ElementTree as ET
import zlib


def read_png(path):
    data = Path(path).read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("Not a PNG")
    pos, compressed, metadata = 8, bytearray(), None
    while pos + 12 <= len(data):
        length = struct.unpack_from(">I", data, pos)[0]
        kind = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + length]
        if len(chunk) != length:
            raise ValueError("Truncated PNG chunk")
        pos += length + 12
        if kind == b"IHDR":
            width, height, depth, mode, compression, filtering, interlace = struct.unpack(">IIBBBBB", chunk)
            if depth != 8 or mode not in (2, 6) or compression or filtering or interlace:
                raise ValueError("Unsupported PNG format")
            metadata = (width, height, 3 if mode == 2 else 4)
        elif kind == b"IDAT":
            compressed.extend(chunk)
        elif kind == b"IEND":
            break
    if metadata is None:
        raise ValueError("Missing PNG header")
    width, height, channels = metadata
    raw = zlib.decompress(compressed)
    stride = width * channels
    if len(raw) != (stride + 1) * height:
        raise ValueError("Invalid PNG raster length")
    result, prev, offset = [], bytearray(stride), 0
    for _ in range(height):
        filter_type = raw[offset]
        current = bytearray(raw[offset + 1:offset + 1 + stride])
        offset += stride + 1
        if filter_type not in (0, 1, 2, 3, 4):
            raise ValueError("Invalid PNG scanline filter")
        for i in range(stride):
            left = current[i - channels] if i >= channels else 0
            up = prev[i]
            above_left = prev[i - channels] if i >= channels else 0
            if filter_type == 1:
                prediction = left
            elif filter_type == 2:
                prediction = up
            elif filter_type == 3:
                prediction = (left + up) // 2
            elif filter_type == 4:
                p = left + up - above_left
                distances = (abs(p - left), abs(p - up), abs(p - above_left))
                prediction = (left, up, above_left)[distances.index(min(distances))]
            else:
                prediction = 0
            current[i] = (current[i] + prediction) & 255
        result.append(current)
        prev = current
    return width, height, channels, result


def fixture_bounds(path):
    nodes = ET.parse(path).getroot().iter()
    candidates = []
    for node in nodes:
        if node.attrib.get("content-desc") != "Launcher QA Fixture":
            continue
        if node.attrib.get("clickable") != "true":
            continue
        match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
        if match:
            candidates.append(tuple(map(int, match.groups())))
    if not candidates:
        raise ValueError("Visible synthetic fixture tile missing")
    return min(candidates, key=lambda bounds: bounds[1])


def main():
    if len(sys.argv) != 4 or sys.argv[3] not in ("red", "green"):
        raise SystemExit("Usage: check-fixture-icon.py hierarchy.xml screenshot.png red|green")
    x1, y1, x2, y2 = fixture_bounds(sys.argv[1])
    width, height, channels, raster = read_png(sys.argv[2])
    x1, x2 = max(0, x1), min(width, x2)
    y1, y2 = max(0, y1), min(height, y2)
    if x2 <= x1 or y2 <= y1:
        raise ValueError("Fixture outside screenshot viewport")
    red, green = 0, 0
    for row in raster[y1:y2]:
        for x in range(x1, x2):
            r, g, b = row[x * channels:x * channels + 3]
            if r >= 190 and g <= 105 and b <= 120:
                red += 1
            if g >= 155 and r <= 95 and b <= 135:
                green += 1
    print(f"Fixture tile {x1,y1,x2,y2}: red pixels={red}, green pixels={green}")
    if sys.argv[3] == "red" and not (red >= 100 and red > 5 * green):
        raise SystemExit("FAIL baseline fixture icon not visibly red")
    if sys.argv[3] == "green" and not (green >= 100 and green > 5 * red):
        raise SystemExit("FAIL updated fixture icon not visibly green")
    print(f"PASS real rendered {sys.argv[3]} fixture icon")


if __name__ == "__main__":
    main()
