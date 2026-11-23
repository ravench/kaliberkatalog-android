package ch.giotta.kaliberkatalog.catalog

import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Ported from Kaliberfinder's CatalogTests.swift; the matching and 1:1 tests stay there. */
class CatalogTest {
    private val all = live.all
    private val fixture = Fixture.catalogue

    private fun finds(query: String, id: String) = CartridgeCatalog.search(query, fixture).any { it.id == id }

    private fun group(id: String) = assertNotNull(live.lookalikeGroup(assertNotNull(live.cartridge(id), id)), id)

    @Test
    fun catalogueLoadsWithUniqueIds() {
        assertTrue(all.size > 90)
        assertEquals(all.size, all.map { it.id }.toSet().size)
    }

    @Test
    fun dimensionsArePlausible() {
        for (c in all) {
            val d = c.dimensions
            assertTrue(d.overallLength > 0, c.name)
            if (c.kind == "shot") { // the hull opened by firing is longer than the loaded shell
                assertTrue(d.caseLength >= d.overallLength, "${c.name}: opened hull shorter than the shell")
            } else {
                assertTrue(d.overallLength + 0.01 >= d.caseLength, "${c.name}: case longer than cartridge")
            }
            assertTrue(d.rim > 0, c.name)
            assertTrue(c.outline.width <= maxOf(c.headDiameter, d.bullet) + 0.01, c.name)
            assertEquals(d.overallLength, c.outline.height, 0.01, c.name)
            d.shoulder?.let { assertTrue(d.neck <= it, c.name) }
        }
    }

    /** A belt wider than the rim (the .378 Weatherby head) is the head's width: the side view is drawn that wide. */
    @Test
    fun aBeltWiderThanTheRimIsTheHeadsWidth() {
        val belted = all.filter { it.dimensions.belt != null }
        assertTrue(belted.isNotEmpty())
        for (c in belted) {
            assertEquals("belted", c.kind, c.name)
            assertEquals(c.dimensions.belt, c.headDiameter, c.name)
            assertTrue(c.headDiameter > c.dimensions.rim, c.name)
            assertEquals(c.headDiameter, c.outline.width, 0.01, c.name)
        }
    }

    /** A group's words find all its rounds: the German ones by default, the app adds their translations. */
    @Test
    fun aGroupsWordsFindIt() {
        fun found(words: String) = CartridgeCatalog.search(words, all).map { it.id }.toSet()
        fun group(belongs: (Cartridge) -> Boolean) = all.filter(belongs).map { it.id }.toSet()
        val groups = listOf(
            group { it.blank } to listOf("Platzpatrone", "Knall"),
            group { it.kind == "shot" } to listOf("Schrotpatrone", "Schrot"),
            group { it.kind == "ball" && !it.blank } to listOf("Rundkugel"),
            group { it.kind == "pellet" } to listOf("Diabolo", "Luftgewehr"),
            group { it.kind == "minie" } to listOf("Minié-Geschoss"),
        )
        assertEquals(listOf(10, 8, 8, 2, 2), groups.map { it.first.size }) // the 6.8/11 is the tenth blank
        for ((members, words) in groups) {
            for (word in words) assertTrue(found(word).containsAll(members), "«$word» misses ${members - found(word)}")
        }
        // The blanks' word finds the blanks and nothing else; a round with a bullet has no group.
        assertEquals(groups[0].first, found("Platzpatrone"))
        assertNull(assertNotNull(live.cartridge("9x19 mm")).groupWordsKey)
        // The app's translations are split at their commas like the German key.
        val ball = assertNotNull(Cartridge.groupWordsKey("ball"))
        val words = CartridgeCatalog.groupWords(ball, listOf("round ball, lead ball", "balle ronde"))
        assertEquals(listOf("Rundkugel", "Bleikugel", "round ball", "lead ball", "balle ronde"), words)
        val english = CartridgeCatalog.search("round ball", all, mapOf(ball to words)).map { it.id }.toSet()
        assertTrue(english.containsAll(groups[2].first), "«round ball» misses ${groups[2].first - english}")
    }

