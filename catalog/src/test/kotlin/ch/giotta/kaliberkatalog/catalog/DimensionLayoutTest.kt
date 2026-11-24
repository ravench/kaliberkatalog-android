package ch.giotta.kaliberkatalog.catalog

import ch.giotta.kaliberkatalog.catalog.DimensionLayout.Part
import ch.giotta.kaliberkatalog.catalog.DimensionLayout.Size
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The dimensioned drawing: where its dimension lines and labels go. Ported from Kaliberfinder's MeasurementsTests. */
class DimensionLayoutTest {
    /** A large and a small phone, inside the screen's margins. */
    private val phone = Size(370.0, 700.0)
    private val small = Size(343.0, 520.0)
    private val label = DimensionLayout.LABEL

    /** The labels at the largest text the drawing takes. */
    private val largest = DimensionLayout.LARGEST_LABEL

    private fun cartridge(id: String) = assertNotNull(live.cartridge(id), id)

    private fun layout(id: String, size: Size = phone, label: Size = this.label) = DimensionLayout(cartridge(id), size, label)

    private fun assertClose(expected: Double, actual: Double, accuracy: Double, message: String? = null) =
        assertTrue(abs(expected - actual) <= accuracy, "${message.orEmpty()}: expected $expected, was $actual")

    /**
     * The drawing shows what the calibre page lists: no bullet on blanks and shot shells, no neck on shot shells, a
     * shoulder only on bottleneck cases, and one length where the case is the whole round.
     */
    @Test
    fun partsFollowTheCalibrePage() {
        fun parts(id: String) = layout(id).dimensions.map { it.part }
        assertEquals(listOf(Part.BULLET, Part.NECK, Part.BASE, Part.RIM, Part.CASE_LENGTH, Part.OVERALL), parts("9x19 mm"))
        assertEquals(
            listOf(Part.BULLET, Part.NECK, Part.SHOULDER, Part.BASE, Part.RIM, Part.CASE_LENGTH, Part.OVERALL),
            parts("7.5x55 mm Swiss (GP 11)"),
        )
        assertEquals(listOf(Part.BASE, Part.RIM, Part.OVERALL), parts("12/70"))
        assertEquals( // the belt is wider than the rim
            listOf(Part.BULLET, Part.NECK, Part.SHOULDER, Part.BASE, Part.BELT, Part.RIM, Part.CASE_LENGTH, Part.OVERALL),
            parts(".378 Weatherby Magnum"),
        )
        assertEquals(listOf(Part.NECK, Part.BASE, Part.RIM, Part.OVERALL), parts(".22 SH (5.6/11)"))
        assertEquals(listOf(Part.NECK, Part.BASE, Part.RIM, Part.OVERALL), parts("9 mm PA Blanc")) // closed inside the case
        assertEquals(listOf(Part.BULLET, Part.NECK, Part.BASE, Part.RIM, Part.OVERALL), parts(".32 S&W Long Wadcutter"))
        assertEquals(listOf(Part.BULLET, Part.OVERALL), parts("6 mm Airsoft-BB"))
    }

    /** Each diameter's line crosses the drawing where it is that wide; a flush-seated bullet is inside the case, so its line is at the mouth. */
    @Test
    fun diametersSitWhereTheDrawingIsThatWide() {
        assertTrue(live.all.size > 400)
        for (c in live.all) {
            val whole = OutlineGeometry(c.outline)
            val empty = OutlineGeometry(c.outline, includeBullet = false)
            val flush = c.hasCase && c.dimensions.overallLength - c.dimensions.caseLength < 0.1
            for (mark in DimensionLayout.diameters(c)) {
                if (mark.part == Part.RIM || (mark.part == Part.BULLET && flush)) continue
                val width = (if (mark.part == Part.BULLET) whole else empty).widthAt(mark.y)
                assertClose(mark.mm, width, 0.25, "${c.id} ${mark.part}")
            }
        }
    }

    /** The base's line is on the body just in front of the extractor groove, rim or belt, not on the rim of a rimless case or on the ramp up to a belt. */
    @Test
    fun baseLineIsInFrontOfTheHead() {
        for (c in live.all.filter { it.hasCase }) {
            assertNotNull(DimensionLayout.bodyStart(c, OutlineGeometry(c.outline, includeBullet = false)), c.id)
        }
        // A belt within 0.25 mm of the base: the base's line is on the body in front of the belt, the belt's own line on
        // the belt where the data has one.
        for (id in listOf("7x73 mm Vom Hofe Belted", ".400/375 Belted Nitro Express", ".378 Weatherby Magnum", ".300 Winchester Magnum")) {
            val c = cartridge(id)
            val empty = OutlineGeometry(c.outline, includeBullet = false)
            val d = c.dimensions
            val height = c.outline.height
            val body = assertNotNull(DimensionLayout.bodyStart(c, empty), id)
            assertClose(d.base, empty.widthAt(height - body - 0.5), 0.05, "$id: on the body")
            assertTrue(empty.widthAt(height - body + 0.3) > d.base + 0.1, "$id: the belt is just below")
            val marks = DimensionLayout.diameters(c)
            assertEquals(d.belt != null, marks.any { it.part == Part.BELT }, id)
            val belt = marks.firstOrNull { it.part == Part.BELT } ?: continue
            assertClose(assertNotNull(d.belt), empty.widthAt(belt.y), 0.05, id)
            assertTrue(marks.first { it.part == Part.BASE }.y < belt.y, "$id: the base's line above the belt's")
        }
    }

