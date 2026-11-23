package ch.giotta.kaliberkatalog.catalog

import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The calibres' sources (Raven, 2026-11-15): Kaliberfinder's SourcesTests on the data, run on the frozen fixture and
 * the live catalogue. The page's own tests are the app's SourcesScreenTest.
 */
class SourcesTest {
    private val works = Fixture.file.sources

    @Test
    fun gp11ListsItsMeasurementSourcesFirst() {
        val gp11 = Fixture.cartridge("7.5x55 mm Swiss (GP 11)")
        assertEquals(
            listOf(listOf("mm"), listOf("shoulder_angle", "case_head"), listOf("primer"), listOf("bullet"), listOf("origin"), listOf("colour_codes")),
            gp11.sources.map { it.parts },
        )
        val measurements = gp11.sources.first()
        assertEquals(
            listOf("Cartridges of the World", "C.I.P.", "Hornady Handbook of Cartridge Reloading"),
            measurements.refs.map { works[it.work].title },
        )
        assertEquals(listOf("573"), measurements.refs.first().pages)
        assertNull(works[measurements.refs[1].work].year, "a web page has no year")
        assertNotNull(works[measurements.refs[1].work].url, "a web page opens in the browser")
    }

    /**
     * A shop's or a load data page is named as a source but is no link (Raven, 2026-11-18): its work has no URL, and a
     * row without one is plain text, as a book's is.
     */
    @Test
    fun shopsAreSourcesWithoutALink() {
        val shops = live.file.sources.filter { it.title in listOf("starlinebrass.com", "ammo-one1.com", "loaddata.com") }
        assertTrue(shops.size >= 16)
        assertTrue(shops.all { it.url == null && it.detail != null }, shops.filter { it.url != null }.toString())
        val linked = live.file.sources.mapNotNull { work -> work.url?.let { runCatching { URI(it).host }.getOrNull() } }
        assertFalse(linked.any { it.endsWith("starlinebrass.com") || it.endsWith("ammo-one1.com") })
        assertTrue(linked.size > 1000)
    }

    @Test
    fun everyRefNamesAWork() {
        for (file in listOf(Fixture.file, live.file)) {
            val refs = file.cartridges.flatMap { c -> c.sources.flatMap { group -> group.refs.map { it.work } } }
            assertTrue(refs.isNotEmpty())
            assertTrue(refs.all { it in file.sources.indices })
        }
    }
}
