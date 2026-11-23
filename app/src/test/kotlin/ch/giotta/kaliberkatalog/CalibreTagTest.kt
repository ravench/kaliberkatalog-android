package ch.giotta.kaliberkatalog

import org.junit.Assert.assertEquals
import org.junit.Test

/** Kaliberfinder's CalibreTagTests.testTagsComeFromTheCatalogue: a calibre's tags, in their order, from the catalogue. */
class CalibreTagTest {
    @Test
    fun tagsComeFromTheCatalogue() {
        assertEquals(listOf(CalibreTag.RARE, CalibreTag.OBSOLETE), CalibreTag.of(cartridge("11x59 mm R Gras")))
        assertEquals(listOf(CalibreTag.OBSOLETE), CalibreTag.of(cartridge("7.5x53.5 mm Swiss (Patrone Modell 1890)")))
        assertEquals(listOf(CalibreTag.RARE), CalibreTag.of(cartridge(".450 No. 2 Nitro Express")))
        assertEquals(emptyList<CalibreTag>(), CalibreTag.of(cartridge("7.5x55 mm Swiss (GP 11)")))
        // What a developer switch hides carries a tag (Raven, 2026-11-15); a cannon round both rules apply to.
        assertEquals(listOf(CalibreTag.UNSOURCED, CalibreTag.CANNON), CalibreTag.of(cartridge("57x121 mm R")))
        assertEquals(listOf(CalibreTag.CANNON), CalibreTag.of(cartridge("20x102 mm")))
        // A one-size group is no tag of the calibre's own: its list row adds the group's name.
        assertEquals(emptyList<CalibreTag>(), CalibreTag.of(cartridge("9x19 mm")))
    }
}