    /** A colour code's description is read in the app's language, in English where calibre-data has no other. */
    @Test
    fun colourCodeDescriptionsFollowTheLanguage() {
        val base = assertNotNull(Fixture.cartridge("7.5x55 mm Swiss (GP 11)").colourCodes.firstOrNull { it.part == "base" })
        assertEquals("Leuchtspur, ältere Kennzeichnung: ganzer Boden lackiert", base.description("de"))
        assertEquals("Tracer, the older marking: the whole base lacquered", base.description("en"))
        assertEquals("Trazadora, marca antigua: todo el culote lacado", base.description("es"))
        assertEquals(base.description, base.description("ja"), "English for a language the data does not have")
        fun code(json: String) = Json.decodeFromString<Cartridge.ColourCode>(json)
        assertEquals("M61", code("""{"part":"tip","colours":["black"],"meaning":["armour-piercing"],"description":"M61","countries":[]}""").description("de"))
        assertNull(code("""{"part":"tip","colours":["red"],"meaning":["tracer"],"countries":[]}""").description("de"))
        // The catalogue in the app carries them, each in all four languages.
        val translated = all.flatMap { it.colourCodes }.mapNotNull { it.descriptions }
        assertTrue(translated.isNotEmpty())
        for (descriptions in translated) assertEquals(listOf("de", "es", "fr", "it"), descriptions.keys.sorted())
    }

    @Test
    fun colourCodesComeFromCalibreData() {
        // GP 11: the red tracer tip, then the older red (tracer) and purple (armour-piercing) bases.
        val gp11 = Fixture.cartridge("7.5x55 mm Swiss (GP 11)")
        assertEquals(listOf("tip", "base", "base"), gp11.colourCodes.map { it.part })
        assertEquals(listOf(listOf("red"), listOf("red"), listOf("purple")), gp11.colourCodes.map { it.colours })
        assertEquals(listOf(listOf("tracer"), listOf("tracer"), listOf("armour-piercing")), gp11.colourCodes.map { it.meaning })
        assertEquals(listOf("CH"), gp11.colourCodes.first().countries.map { it.code })
        assertTrue(Fixture.cartridge("9x19 mm").colourCodes.isEmpty())
        assertTrue(all.flatMap { it.colourCodes }.size > 30)
    }

    @Test
    fun everyCategoryIsKnown() {
        val known = Category.entries.map { it.german }.toSet()
        for (c in all) assertTrue(c.category in known, "${c.name}: unknown category ${c.category}")
    }

    @Test
    fun unsourcedCalibresStayHiddenUnlessTheDeveloperSwitchIsOn() {
        val shown = CartridgeCatalog.visible(fixture, showUnsourced = false, showCannon = false)
        assertEquals(fixture.filter { it.wellSourced && !it.isCannon }, shown)
        assertFalse(shown.any { it.id == "7.5 mm Kadettenpatrone" }, "its measurements are listed as unsourced")
        assertTrue(shown.any { it.id == "9x19 mm" })
        assertEquals(fixture, CartridgeCatalog.visible(fixture, showUnsourced = true, showCannon = true))
    }

    /** The cannon rounds need their own switch, then follow the same rules as every calibre. A partial entry has only a page. */
    @Test
    fun cannonCalibresFollowTheSameRulesBehindTheirSwitch() {
        val vulcan = Fixture.cartridge("20x102 mm")
        val partial = Fixture.cartridge("57x121 mm R")
        assertTrue(vulcan.isCannon && vulcan.wellSourced && vulcan.isDrawn)
        assertTrue(partial.isCannon && !partial.wellSourced && !partial.isDrawn)
        assertFalse(vulcan in CartridgeCatalog.visible(fixture, showUnsourced = true, showCannon = false))
        assertTrue(vulcan in CartridgeCatalog.visible(fixture, showUnsourced = false, showCannon = true))
        assertFalse(partial in CartridgeCatalog.visible(fixture, showUnsourced = false, showCannon = true), "unsourced")
        assertTrue(partial in CartridgeCatalog.visible(fixture, showUnsourced = true, showCannon = true))
        assertTrue(all.any { it.id == vulcan.id }, "a drawn cannon round is drawn like any other")
        assertFalse(all.any { it.id == partial.id }, "a partial entry is never drawn")
        assertNotNull(live.cartridge(partial.id), "its page is still reachable by id")
    }

