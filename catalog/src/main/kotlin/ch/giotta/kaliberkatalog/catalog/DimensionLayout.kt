package ch.giotta.kaliberkatalog.catalog

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Where the parts of the dimensioned drawing go, in dp inside [size]: the label column, the cartridge as large as fits,
 * then the length lines and their labels; centred. Its points are in dp too. Ported from Kaliberfinder's DimensionLayout
 * (MeasurementsView.swift).
 */
class DimensionLayout(c: Cartridge, size: Size, label: Size) {
    /** A width and a height in dp. */
    data class Size(val width: Double, val height: Double)

    /** The measurements of the drawing. */
    enum class Part {
        BULLET, NECK, SHOULDER, BASE, BELT, RIM, OVERALL, CASE_LENGTH;

        val isDiameter get() = this != OVERALL && this != CASE_LENGTH
    }

    /** A measurement in mm at a height of the outline (mm from the tip, as in [Cartridge.Outline]). */
    data class Mark(
        val part: Part,
        val mm: Double,
        /** Diameters: where the drawing is that wide (the base for the rim). Lengths: their top. */
        val y: Double,
    )

    /** A dimension line from [from] to [to], and its label. */
    data class Dimension(
        val part: Part,
        val mm: Double,
        val from: Point,
        val to: Point,
        /** Middle of the label's side that faces the drawing. */
        val anchor: Point,
    ) {
        /** Diameter labels stand left of the drawing and end at [anchor]; length labels start there. */
        val trailing get() = part.isDiameter
    }

    /** Dp per mm of the drawing. */
    val scale: Double

    /** Top left corner of the outline in the view. */
    val origin: Point

    /** Diameters top to bottom, then the lengths, inner first. */
    val dimensions: List<Dimension>

    /** The labels' size: as asked, or narrower where the screen is too narrow for it and the drawing. */
    val labelSize: Size

    /** Extension lines and the leaders from the diameter labels to their lines. */
    val lines: List<List<Point>>

    /** Filled arrowheads at both ends of each dimension line. */
    val arrowheads: List<List<Point>>

    init {
        val o = c.outline
        val lengths = lengths(c)
        val diameters = diameters(c)
        val n = lengths.size.toDouble()
        val lh = label.height
        val gaps = LABEL_GAP + LENGTH_GAP + (n - 1) * LENGTH_SPACING + LABEL_INSET
        // Large text: narrower labels (their text shrinks) keep the drawing at least MIN_SCALE.
        val lw = max(0.0, min(label.width, (size.width - gaps - MIN_SCALE * o.width) / 2))
        val left = lw + LABEL_GAP
        val right = LENGTH_GAP + (n - 1) * LENGTH_SPACING + LABEL_INSET + lw
        // Half a label above the tip (a flush wadcutter's bullet is labelled there) and the rim's line below the base.
        val above = lh / 2
        val below = if (c.hasCase) RIM_GAP + lh / 2 else 0.0
        val s = max(0.0, min((size.width - left - right) / o.width, (size.height - above - below) / o.height))
        val ox = (size.width - left - o.width * s - right) / 2 + left
        val oy = (size.height - above - o.height * s - below) / 2 + above
        val cx = ox + o.width / 2 * s
        val base = oy + o.height * s
        val dimensions = mutableListOf<Dimension>()
        val lines = mutableListOf<List<Point>>()
        val arrowheads = mutableListOf<List<Point>>()

        val bounds = lh / 2..max(lh / 2, size.height - lh / 2)
        val ys = diameters.map { if (it.part == Part.RIM) base + RIM_GAP else oy + it.y * s }
        // A rim on a foot (the «Mauser base» of the 11x59 mm R Gras): its extension lines start at the rim, not the base.
        val rimFrom = base - (if (c.hasCase) rimUnderside(c) else 0.0) * s
        val labelYs = spread(ys, lh, bounds)
        for ((i, mark) in diameters.withIndex()) {
            val y = ys[i]
            val xs = listOf(cx - mark.mm / 2 * s, cx + mark.mm / 2 * s)
            val a = Point(xs[0], y)
            val b = Point(xs[1], y)
            arrowheads.add(arrowhead(a, -1.0, 0.0))
            arrowheads.add(arrowhead(b, 1.0, 0.0))
            if (mark.part == Part.RIM) xs.forEach { lines.add(listOf(Point(it, rimFrom + 2), Point(it, y + 4))) }
            val labelRight = ox - LABEL_GAP
            lines.add(listOf(Point(labelRight + 3, labelYs[i]), Point(ox - 6, y), a))
            dimensions.add(Dimension(mark.part, mark.mm, a, b, Point(labelRight, labelYs[i])))
        }

        val whole = OutlineGeometry(o)
        val empty = OutlineGeometry(o, includeBullet = false)
        val firstX = ox + o.width * s + LENGTH_GAP
        val outerX = firstX + (n - 1) * LENGTH_SPACING
        // Both labels stand right of the outer line, each level with the middle of its own line. The overall length's
        // middle is the higher one, so reversed they run top to bottom for spread.
        val middles = lengths.map { (oy + it.y * s + base) / 2 }
        val lengthYs = spread(middles.reversed(), lh, bounds).reversed()
        for ((i, mark) in lengths.withIndex()) {
            val x = firstX + i * LENGTH_SPACING
            val topY = oy + mark.y * s
            val top = Point(x, topY)
            val bottom = Point(x, base)
            arrowheads.add(arrowhead(top, 0.0, -1.0))
            arrowheads.add(arrowhead(bottom, 0.0, 1.0))
            val edge = (if (mark.part == Part.OVERALL) whole else empty).widthAt(mark.y + 0.01) / 2
            lines.add(listOf(Point(cx + edge * s + 2, topY), Point(x + 3, topY)))
            dimensions.add(Dimension(mark.part, mark.mm, top, bottom, Point(outerX + LABEL_INSET, lengthYs[i])))
        }
        // The lengths share the base's extension line, out to the outer one. It starts beside the rim, also where the
        // rim stands on a narrower foot, so it does not cross the rim's extension line.
        val baseEdge = max(whole.widthAt(o.height - 0.01), if (c.hasCase) c.dimensions.rim else 0.0) / 2
        lines.add(listOf(Point(cx + baseEdge * s + 2, base), Point(outerX + 3, base)))

        scale = s
        origin = Point(ox, oy)
        labelSize = Size(lw, lh)
        this.dimensions = dimensions
        this.lines = lines
        this.arrowheads = arrowheads
    }

