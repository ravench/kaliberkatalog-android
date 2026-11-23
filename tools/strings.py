#!/usr/bin/env python3
"""Writes the app's texts from tools/translations.py: app/src/main/res/values*/strings.xml, GroupWords.kt and
CountryNames.kt.

    python3 tools/strings.py

English is the default (values/), which every language the app lacks shows too; German, the source, is
values-de. Fails if a text has no translation, so new UI text can't ship untranslated. The countries' names
come from calibre-data, which names each in every language of the app, as for Kaliberfinder (localize.py).
"""
import json
import os
import sys
from xml.sax.saxutils import escape

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.dirname(__file__))
sys.path.insert(0, os.path.join(ROOT, "calibre-data"))
from translations import T  # noqa: E402
from cartridges import COUNTRY_NAMES  # noqa: E402

RES = os.path.join(ROOT, "app", "src", "main", "res")
KOTLIN = os.path.join(ROOT, "app", "src", "main", "kotlin", "ch", "giotta", "kaliberkatalog")
GROUP_WORDS = os.path.join(KOTLIN, "GroupWords.kt")
COUNTRY_NAMES_KT = os.path.join(KOTLIN, "CountryNames.kt")

# Resource name -> German text, the key of its translations.
STRINGS = {
    "app_name": "Kaliberkatalog",
    "calibres": "Kaliber",
    "search_prompt": "Kaliber, z. B. 9 mm Luger",
    "clear_search": "Suche löschen",
    "no_results": "Keine Ergebnisse für «%1$s»",
    "no_results_hint": "Prüfe die Schreibweise oder versuche eine andere Suche.",
    "category_rimfire": "Randfeuer",
    "category_handgun": "Kurzwaffe",
    "category_rifle": "Langwaffe",
    "category_black_powder": "Schwarzpulver",
    "category_shotgun": "Flinte",
    "category_air": "Luftdruck",
    "category_cannon": "Kanone",
    "back": "Zurück",
    "dimensions": "Masse",
    "overall_length": "Gesamtlänge",
    "case_length": "Hülsenlänge",
    "shell_length_open": "Hülsenlänge (offen)",
    "rim_diameter": "Randdurchmesser",
    "belt_diameter": "Gürteldurchmesser",
    "base_diameter": "Bodendurchmesser",
    "shoulder_diameter": "Schulterdurchmesser",
    "neck_diameter": "Halsdurchmesser",
    "bullet_diameter": "Geschossdurchmesser",
    "primer": "Zündhütchen",
    "primer_rimfire": "Randfeuer, ohne Zündhütchen",
    "case_base_with_primer": "Boden mit Zündhütchen %1$s",
    "primer_small_pistol": "Pistole klein",
    "primer_large_pistol": "Pistole gross",
    "primer_small_rifle": "Büchse klein",
    "primer_large_rifle": "Büchse gross",
    "primer_miniature": "Miniatur",
    "primer_shotshell_209": "Schrot (209)",
    "primer_bmg_35": ".50 BMG (#35)",
    "primer_berdan_nitro": "Berdan Nitro",
    "category": "Kategorie",
    "kind": "Bauart",
    "kind_rimless": "Randlos",
    "kind_rimmed": "Mit Rand",
    "kind_semi_rimmed": "Halbrand",
    "kind_belted": "Gürtelhülse",
    "kind_rimfire": "Randfeuer",
    "kind_shot": "Schrotpatrone",
    "kind_ball": "Rundkugel",
    "kind_minie": "Minié-Geschoss",
    "kind_pellet": "Diabolo",
    "kind_other": "Andere",
    "origin": "Herkunft",
    "prevalence": "Verbreitung",
    "worldwide": "Weltweit verbreitet",
    "also_known_as": "Auch bekannt als",
    "rounded_values": "Gerundete Richtwerte, keine Wiederladedaten.",
    "colour_codes": "Farbcodes",
    "tip_order": "Spitze: Farben von der Spitze abwärts.",
    "part_tip": "Geschossspitze",
    "part_base": "Hülsenboden",
    "part_annulus": "Zündhütchenring",
    "meaning_ball": "Standardgeschoss",
    "meaning_armour_piercing": "Panzerbrechend",
    "meaning_incendiary": "Brand",
    "meaning_explosive": "Spreng",
    "meaning_tracer": "Leuchtspur",
    "meaning_spotting": "Markierung",
    "meaning_subsonic": "Unterschall",
    "meaning_frangible": "Zerfallend",
    "meaning_proof": "Beschuss",
    "meaning_reduced_charge": "Reduzierte Ladung",
    "colour_black": "Schwarz",
    "colour_white": "Weiss",
    "colour_grey": "Grau",
    "colour_silver": "Silber",
    "colour_red": "Rot",
    "colour_orange": "Orange",
    "colour_yellow": "Gelb",
    "colour_green": "Grün",
    "colour_blue": "Blau",
    "colour_purple": "Violett",
    "colour_brown": "Braun",
    # The sources page (Kaliberfinder's SourcesView) and the parts its sections name (Cartridge.SourceGroup.title) that
    # the calibre page has no text for.
    "sources": "Quellen",
    "own_measurement": "Eigene Messung",
    "pages": "S. %1$s",
    "opens_website": "Öffnet die Website",
    "source_shoulder_angle": "Schulterwinkel",
    "source_nose": "Kopflänge",
    "source_flat": "Breite der Abflachung",
    "source_crimp_length": "Bördellänge",
    "source_bullet": "Geschoss",
    "source_case": "Hülsenmaterial",
    # The lookalike group's row and page (Kaliberfinder's LookalikeGroupView) and what tells its calibres apart
    # (LookalikeGroup.title).
    "group": "Gruppe",
    "group_title": "Gruppe %1$s",
    "differ_in": "Unterscheiden sich in: %1$s",
    "differs_rim": "Hülsenrand",
    "differs_bore": "Laufdurchmesser",
    "differs_length": "Länge",
    "differs_seating": "Setztiefe des Geschosses",
    "differs_load": "Ladung",
    "differs_ignition": "Zündung",
    # The dimensioned drawing (Kaliberfinder's MeasurementsView): its row on the calibre page, and the short names over its
    # values (DimensionLayout.Part.title); a label is read with the calibre page's name instead (Part.spokenTitle).
    "show_measurements": "Masszeichnung anzeigen",
    "drawing_bullet": "Geschoss",
    "drawing_neck": "Hals",
    "drawing_shoulder": "Schulter",
    "drawing_base": "Boden",
    "drawing_belt": "Gürtel",
    "drawing_rim": "Rand",
    "drawing_overall": "Gesamt",
    "drawing_case_length": "Hülse",
    # The settings (Kaliberfinder's SettingsView) and the units' names (LengthUnit.title). The ODbL line is this app's own:
    # the notice the Open Database License asks of an app that shows calibre-data.
    "settings": "Einstellungen",
    "units": "Einheiten",
    "unit_metric": "Metrisch (mm)",
    "unit_imperial": "Imperial (Zoll)",
    "about": "Info",
    "version": "Version",
    "calibre_data": "Kaliberdaten",
    "odbl_notice": "Enthält Informationen aus calibre-data, die unter der Open Database License (ODbL) verfügbar sind.",
    # The developer's switches (Kaliberfinder's DeveloperOptionsView) and the notes under them. The cannon's is this app's
    # own: Kaliberfinder's adds «zu gross für den Bildschirm», for its 1:1 view, which this app has not.
    "developer_options": "Entwickleroptionen",
    "show_unsourced": "Unbelegte Kaliber zeigen",
    "show_unsourced_note": "Kaliber, deren Masse in calibre-data nicht gut belegt sind: Teile ihrer Zeichnung sind geschätzt.",
    "show_cannon": "Kanonenkaliber zeigen",
    "show_cannon_note": "Munition von 20 bis 57 mm.",
    "show_original": "Ursprüngliche Materialien zeigen",
    "show_original_note": "Zeichnet Geschoss und Hülse, wie die Patrone zuerst ausgegeben wurde, statt wie man sie heute meist findet.",
}
# The keys of Cartridge.groupWordsKey: searched in every language at once, so GroupWords.kt rather than resources.
GROUPS = ["Platzpatrone, Knallpatrone, Knall", "Schrotpatrone, Schrot, Flintenpatrone", "Rundkugel, Bleikugel",
          "Diabolo, Luftgewehrkugel, Luftgewehr", "Minié-Geschoss"]
