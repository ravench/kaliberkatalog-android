package ch.giotta.kaliberkatalog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import ch.giotta.kaliberkatalog.catalog.Cartridge
import ch.giotta.kaliberkatalog.catalog.OutlineGeometry
import com.github.takahirom.roborazzi.captureRoboImage
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 1080 x 1920 px, a phone's 9:16, which Google Play asks of the screenshots it recommends apps with: 432 dp wide, where
 * the drawing's bar has room for the GP 11's whole name beside its sources button (at 360 dp it ends in «…»).
 */
private const val PHONE = "w432dp-h768dp-400dpi"

/**
 * Google Play's images for the listing: the phone screenshots in each of the listing's languages (store/), numbered in
 * the order the listing shows them, in app/build/store-screens/<language>/, and beside them the icon and the feature
 * graphic, which have no words and serve every language. Taken only where STORE_SCREENS is set:
 * .github/workflows/store-screens.yml.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StoreScreensTest {
    @get:Rule
    val compose = createComposeRule()

    @Test @Config(qualifiers = "de-rCH-$PHONE") // Swiss, as the app's German
    fun german() = tour("de")

    @Test @Config(qualifiers = "en-$PHONE")
    fun english() = tour("en")

    @Test @Config(qualifiers = "fr-$PHONE")
    fun french() = tour("fr")

    @Test @Config(qualifiers = "it-$PHONE")
    fun italian() = tour("it")

    @Test @Config(qualifiers = "es-$PHONE")
    fun spanish() = tour("es")

    /** The launcher icon as launchers show it, the middle 72 of its layers' 108 dp, as the full square Google Play asks
     * for: it rounds the corners and adds the shadow itself. */
    @Test @Config(qualifiers = "w512dp-h512dp-mdpi")
    fun icon() {
        compose.setContent {
            Box(Modifier.size(512.dp).clipToBounds(), contentAlignment = Alignment.Center) {
                for (layer in listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground)) {
                    Image(painterResource(layer), null, Modifier.requiredSize(768.dp))
                }
            }
        }
        val icon = save("icon", 512, 512, BufferedImage.TYPE_INT_ARGB) // Play takes a 32-bit PNG of 1 MB at most
        assertTrue(File("build/store-screens/icon.png").length() <= 1 shl 20)
        // Opaque to the corners: Play would show its own colour through.
        assertTrue(icon.getRGB(0, 0, 512, 512, null, 0, 512).all { it ushr 24 == 0xFF })
    }

    /** The feature graphic: nine calibres to scale on a ruler, the GP 11 in the middle with the icon's label, on its
     * beige. An extension of the icon, as Google Play asks, rather than the icon again. */
    @Test @Config(qualifiers = "w1024dp-h500dp-mdpi")
    fun featureGraphic() {
        val catalog = (RuntimeEnvironment.getApplication() as KaliberkatalogApp).catalog
        val calibres = LINEUP.map { requireNotNull(catalog.cartridge(it)) { it } }
        compose.setContent { Canvas(Modifier.fillMaxSize()) { lineup(calibres) } }
        save("feature-graphic", 1024, 500, BufferedImage.TYPE_INT_RGB)
    }

    /** A search and a lookalike group, then the GP 11: its page, its dimensioned drawing, colour codes and sources.
     * The drawing comes first in the listing: no other screen shows at a glance what the app is for. */
    private fun tour(language: String) {
        val app = RuntimeEnvironment.getApplication() as KaliberkatalogApp
        val back = app.getString(R.string.back)
        compose.setContent { App(app.catalog) }
        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("7.62")
        field.performImeAction() // closes the keyboard, so no cursor blinks in the screen
        screen(language, "3-search")
        field.performTextReplacement("5.56")
        field.performImeAction()
        open("5.56x45 mm NATO")
        compose.onNode(hasText(app.getString(R.string.group)) and hasClickAction()).performClick()
        screen(language, "4-lookalikes")
        repeat(2) { compose.onNodeWithContentDescription(back).performClick() }
        field.performTextReplacement("GP 11")
        field.performImeAction()
        open("7.5x55 mm Swiss (GP 11)")
        screen(language, "2-calibre")
        compose.onNodeWithText(app.getString(R.string.show_measurements)).performClick()
        screen(language, "1-drawing")
        compose.onNodeWithContentDescription(back).performClick()
        compose.onNodeWithText(app.getString(R.string.category)).performScrollTo() // the row under the colour codes
        wholeRowOnTop()
        screen(language, "5-colour-codes")
        compose.onNodeWithText(app.getString(R.string.sources)).performScrollTo().performClick()
        screen(language, "6-sources")
    }

    /** Scrolls the page on to its next row, so that no row is cut in half at the top: where performScrollTo stops depends
     * on how the rows above wrap, which differs between languages. */
    private fun wholeRowOnTop() {
        val page = compose.onNode(hasScrollAction())
        val top = page.fetchSemanticsNode().boundsInRoot.top
        // The rows' unclipped tops: boundsInRoot would clip a half-hidden row to start at the top.
        val row = page.fetchSemanticsNode().children.map { it.positionInRoot.y }.filter { it >= top }.min()
        page.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, row - top) }
    }

    /** Opens the page of the calibre [name] from the list. */
    private fun open(name: String) = compose.onNode(hasText(name) and hasClickAction() and !hasSetTextAction()).performClick()

    /** The screen as Google Play takes it: 1080 x 1920 px, a PNG without an alpha channel. */
    private fun screen(language: String, name: String) = save("$language/$name", 1080, 1920, BufferedImage.TYPE_INT_RGB)

    /** The screen, [width] x [height] px, as build/store-screens/[name].png with pixels of [type]. */
    private fun save(name: String, width: Int, height: Int, type: Int): BufferedImage {
        val file = File("build/store-screens/$name.png")
        compose.onRoot().captureRoboImage(file.path)
        val image = ImageIO.read(file)
        assertEquals(width to height, image.width to image.height)
        val saved = BufferedImage(width, height, type)
        saved.createGraphics().run {
            drawImage(image, 0, 0, null)
            dispose()
        }
        ImageIO.write(saved, "png", file)
        return saved
    }
}