    @Test
    fun searchMatchesAliasesIgnoringSpacesDotsAndMM() {
        assertTrue(finds("9 mm Luger", "9x19 mm"))
        assertTrue(finds("9MM LUGER", "9x19 mm"))
        assertTrue(finds("GP90", "5.6 mm GP 90"))
        assertTrue(finds("win 308", ".308 Winchester"))
        assertTrue(finds("Gras", "11x59 mm R Gras"))
        assertEquals(fixture.size, CartridgeCatalog.search("", fixture).size)
        assertTrue(CartridgeCatalog.search("no such calibre", fixture).isEmpty())
    }

    @Test
    fun gp1890IsSeparateFromGP11AndGP90() {
        val gp1890 = Fixture.cartridge("7.5x53.5 mm Swiss (Patrone Modell 1890)")
        // The army's 1892 drawing, not the 54.6 misread on a 1906 round.
        assertEquals(53.5, gp1890.dimensions.caseLength, 0.001)
        assertEquals(77.2, gp1890.dimensions.overallLength, 0.001)
        assertEquals(Category.RIFLE.german, gp1890.category)
        assertTrue(gp1890.hasCase)
        for (text in listOf("GP 1890", "GP 90/03", "7.5mm GP 90", "Patrone 1890", "7,5x53,5")) assertTrue(finds(text, gp1890.id), text)
        // The 1923 rework is its own round.
        assertFalse(finds("GP 90/23", gp1890.id))
        assertTrue(finds("GP 90/23", "7.5x54.5 mm Swiss (GP 90/23)"))
    }

    @Test
    fun searchTakesWordsInAnyOrderAcrossAllNames() {
        // Words in another order, or with words of the name left out.
        assertTrue(finds("Vetterli 10.4", "10.4x38 mm R Vetterli"))
        assertTrue(finds("10.4 Vetterli", "10.4x38 mm R Vetterli"))
        assertTrue(finds("Luger 9mm", "9x19 mm"))
        assertTrue(finds("Swiss 7.5", "7.5x55 mm Swiss (GP 11)"))
        assertTrue(finds("Revolver 1882", "7.5 mm Swiss Ordnance 1882"))
        // Each word may be in a different name: «GP 11» in the name, «K31» a search alias.
        assertTrue(finds("GP 11 K31", "7.5x55 mm Swiss (GP 11)"))
        // Only other names hold the word (search aliases, «Auch bekannt als»).
        assertTrue(finds("Parabellum", "9x19 mm"))
        assertTrue(finds("Schmidt Rubin", "7.5x53.5 mm Swiss (Patrone Modell 1890)"))
        assertTrue(finds("9 mm Para", "9x19 mm"))
        // Hyphens are like spaces, accents don't matter.
        assertTrue(finds("Martini Henry", ".577/450 Martini-Henry"))
        assertTrue(finds("Mannlicher Schonauer", "6.5x54 mm Mannlicher-Schönauer"))
        // A headstamp typed as it is, where dropping "mm" joins two words.
        assertTrue(finds("357 REM MAG", ".357 Magnum"))
        // Every word must be found.
        assertEquals(emptyList(), CartridgeCatalog.search("Vetterli 9", fixture).map { it.id })
        assertTrue(finds("Mauser 8", "8x57 mm IS"))
        assertFalse(finds("Mauser 8", "7x57 mm Mauser"))
    }

    @Test
    fun kadettenpatroneIsItsOwnRareRound() {
        val kp = Fixture.cartridge("7.5 mm Kadettenpatrone")
        assertEquals(Category.RIFLE.german, kp.category)
        // The GP 90/23's case, longer overall, a jacketed round nose; a longer case than the GP 1890's.
        val gp9023 = Fixture.cartridge("7.5x54.5 mm Swiss (GP 90/23)")
        val gp1890 = Fixture.cartridge("7.5x53.5 mm Swiss (Patrone Modell 1890)")
        assertEquals(gp9023.dimensions.caseLength, kp.dimensions.caseLength, 0.001)
        assertEquals(gp9023.dimensions.rim, kp.dimensions.rim, 0.001)
        assertEquals(77.5, kp.dimensions.overallLength, 0.001)
        assertTrue(kp.dimensions.caseLength > gp1890.dimensions.caseLength)
        assertEquals("cupronickel", kp.outline.bulletMaterial, "a cupronickel jacket, not the GP 1890's steel cap")
        assertNull(kp.outline.patch)
        assertTrue(kp.obsolete)
        assertTrue(kp.rare)
        for (text in listOf("Kadettenpatrone", "Kadetten-Pat", "Cart. 7,5 mm cadetti", "KG97")) assertTrue(finds(text, kp.id), text)
        assertFalse(finds("GP 1890", kp.id))
    }

