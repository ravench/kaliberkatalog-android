#!/usr/bin/env python3
"""The launcher icon: three index cards in a stack, the front one with the 7.5x55 mm Swiss (GP 11) on it, drawn from
its catalogue outline in Kaliberfinder's brass and copper; the two behind with tabs in the burgundy and amber of
Schiesstagebuch's journal; on the beige of both apps' icons.

    python3 tools/app_icon.py

Writes the adaptive icon's layers as vector drawables of 108 x 108 dp, the cards inside the 66 dp circle that every
launcher's mask keeps: ic_launcher_background (the beige), ic_launcher_foreground (the cards) and
ic_launcher_monochrome (their silhouettes, which Android tints for themed icons).
"""
import json, math, os, re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "app", "src", "main", "assets", "Cartridges.json")
DRAWABLE = os.path.join(ROOT, "app", "src", "main", "res", "drawable")
CARTRIDGE = "7.5x55 mm Swiss (GP 11)"
SIZE = 108          # dp, the adaptive icon's layers
SAFE = 66           # dp, the circle no launcher's mask cuts into
LINE = 3 / 8.6      # mm, the cartridge's outline, as Kaliberfinder's 3 px at 8.6 px per mm
W, H, R = 47, 29, 2.4   # dp, a card and its corners
STEP, TAB = 5, 5.5      # dp, how much of each card behind shows above the next one, and how high its tab stands
TAB_W, TAB_R = 17, 1.8  # dp, a tab's width and corners
HEADER = 9.2            # dp below a card's top, the burgundy line under its title
TILT = -5               # degrees, the stack turned anticlockwise
GAP = 0.8               # dp, between the cards' silhouettes in the monochrome layer

# Kaliberfinder's brass and copper; Schiesstagebuch's pages, lines, burgundy cover and amber ribbon; both apps' beige.
BRASS, BRASS_HI, COPPER, COPPER_HI, INK = "#E3C16F", "#F3DC9C", "#C0743A", "#DE9A62", "#3A2A12"
PAPER = ("#E9E4D6", "#EFEBE0", "#F4F1E8")  # the cards, back to front
LINES, COVER, AMBER = "#9AA6B2", "#7A2E2A", "#E3A230"
BEIGE = ("#F1E6D2", "#DCC9A8")
TABS = ((6, COVER), (26, AMBER))  # the back cards' tabs, back first: left edge in dp, colour


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


def place(x, y):
    """Card space, where the front card's top left corner is 0, 0, to the icon's dp: the stack centred and tilted."""
    a = math.radians(TILT)
    x, y = x - W / 2, y + (2 * STEP + TAB - H) / 2
    return SIZE / 2 + x * math.cos(a) - y * math.sin(a), SIZE / 2 + x * math.sin(a) + y * math.cos(a)


def rounded(x, y, w, h, radii):
    """A rectangle in card space, its corners (top right, bottom right, bottom left, top left) rounded by these radii,
    each arc in six straight pieces."""
    points = []
    for k, r in enumerate(radii):
        cx, cy = (x + w - r if k < 2 else x + r), (y + r if k in (0, 3) else y + h - r)
        points += [(cx + r * math.cos(math.radians(90 * k - 90 + 15 * i)),
                    cy + r * math.sin(math.radians(90 * k - 90 + 15 * i))) for i in range(7 if r else 1)]
    return points


def card(y):
    return rounded(0, y, W, H, (R,) * 4)


def bar(x0, x1, y, width):
    """A line in card space from x0 to x1 at y, its ends round."""
    return rounded(x0 - width / 2, y - width / 2, x1 - x0 + width, width, (width / 2,) * 4)


def writing():
    """The front card's title and the burgundy line under it, each with its colour."""
    return [(bar(4.5, 21, 5, 1.8), LINES), (bar(3, W - 3, HEADER, 0.9), COVER)]


def tab(x, y, h):
    return rounded(x, y, TAB_W, h, (TAB_R, 0, 0, TAB_R))