    /** The lines are as long as their values at the drawing's scale, the diameters across the drawing's axis, the rim's below the base and the lengths ending on the base. */
    @Test
    fun linesAreAsLongAsTheValues() {
        val gp11 = layout("7.5x55 mm Swiss (GP 11)")
        val s = gp11.scale
        val axis = gp11.origin.x + 12.6 / 2 * s
        val base = gp11.origin.y + 77.5 * s
        for (d in gp11.dimensions) {
            assertClose(d.mm * s, hypot(d.to.x - d.from.x, d.to.y - d.from.y), 1e-6, "${d.part}")
            if (d.part.isDiameter) {
                assertEquals(d.from.y, d.to.y, "${d.part}")
                assertClose(axis, (d.from.x + d.to.x) / 2, 1e-6, "${d.part}")
            } else {
                assertClose(base, d.to.y, 1e-6, "${d.part}")
            }
        }
        val rim = gp11.dimensions.first { it.part == Part.RIM }
        assertClose(base + DimensionLayout.RIM_GAP, rim.from.y, 1e-6)
        assertEquals(2 * gp11.dimensions.size, gp11.arrowheads.size)
    }

    /**
     * A rim on a foot («Mauser base»): the rim's dimension line stays below the base, but its extension lines start at
     * the rim's underside, not at the base under the narrower foot. Only the seven calibres with a foot have one.
     */
    @Test
    fun aRimOnAFootIsDimensionedFromTheRim() {
        val feet = mapOf(
            "11x59 mm R Gras" to 1.0, "10.4x38 mm R Vetterli Centrefire" to 0.8, "9.5x60 mm R Turkish Mauser" to 0.9,
            "10.15x61 mm R Jarmann" to 0.8, "10.15x63 mm R Serbian Mauser" to 0.8, "11.15x58 mm R Werndl M77" to 0.8,
            "11x60 mm R Japanese Murata" to 0.9,
        )
        for (c in live.all) assertClose(feet[c.name] ?: 0.0, DimensionLayout.rimUnderside(c), 1e-6, c.name)
        val gras = layout("11x59 mm R Gras")
        val base = gras.origin.y + cartridge("11x59 mm R Gras").outline.height * gras.scale
        val rim = gras.dimensions.first { it.part == Part.RIM }
        assertClose(base + DimensionLayout.RIM_GAP, rim.from.y, 1e-6)
        // The two vertical lines that end just above the rim's line: they start 1 mm (the foot) above the base, plus 2 dp.
        val extensions = gras.lines.filter { it.size == 2 && it[0].x == it[1].x && abs(it[1].y - (rim.from.y + 4)) < 1e-6 }
        assertEquals(2, extensions.size)
        for (line in extensions) assertClose(base - 1.0 * gras.scale + 2, line[0].y, 1e-6)
        // The lengths' line along the base starts right of the rim's right extension line, not under the rim.
        val baseLine = gras.lines.first { it.size == 2 && it[0].y == it[1].y && abs(it[0].y - base) < 1e-6 }
        assertTrue(baseLine[0].x > extensions.maxOf { it[0].x })
    }

    /**
     * On a phone every calibre fits, also with the largest labels: lines and labels stay on screen, the diameter labels
     * keep the order of their lines, and labels do not overlap.
     */
    @Test
    fun everyCalibreFitsAPhone() {
        val slack = 1e-6
        for ((size, asked) in listOf(phone to label, small to label, phone to largest, small to largest)) {
            for (c in live.all) {
                val layout = DimensionLayout(c, size, asked)
                val label = layout.labelSize
                // Labels give way down to MIN_SCALE; a round too long for the screen at that scale (the 150 mm 5.66x39 mm
                // MPS dart) is bounded by the height alone.
                val tallest = (size.height - DimensionLayout.RIM_GAP - asked.height) / c.outline.height
                assertTrue(layout.scale >= minOf(DimensionLayout.MIN_SCALE, tallest) - 1e-9, c.id)
                assertTrue(label.width <= asked.width)
                val points = layout.dimensions.flatMap { listOf(it.from, it.to) } + layout.lines.flatten() + layout.arrowheads.flatten()
                for (p in points) {
                    assertTrue(p.x >= -slack && p.x <= size.width + slack && p.y >= -slack && p.y <= size.height + slack, "${c.id} $p")
                }
                val diameters = layout.dimensions.filter { it.trailing }
                for ((a, b) in diameters.zipWithNext()) {
                    assertTrue(a.from.y < b.from.y, "${c.id} ${a.part} ${b.part}")
                    assertTrue(b.anchor.y - a.anchor.y >= label.height - slack, "${c.id} ${a.part} ${b.part}")
                }
                val lengths = layout.dimensions.filter { !it.trailing }
                for ((a, b) in lengths.zipWithNext()) assertTrue(abs(b.anchor.y - a.anchor.y) >= label.height - slack, "${c.id} lengths")
                for (d in layout.dimensions) {
                    val left = if (d.trailing) d.anchor.x - label.width else d.anchor.x
                    assertTrue(left >= -slack, "${c.id} ${d.part}")
                    assertTrue(left + label.width <= size.width + slack, "${c.id} ${d.part}")
                    assertTrue(d.anchor.y - label.height / 2 >= -slack, "${c.id} ${d.part}")
                    assertTrue(d.anchor.y + label.height / 2 <= size.height + slack, "${c.id} ${d.part}")
                }
            }
        }
    }