    @Test
    fun lookalikeGroupsNameCatalogueCalibresOnce() {
        val groups = live.file.lookalikes
        assertTrue(groups.size >= 20)
        for (g in groups) {
            assertTrue(g.calibres.size >= 2)
            assertEquals(g.calibres.size, live.members(g).size, "${g.calibres} names a calibre the catalogue lacks")
            assertTrue(g.differs.isNotEmpty())
        }
        val members = groups.flatMap { it.calibres }
        assertEquals(members.size, members.toSet().size, "a calibre in two groups")
        val win = group(".308 Winchester")
        assertEquals(listOf(".308 Winchester", "7.62x51 mm NATO"), win.calibres)
        assertEquals(listOf("load"), win.differs)
        val vetterli = group("10.4x38 mm R Vetterli")
        assertEquals(listOf("10.4x38 mm R Vetterli", "10.4x38 mm R Vetterli Centrefire"), vetterli.calibres)
        assertEquals(listOf("ignition"), vetterli.differs)
        assertNull(live.lookalikeGroup(assertNotNull(live.cartridge("7.5x55 mm Swiss (GP 11)"))))
        assertEquals(listOf("rim", "load"), group("7x57 mm Mauser").differs)
    }

    /** A group goes by calibre-data's name: its parent's, or the family's. */
    @Test
    fun lookalikeGroupsCarryTheDatabaseName() {
        assertEquals("7.62x51 mm", group("7.62x51 mm NATO").name) // named by what the members share
        val mauser = group("7.62x25 mm Tokarev")
        assertEquals(listOf("7.63x25 mm", ".30 Borchardt"), listOf(mauser.name, mauser.calibres[0]))
        assertEquals("7x57 mm", group("7x57 mm R").name)
        // A name is a group's own: no calibre's but a member's, and no two groups share one.
        val groups = live.file.lookalikes
        assertEquals(groups.size, groups.map { it.name }.toSet().size)
        for (g in groups) {
            if (live.cartridge(g.name) != null) assertTrue(g.name in g.calibres, "${g.name} names a group it is not in")
        }
    }

    /** One-size groups (told apart by load, seating or ignition) stand for their members in rows; the others don't. */
    @Test
    fun onlyAOneSizeGroupStandsForItsMembers() {
        val win = assertNotNull(live.cartridge(".308 Winchester"))
        assertEquals("7.62x51 mm", live.sizeGroup(win)?.name)
        val mauser = assertNotNull(live.cartridge("7x57 mm Mauser"))
        assertNotNull(live.lookalikeGroup(mauser))
        assertNull(live.sizeGroup(mauser), "the rimmed 7x57 R stands apart")
    }

    @Test
    fun normalized() {
        assertEquals("75x55", CartridgeCatalog.normalized(" 7,5 × 55 mm "))
        assertEquals("45acp", CartridgeCatalog.normalized(".45 ACP"))
        assertEquals("mannlicherschonauer", CartridgeCatalog.normalized("Mannlicher-Schönauer"))
    }

    @Test
    fun groupedKeepsCategoryOrder() {
        val everything = CartridgeCatalog.visible(live.file.cartridges, showUnsourced = true, showCannon = true)
        assertEquals(Category.entries.map { it.german }, CartridgeCatalog.grouped(everything).map { it.first })
    }

    @Test
    fun decodeRejectsGarbage() {
        assertNull(CartridgeCatalog.decode("nope"))
        assertNull(CartridgeCatalog.decode("""{"version":1}"""))
    }

    /** Settings show the calibre-data commit the catalogue was generated from. */
    @Test
    fun catalogueNamesItsCalibreDataCommit() {
        val source = assertNotNull(live.file.calibreData)
        assertTrue(Regex("^[0-9a-f]{40}$").matches(source.commit), source.commit)
    }

    @Test
    fun unreadableDateKeepsTheCatalogue() {
        val json = """{"version":1,"calibreData":{"commit":"04653a888e18b4cf5cd8d619d42f8d8501ebc369","date":"soon"},"cartridges":[]}"""
        assertEquals("soon", CartridgeCatalog.decode(json)?.file?.calibreData?.date)
    }
}
