package ch.giotta.kaliberkatalog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.giotta.kaliberkatalog.catalog.Cartridge
import ch.giotta.kaliberkatalog.catalog.OutlineGeometry
import ch.giotta.kaliberkatalog.catalog.Point

/** The colours and lines of a cartridge drawing. Ported from Kaliberfinder's CartridgeView.swift. */
object CartridgeDrawing {
    val brass = Color(0.89f, 0.76f, 0.44f)
    val copper = Color(0.72f, 0.45f, 0.20f)
    val lead = Color(0.54f, 0.55f, 0.57f)
    val shotShell = Color(0.75f, 0.22f, 0.17f)
    val steel = Color(0.65f, 0.68f, 0.71f)
    val paper = Color(0.95f, 0.93f, 0.88f)
    val lacqueredSteel = Color(0.42f, 0.42f, 0.33f)
    val nickel = Color(0.78f, 0.80f, 0.81f)
    val aluminium = Color(0.66f, 0.68f, 0.69f)

    /** Plastic tip (V-Max, ELD, FTX, Ballistic Tip): most makers use red. */
    val polymer = Color(0.80f, 0.10f, 0.14f)

    /** Airsoft BB: most are white plastic. */
    val plastic = Color(0.97f, 0.97f, 0.96f)

    /** A hollow point's cavity is shaded, and its hidden line is bolder than the outline, with short dashes. */
    val cavityShade = Color.Black.copy(alpha = 0.35f)
    val line = Color.Black.copy(alpha = 0.7f)

    /**
     * Fill of the bullet: steel cap (GP 1890), steel jacket (7.65x53 mm Argentine) or steel BB, bare lead, plastic
     * airsoft BB, the silver cupronickel jacket of early service rounds (the 6 mm Lee Navy), or copper jacket; other
     * balls and pellets are lead. Today's jacket unless [original]: the GP 11 is copper today.
     */
    fun bulletFill(cartridge: Cartridge, original: Boolean = false): Color {
        val o = cartridge.outline
        return when (if (original) o.bulletMaterial else o.todayBulletMaterial ?: o.bulletMaterial) {
            "copper" -> copper
            "steel" -> steel
            "cupronickel" -> nickel
            "lead" -> lead
            "plastic" -> plastic
            else -> if (cartridge.hasCase) copper else lead
        }
    }

    /**
     * Fill of the case: brass unless the outline names another material; shot shells are plastic. Today's case
     * unless [original]: the 7.62x54 mm R is mostly steel today, its first cases were brass.
     */
    fun caseFill(cartridge: Cartridge, original: Boolean = false): Color {
        if (cartridge.kind == "shot") return shotShell
        val o = cartridge.outline
        return when (if (original) o.caseMaterial else o.todayCaseMaterial ?: o.caseMaterial) {
            "copper" -> copper
            "steel" -> lacqueredSteel
            "nickel" -> nickel
            "aluminium" -> aluminium
            "paper" -> paper
            else -> brass
        }
    }

    /**
     * Fill of the case head: a separate head part is brass (shot shell) or steel (hybrid case); without one, the
     * case's own material reaches the rim.
     */
    fun headFill(cartridge: Cartridge, original: Boolean = false): Color {
        if (cartridge.outline.head == null) return caseFill(cartridge, original)
        return if (cartridge.outline.headMaterial == "steel") steel else brass
    }

    /** SwiftUI's miter limit (Compose's is 4): sharp corners stay pointed as in Kaliberfinder. */
    val miter = 10f

    /** Width of the outline of every part, in dp. */
    fun lineWidth(dpPerMm: Float) = (dpPerMm * 0.08f).coerceIn(0.3f, 0.8f)

    /** The cavity's line width and its dash and gap, in dp. */
    fun cavityStroke(dpPerMm: Float) =
        Triple((dpPerMm * 0.15f).coerceIn(0.6f, 1.4f), maxOf(1.5f, dpPerMm * 0.4f), maxOf(1f, dpPerMm * 0.25f))
}

/** Draws a cartridge outline at [dpPerMm]. */
@Composable
fun CartridgeView(cartridge: Cartridge, dpPerMm: Float, modifier: Modifier = Modifier) {
    val o = cartridge.outline
    // Drawn in this order, each filled and outlined.
    val parts = remember(cartridge) {
        listOf(
            o.case to CartridgeDrawing.caseFill(cartridge),
            o.head to CartridgeDrawing.headFill(cartridge),
            o.bullet to CartridgeDrawing.bulletFill(cartridge),
            o.patch to CartridgeDrawing.paper,
            o.tip to if (o.tipMaterial == "polymer") CartridgeDrawing.polymer else CartridgeDrawing.lead,
        ).mapNotNull { (d, fill) -> d?.let { OutlineGeometry.parse(it) to fill } }
    }
    val cavity = remember(cartridge) { o.cavity?.let(OutlineGeometry::parse) }
    Canvas(modifier.size((o.width * dpPerMm).dp, (o.height * dpPerMm).dp)) {
        val scale = size.height / o.height.toFloat() // px per mm
        val line = Stroke(CartridgeDrawing.lineWidth(dpPerMm).dp.toPx(), miter = CartridgeDrawing.miter)
        for ((polygons, fill) in parts) {
            val path = polygonPath(polygons, scale, closed = true)
            drawPath(path, fill)
            drawPath(path, CartridgeDrawing.line, style = line)
        }
        if (cavity != null) {
            val (width, dash, gap) = CartridgeDrawing.cavityStroke(dpPerMm)
            drawPath(polygonPath(cavity, scale, closed = true), CartridgeDrawing.cavityShade)
            // Stroked open: the mouth lies on the bullet's front edge, which the outline already draws.
            val dashes = PathEffect.dashPathEffect(floatArrayOf(dash.dp.toPx(), gap.dp.toPx()))
            val stroke = Stroke(width.dp.toPx(), miter = CartridgeDrawing.miter, pathEffect = dashes)
            drawPath(polygonPath(cavity, scale, closed = false), CartridgeDrawing.line, style = stroke)
        }
    }
}

/**
 * Polygons in millimetres as a path at [scale] px per mm. Unless [closed], each stays open from its last point back to
 * its first (a hollow point's mouth).
 */
internal fun polygonPath(polygons: List<List<Point>>, scale: Float, closed: Boolean) = Path().apply {
    for (poly in polygons) {
        val first = poly.firstOrNull() ?: continue
        moveTo(first.x.toFloat() * scale, first.y.toFloat() * scale)
        for (p in poly.drop(1)) lineTo(p.x.toFloat() * scale, p.y.toFloat() * scale)
        if (closed) close()
    }
}

/** A cartridge outline scaled to fit [width] × [height], standing on the bottom, for list rows. */
@Composable
fun CartridgeThumbnail(cartridge: Cartridge, width: Dp = 28.dp, height: Dp = 44.dp) {
    val o = cartridge.outline
    val dpPerMm = minOf(width.value / o.width, height.value / o.height).toFloat()
    Box(Modifier.size(width, height), contentAlignment = Alignment.BottomCenter) { CartridgeView(cartridge, dpPerMm) }
}
