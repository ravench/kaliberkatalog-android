package ch.giotta.kaliberkatalog.catalog

/** A point in millimetres. */
data class Point(val x: Double, val y: Double)

/**
 * Polygons of a cartridge outline (millimetres) and its width at a height. The generated outlines only use the SVG
 * commands M, L and Z. Ported from Kaliberfinder's OutlineGeometry.swift, without what only the photo matching uses.
 */
class OutlineGeometry(outline: Cartridge.Outline, includeBullet: Boolean = true) {
    /** Without the bullet, the polygons are those of the empty case. */
    val polygons = listOfNotNull(outline.case, outline.head, outline.bullet.takeIf { includeBullet }).flatMap(::parse)
    val width = outline.width
    val height = outline.height

    /** Horizontal extent of the outline at height [y] (0 where the outline is empty). */
    fun widthAt(y: Double): Double {
        var minX = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        for (poly in polygons) {
            for (i in poly.indices) {
                val a = poly[i]
                val b = poly[(i + 1) % poly.size]
                if (y < minOf(a.y, b.y) || y > maxOf(a.y, b.y)) continue
                if (a.y == b.y) {
                    minX = minOf(minX, a.x, b.x)
                    maxX = maxOf(maxX, a.x, b.x)
                } else {
                    val x = a.x + (y - a.y) / (b.y - a.y) * (b.x - a.x)
                    minX = minOf(minX, x)
                    maxX = maxOf(maxX, x)
                }
            }
        }
        return if (maxX > minX) maxX - minX else 0.0
    }

    companion object {
        /** Parses "M x y L x y … Z" path data into closed polygons. */
        fun parse(d: String): List<List<Point>> {
            val result = mutableListOf<List<Point>>()
            var current = mutableListOf<Point>()
            val numbers = mutableListOf<Double>()
            val token = StringBuilder()
            fun flushToken() {
                token.toString().toDoubleOrNull()?.let {
                    numbers += it
                    if (numbers.size == 2) {
                        current += Point(numbers[0], numbers[1])
                        numbers.clear()
                    }
                }
                token.clear()
            }
            fun endPolygon() {
                flushToken()
                if (current.size > 1) result += current
                current = mutableListOf()
            }
            for (ch in d) {
                when (ch) {
                    'M', 'Z', 'z' -> endPolygon()
                    'L', ' ', ',' -> flushToken()
                    else -> token.append(ch)
                }
            }
            endPolygon()
            return result
        }
    }
}
