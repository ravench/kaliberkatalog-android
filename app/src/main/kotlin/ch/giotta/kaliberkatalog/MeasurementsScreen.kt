package ch.giotta.kaliberkatalog

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ch.giotta.kaliberkatalog.catalog.Cartridge
import ch.giotta.kaliberkatalog.catalog.DimensionLayout
import ch.giotta.kaliberkatalog.catalog.DimensionLayout.Part
import ch.giotta.kaliberkatalog.catalog.LengthUnit
import java.util.Locale
import kotlin.math.min

/**
 * The text size where a label is as high as the largest (54 dp for 36): there its text stops growing, as Kaliberfinder's
 * stops at xxxLarge.
 */
internal const val MAX_FONT_SCALE = 1.5f

/**
 * A calibre's catalogue measurements on a drawing of it: the diameters as horizontal dimension lines with their labels in a
 * column on the left, the lengths as vertical ones on the right. The drawing is as large as fits, not at real size. Ported
 * from Kaliberfinder's MeasurementsView.swift; where everything goes is the catalogue's [DimensionLayout].
 */
@Composable
fun MeasurementsScreen(cartridge: Cartridge, onBack: () -> Unit, onSources: () -> Unit) {
    val unit = appSettings.unit
    val locale = LocalConfiguration.current.locales[0]
    val screen = LocalDensity.current
    // The labels grow with the text up to the largest label, where their text stops growing: the drawing needs the room.
    // Long values then shrink a little to fit; the values in any size are on the calibre's page.
    val fontScale = min(screen.fontScale, MAX_FONT_SCALE)
    val largest = DimensionLayout.LARGEST_LABEL
    val asked = DimensionLayout.Size(
        min(DimensionLayout.LABEL.width * fontScale, largest.width),
        min(DimensionLayout.LABEL.height * fontScale, largest.height),
    )
    Scaffold(
        topBar = {
            PageBar(cartridge.name, onBack) {
                // The same page as the calibre page's «Quellen» row; its first group is the measurements (Raven, 2026-11-15).
                if (cartridge.sources.isNotEmpty()) {
                    IconButton(onClick = onSources) { Icon(painterResource(R.drawable.ic_sources), stringResource(R.string.sources)) }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).consumeWindowInsets(padding).padding(16.dp)) {
            // Not mirrored: the labels stay beside their lines also on a phone that reads right to left.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val size = DimensionLayout.Size(maxWidth.value.toDouble(), maxHeight.value.toDouble())
                    val layout = remember(cartridge, size, asked) { DimensionLayout(cartridge, size, asked) }
                    val ink = LocalContentColor.current
                    CartridgeView(cartridge, layout.scale.toFloat(), Modifier.offset(layout.origin.x.dp, layout.origin.y.dp))
                    Canvas(Modifier.fillMaxSize()) {
                        // The layout's points are in dp.
                        val lines = layout.dimensions.map { listOf(it.from, it.to) } + layout.lines
                        drawPath(polygonPath(lines, density, closed = false), ink, style = Stroke(0.8.dp.toPx()))
                        drawPath(polygonPath(layout.arrowheads, density, closed = true), ink)
                    }
                    CompositionLocalProvider(LocalDensity provides Density(screen.density, fontScale)) {
                        for (dimension in layout.dimensions) DimensionLabel(dimension, layout.labelSize, unit, locale)
                    }
                }
            }
        }
    }
}

/**
 * A dimension's label, its name over its value, beside its line: ending at the anchor for a diameter, starting there for a
 * length. Read as the calibre page's name of the value, then the value.
 */
@Composable
private fun DimensionLabel(dimension: DimensionLayout.Dimension, size: DimensionLayout.Size, unit: LengthUnit, locale: Locale) {
    val spoken = stringResource(spokenTitle(dimension.part)) + " " + unit.format(dimension.mm, locale)
    val trailing = dimension.trailing
    val x = if (trailing) dimension.anchor.x - size.width else dimension.anchor.x
    Column(
        Modifier.offset(x.dp, (dimension.anchor.y - size.height / 2).dp).size(size.width.dp, size.height.dp)
            .clearAndSetSemantics { contentDescription = spoken },
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        horizontalAlignment = if (trailing) Alignment.End else Alignment.Start,
    ) {
        val (name, value) = labelStyles()
        LabelLine(stringResource(partTitle(dimension.part)), name)
        LabelLine(DimensionLayout.valueText(dimension, unit, locale), value)
    }
}

/**
 * A label's name and value styles. Readable at a glance (Raven 2026-11-09): the name in full colour, the value a size larger,
 * with even digits. At the font's own line height, as on iPhone: Material's would make the two lines 37 dp high, one more than
 * a label has, and the value would shrink to fit.
 */
@Composable
internal fun labelStyles(): Pair<TextStyle, TextStyle> {
    val type = MaterialTheme.typography
    val value = type.bodyMedium.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
    return type.bodySmall.copy(lineHeight = TextUnit.Unspecified) to value.copy(lineHeight = TextUnit.Unspecified)
}

/** A line of a label, kept to one line: shrunk to as little as 60 % where it is too long. */
@Composable
private fun LabelLine(text: String, style: TextStyle) = BasicText(
    text,
    style = style.copy(color = LocalContentColor.current),
    softWrap = false,
    maxLines = 1,
    autoSize = TextAutoSize.StepBased(minFontSize = style.fontSize * 0.6f, maxFontSize = style.fontSize),
)

/** The short name over a value, as Kaliberfinder's DimensionLayout.Part.title. */
@StringRes
fun partTitle(part: Part) = when (part) {
    Part.BULLET -> R.string.drawing_bullet
    Part.NECK -> R.string.drawing_neck
    Part.SHOULDER -> R.string.drawing_shoulder
    Part.BASE -> R.string.drawing_base
    Part.BELT -> R.string.drawing_belt
    Part.RIM -> R.string.drawing_rim
    Part.OVERALL -> R.string.drawing_overall
    Part.CASE_LENGTH -> R.string.drawing_case_length
}

/** The calibre page's name of a value, which its label is read with (Part.spokenTitle). */
@StringRes
fun spokenTitle(part: Part) = when (part) {
    Part.BULLET -> R.string.bullet_diameter
    Part.NECK -> R.string.neck_diameter
    Part.SHOULDER -> R.string.shoulder_diameter
    Part.BASE -> R.string.base_diameter
    Part.BELT -> R.string.belt_diameter
    Part.RIM -> R.string.rim_diameter
    Part.OVERALL -> R.string.overall_length
    Part.CASE_LENGTH -> R.string.case_length
}
