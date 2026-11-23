"""Checks on the app's texts: tools/translations.py and what tools/strings.py makes of it."""
import os
import re
import sys
import unittest

sys.path.insert(0, os.path.dirname(__file__))
import strings  # noqa: E402
from translations import T  # noqa: E402

# The German texts are Swiss Standard German: ss, «», decimal point (Kaliberfinder's test_translations.py).
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[a-zA-Z]")
SWISS = [(re.compile(r"ß"), "ss"), (re.compile(r"[„“”]"), "«»"), (re.compile(r"\d,\d"), "decimal point, 10.9")]


def swiss_problems(texts):
    return [(text, hint) for text in texts for pattern, hint in SWISS if pattern.search(text)]


class StringsTests(unittest.TestCase):
    def test_generated_files_are_current(self):
        for path, content in strings.outputs().items():
            with open(path, encoding="utf-8") as f:
                self.assertEqual(f.read(), content, f"{path}: run python3 tools/strings.py")

    def test_every_text_has_four_translations(self):
        for key, values in T.items():
            self.assertIsInstance(values, tuple, key)  # a missing comma makes a one-word tuple a string
            self.assertEqual(len(values), 4, key)

    def test_every_text_is_translated_and_every_translation_used(self):
        used = set(strings.STRINGS.values()) | set(strings.GROUPS)
        self.assertEqual(sorted(used - set(T)), [])
        self.assertEqual(sorted(set(T) - used), [])

    def test_english_is_the_default_and_german_its_own_folder(self):
        xml = strings.outputs()
        default = xml[os.path.join(strings.RES, "values", "strings.xml")]
        self.assertIn('<string name="category_rimfire">Rimfire</string>', default)
        self.assertIn('<string name="category_rimfire">Randfeuer</string>', xml[os.path.join(strings.RES, "values-de", "strings.xml")])
        with open(os.path.join(strings.RES, "resources.properties")) as f:
            self.assertIn("unqualifiedResLocale=en", f.read())

    def test_every_country_has_its_name_from_calibre_data(self):
        """As Kaliberfinder's test_every_catalogue_country_has_a_translated_name: calibre-data names each country in
        every language, so translations.py holds none of them twice."""
        xml = strings.outputs()
        for folder, name in [("values", "Switzerland"), ("values-de", "Schweiz"), ("values-fr", "Suisse"),
                             ("values-it", "Svizzera"), ("values-es", "Suiza")]:
            self.assertIn(f'<string name="country_ch">{name}</string>', xml[os.path.join(strings.RES, folder, "strings.xml")])
        titles = xml[strings.COUNTRY_NAMES_KT]
        for code, names in strings.COUNTRY_NAMES.items():
            self.assertEqual(set(names), {"en", "de", "fr", "it", "es"}, code)
            self.assertIn(f'"{code}" -> R.string.country_{code.lower()}', titles)
            self.assertNotIn(names["de"], T)

    def test_placeholders_survive_translation(self):
        for key, values in T.items():
            for value in values:
                self.assertEqual(sorted(PLACEHOLDER.findall(value)), sorted(PLACEHOLDER.findall(key)), f"{key!r} -> {value!r}")
                self.assertTrue(value.strip(), key)

    def test_french_quotes_keep_their_no_break_spaces(self):
        self.assertIn("«\u00a0%1$s\u00a0»", T["Keine Ergebnisse für «%1$s»"][1])

    def test_the_app_keeps_the_libraries_texts_in_its_languages_only(self):
        languages = ["en" if folder == "values" else folder.removeprefix("values-") for folder in strings.FOLDERS]
        with open(os.path.join(strings.RES, "..", "..", "..", "build.gradle.kts"), encoding="utf-8") as f:
            line = next(line for line in f if "localeFilters" in line)
        self.assertEqual(sorted(re.findall(r'"([a-z]{2})"', line)), sorted(languages))

    def test_android_escapes(self):
        self.assertEqual(strings.android("l'orthographe"), "l\\'orthographe")
        self.assertEqual(strings.android('a "b" & <c>'), 'a \\"b\\" &amp; &lt;c&gt;')
        self.assertEqual(strings.android("@home"), "\\@home")

    def test_german_is_swiss(self):
        self.assertEqual(swiss_problems(list(T)), [])
        found = swiss_problems(["Grösse", "Größe", "„Schrot“", "Abstand 1,5 cm", "10.9"])
        self.assertEqual([text for text, _ in found], ["Größe", "„Schrot“", "Abstand 1,5 cm"])

    def test_texts_both_apps_have_match_kaliberfinder(self):
        """Kaliberfinder's translations (2026-11-23) for the texts both apps show, so the apps read alike."""
        self.assertEqual(T["Kaliber, z. B. 9 mm Luger"][0], "Calibre, e.g. 9 mm Luger")
        self.assertEqual(T["Randfeuer"], ("Rimfire", "Percussion annulaire", "Percussione anulare", "Fuego anular"))


if __name__ == "__main__":
    unittest.main()