def on_card(cartridge):
    """The cartridge's polygons (case, bullet) and the map from its outline's mm (upright, tip at y 0) to card space:
    lying on the front card halfway between its header line and its bottom, tip to the right, 4 dp from either side."""
    o = cartridge["outline"]
    s = (W - 8) / o["height"]
    to_card = lambda x, y: (4 + (o["height"] - y) * s, (HEADER + H) / 2 + (x - o["width"] / 2) * s)
    return [[[to_card(*p) for p in poly] for poly in polygons(o[key])] for key in ("case", "bullet")], to_card, s


def path_data(polys):
    return " ".join("M" + " L".join("%.2f,%.2f" % place(*p) for p in poly) + " Z" for poly in polys)


def fill(polys, colour, alpha=1, even_odd=False):
    return (f'    <path android:pathData="{path_data(polys)}" android:fillColor="{colour}"'
            + (f' android:fillAlpha="{alpha}"' if alpha != 1 else "")
            + (' android:fillType="evenOdd"' if even_odd else "") + " />\n")


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
    """Back to front, each card on its shadow; then the front card's title, its header line and the cartridge, filled,
    lit down its left third (clipped to the part) and outlined, as Kaliberfinder's."""
    parts, to_card, s = on_card(cartridge)
    body = [fill([rounded(0.6, 1.6, W, H, (R,) * 4)], "#000000", 0.14)]
    for k, (x, colour) in enumerate(TABS):
        y = -(2 - k) * STEP
        if k:
            body.append(fill([rounded(0, y - 0.9, W, 1.4, (0.7,) * 4)], "#000000", 0.1))
        body += [fill([tab(x, y - TAB, TAB + 2)], colour), fill([card(y)], PAPER[k])]
    body += [fill([rounded(0, -0.9, W, 1.4, (0.7,) * 4)], "#000000", 0.1), fill([card(0)], PAPER[2])]
    body += [fill([shape], colour) for shape, colour in writing()]
    width, height = cartridge["outline"]["width"], cartridge["outline"]["height"]
    light = [[to_card(x, y) for x, y in ((width * 0.18, 0), (width * 0.34, 0), (width * 0.34, height),
                                         (width * 0.18, height))]]
    for polys, colour, hi in zip(parts, (BRASS, COPPER), (BRASS_HI, COPPER_HI)):
        d = path_data(polys)
        body += [fill(polys, colour),
                 f'    <group>\n        <clip-path android:pathData="{d}" />\n    ' + fill(light, hi, 0.8)
                 + "    </group>\n",
                 f'    <path android:pathData="{d}" android:strokeColor="{INK}" android:strokeWidth="{LINE * s:.2f}"\n'
                 '        android:strokeLineJoin="round" />\n']
    return vector("".join(body))


def monochrome(cartridge):
    """The cards' silhouettes apart from each other, the ones behind fainter, and the title, header line and cartridge
    cut out of the front one, its bullet half filled again: Android tints this layer by its alpha. No two shapes
    overlap, so none shows through another."""
    parts, _, _ = on_card(cartridge)
    (back_x, _), (middle_x, _) = TABS
    # The back card only up to the middle card's tab: the sliver right of it would read as a tab of its own.
    back = [tab(back_x, -2 * STEP - TAB, TAB + 1), rounded(0, -2 * STEP, middle_x - GAP, STEP - GAP, (0, 0, 0, R))]
    middle = [tab(middle_x, -STEP - TAB, TAB + 1), rounded(0, -STEP, W, STEP - GAP, (R, 0, 0, R))]
    front = [card(0)] + [shape for shape, _ in writing()] + sum(parts, [])
    return vector(fill(back, "#FFFFFFFF", 0.5) + fill(middle, "#FFFFFFFF", 0.75)
                  + fill(front, "#FFFFFFFF", even_odd=True) + fill(parts[1], "#FFFFFFFF", 0.5))


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
