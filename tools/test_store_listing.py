"""Checks on the Google Play listing's texts in store/: a folder per language, a file per field (Kaliberfinder's
test_store_listing.py for its App Store texts)."""
import json
import os
import re
import sys
import unittest

sys.path.insert(0, os.path.dirname(__file__))
import strings  # noqa: E402
from test_strings import swiss_problems  # noqa: E402
from translations import T  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
STORE = os.path.join(ROOT, "store")
LANGS = ["de", "en", "fr", "it", "es"]  # German, then translations.py's order
LIMITS = {"title": 30, "short_description": 80, "full_description": 4000}  # Google Play's, in characters


def text(lang, field):
    with open(os.path.join(STORE, lang, field + ".txt"), encoding="utf-8") as f:
        return f.read().rstrip("\n")


class StoreTextTests(unittest.TestCase):
    def test_the_apps_languages_within_google_plays_limits(self):
        res = os.path.join(ROOT, "app", "src", "main", "res")
        app = {"en"} | {d[len("values-"):] for d in os.listdir(res) if re.fullmatch(r"values-[a-z]{2}", d)}
        self.assertEqual(set(os.listdir(STORE)), app)
        self.assertEqual(set(LANGS), app)
        for lang in LANGS:
            self.assertEqual(sorted(os.listdir(os.path.join(STORE, lang))), sorted(f + ".txt" for f in LIMITS), lang)
            self.assertEqual(text(lang, "title"), "Kaliberkatalog", lang)  # the same name in every language
            for field, limit in LIMITS.items():
                value = text(lang, field)
                self.assertTrue(value and value == value.strip(), (lang, field))
                self.assertLessEqual(len(value), limit, (lang, field))

    def test_the_number_of_calibres_holds(self):
        """«Über 700 Kaliber», as in Kaliberfinder's listing: both descriptions name it, and the list shows more with
        the developer switches off (well sourced, no cannon rounds: CartridgeCatalog.visible)."""
        with open(os.path.join(ROOT, "app", "src", "main", "assets", "Cartridges.json"), encoding="utf-8") as f:
            shown = [c for c in json.load(f)["cartridges"] if c["wellSourced"] and c["category"] != "Kanone"]
        self.assertGreater(len(shown), 700)
        claims = {"de": "Über 700 Kaliber", "en": "Over 700 calibres", "fr": "Plus de 700 calibres",
                  "it": "Oltre 700 calibri", "es": "Más de 700 calibres"}
        for lang, claim in claims.items():
            for field in ("short_description", "full_description"):
                self.assertIn(claim, text(lang, field), (lang, field))
                # No other count: the only other long number is the GP 1890's year.
                self.assertLessEqual(set(re.findall(r"\d{3,}", text(lang, field))), {"700", "1890"}, (lang, field))

    def test_every_description_says_matching_dimensions_are_no_clearance_to_fire(self):
        words = {"de": "keine Freigabe zum Schiessen", "en": "do not mean a cartridge is safe to fire", "fr": "n'autorise pas le tir",
                 "it": "non autorizza a sparare", "es": "no autoriza a disparar"}
        for lang, sentence in words.items():
            self.assertIn(sentence, text(lang, "full_description"), lang)

    def test_every_description_names_all_five_languages(self):
        named = {"de": ["Deutsch", "Englisch", "Französisch", "Italienisch", "Spanisch"],
                 "en": ["English", "German", "French", "Italian", "Spanish"],
                 "fr": ["Français", "allemand", "anglais", "italien", "espagnol"],
                 "it": ["Italiano", "tedesco", "francese", "inglese", "spagnolo"],
                 "es": ["Español", "alemán", "inglés", "francés", "italiano"]}
        for lang, words in named.items():
            for word in words:
                self.assertIn(word, text(lang, "full_description"), lang)

    def test_the_search_examples_are_words_the_search_reads(self):
        """Every word a description quotes for the search is one of the group words the app's search reads in that
        language (strings.GROUPS, tools/translations.py)."""
        quote = r"[«“] ?([^»”]+?) ?[»”]"
        for column, lang in enumerate(LANGS):
            texts = strings.GROUPS if lang == "de" else [T[key][column - 1] for key in strings.GROUPS]
            words = {w.strip() for t in texts for w in t.split(",")}
            # «wie «Platzpatrone» oder «Schrot»», «like “blank” or “shotshell”», …
            examples = re.findall(rf"(?:wie|like|comme|come|como) {quote} (?:oder|or|ou|o) {quote}",
                                  text(lang, "full_description"))
            self.assertEqual(len(examples), 1, lang)
            for word in examples[0]:
                self.assertIn(word, words, lang)

    def test_no_text_names_a_single_standard_as_the_source(self):
        """Many calibres have no C.I.P. or SAAMI sheet (Raven, for Kaliberfinder's listing)."""
        for lang in LANGS:
            for field in LIMITS:
                self.assertIsNone(re.search(r"SAAMI|\bC\.? ?I\.? ?P\b", text(lang, field), re.I), (lang, field))

    def test_the_screenshots_are_taken_in_every_language(self):
        """StoreScreensTest takes the phone screenshots in each of the listing's languages, each in its own language."""
        with open(os.path.join(ROOT, "app", "src", "test", "kotlin", "ch", "giotta", "kaliberkatalog", "StoreScreensTest.kt"),
                  encoding="utf-8") as f:
            tours = re.findall(r'@Test @Config\(qualifiers = "([a-z]{2})-.*\n\s*fun \w+\(\) = tour\("([a-z]{2})"\)', f.read())
        self.assertEqual(sorted(language for _, language in tours), sorted(LANGS))
        for qualifier, language in tours:
            self.assertEqual(qualifier, language)

    def test_the_german_texts_are_swiss(self):
        self.assertEqual(swiss_problems([text("de", field) for field in LIMITS]), [])


if __name__ == "__main__":
    unittest.main()
