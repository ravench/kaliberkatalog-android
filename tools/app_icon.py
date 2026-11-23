#!/usr/bin/env python3
"""The launcher icon: the 7.5x55 mm Swiss (GP 11), drawn from its catalogue outline in Kaliberfinder's brass and copper,
with a collection label tied to its case in the paper and lines of Schiesstagebuch's journal, on the beige of both
apps' icons.

    python3 tools/app_icon.py

Writes the adaptive icon's layers as vector drawables of 108 x 108 dp, everything inside the 66 dp circle that every
launcher's mask keeps: ic_launcher_background (the beige), ic_launcher_foreground (the cartridge and its label) and
ic_launcher_monochrome (their silhouettes, which Android tints for themed icons). Every path is straight pieces, its
curves too, so that tools/test_app_icon.py can measure each point.
"""
import json, math, os, re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "app", "src", "main", "assets", "Cartridges.json")
DRAWABLE = os.path.join(ROOT, "app", "src", "main", "res", "drawable")
CARTRIDGE = "7.5x55 mm Swiss (GP 11)"
SIZE = 108          # dp, the adaptive icon's layers
SAFE = 66           # dp, the circle no launcher's mask cuts into
LINE = 3 / 8.6      # mm, the cartridge's outline, as Kaliberfinder's 3 px at 8.6 px per mm
LENGTH, BASE, POINTING = 52, (31, 69), 45  # dp, the cartridge's length and its base's centre; degrees clockwise from up
TIED = 9            # mm above the base, where the string goes round the case
CORNER, TURN = (51, 60.5), 24      # dp, the label's top left corner; degrees clockwise
TAG_W, TAG_H, CUT = 23, 12.5, 3.6  # dp, the label and its corners cut off at the eyelet's end
EYE = (3.6, TAG_H / 2)             # dp on the label, the eyelet's centre
WRITING = ((9, 19, 4.2), (9, 14.5, 8.3))  # dp on the label, its two lines of writing: from x to x at y
PEN = 1.7           # dp, their width

# Kaliberfinder's brass and copper; Schiesstagebuch's pages and lines; a brown string; both apps' beige.
BRASS, BRASS_HI, COPPER, COPPER_HI, INK = "#E3C16F", "#F3DC9C", "#C0743A", "#DE9A62", "#3A2A12"
PAPER, RING, RING_EDGE, LINES, STRING = "#F4F1E8", "#E9E4D6", "#C9BFA8", "#9AA6B2", "#5A4632"
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


def turned(origin, degrees, scale=1, shift=(0, 0)):
    """The map from a shape's own space to the icon's dp: moved by shift, scaled, and turned clockwise by degrees about
    its 0, 0, which lands on origin."""
    a = math.radians(degrees)

    def to_dp(x, y):
        x, y = (x + shift[0]) * scale, (y + shift[1]) * scale
        return origin[0] + x * math.cos(a) - y * math.sin(a), origin[1] + x * math.sin(a) + y * math.cos(a)
    return to_dp


def on_icon(cartridge):
    """The map from the cartridge's outline (mm, upright, tip at y 0) to dp, and its scale: base at BASE, pointing up
    and to the right."""
    o = cartridge["outline"]
    s = LENGTH / o["height"]
    return turned(BASE, POINTING, s, (-o["width"] / 2, -o["height"])), s


def parts(cartridge):
    """The case's and the bullet's polygons in dp."""
    to_dp, _ = on_icon(cartridge)
    return [[[to_dp(*p) for p in poly] for poly in polygons(cartridge["outline"][key])] for key in ("case", "bullet")]


ON_TAG = turned(CORNER, TURN)


def tag():
    """The label in dp: a rectangle, its corners cut off at the eyelet's end."""
    return [ON_TAG(*p) for p in ((0, CUT), (CUT, 0), (TAG_W, 0), (TAG_W, TAG_H), (CUT, TAG_H), (0, TAG_H - CUT))]


def circle(centre, r):
    """A circle on the label in 24 straight pieces, in dp."""
    return [ON_TAG(centre[0] + r * math.cos(math.pi * k / 12), centre[1] + r * math.sin(math.pi * k / 12)) for k in range(24)]


def writing(x0, x1, y):
    """A line of writing on the label from x0 to x1 at y, its ends round (each in six pieces), in dp."""
    r = PEN / 2
    ends = [(x1, a) for a in range(-90, 91, 30)] + [(x0, a) for a in range(90, 271, 30)]
    return [ON_TAG(x + r * math.cos(math.radians(a)), y + r * math.sin(math.radians(a))) for x, a in ends]


