"""Checks on tools/app_icon.py and the launcher icon it writes (Kaliberfinder's test_app_icon.py)."""
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


class AppIconTests(unittest.TestCase):
    def test_generated_files_are_current(self):
        for path, content in app_icon.outputs().items():
            with open(path, encoding="utf-8") as f:
                self.assertEqual(f.read(), content, f"{path}: run python3 tools/app_icon.py")

    def test_the_label_hangs_clear_of_the_cartridge_on_a_string_tied_round_the_case(self):
        with open(app_icon.DATA, encoding="utf-8") as f:
            cartridge = {c["id"]: c for c in json.load(f)["cartridges"]}[app_icon.CARTRIDGE]
        (case,), (bullet,) = app_icon.parts(cartridge)
        tag = app_icon.tag()
        # Clear of it: the themed icon would merge them into one shape.
        self.assertFalse(overlap(case, tag))
        self.assertFalse(overlap(bullet, tag))
        string = app_icon.string(cartridge)
        self.assertTrue(inside(string[0], app_icon.circle(app_icon.EYE, 1.25)))  # through the eyelet
        self.assertTrue(inside(string[-1], case))

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