/** The feature graphic's calibres, the GP 11 in the middle, the others falling away to both sides. */
private val LINEUP = listOf(
    ".22 LR", ".45 ACP", "7.62x39 mm", "12/70", "7.5x55 mm Swiss (GP 11)",
    ".308 Winchester", "5.6 mm GP 90", ".357 Magnum", "9x19 mm",
)

// The icon's colours and its label's measures, from tools/app_icon.py: change them there and here together.
private val BEIGE = listOf(Color(0xFFF1E6D2), Color(0xFFDCC9A8))
private val INK = Color(0xFF3A2A12)
private val PAPER = Color(0xFFF4F1E8)
private val RING = Color(0xFFE9E4D6)
private val RING_EDGE = Color(0xFFC9BFA8)
private val LINES = Color(0xFF9AA6B2)
private val STRING = Color(0xFF5A4632)
private const val LABEL_W = 23f
private const val LABEL_H = 12.5f
private const val CUT = 3.6f
private const val TIED = 9f // mm above the base, where the string goes round the case

/**
 * [lineup] standing on a ruler, to scale, the tallest 330 px, on the icon's beige; each drawn as the icon draws its
 * GP 11: filled in the app's colours, lit down its left third, outlined and on a soft shadow. The GP 11 wears the
 * icon's label.
 */
private fun DrawScope.lineup(lineup: List<Cartridge>) {
    drawRect(Brush.verticalGradient(BEIGE))
    val mm = 330f / lineup.maxOf { it.outline.height }.toFloat() // px per mm
    val base = 410f
    val gap = 34f
    // The ruler, at the cartridges' scale: a tick each millimetre, longer at 5 and 10.
    drawRect(Color(0xFFE9D6AE), Offset(0f, base), Size(size.width, 34f))
    drawLine(Color(0xFFBFA67A), Offset(0f, base + 34f), Offset(size.width, base + 34f))
    for (k in 0..(size.width / mm).toInt()) {
        val tick = if (k % 10 == 0) 14f else if (k % 5 == 0) 10f else 6f
        drawLine(Color(0xFF8C7350), Offset(k * mm, base), Offset(k * mm, base + tick))
    }
    var left = (size.width - lineup.sumOf { it.outline.width }.toFloat() * mm - gap * (lineup.size - 1)) / 2
    var labelled: Pair<Cartridge, Float>? = null
    for (c in lineup) {
        cartridge(c, left, base, mm)
        if (c.id == "7.5x55 mm Swiss (GP 11)") labelled = c to left
        left += c.outline.width.toFloat() * mm + gap
    }
    labelled?.let { (c, x) -> label(c, x, base, mm) }
}