def string(cartridge):
    """The string from the eyelet to the knot on the case, TIED mm above the base: a curve in 16 pieces, in dp."""
    to_dp, _ = on_icon(cartridge)
    o = cartridge["outline"]
    (ex, ey), (kx, ky) = ON_TAG(*EYE), to_dp(o["width"] / 2, o["height"] - TIED)
    curve = ((ex, ey), (ex - 6, ey + 1), (kx + 2, ky + 7), (kx, ky))
    return [tuple((1 - t) ** 3 * p0 + 3 * (1 - t) ** 2 * t * p1 + 3 * (1 - t) * t ** 2 * p2 + t ** 3 * p3
                  for p0, p1, p2, p3 in zip(*curve)) for t in (i / 16 for i in range(17))]


def band(cartridge):
    """The string round the case at the knot, a little slanted, as round a cylinder seen from above, in dp."""
    to_dp, _ = on_icon(cartridge)
    w, h = cartridge["outline"]["width"], cartridge["outline"]["height"]
    return [to_dp(-0.6, h - TIED - 0.6), to_dp(w + 0.6, h - TIED + 0.6)]


def shifted(polys, dx, dy):
    return [[(x + dx, y + dy) for x, y in poly] for poly in polys]


def path_data(polys, closed=True):
    return " ".join("M" + " L".join("%.2f,%.2f" % p for p in poly) + (" Z" if closed else "") for poly in polys)


def fill(polys, colour, alpha=1, even_odd=False):
    return (f'    <path android:pathData="{path_data(polys)}" android:fillColor="{colour}"'
            + (f' android:fillAlpha="{alpha}"' if alpha != 1 else "")
            + (' android:fillType="evenOdd"' if even_odd else "") + " />\n")


def stroke(points, colour, width, closed=False):
    return (f'    <path android:pathData="{path_data([points], closed)}" android:strokeColor="{colour}"\n'
            f'        android:strokeWidth="{width:.2f}" android:strokeLineCap="round" android:strokeLineJoin="round" />\n')


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


def foreground(cartridge):
    """Back to front: the case's shadow; the cartridge, filled, lit down its left third (clipped to the part) and
    outlined, as Kaliberfinder's; the string round the case; the label on its shadow, with its writing and eyelet; and
    the string from the eyelet to the case."""
    to_dp, s = on_icon(cartridge)
    case, bullet = parts(cartridge)
    width, height = cartridge["outline"]["width"], cartridge["outline"]["height"]
    light = [[to_dp(x, y) for x, y in ((width * 0.18, 0), (width * 0.34, 0), (width * 0.34, height), (width * 0.18, height))]]
    body = [fill(shifted(case, 0.8, 2), "#000000", 0.12)]
    for polys, colour, hi in ((case, BRASS, BRASS_HI), (bullet, COPPER, COPPER_HI)):
        d = path_data(polys)
        body += [fill(polys, colour),
                 f'    <group>\n        <clip-path android:pathData="{d}" />\n    ' + fill(light, hi, 0.8)
                 + "    </group>\n",
                 f'    <path android:pathData="{d}" android:strokeColor="{INK}" android:strokeWidth="{LINE * s:.2f}"\n'
                 '        android:strokeLineJoin="round" />\n']
    body += [stroke(band(cartridge), STRING, 0.9), fill(shifted([tag()], 0.6, 1.6), "#000000", 0.14), fill([tag()], PAPER)]
    body += [fill([writing(*line)], LINES) for line in WRITING]
    body += [fill([circle(EYE, 2.3)], RING), stroke(circle(EYE, 2.3), RING_EDGE, 0.5, closed=True),
             fill([circle(EYE, 1.25)], STRING), stroke(string(cartridge), STRING, 0.8)]
    return vector("".join(body))


def monochrome(cartridge):
    """The cartridge's and the label's silhouettes, the bullet half filled, the eyelet and the writing cut out of the
    label, and the string between them: Android tints this layer by its alpha. Only the string touches another shape,
    and both are opaque, so nothing shows through."""
    case, bullet = parts(cartridge)
    label = [tag(), circle(EYE, 1.5)] + [writing(*line) for line in WRITING]
    return vector(fill(case, "#FFFFFFFF") + fill(bullet, "#FFFFFFFF", 0.6) + stroke(string(cartridge), "#FFFFFFFF", 0.9)
                  + fill(label, "#FFFFFFFF", even_odd=True))


def outputs():
    """Each file this script writes, with its content."""
    cartridge = {c["id"]: c for c in json.load(open(DATA))["cartridges"]}[CARTRIDGE]
    return {
        os.path.join(DRAWABLE, "ic_launcher_background.xml"): background(),
        os.path.join(DRAWABLE, "ic_launcher_foreground.xml"): foreground(cartridge),
        os.path.join(DRAWABLE, "ic_launcher_monochrome.xml"): monochrome(cartridge),
    }


def main():
    for name, text in outputs().items():
        with open(name, "w") as f:
            f.write(text)


if __name__ == "__main__":
    main()