    /** Labels keep their asked width where they fit, even the largest on a small phone, and give way on a narrower view (split screen); nothing breaks on a view that has no size yet. */
    @Test
    fun labelsGiveWayOnlyWhenNeeded() {
        assertEquals(label, layout("9x19 mm").labelSize)
        assertEquals(largest, layout(".700 Nitro Express", size = small, label = largest).labelSize)
        val large = layout("9x19 mm", size = Size(260.0, 520.0), label = largest)
        assertTrue(large.labelSize.width < largest.width)
        assertEquals(largest.height, large.labelSize.height)
        assertEquals(0.0, layout("9x19 mm", size = Size(0.0, 0.0)).scale)
    }

    /** The overall length's line stands right next to the case length's, not beyond its label (Raven, 2026-11-09: it stuck out); both labels are right of it, the overall length's higher. */
    @Test
    fun lengthsStandSideBySide() {
        for (id in listOf("9 mm Flobert Bosquette", "9x19 mm", "7.5x55 mm Swiss (GP 11)", ".38 Special Wadcutter")) {
            val drawing = layout(id)
            val right = drawing.origin.x + cartridge(id).outline.width * drawing.scale
            val caseLength = drawing.dimensions.first { it.part == Part.CASE_LENGTH }
            val overall = drawing.dimensions.first { it.part == Part.OVERALL }
            assertClose(right + DimensionLayout.LENGTH_GAP, caseLength.from.x, 1e-6, id)
            assertClose(DimensionLayout.LENGTH_SPACING, overall.from.x - caseLength.from.x, 1e-6, id)
            for (d in listOf(caseLength, overall)) assertClose(overall.from.x + DimensionLayout.LABEL_INSET, d.anchor.x, 1e-6, id)
            assertTrue(overall.anchor.y < caseLength.anchor.y, id)
        }
    }

    /** Labels that would overlap move apart; when the last one reaches the bottom, the others move up. */
    @Test
    fun spreadKeepsLabelsApart() {
        assertEquals(listOf(100.0, 130.0, 300.0), DimensionLayout.spread(listOf(100.0, 110.0, 300.0), 30.0, 15.0..685.0))
        assertEquals(listOf(625.0, 655.0, 685.0), DimensionLayout.spread(listOf(660.0, 670.0, 680.0), 30.0, 15.0..685.0))
        assertEquals(listOf(15.0, 45.0), DimensionLayout.spread(listOf(0.0, 5.0), 30.0, 15.0..685.0))
    }

    /** Values follow the unit; diameters carry «Ø». */
    @Test
    fun valuesFollowTheUnit() {
        val nine = layout("9x19 mm")
        val bullet = nine.dimensions.first { it.part == Part.BULLET }
        val overall = nine.dimensions.first { it.part == Part.OVERALL }
        val en = Locale.US
        assertEquals("Ø 9.0 mm", DimensionLayout.valueText(bullet, LengthUnit.METRIC, en))
        assertEquals("Ø 0.356 in", DimensionLayout.valueText(bullet, LengthUnit.IMPERIAL, en))
        assertEquals("29.7 mm", DimensionLayout.valueText(overall, LengthUnit.METRIC, en))
        assertEquals("1.169 in", DimensionLayout.valueText(overall, LengthUnit.IMPERIAL, en))
        // A whole number keeps a decimal (Raven, 2026-11-09: «Show 9mm as 9.0mm»).
        val shoulder = layout(".223 Remington").dimensions.first { it.part == Part.SHOULDER }
        assertEquals("Ø 9.0 mm", DimensionLayout.valueText(shoulder, LengthUnit.METRIC, en))
    }

    /** A ball's diameter is at its equator. */
    @Test
    fun ballDiameterIsAtTheEquator() {
        assertClose(3.0, DimensionLayout.diameters(cartridge("6 mm Airsoft-BB")).first().y, 0.15)
    }

    /** The calibre page lists no measurements beside a drawing, so the drawing is the only place for a belt's diameter. */
    @Test
    fun everyBeltIsDrawn() {
        val belted = live.all.filter { it.dimensions.belt != null }
        assertTrue(belted.size > 10)
        for (c in belted) assertTrue(DimensionLayout.diameters(c).any { it.part == Part.BELT }, "${c.name}: no belt on the drawing")
    }
}