# Resource folder -> the column of T, None for the German key itself.
FOLDERS = {"values": 0, "values-de": None, "values-fr": 1, "values-it": 2, "values-es": 3}


def android(text):
    """A text as strings.xml needs it: XML-escaped, with Android's backslash escapes.

    Not handled, as no text has them: tabs and runs of spaces (aapt2 collapses them), unnumbered %s.
    """
    text = escape(text).replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"').replace("\n", "\\n")
    return "\\" + text if text[:1] in "@?" else text


def strings_xml(folder):
    column, language = FOLDERS[folder], "en" if folder == "values" else folder.removeprefix("values-")
    lines = ['<?xml version="1.0" encoding="utf-8"?>',
             "<!-- Generated by tools/strings.py from tools/translations.py and calibre-data's country names. -->",
             "<resources>"]
    # The language of these texts, which Android picks from the phone's languages: calibre-data's texts follow it.
    lines.append(f'    <string name="language">{language}</string>')
    for name, german in STRINGS.items():
        text = german if column is None else T[german][column]
        lines.append(f'    <string name="{name}">{android(text)}</string>')
    for code, names in sorted(COUNTRY_NAMES.items()):
        lines.append(f'    <string name="country_{code.lower()}">{android(names[language])}</string>')
    return "\n".join(lines + ["</resources>", ""])


