package ch.giotta.kaliberkatalog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.giotta.kaliberkatalog.catalog.Cartridge
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The country's name in the app's language; the data's German name for a country newer than the app. */
@Composable
fun countryName(country: Cartridge.Country) = countryTitle(country.code)?.let { stringResource(it) } ?: country.name

/** [size] sp in dp: grows with the text size, as Kaliberfinder's @ScaledMetric. */
@Composable
private fun scaled(size: Int) = with(LocalDensity.current) { size.sp.toDp() }

/**
 * A list row's trailing marks: a globe when the calibre is in use worldwide, then the flag of the state that introduced
 * it (Raven 2026-11-07: the globe left of the flag). At least 42 wide, so rows without marks keep their layout. Ported
 * from Kaliberfinder's OriginMarks (CalibreTags.swift), which the catalogue gives one calibre at a time.
 */
@Composable
fun OriginMarks(cartridge: Cartridge) {
    Row(
        Modifier.widthIn(min = scaled(42)),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cartridge.worldwide) WorldwideMark()
        cartridge.country?.let { CountryFlag(it) }
    }
}

/** Globe for a calibre in common use worldwide, shown next to the flag of the state that introduced it. */
@Composable
fun WorldwideMark() = Icon(
    painterResource(R.drawable.ic_globe),
    stringResource(R.string.worldwide),
    Modifier.size(scaled(14)),
    tint = MaterialTheme.colorScheme.onSurfaceVariant,
)

/** The calibre's country flag, read as the country's name: the emoji, or a drawn flag for former states and unions. */
@Composable
fun CountryFlag(country: Cartridge.Country) {
    val name = countryName(country)
    val label = Modifier.clearAndSetSemantics { contentDescription = name }
    val flag = country.flag
    if (flag != null) {
        Text(flag, label, style = MaterialTheme.typography.bodyLarge)
    } else {
        val height = scaled(12)
        val shape = RoundedCornerShape(2.dp)
        DrawnFlag(country.code, label.size(height * 1.5f, height).clip(shape).border(0.5.dp, Color.Black.copy(alpha = 0.25f), shape))
    }
}

/** Codes [DrawnFlag] can draw; generate_data.py DRAWN_FLAGS must match. */
val drawnFlagCodes = setOf("RUE", "AH", "SU", "CS", "OE", "NATO")

/** Simple flags for former states and unions, which have no flag emoji (calibre-data COUNTRIES without one). */
@Composable
fun DrawnFlag(code: String, modifier: Modifier = Modifier) = Canvas(modifier) {
    val h = size.height
    when (code) {
        "RUE" -> stripes(listOf(Color.Black, gold, Color.White))
        "AH" -> stripes(listOf(Color.Black, gold))
        "SU" -> {
            drawRect(Color(0.8f, 0f, 0f))
            star(Offset(h * 0.325f, h * 0.275f), h * 0.35f, Color(1f, 0.84f, 0f)) // 0.15 h from the left, 0.1 h from the top
        }
        "CS" -> {
            stripes(listOf(Color.White, Color(0.84f, 0.08f, 0.1f)))
            drawPath(polygon(listOf(Offset.Zero, center, Offset(0f, h))), Color(0.07f, 0.27f, 0.49f))
        }
        "OE" -> { // a white crescent and star on red
            val red = Color(0.89f, 0.04f, 0.09f)
            val c = Offset(size.width * 0.42f, h / 2)
            drawRect(red)
            drawCircle(Color.White, h * 0.28f, c)
            drawCircle(red, h * 0.225f, c + Offset(h * 0.11f, 0f))
            star(c + Offset(h * 0.3f, 0f), h * 0.22f, Color.White)
        }
        "NATO" -> { // the compass rose: a four-pointed star in a ring
            drawRect(Color(0f, 0.29f, 0.56f))
            val r = h * 0.3f
            drawPath(polygon((0 until 8).map { center + polar(if (it % 2 == 0) r else r * 0.25f, it * PI / 4) }), Color.White)
            drawCircle(Color.White, r * 1.3f, center, style = Stroke(r * 0.15f))
        }
        else -> drawRect(Color.Gray)
    }
}

private val gold = Color(0.95f, 0.76f, 0f)

/**
 * Bands of equal height from the top down; each reaches the bottom, so no seam shows between two. A list, as Kotlin
 * takes no vararg of a value class such as Color.
 */
private fun DrawScope.stripes(colours: List<Color>) {
    val band = size.height / colours.size
    colours.forEachIndexed { i, colour -> drawRect(colour, Offset(0f, i * band), Size(size.width, size.height - i * band)) }
}

/** A five-pointed star [diameter] across its points, at [centre], with a point up (SF Symbols' star.fill). */
private fun DrawScope.star(centre: Offset, diameter: Float, colour: Color) {
    val r = diameter / 2
    drawPath(polygon((0 until 10).map { centre + polar(if (it % 2 == 0) r else r * 0.382f, it * PI / 5) }), colour)
}

private fun polygon(points: List<Offset>) = Path().apply {
    moveTo(points[0].x, points[0].y)
    for (p in points.drop(1)) lineTo(p.x, p.y)
    close()
}

/** The point [distance] from the origin at [angle], clockwise from straight up. */
private fun polar(distance: Float, angle: Double) = Offset(distance * sin(angle).toFloat(), -distance * cos(angle).toFloat())
