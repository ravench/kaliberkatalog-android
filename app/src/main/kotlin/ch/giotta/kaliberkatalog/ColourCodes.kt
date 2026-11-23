package ch.giotta.kaliberkatalog

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ch.giotta.kaliberkatalog.catalog.Cartridge

/**
 * One colour code of a calibre, read together: the painted tip, base or primer ring, what it means, where it sits, the
 * loads and the countries. Ported from Kaliberfinder's ColourCodeRow (CartridgeDetailView.swift).
 */
@Composable
fun ColourCodeRow(code: Cartridge.ColourCode) {
    val meanings = code.meaning.map { title(meaningTitle(it), it) }
    // The loads in the app's language, in English where calibre-data has no other (Raven, 2026-11-18).
    val where = listOf(title(partTitle(code.part), code.part)) + listOfNotNull(code.description(stringResource(R.string.language)))
    ListItem(
        headlineContent = { Text(meanings.joinToString(" · ")) },
        modifier = Modifier.semantics(mergeDescendants = true) {},
        supportingContent = { Text(where.joinToString(" · ")) },
        leadingContent = { ColourSwatch(code.part, code.colours) },
        trailingContent = if (code.countries.isEmpty()) null else ({
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { code.countries.forEach { CountryFlag(it) } }
        }),
    )
}

/** The footer's note on the band order, only with a tip: a base or primer ring has one colour (Raven, 2026-11-18). */
fun explainsTipOrder(codes: List<Cartridge.ColourCode>) = codes.any { it.part == "tip" }

/**
 * A painted bullet tip (the colour bands from the tip down, in a small capsule), a painted case base (a disc, as the
 * case is seen from below) or a primer ring (a ring around a brass primer), read as where it is and its colours.
 */
@Composable
fun ColourSwatch(part: String, colours: List<String>) {
    val height = scaled(30)
    val edge = MaterialTheme.colorScheme.onSurfaceVariant // a white tip, base or ring still shows on the white row
    val label = title(partTitle(part), part) + ": " + colours.map { title(colourTitle(it), it) }.joinToString(", ")
    Canvas(Modifier.size(height * 0.8f, height).clearAndSetSemantics { contentDescription = label }) {
        val h = size.height
        val line = Stroke(0.5.dp.toPx())
        val first = swatchColour(colours.firstOrNull())
        when (part) {
            "annulus" -> {
                drawCircle(first, h * 0.3f)
                drawCircle(CartridgeDrawing.brass, h * 0.18f)
                drawCircle(edge, h * 0.3f, style = line)
            }
            "base" -> {
                drawCircle(first, h * 0.4f)
                drawCircle(edge, h * 0.4f, style = line)
            }
            else -> {
                val w = h / 2
                val capsule = Path().apply { addRoundRect(RoundRect(Rect(Offset((size.width - w) / 2, 0f), Size(w, h)), CornerRadius(w / 2))) }
                clipPath(capsule) { stripes(colours.map(::swatchColour)) }
                drawPath(capsule, edge, style = line)
            }
        }
    }
}

/** The colour of each calibre-data colour name: iOS's, as Kaliberfinder draws them; a test keeps it complete. */
val swatchColours = mapOf(
    "black" to Color.Black, "white" to Color.White, "grey" to Color(142, 142, 147), "silver" to Color(0.78f, 0.78f, 0.78f),
    "red" to Color(255, 59, 48), "orange" to Color(255, 149, 0), "yellow" to Color(255, 204, 0), "green" to Color(52, 199, 89),
    "blue" to Color(0, 122, 255), "purple" to Color(175, 82, 222), "brown" to Color(162, 132, 94),
)

private fun swatchColour(name: String?) = swatchColours[name] ?: Color.Transparent

/** A text of the app by its resource, or the data's own [key] for one newer than the app. */
@Composable
internal fun title(@StringRes id: Int?, key: String) = id?.let { stringResource(it) } ?: key

/** Where the paint is, as Kaliberfinder's ColourCodeRow.partTitle. */
@StringRes
fun partTitle(part: String) = when (part) {
    "tip" -> R.string.part_tip
    "base" -> R.string.part_base
    "annulus" -> R.string.part_annulus
    else -> null
}

/** What a colour code means, as Kaliberfinder's Cartridge.ColourCode.title. */
@StringRes
fun meaningTitle(meaning: String) = when (meaning) {
    "ball" -> R.string.meaning_ball
    "armour-piercing" -> R.string.meaning_armour_piercing
    "incendiary" -> R.string.meaning_incendiary
    "explosive" -> R.string.meaning_explosive
    "tracer" -> R.string.meaning_tracer
    "spotting" -> R.string.meaning_spotting
    "subsonic" -> R.string.meaning_subsonic
    "frangible" -> R.string.meaning_frangible
    "proof" -> R.string.meaning_proof
    "reduced-charge" -> R.string.meaning_reduced_charge
    else -> null
}

/** A colour's name, as the swatch is read (Kaliberfinder's Cartridge.ColourCode.colourName). */
@StringRes
fun colourTitle(colour: String) = when (colour) {
    "black" -> R.string.colour_black
    "white" -> R.string.colour_white
    "grey" -> R.string.colour_grey
    "silver" -> R.string.colour_silver
    "red" -> R.string.colour_red
    "orange" -> R.string.colour_orange
    "yellow" -> R.string.colour_yellow
    "green" -> R.string.colour_green
    "blue" -> R.string.colour_blue
    "purple" -> R.string.colour_purple
    "brown" -> R.string.colour_brown
    else -> null
}