def group_words_kt():
    def kotlin(text):
        return json.dumps(text, ensure_ascii=False).replace("$", "\\$")
    entries = [f"    {kotlin(key)} to listOf({', '.join(kotlin(t) for t in T[key])})," for key in GROUPS]
    return "\n".join([
        "// Generated by tools/strings.py from tools/translations.py.",
        "package ch.giotta.kaliberkatalog",
        "",
        "import ch.giotta.kaliberkatalog.catalog.CartridgeCatalog",
        "",
        "/** Each group's words in every language of the app, whatever language it runs in, by its German key. */",
        "val wordsByGroup = mapOf(",
        *entries,
        ").mapValues { (key, translations) -> CartridgeCatalog.groupWords(key, translations) }",
        "",
    ])


def country_names_kt():
    entries = [f'    "{code}" -> R.string.country_{code.lower()}' for code in sorted(COUNTRY_NAMES)]
    return "\n".join([
        "// Generated by tools/strings.py from calibre-data's country names.",
        "package ch.giotta.kaliberkatalog",
        "",
        "import androidx.annotation.StringRes",
        "",
        "/** The name of a calibre-data country, by its code; null for one newer than the app. */",
        "@StringRes",
        "fun countryTitle(code: String) = when (code) {",
        *entries,
        "    else -> null",
        "}",
        "",
    ])


def outputs():
    """Path -> content of every generated file."""
    files = {os.path.join(RES, folder, "strings.xml"): strings_xml(folder) for folder in FOLDERS}
    files[GROUP_WORDS] = group_words_kt()
    files[COUNTRY_NAMES_KT] = country_names_kt()
    return files


def main():
    missing = sorted((set(STRINGS.values()) | set(GROUPS)) - set(T))
    if missing:
        sys.exit("Missing translations:\n  " + "\n  ".join(missing))
    for path, content in outputs().items():
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            f.write(content)


if __name__ == "__main__":
    main()
