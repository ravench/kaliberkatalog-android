"""Checks on tools/app_icon.py and the launcher icon it writes (Kaliberfinder's test_app_icon.py)."""
import math
import os
import re
import sys
import unittest

sys.path.insert(0, os.path.dirname(__file__))
import app_icon  # noqa: E402

MAIN = os.path.join(app_icon.ROOT, "app", "src", "main")


class AppIconTests(unittest.TestCase):
    def test_generated_files_are_current(self):
        for path, content in app_icon.outputs().items():
            with open(path, encoding="utf-8") as f:
                self.assertEqual(f.read(), content, f"{path}: run python3 tools/app_icon.py")

    def test_three_different_cartridges(self):
        self.assertEqual(len(set(app_icon.CARTRIDGES)), 3)

    def test_the_cartridges_stay_inside_the_circle_every_mask_keeps(self):
        for name in ("ic_launcher_foreground.xml", "ic_launcher_monochrome.xml"):
            with open(os.path.join(app_icon.DRAWABLE, name), encoding="utf-8") as f:
                points = [(float(x), float(y)) for x, y in re.findall(r"(-?[\d.]+),(-?[\d.]+)", f.read())]
            centre = app_icon.SIZE / 2
            far = max(math.hypot(x - centre, y - centre) for x, y in points)
            self.assertLessEqual(far, app_icon.SAFE / 2 + 0.05, name)
            self.assertGreater(far, app_icon.SAFE / 2 - 1, f"{name}: as large as fits")

    def test_the_app_shows_the_adaptive_icon(self):
        with open(os.path.join(MAIN, "AndroidManifest.xml"), encoding="utf-8") as f:
            self.assertIn('android:icon="@mipmap/ic_launcher"', f.read())
        with open(os.path.join(MAIN, "res", "mipmap-anydpi", "ic_launcher.xml"), encoding="utf-8") as f:
            icon = f.read()
        for layer in ("background", "foreground", "monochrome"):
            self.assertIn(f'<{layer} android:drawable="@drawable/ic_launcher_{layer}" />', icon)


if __name__ == "__main__":
    unittest.main()
