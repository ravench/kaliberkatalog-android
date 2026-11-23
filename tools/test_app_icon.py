"""Checks on tools/app_icon.py and the launcher icon it writes."""
import json
import math
import os
import re
import sys
import unittest

sys.path.insert(0, os.path.dirname(__file__))
import app_icon  # noqa: E402

MAIN = os.path.join(app_icon.ROOT, "app", "src", "main")


def inside(point, polygon):
    """Whether the point lies inside the polygon (ray casting)."""
    x, y = point
    crossings = 0
    for (x1, y1), (x2, y2) in zip(polygon, polygon[1:] + polygon[:1]):
        if (y1 > y) != (y2 > y) and x < x1 + (y - y1) * (x2 - x1) / (y2 - y1):
            crossings += 1
    return crossings % 2 == 1


def overlap(p, q):
    """Whether two polygons overlap: a corner of one inside the other, or two of their edges crossing."""
    def side(a, b, c):
        return (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0])

    def edges(poly):
        return list(zip(poly, poly[1:] + poly[:1]))
    crossing = any(side(a, b, c) * side(a, b, d) < 0 and side(c, d, a) * side(c, d, b) < 0
                   for a, b in edges(p) for c, d in edges(q))
    return crossing or any(inside(c, q) for c in p) or any(inside(c, p) for c in q)


def distance(point, polygon):
    """The distance from the point to the polygon's edge."""
    def to_edge(a, b):
        dx, dy = b[0] - a[0], b[1] - a[1]
        t = max(0, min(1, ((point[0] - a[0]) * dx + (point[1] - a[1]) * dy) / (dx * dx + dy * dy)))
        return math.hypot(point[0] - a[0] - t * dx, point[1] - a[1] - t * dy)
    return min(to_edge(a, b) for a, b in zip(polygon, polygon[1:] + polygon[:1]))


def gap(p, q):
    """The distance between two polygons that don't overlap: from a corner of one to an edge of the other."""
    return min([distance(c, q) for c in p] + [distance(c, p) for c in q])


class AppIconTests(unittest.TestCase):
    def test_generated_files_are_current(self):
        for path, content in app_icon.outputs().items():
            with open(path, encoding="utf-8") as f:
                self.assertEqual(f.read(), content, f"{path}: run python3 tools/app_icon.py")

    def test_the_label_hangs_clear_of_the_cartridge_on_a_string_tied_round_the_case(self):
        with open(app_icon.DATA, encoding="utf-8") as f:
            cartridge = {c["id"]: c for c in json.load(f)["cartridges"]}[app_icon.CARTRIDGE]
        (case,), (bullet,) = app_icon.parts(cartridge)
        label = app_icon.label()
        # 2 dp clear at least: closer, the themed icon merges them into one shape at home-screen size.
        for part in (case, bullet):
            self.assertFalse(overlap(part, label))
            self.assertGreater(gap(part, label), 2)
        # The bullet stands on the case's mouth, not over the case, where the themed icon's half alpha would show.
        self.assertTrue(all(not inside(p, case) or distance(p, case) < 0.01 for p in bullet))
        # The eyelet and the writing lie on the label, apart from each other: the themed icon cuts them out of it.
        marks = [app_icon.circle(app_icon.EYE, app_icon.RIM)] + [app_icon.writing(*line) for line in app_icon.WRITING]
        for k, mark in enumerate(marks):
            self.assertTrue(all(inside(p, label) for p in mark), k)
            self.assertFalse(any(overlap(mark, other) for other in marks[k + 1:]), k)
        string = app_icon.string(cartridge)
        self.assertTrue(inside(string[0], app_icon.circle(app_icon.EYE, app_icon.HOLE)))  # through the eyelet
        self.assertTrue(inside(string[-1], case))
        self.assertFalse(any(inside(p, bullet) for p in string))
        # The themed icon's string starts at the rim of the hole it cuts, so the hole stays open.
        clear = app_icon.string(cartridge, clear=app_icon.CUT_OUT + app_icon.CORD / 2)
        self.assertGreaterEqual(min(math.dist(p, string[0]) for p in clear), app_icon.CUT_OUT + app_icon.CORD / 2)
        self.assertEqual(clear[-1], string[-1])

    def test_everything_stays_inside_the_circle_every_mask_keeps(self):
        centre = app_icon.SIZE / 2
        for name in ("ic_launcher_foreground.xml", "ic_launcher_monochrome.xml"):
            with open(os.path.join(app_icon.DRAWABLE, name), encoding="utf-8") as f:
                paths = re.findall(r"<path\b[^>]*>", f.read())
            far = 0
            for path in paths:
                data = re.search(r'android:pathData="([^"]*)"', path).group(1)
                self.assertLessEqual(set(re.findall(r"[A-Za-z]", data)), {"M", "L", "Z"}, "straight pieces only")
                width = re.search(r'android:strokeWidth="([\d.]+)"', path)
                for x, y in re.findall(r"(-?[\d.]+),(-?[\d.]+)", data):
                    far = max(far, math.hypot(float(x) - centre, float(y) - centre) + (float(width.group(1)) / 2 if width else 0))
            self.assertLessEqual(far, app_icon.SAFE / 2, name)

    def test_the_app_shows_the_adaptive_icon(self):
        with open(os.path.join(MAIN, "AndroidManifest.xml"), encoding="utf-8") as f:
            self.assertIn('android:icon="@mipmap/ic_launcher"', f.read())
        with open(os.path.join(MAIN, "res", "mipmap-anydpi", "ic_launcher.xml"), encoding="utf-8") as f:
            icon = f.read()
        for layer in ("background", "foreground", "monochrome"):
            self.assertIn(f'<{layer} android:drawable="@drawable/ic_launcher_{layer}" />', icon)


if __name__ == "__main__":
    unittest.main()