    companion object {
        /** A label's size (name over value) at the default text size; the drawing grows it with the text. */
        val LABEL = Size(86.0, 36.0)

        /**
         * The largest label: as wide as the .700 Nitro Express, the widest calibre, leaves room for on a small phone at
         * [MIN_SCALE]; as high as [LABEL] at the largest text the drawing takes.
         */
        val LARGEST_LABEL = Size(114.0, 54.0)

        /** Label column to drawing: room for a leader to bend to a label moved off its line. */
        const val LABEL_GAP = 18.0

        /** Drawing to the first length line. */
        const val LENGTH_GAP = 12.0

        /** The outer length line to the length labels. */
        const val LABEL_INSET = 4.0

        /** The case length's line to the overall length's, right next to it. */
        const val LENGTH_SPACING = 12.0

        /** Base to the rim's dimension line. */
        const val RIM_GAP = 20.0

        /** Smallest scale the labels give way to, in dp per mm. */
        const val MIN_SCALE = 3.0
        const val ARROW_LENGTH = 6.0
        const val ARROW_WIDTH = 4.4

        /** Sampling step along the outline, in mm. */
        const val STEP = 0.1

        /** "Ø 8.5 mm" for a diameter, "55.6 mm" for a length; always with a decimal, so 9 mm reads 9.0 mm. */
        fun valueText(dimension: Dimension, unit: LengthUnit, locale: Locale) =
            (if (dimension.part.isDiameter) "Ø " else "") + unit.format(dimension.mm, locale)

        /** The diameters of the drawing, top to bottom, each where the drawing is that wide. */
        fun diameters(c: Cartridge): List<Mark> {
            val d = c.dimensions
            val height = c.outline.height
            val mouth = height - d.caseLength
            val empty = OutlineGeometry(c.outline, includeBullet = false)
            val marks = mutableListOf<Mark>()
            if (c.kind != "shot" && !c.blank) {
                // Just above the case mouth a bullet is still parallel; a ball, Minié bullet or pellet where it is widest.
                val y = if (c.hasCase) mouth - min(1.5, 0.15 * mouth) else widest(OutlineGeometry(c.outline))
                marks.add(Mark(Part.BULLET, d.bullet, y))
            }
            if (!c.hasCase) return marks
            if (c.kind != "shot") {
                // Halfway down the neck, at most 1.5 mm below the mouth (a straight case narrows to the mouth).
                var neck = 0.0
                while (mouth + neck + STEP < height && abs(empty.widthAt(mouth + neck + STEP) - d.neck) <= 0.15) neck += STEP
                marks.add(Mark(Part.NECK, d.neck, mouth + min(1.5, neck / 2)))
            }
            val shoulder = d.shoulder
            if (shoulder != null) {
                // Just below where the shoulder meets the body.
                var y = mouth
                while (y < height && empty.widthAt(y) < shoulder - 0.15) y += STEP
                marks.add(Mark(Part.SHOULDER, shoulder, y + 0.5))
            }
            val body = bodyStart(c, empty) ?: min(3.3, d.caseLength / 3)
            marks.add(Mark(Part.BASE, d.base, height - body - 1))
            val belt = d.belt
            val beltY = beltMiddle(c, body, empty)
            if (belt != null && beltY != null) marks.add(Mark(Part.BELT, belt, beltY))
            marks.add(Mark(Part.RIM, d.rim, height))
            return marks
        }

        /**
         * The lengths, inner first: the case length where it differs from the overall length (not for blanks, shot
         * shells and flush-seated wadcutters), then the overall length.
         */
        fun lengths(c: Cartridge): List<Mark> {
            val d = c.dimensions
            val overall = Mark(Part.OVERALL, d.overallLength, 0.0)
            if (!c.hasCase || d.caseLength >= d.overallLength - 0.05) return listOf(overall)
            return listOf(Mark(Part.CASE_LENGTH, d.caseLength, c.outline.height - d.caseLength), overall)
        }

        /**
         * Height above the base (mm) of the rim's underside: 0 where the rim stands on the base, the foot's height where it
         * stands on a foot (calibre-data's [case_head] foot, drawn narrower than the rim). Read off the outline.
         */
        fun rimUnderside(c: Cartridge): Double {
            val empty = OutlineGeometry(c.outline, includeBullet = false)
            val height = c.outline.height
            // Up to 3 mm (the feet are 0.8 to 1.0 mm); none for a case drawn without a head (cannon rounds without one).
            if (empty.widthAt(height - 0.01) <= 0) return 0.0
            for (i in 0 until 30) {
                if (empty.widthAt(height - i * STEP - 0.01) >= c.dimensions.rim - 0.05) return i * STEP
            }
            return 0.0
        }

        /**
         * Height above the base (mm) where the body starts in front of the rim, extractor groove or belt: going up from
         * the base, the first place where the case comes to the base diameter after being wider or narrower, and stays
         * there for 1 mm (a belt's ramp only crosses it). Null if none.
         */
        fun bodyStart(c: Cartridge, empty: OutlineGeometry): Double? {
            val d = c.dimensions
            val height = c.outline.height
            // A belt is often within 0.25 mm of the base (7x73 mm Vom Hofe: 13.54 against 13.39), so a belted case's body
            // is where the drawing is at the base diameter exactly, which the sheets give in front of the belt.
            val tolerance = if (c.kind == "belted") 0.05 else 0.25
            fun atBase(t: Double) = abs(empty.widthAt(height - t) - d.base) <= tolerance
            var below = atBase(0.01)
            for (t in stride(STEP, min(d.caseLength, 20.0), STEP)) {
                val here = atBase(t)
                if (here && !below && stride(t, t + 1, STEP).all(::atBase)) return t
                below = here
            }
            return null
        }

        /**
         * Height (mm from the top) of the middle of the belt: the stretch below the body start, above the groove, where
         * the drawing is at the belt diameter. Null when the drawing has no such stretch.
         */
        fun beltMiddle(c: Cartridge, body: Double, empty: OutlineGeometry): Double? {
            val belt = c.dimensions.belt ?: return null
            val height = c.outline.height
            val ts = stride(STEP, body, STEP).filter { abs(empty.widthAt(height - it) - belt) <= 0.05 }.toList()
            val top = ts.maxOrNull() ?: return null
            // The stretch ending at the top: down to the first gap.
            var bottom = top
            for (t in ts.reversed()) if (t >= bottom - STEP * 1.5) bottom = t
            return height - (top + bottom) / 2
        }

        /** Height of the middle of the widest stretch of a drawing: the equator of a ball, the band of a Minié bullet, the head of a pellet. */
        fun widest(g: OutlineGeometry): Double {
            val ys = generateSequence(0) { it + 1 }.map { it * STEP }.takeWhile { it <= g.height }.toList()
            val widths = ys.map(g::widthAt)
            val most = widths.maxOrNull() ?: 0.0
            var bestStart = 0
            var bestEnd = -1
            var start = 0
            for (i in ys.indices) {
                if (widths[i] < most - 0.02) {
                    start = i + 1
                } else if (i - start > bestEnd - bestStart) {
                    bestStart = start
                    bestEnd = i
                }
            }
            return ys[(bestStart + bestEnd) / 2]
        }

        /** Moves label centres [ys] (top to bottom) apart until they are [spacing] apart, inside [bounds] where there is room. */
        fun spread(ys: List<Double>, spacing: Double, bounds: ClosedFloatingPointRange<Double>): List<Double> {
            val result = ys.toMutableList()
            for (i in result.indices) result[i] = max(result[i], if (i == 0) bounds.start else result[i - 1] + spacing)
            for (i in result.indices.reversed()) {
                result[i] = min(result[i], if (i == result.lastIndex) bounds.endInclusive else result[i + 1] - spacing)
            }
            return result
        }

        /** A filled arrowhead with its tip at [tip], pointing along ([dx], [dy]). */
        fun arrowhead(tip: Point, dx: Double, dy: Double): List<Point> {
            val bx = tip.x - dx * ARROW_LENGTH
            val by = tip.y - dy * ARROW_LENGTH
            val w = ARROW_WIDTH / 2
            return listOf(tip, Point(bx - dy * w, by + dx * w), Point(bx + dy * w, by - dx * w))
        }

        /** Swift's stride(from:to:by:): [from] + n · [by] below [to], computed rather than added up, as Swift does. */
        private fun stride(from: Double, to: Double, by: Double) = generateSequence(0) { it + 1 }.map { from + it * by }.takeWhile { it < to }
    }
}
