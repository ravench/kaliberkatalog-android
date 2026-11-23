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
    # The calibre page (Kaliberfinder's CartridgeDetailView).
    "Zurück": ("Back", "Retour", "Indietro", "Atrás"),
    "Masse": ("Dimensions", "Dimensions", "Dimensioni", "Dimensiones"),
    "Gesamtlänge": ("Overall length", "Longueur totale", "Lunghezza totale", "Longitud total"),
    "Hülsenlänge": ("Case length", "Longueur de douille", "Lunghezza del bossolo", "Longitud de la vaina"),
    "Hülsenlänge (offen)": ("Shell length (open)", "Longueur de douille (ouverte)", "Lunghezza del bossolo (aperto)",
                            "Longitud de la vaina (abierta)"),
    "Randdurchmesser": ("Rim diameter", "Diamètre du bourrelet", "Diametro del collarino", "Diámetro de la pestaña"),
    "Gürteldurchmesser": ("Belt diameter", "Diamètre de la ceinture", "Diametro della cintura", "Diámetro del cinturón"),
    "Bodendurchmesser": ("Base diameter", "Diamètre au culot", "Diametro alla base", "Diámetro del culote"),
    "Schulterdurchmesser": ("Shoulder diameter", "Diamètre de l'épaulement", "Diametro della spalla", "Diámetro del hombro"),
    "Halsdurchmesser": ("Neck diameter", "Diamètre du collet", "Diametro del colletto", "Diámetro del cuello"),
    "Geschossdurchmesser": ("Bullet diameter", "Diamètre du projectile", "Diametro della palla", "Diámetro de la bala"),
    "Zündhütchen": ("Primer", "Amorce", "Innesco", "Pistón"),
    "Randfeuer, ohne Zündhütchen": ("Rimfire, no primer", "Percussion annulaire, sans amorce", "Percussione anulare, senza innesco",
                                    "Fuego anular, sin pistón"),
    "Boden mit Zündhütchen %1$s": ("Case head with %1$s primer", "Culot avec amorce %1$s", "Fondello con innesco %1$s",
                                   "Culote con pistón %1$s"),
    # Primer types (Kaliberfinder's Cartridge.Primer.title).
    "Pistole klein": ("Small pistol", "Petit pistolet", "Pistola piccola", "Pistola pequeña"),
    "Pistole gross": ("Large pistol", "Grand pistolet", "Pistola grande", "Pistola grande"),
    "Büchse klein": ("Small rifle", "Petite carabine", "Carabina piccola", "Rifle pequeño"),
    "Büchse gross": ("Large rifle", "Grande carabine", "Carabina grande", "Rifle grande"),
    "Miniatur": ("Miniature", "Miniature", "Miniatura", "Miniatura"),
    "Schrot (209)": ("Shotshell (209)", "Cartouche à plombs (209)", "Cartuccia a pallini (209)", "Cartucho de escopeta (209)"),
    ".50 BMG (#35)": (".50 BMG (#35)", ".50 BMG (#35)", ".50 BMG (#35)", ".50 BMG (#35)"),
    "Berdan Nitro": ("Berdan Nitro", "Berdan Nitro", "Berdan Nitro", "Berdan Nitro"),
    "Kategorie": ("Category", "Catégorie", "Categoria", "Categoría"),
    "Bauart": ("Type", "Type", "Tipo", "Tipo"),
    # Kinds (Kaliberfinder's CartridgeDetailView.kindTitle); rimfire is the category's text, Minié the group's words.
    "Randlos": ("Rimless", "Sans bourrelet", "Senza collarino", "Sin pestaña"),
    "Mit Rand": ("Rimmed", "À bourrelet", "Con collarino", "Con pestaña"),
    "Halbrand": ("Semi-rimmed", "À demi-bourrelet", "Con semicollarino", "Con semipestaña"),
    "Gürtelhülse": ("Belted", "À ceinture", "Con cintura", "Con cinturón"),
    "Schrotpatrone": ("Shotshell", "Cartouche à plombs", "Cartuccia a pallini", "Cartucho de escopeta"),
    "Rundkugel": ("Round ball", "Balle ronde", "Palla sferica", "Bala esférica"),
    "Diabolo": ("Diabolo pellet", "Plomb diabolo", "Piombino diabolo", "Balín diábolo"),
    "Andere": ("Other", "Autre", "Altro", "Otro"),
    "Verbreitung": ("Prevalence", "Diffusion", "Diffusione", "Difusión"),
    "Weltweit verbreitet": ("Used worldwide", "Répandu dans le monde entier", "Diffuso in tutto il mondo",
                            "Difundido en todo el mundo"),
    "Auch bekannt als": ("Also known as", "Aussi appelé", "Noto anche come", "También conocido como"),
    "Gerundete Richtwerte, keine Wiederladedaten.": (
        "Rounded reference values, not reloading data.", "Valeurs indicatives arrondies, pas de données de rechargement.",
        "Valori indicativi arrotondati, non dati di ricarica.", "Valores orientativos redondeados, no son datos de recarga."),
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
