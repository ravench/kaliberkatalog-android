"""Helpers for the PR screens in .github/workflows/build.yml: before/after screenshots on pull requests.

Kaliberfinder's tools/pr_screens.py without its iOS parts: PRScreensTest (Robolectric and Roborazzi) writes the
screens straight under their names.

    python3 tools/pr_screens.py changes <before dir> <after dir> <out dir> [<tour file>]
    python3 tools/pr_screens.py comment <out dir> <image base url> <run url>
"""
import filecmp
import json
import os
import shutil
import sys

MARKER = "<!-- pr-screens -->"
TOUR = "app/src/test/kotlin/ch/giotta/kaliberkatalog/PRScreensTest.kt"


def _pngs(folder):
    return set(n for n in os.listdir(folder) if n.endswith(".png")) if os.path.isdir(folder) else set()


def same(a, b, tolerance=48, max_pixels=20, blur=2):
    """True if two screenshots look the same.

    Identical files are the same. Otherwise, with Pillow, both are blurred a little and a few
    pixels off by a few levels still count as the same: text rasterised at a fractional offset
    differs from run to run along its edges, but little once blurred, while a changed word or
    control stays well over the tolerance. Without Pillow any difference counts.
    """
    if filecmp.cmp(a, b, shallow=False):
        return True
    try:
        from PIL import Image, ImageChops, ImageFilter
    except ImportError:
        return False
    try:
        ia, ib = Image.open(a).convert("RGB"), Image.open(b).convert("RGB")
    except OSError:  # not an image Pillow can read
        return False
    if ia.size != ib.size:
        return False
    if blur:
        ia, ib = ia.filter(ImageFilter.GaussianBlur(blur)), ib.filter(ImageFilter.GaussianBlur(blur))
    r, g, bl = ImageChops.difference(ia, ib).split()
    worst = ImageChops.lighter(ImageChops.lighter(r, g), bl)
    return sum(worst.histogram()[tolerance + 1:]) <= max_pixels


def changes(before_dir, after_dir, tour=None):
    """[(name, status)] for screens that are new, changed, gone or missed, by name.

    Both runs render the same way minutes apart, so an unchanged screen gives (almost)
    the same image. A screen of the base that this run lacks is "gone" only if the tour
    (the text of its Kotlin file) no longer names it; while the tour still has the stop,
    this run just did not take it: "missed".
    """
    before, after = _pngs(before_dir), _pngs(after_dir)
    result = []
    for name in sorted(before | after):
        if name not in before:
            result.append((name, "new"))
        elif name not in after:
            stem = os.path.splitext(name)[0]
            result.append((name, "missed" if tour is not None and f'"{stem}"' in tour else "gone"))
        elif not same(os.path.join(before_dir, name), os.path.join(after_dir, name)):
            result.append((name, "changed"))
    return result


def collect(before_dir, after_dir, out_dir, tour=None):
    """Copies the changed screens to out_dir/before and out_dir/after and writes out_dir/changes.json."""
    found = changes(before_dir, after_dir, tour)
    for side, folder in (("before", before_dir), ("after", after_dir)):
        os.makedirs(os.path.join(out_dir, side), exist_ok=True)
        for name, _ in found:
            src = os.path.join(folder, name)
            if os.path.exists(src):
                shutil.copyfile(src, os.path.join(out_dir, side, name))
    summary = {"changes": found, "total": len(_pngs(after_dir)), "base": len(_pngs(before_dir))}
    with open(os.path.join(out_dir, "changes.json"), "w") as f:
        json.dump(summary, f)
    return summary


def _img(url):
    return f'<img src="{url}" width="250">'


def comment(summary, base_url, run_url):
    """Markdown for the pull request comment; base_url is where out_dir/before and /after are served."""
    found, total = summary["changes"], summary["total"]
    lines = [MARKER, "### Screenshots", ""]
    if summary.get("base", 1) == 0:
        lines += ["The screens of the base branch could not be taken, so every screen is shown as new.", ""]
    if not found:
        lines += [f"No visible change on the {total} screens of the tour.",
                  f"If this pull request changes a screen the tour does not reach, add it to `{TOUR}`."]
    else:
        lines += [f"{len(found)} of {total} screens look different (phone, German).", "",
                  "| Screen | Before | After |", "| --- | --- | --- |"]
        for name, status in found:
            before = "—" if status == "new" else _img(f"{base_url}/before/{name}")
            after = "—" if status in ("gone", "missed") else _img(f"{base_url}/after/{name}")
            note = {"changed": "", "missed": " (not taken in this run)"}.get(status, f" ({status})")
            label = os.path.splitext(name)[0] + note
            lines.append(f"| {label} | {before} | {after} |")
    lines += ["", f"All screens of this run: [workflow artifacts]({run_url})."]
    return "\n".join(lines) + "\n"


def main(argv):
    if argv[1:2] == ["changes"] and len(argv) in (5, 6):
        tour = None
        if len(argv) == 6:
            with open(argv[5]) as f:
                tour = f.read()
        summary = collect(argv[2], argv[3], argv[4], tour)
        for name, status in summary["changes"]:
            print(f"{status}: {name}")
    elif argv[1:2] == ["comment"] and len(argv) == 5:
        with open(os.path.join(argv[2], "changes.json")) as f:
            sys.stdout.write(comment(json.load(f), argv[3], argv[4]))
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main(sys.argv)
