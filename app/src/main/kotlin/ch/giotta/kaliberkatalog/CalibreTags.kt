package ch.giotta.kaliberkatalog

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.giotta.kaliberkatalog.catalog.Cartridge

/**
 * The «Selten», «Veraltet», «Unbelegt», «Kanone» and «Gruppe» tags of a calibre, in display order, with the words of its
 * chip and what it means on the calibre's page; none for the group, whose page the calibre's page names instead. Ported
 * from Kaliberfinder's CalibreTag (CalibreTags.swift).
 */
enum class CalibreTag(@StringRes val title: Int, @StringRes val explanation: Int?) {
    RARE(R.string.tag_rare, R.string.tag_rare_explanation),
    OBSOLETE(R.string.tag_obsolete, R.string.tag_obsolete_explanation),
    // Hidden unless its developer switch is on (Raven, 2026-11-15: a tag on what a switch hides).
    UNSOURCED(R.string.tag_unsourced, R.string.tag_unsourced_explanation),
    CANNON(R.string.tag_cannon, R.string.tag_cannon_explanation),
    GROUP(R.string.group, null);

    /**
     * Kaliberfinder's colour of the tag: orange, grey, purple, teal, blue. On a light background darker than iOS's, so the
     * chip's small words read (4.5:1 on their own tint).
     */
    val colour: Color
        @Composable get() {
            val dark = isSystemInDarkTheme()
            return when (this) {
                RARE -> if (dark) Color(0xFFFF9F0A) else Color(0xFF9A4F00)
                OBSOLETE -> MaterialTheme.colorScheme.onSurfaceVariant
                UNSOURCED -> if (dark) Color(0xFFD08CF5) else Color(0xFF8E35BF)
                CANNON -> if (dark) Color(0xFF40C8E0) else Color(0xFF0F6573)
                GROUP -> if (dark) Color(0xFF409CFF) else Color(0xFF005FCC)
            }
        }

    companion object {
        /** A calibre's own tags; its one-size group's is the group's name, which its list row adds (CartridgeRow). */
        fun of(c: Cartridge) = listOfNotNull(
            RARE.takeIf { c.rare },
            OBSOLETE.takeIf { c.obsolete },
            UNSOURCED.takeIf { !c.wellSourced },
            CANNON.takeIf { c.isCannon },
        )
    }
}

/**
 * A small capsule with the tag's icon and word, e.g. «Selten»; [text] replaces the word (a group's name), which is read as
 * «Gruppe …»: alone it would sound like a second calibre beside the row's.
 */
@Composable
fun TagChip(tag: CalibreTag, text: String? = null) {
    val colour = tag.colour
    val spoken = text?.let { stringResource(R.string.group_title, it) }
    Row(
        Modifier.background(colour.copy(alpha = 0.15f), CircleShape).padding(horizontal = 6.dp, vertical = 1.dp)
            .then(if (spoken == null) Modifier else Modifier.clearAndSetSemantics { contentDescription = spoken }),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides colour) {
            TagIcon(tag, Modifier.size(scaled(11)))
            // One line always: a narrow row moves the chip to the next line rather than break its words, and a name wider
            // than the row ends in «…».
            Text(
                text ?: stringResource(tag.title),
                fontWeight = FontWeight.SemiBold,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/**
 * The tag's icon in the content colour, not read (its words say it): a magnifier with a question mark for rare (check it
 * again), a crossed-out factory for obsolete (no longer made), one big shell for cannon (also the icon of its developer
 * switch, Raven 2026-11-19), an equals sign for the group, drawn on a 16 × 16 grid as Kaliberfinder's TagIcon. Unsourced is a
 * Material Symbol for Kaliberfinder's SF Symbol.
 */
@Composable
fun TagIcon(tag: CalibreTag, modifier: Modifier = Modifier) {
    val ink = LocalContentColor.current
    if (tag == CalibreTag.UNSOURCED) {
        Icon(painterResource(R.drawable.ic_unsourced), null, modifier, tint = ink)
        return
    }
    // Its own layer: the gap around the factory's slash cuts the factory only, not what is under the icon.
    Canvas(modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        val s = size.minDimension / 16
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        fun line(from: Offset, to: Offset, width: Float) = drawLine(ink, from, to, width * s, StrokeCap.Round)
        when (tag) {
            CalibreTag.RARE -> {
                drawCircle(ink, 5 * s, p(6.6f, 6.6f), style = Stroke(1.8f * s))
                line(p(10.3f, 10.3f), p(14.3f, 14.3f), 2.3f)
                val hook = Path().apply {
                    arcTo(Rect(p(6.65f, 5.3f), 1.65f * s), 180f, 240f, forceMoveTo = true)
                    lineTo(6.6f * s, 8 * s)
                }
                drawPath(hook, ink, style = Stroke(1.5f * s, cap = StrokeCap.Round))
                drawCircle(ink, 0.9f * s, p(6.6f, 9.6f))
            }
            CalibreTag.OBSOLETE -> {
                val factory = Path().apply {
                    moveTo(2 * s, 14 * s)
                    for ((x, y) in listOf(2f to 7.5f, 5.5f to 9.7f, 5.5f to 7.5f, 9f to 9.7f, 9f to 3.5f, 12.5f to 3.5f, 12.5f to 14f)) {
                        lineTo(x * s, y * s)
                    }
                    close()
                }
                drawPath(factory, ink, style = Stroke(1.7f * s, join = StrokeJoin.Round))
                // A gap around the slash, so it reads as crossed out.
                drawLine(Color.Black, p(1.5f, 2f), p(14.5f, 14.5f), 4 * s, blendMode = BlendMode.DstOut)
                line(p(1.5f, 2f), p(14.5f, 14.5f), 1.8f)
            }
            CalibreTag.CANNON -> {
                val shell = Path().apply {
                    // The ogive, then the case walls.
                    moveTo(4.8f * s, 7 * s)
                    cubicTo(4.8f * s, 3.6f * s, 6.6f * s, 1.7f * s, 8 * s, 1 * s)
                    cubicTo(9.4f * s, 1.7f * s, 11.2f * s, 3.6f * s, 11.2f * s, 7 * s)
                    lineTo(11.2f * s, 14.6f * s)
                    lineTo(4.8f * s, 14.6f * s)
                    lineTo(4.8f * s, 7 * s)
                    // The driving band (no line at the ogive's base, which would merge with it) and the rim.
                    moveTo(4.8f * s, 8.6f * s)
                    lineTo(11.2f * s, 8.6f * s)
                    moveTo(3.6f * s, 14.6f * s)
                    lineTo(12.4f * s, 14.6f * s)
                }
                drawPath(shell, ink, style = Stroke(1.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            CalibreTag.GROUP -> {
                line(p(3f, 5.5f), p(13f, 5.5f), 2.3f)
                line(p(3f, 10.5f), p(13f, 10.5f), 2.3f)
            }
            CalibreTag.UNSOURCED -> Unit
        }
    }
}
