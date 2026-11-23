package ch.giotta.kaliberkatalog

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ch.giotta.kaliberkatalog.catalog.Cartridge

/**
 * The case head seen from below at [dpPerMm]: rim, and the primer in the centre, in the head's colour in the side view.
 * Rimfire cases show the folded rim instead of a primer. Nothing for balls and pellets. Ported from Kaliberfinder's
 * CaseBaseView.swift.
 */
@Composable
fun CaseBaseView(cartridge: Cartridge, dpPerMm: Float, modifier: Modifier = Modifier) {
    if (!cartridge.hasCase) return
    val primer = cartridge.primer
    val description = if (primer == null) {
        stringResource(R.string.primer_rimfire)
    } else {
        stringResource(R.string.case_base_with_primer, primerTitle(primer.type)?.let { stringResource(it) } ?: primer.type)
    }
    val diameter = (cartridge.headDiameter * dpPerMm).dp
    Canvas(modifier.size(diameter).semantics { contentDescription = description }) {
        val pxPerMm = size.width / cartridge.headDiameter.toFloat()
        val stroke = Stroke(CartridgeDrawing.lineWidth(dpPerMm).dp.toPx())
        drawCircle(CartridgeDrawing.headFill(cartridge))
        drawCircle(CartridgeDrawing.line, style = stroke)
        // Edge of the head face (chamfer, or the step to the body on rimmed cases).
        drawCircle(Color.Black.copy(alpha = 0.25f), faceDiameter(cartridge).toFloat() * pxPerMm / 2, style = stroke)
        if (primer != null) {
            val radius = primer.diameter.toFloat() * pxPerMm / 2
            drawCircle(primerColor, radius)
            drawCircle(Color.Black.copy(alpha = 0.6f), radius, style = stroke)
        }
    }
}

val primerColor = Color(0.78f, 0.78f, 0.80f)

/** Diameter of the flat head face inside the rim's chamfer or fold, in mm. */
fun faceDiameter(c: Cartridge) = when (c.kind) {
    "rimmed", "rimfire" -> c.dimensions.base
    else -> c.headDiameter * 0.9
}

/** The title of a primer type, as Kaliberfinder's Cartridge.Primer.title; null for one newer than the app. */
@StringRes
fun primerTitle(type: String) = when (type) {
    "small-pistol" -> R.string.primer_small_pistol
    "large-pistol" -> R.string.primer_large_pistol
    "small-rifle" -> R.string.primer_small_rifle
    "large-rifle" -> R.string.primer_large_rifle
    "miniature" -> R.string.primer_miniature
    "shotshell-209" -> R.string.primer_shotshell_209
    "bmg-35" -> R.string.primer_bmg_35
    "berdan-nitro" -> R.string.primer_berdan_nitro // in several sizes: the row shows the calibre's own diameter
    else -> null
}
