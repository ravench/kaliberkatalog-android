package ch.giotta.kaliberkatalog.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

/** Ported from Kaliberfinder's OutlineGeometryTests.swift. */
class OutlineGeometryTest {
    private fun cartridge(id: String) = assertNotNull(live.cartridge(id), id)

    @Test
    fun parsesClosedPolygons() {
        val polys = OutlineGeometry.parse("M0 0 L10 0 L10 5 Z M1 1 L2 1 L2 2 Z")
        assertEquals(2, polys.size)
        assertEquals(listOf(Point(0.0, 0.0), Point(10.0, 0.0), Point(10.0, 5.0)), polys[0])
        assertEquals(3, polys[1].size)
    }

    @Test
    fun parsesDecimalsAndNegatives() {
        val polys = OutlineGeometry.parse("M-1.50 2.25 L3.00 -4.75 L0 0 Z")
        assertEquals(Point(-1.5, 2.25), polys.first().first())
        assertEquals(Point(3.0, -4.75), polys.first()[1])
    }

    /** calibre-data exports a hollow point's cavity as its own closed triangle: it stays out of the silhouette. */
    @Test
    fun cavityDecodesButStaysOutOfTheSilhouette() {
        val json = """{"width": 10, "height": 20, "case": "M0 20 L10 20 L10 8 L0 8 Z", "bullet": "M2 8 L8 8 L5 0 Z", "cavity": "M6 2 L5 4 L4 2 Z"}"""
        val outline = Json.decodeFromString<Cartridge.Outline>(json)
        assertEquals(listOf(listOf(Point(6.0, 2.0), Point(5.0, 4.0), Point(4.0, 2.0))), OutlineGeometry.parse(assertNotNull(outline.cavity)))
        assertEquals(2, OutlineGeometry(outline).polygons.size)
        assertEquals(3.0, OutlineGeometry(outline).widthAt(4.0), 0.01, "the nose, not the cavity")
        assertNull(Json.decodeFromString<Cartridge.Outline>("""{"width": 1, "height": 1}""").cavity)
    }

    @Test
    fun widthOfStraightCase() {
        val nine = cartridge("9x19 mm")
        val g = OutlineGeometry(nine.outline)
        // Halfway down the case the body is close to the base diameter.
        assertEquals(nine.dimensions.base, g.widthAt(22.0), 0.4)
        // The tip is much narrower than the body.
        assertTrue(g.widthAt(0.5) < nine.dimensions.bullet * 0.7)
        assertEquals(0.0, g.widthAt(-1.0))
        assertEquals(0.0, g.widthAt(nine.outline.height + 1))
    }

    @Test
    fun widthOfBottleneckCase() {
        val gp11 = cartridge("7.5x55 mm Swiss (GP 11)")
        val g = OutlineGeometry(gp11.outline)
        val h = gp11.outline.height
        assertEquals(gp11.dimensions.neck, g.widthAt(h - gp11.dimensions.caseLength + 2), 0.3)
        assertEquals(gp11.dimensions.base, g.widthAt(h - 10), 0.6)
    }

    /** A soft point's lead tip is drawn over the bullet but is no part of the silhouette. */
    @Test
    fun aSoftPointsTipStaysOutOfTheSilhouette() {
        val c = cartridge("9.3x62 mm")
        val tip = assertNotNull(c.outline.tip?.let(OutlineGeometry::parse)?.first(), "lead tip")
        assertEquals(0.0, tip.minOf { it.y }, 0.01, "the tip reaches the front of the bullet")
        assertEquals(OutlineGeometry(c.outline, includeBullet = false).polygons.size + 1, OutlineGeometry(c.outline).polygons.size)
    }

    @Test
    fun everyOutlineParses() {
        for (c in live.all) {
            val g = OutlineGeometry(c.outline)
            assertTrue(g.polygons.isNotEmpty(), c.name)
            assertTrue(g.widthAt(c.outline.height * 0.6) > 0, c.name)
        }
    }
}
