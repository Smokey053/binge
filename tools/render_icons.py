#!/usr/bin/env python3
"""Regenerates the icon PNGs used by Binge from the SVG sources in binge/icons-src.

Each icon is rendered white-on-transparent at 96x96. The app tints them at runtime, so keep them
single-colour (white). Needs `rsvg-convert` (librsvg) on the PATH.

    python3 tools/render_icons.py
"""
import pathlib
import subprocess

ROOT = pathlib.Path(__file__).resolve().parent.parent / "binge"
SOURCES = ROOT / "icons-src"
OUTPUT = ROOT / "src/main/resources/com/binge/icons"
SIZE = 96

OUTPUT.mkdir(parents=True, exist_ok=True)
for svg in sorted(SOURCES.glob("*.svg")):
    png = OUTPUT / (svg.stem + ".png")
    subprocess.run(["rsvg-convert", "-w", str(SIZE), "-h", str(SIZE), str(svg), "-o", str(png)], check=True)
    print("rendered", png.name)
