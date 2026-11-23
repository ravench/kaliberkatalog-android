#!/usr/bin/env python3
"""The launcher icon: Kaliberfinder's three cartridges side by side (.22 LR, 9x19 mm, 7.5x55 Swiss GP 11), drawn
from the catalogue outlines at the same scale, on its beige, without its measuring line: the catalogue does not
measure. Ported from Kaliberfinder's tools/app_icon.py.

    python3 tools/app_icon.py

Writes the adaptive icon's layers as vector drawables of 108 x 108 dp, the cartridges inside the 66 dp circle that
every launcher's mask keeps: ic_launcher_background (the beige), ic_launcher_foreground (the cartridges in brass and
copper) and ic_launcher_monochrome (their silhouettes, which Android tints for themed icons).
"""
import json, math, os, re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "app", "src", "main", "assets", "Cartridges.json")
DRAWABLE = os.path.join(ROOT, "app", "src", "main", "res", "drawable")
CARTRIDGES = [".22 LR", "9x19 mm", "7.5x55 mm Swiss (GP 11)"]
SIZE = 108          # dp, the adaptive icon's layers
SAFE = 66           # dp, the circle no launcher's mask cuts into
GAP = 66 / 8.6      # mm between cartridges, as Kaliberfinder's 66 px at 8.6 px per mm
LINE = 3 / 8.6      # mm, the outline's width, as Kaliberfinder's 3 px

# Kaliberfinder's light look: brass and copper with a highlight down the left third, a dark brown line, a beige gradient.
BRASS, BRASS_HI, COPPER, COPPER_HI, INK = "#E3C16F", "#F3DC9C", "#C0743A", "#DE9A62", "#3A2A12"
BEIGE = ("#F1E6D2", "#DCC9A8")


def polygons(d):
    """The outline's polygons, from the catalogue's M/L/Z path data."""
    out, cur = [], []
    for cmd, x, y in re.findall(r"([MLZ])\s*(?:(-?[\d.]+)\s+(-?[\d.]+))?", d):
        if cmd == "M":
            cur = [(float(x), float(y))]
        elif cmd == "L":
            cur.append((float(x), float(y)))
        else:
            out.append(cur)
    return out


def layout(cartridges):
    """Each cartridge's (left, top) in dp and the scale in dp per mm: side by side on one base, the whole row as large as
    fits the safe circle, centred."""
    width = sum(c["outline"]["width"] for c in cartridges) + GAP * (len(cartridges) - 1)
    height = max(c["outline"]["height"] for c in cartridges)
    scale = SAFE / math.hypot(width, height)  # the row's corners on the circle
    left, base = (SIZE - width * scale) / 2, (SIZE + height * scale) / 2
    places = []
    for c in cartridges:
        places.append((left, base - c["outline"]["height"] * scale))
        left += (c["outline"]["width"] + GAP) * scale
    return places, scale


def path(polys, left, top, scale):
    return " ".join("M" + " L".join(f"{left + x * scale:.2f},{top + y * scale:.2f}" for x, y in p) + " Z" for p in polys)


def vector(body):
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            "<!-- Written by tools/app_icon.py: change that and run it again. -->\n"
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    xmlns:aapt="http://schemas.android.com/aapt"\n'
            f'    android:width="{SIZE}dp"\n    android:height="{SIZE}dp"\n'
            f'    android:viewportWidth="{SIZE}"\n    android:viewportHeight="{SIZE}">\n{body}</vector>\n')


def background():
    return vector(
        f'    <path android:pathData="M0,0 L{SIZE},0 L{SIZE},{SIZE} L0,{SIZE} Z">\n'
        '        <aapt:attr name="android:fillColor">\n'
        f'            <gradient android:type="linear" android:startX="0" android:startY="0" android:endX="0" android:endY="{SIZE}"\n'
        f'                android:startColor="{BEIGE[0]}" android:endColor="{BEIGE[1]}" />\n'
        "        </aapt:attr>\n    </path>\n")


def foreground(cartridges):
    """Case, then bullet, each filled, lit down its left third (clipped to the part) and outlined, as Kaliberfinder's."""
    places, scale = layout(cartridges)
    body = []
    for c, (left, top) in zip(cartridges, places):
        o = c["outline"]
        w, h = o["width"] * scale, o["height"] * scale
        light = f"M{left + w * 0.18:.2f},{top:.2f} h{w * 0.16:.2f} v{h:.2f} h{-w * 0.16:.2f} Z"
        for key, fill, hi in (("case", BRASS, BRASS_HI), ("bullet", COPPER, COPPER_HI)):
            d = path(polygons(o[key]), left, top, scale)
            body.append(f'    <path android:pathData="{d}" android:fillColor="{fill}" />\n'
                        f'    <group>\n        <clip-path android:pathData="{d}" />\n'
                        f'        <path android:pathData="{light}" android:fillColor="{hi}" android:fillAlpha="0.8" />\n'
                        "    </group>\n"
                        f'    <path android:pathData="{d}" android:strokeColor="{INK}" android:strokeWidth="{LINE * scale:.2f}"\n'
                        '        android:strokeLineJoin="round" />\n')
    return vector("".join(body))


def monochrome(cartridges):
    """The silhouettes, the bullets lighter than the cases: Android tints this layer by its alpha."""
    places, scale = layout(cartridges)
    body = []
    for c, (left, top) in zip(cartridges, places):
        for key, alpha in (("case", "1"), ("bullet", "0.6")):
            d = path(polygons(c["outline"][key]), left, top, scale)
            body.append(f'    <path android:pathData="{d}" android:fillColor="#FFFFFFFF" android:fillAlpha="{alpha}" />\n')
    return vector("".join(body))


def outputs():
    """Each file this script writes, with its content."""
    by_id = {c["id"]: c for c in json.load(open(DATA))["cartridges"]}
    cartridges = [by_id[i] for i in CARTRIDGES]
    return {
        os.path.join(DRAWABLE, "ic_launcher_background.xml"): background(),
        os.path.join(DRAWABLE, "ic_launcher_foreground.xml"): foreground(cartridges),
        os.path.join(DRAWABLE, "ic_launcher_monochrome.xml"): monochrome(cartridges),
    }


def main():
    for name, text in outputs().items():
        with open(name, "w") as f:
            f.write(text)


if __name__ == "__main__":
    main()