/** [c] at [mm] px per mm, its left side at [left], standing on [base]. */
private fun DrawScope.cartridge(c: Cartridge, left: Float, base: Float, mm: Float) {
    val o = c.outline
    val tip = if (o.tipMaterial == "polymer") CartridgeDrawing.polymer else CartridgeDrawing.lead
    val parts = listOf(o.case to CartridgeDrawing.caseFill(c), o.head to CartridgeDrawing.headFill(c),
        o.bullet to CartridgeDrawing.bulletFill(c), o.tip to tip)
        .mapNotNull { (d, fill) -> d?.let { polygonPath(OutlineGeometry.parse(it), mm, closed = true) to fill } }
    val width = o.width.toFloat() * mm
    val height = o.height.toFloat() * mm
    translate(left, base - height) {
        translate(0.8f * mm, 1.2f * mm) { for ((path, _) in parts) drawPath(path, Color.Black.copy(alpha = 0.13f)) }
        for ((path, fill) in parts) {
            drawPath(path, fill)
            val light = lerp(fill, Color.White, 0.45f).copy(alpha = 0.8f)
            clipPath(path) { drawRect(light, Offset(width * 0.18f, 0f), Size(width * 0.16f, height)) }
            drawPath(path, INK, style = Stroke(1.2f, join = StrokeJoin.Round))
        }
    }
}

/** The icon's label, 86 px wide, on a string tied round [c]'s case TIED mm above the base, lying on the ruler. */
private fun DrawScope.label(c: Cartridge, left: Float, base: Float, mm: Float) {
    val k = 86f / LABEL_W // px per unit of the icon's label
    val knot = Offset(left + c.outline.width.toFloat() * mm, base - TIED * mm)
    val eye = knot + Offset(26f, 46f)
    val cord = 1.1f * k * 0.9f
    drawLine(STRING, Offset(left - 1f, knot.y - 1.5f), knot + Offset(1f, 1.5f), cord, StrokeCap.Round) // round the case
    val string = Path().apply {
        moveTo(knot.x, knot.y)
        cubicTo(knot.x + 14f, knot.y + 4f, eye.x - 10f, eye.y - 14f, eye.x, eye.y)
    }
    drawPath(string, STRING, style = Stroke(cord, cap = StrokeCap.Round))
    val shape = Path().apply {
        moveTo(0f, CUT * k)
        lineTo(CUT * k, 0f)
        lineTo(LABEL_W * k, 0f)
        lineTo(LABEL_W * k, LABEL_H * k)
        lineTo(CUT * k, LABEL_H * k)
        lineTo(0f, (LABEL_H - CUT) * k)
        close()
    }
    rotate(-14f, pivot = eye) {
        translate(eye.x - CUT * k, eye.y - LABEL_H / 2 * k) {
            translate(0.6f * k, 1.6f * k) { drawPath(shape, Color.Black.copy(alpha = 0.14f)) }
            drawPath(shape, PAPER)
            for ((x0, x1, y) in listOf(Triple(9f, 19f, 4.2f), Triple(9f, 14.5f, 8.3f))) {
                drawLine(LINES, Offset(x0 * k, y * k), Offset(x1 * k, y * k), 1.7f * k, StrokeCap.Round)
            }
            val centre = Offset(CUT * k, LABEL_H / 2 * k)
            drawCircle(RING, 2.3f * k, centre)
            drawCircle(RING_EDGE, 2.3f * k, centre, style = Stroke(0.5f * k))
            drawCircle(STRING, 1.25f * k, centre)
        }
    }
}
