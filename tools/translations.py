"""The app's texts: German source -> (English, French, Italian, Spanish).

Kaliberfinder's tools/translations.py has the same format: a text both apps show takes Kaliberfinder's
translation verbatim. Use %1$s, %2$s for values (Android's format).
"""

T = {
    "Kaliberkatalog": ("Kaliberkatalog", "Kaliberkatalog", "Kaliberkatalog", "Kaliberkatalog"),
    "Kaliber": ("Calibres", "Calibres", "Calibri", "Calibres"),
    "Kaliber, z. B. 9 mm Luger": ("Calibre, e.g. 9 mm Luger", "Calibre, p. ex. 9 mm Luger", "Calibro, p. es. 9 mm Luger",
                                  "Calibre, p. ej. 9 mm Luger"),
    "Suche löschen": ("Clear search", "Effacer la recherche", "Cancella la ricerca", "Borrar la búsqueda"),
    "Keine Ergebnisse für «%1$s»": ("No results for “%1$s”", "Aucun résultat pour «\u00a0%1$s\u00a0»", "Nessun risultato per «%1$s»",
                                     "No hay resultados para «%1$s»"),
    "Prüfe die Schreibweise oder versuche eine andere Suche.": (
        "Check the spelling or try another search.", "Vérifiez l'orthographe ou essayez une autre recherche.",
        "Controlla l'ortografia o prova un'altra ricerca.", "Comprueba la ortografía o prueba otra búsqueda."),
    # Categories (CartridgeCatalog's Category), named in German in the data.
    "Randfeuer": ("Rimfire", "Percussion annulaire", "Percussione anulare", "Fuego anular"),
    "Kurzwaffe": ("Handgun", "Arme de poing", "Arma corta", "Arma corta"),
    "Langwaffe": ("Rifle", "Arme longue", "Arma lunga", "Arma larga"),
    "Schwarzpulver": ("Black powder", "Poudre noire", "Polvere nera", "Pólvora negra"),
    "Flinte": ("Shotgun", "Fusil de chasse", "Fucile a canna liscia", "Escopeta"),
    "Luftdruck": ("Air gun", "Air comprimé", "Aria compressa", "Aire comprimido"),
    "Kanone": ("Cannon", "Canon", "Cannone", "Cañón"),
    # Search words of a group of rounds (Cartridge.groupWordsKey), never shown: the everyday words for it in each
    # language, comma-separated. The search matches the words of every language.
    "Platzpatrone, Knallpatrone, Knall": ("blank, blank cartridge", "à blanc, cartouche à blanc", "a salve, cartuccia a salve",
                                          "de fogueo, cartucho de fogueo, de salvas"),
    "Schrotpatrone, Schrot, Flintenpatrone": ("shotshell, shotgun shell, shotgun cartridge", "cartouche de chasse, cartouche à plombs",
                                              "cartuccia da caccia, cartuccia a pallini",
                                              "cartucho de caza, cartucho de escopeta, cartucho de perdigones"),
    "Rundkugel, Bleikugel": ("round ball, lead ball", "balle ronde", "palla tonda, palla sferica", "bala esférica, bala redonda"),
    "Diabolo, Luftgewehrkugel, Luftgewehr": ("pellet, air rifle, air gun", "plomb, air comprimé", "piombino, pallino, aria compressa",
                                             "balín, balines, aire comprimido"),
    "Minié-Geschoss": ("Minié ball", "Balle Minié", "Palla Minié", "Bala Minié"),
}
