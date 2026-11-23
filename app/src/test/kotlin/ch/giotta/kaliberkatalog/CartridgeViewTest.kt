package ch.giotta.kaliberkatalog

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import ch.giotta.kaliberkatalog.catalog.Cartridge
import ch.giotta.kaliberkatalog.catalog.OutlineGeometry
import ch.giotta.kaliberkatalog.catalog.Point
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** What [CartridgeView] draws: its size in dp, each part in its colour and order, the cavity's open line. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xhdpi") // 2 px per dp: a scale taken as px would draw at half the size
class CartridgeViewTest {
    @get:Rule
    val compose = createComposeRule()

    private val dpPerMm = 8f

    private fun draw(cartridge: Cartridge): ImageBitmap {
        compose.setContent { CartridgeView(cartridge, dpPerMm, Modifier.testTag("drawing")) }
        return compose.onNodeWithTag("drawing").captureToImage()
    }

    /** The colour [x] and [y] millimetres from the drawing's top left corner. */
    private fun ImageBitmap.at(x: Double, y: Double): Int {
        val pxPerMm = dpPerMm * compose.density.density
        return toPixelMap()[(x * pxPerMm).toInt(), (y * pxPerMm).toInt()].toArgb()
    }

    @Test
    fun drawsAtItsScaleWithEachPartInItsColour() {
        val nine = cartridge("9x19 mm")
        val o = nine.outline
        val d = nine.dimensions
        val image = draw(nine)
        assertEquals(o.width * dpPerMm * 2, image.width.toDouble(), 1.0)
        assertEquals(o.height * dpPerMm * 2, image.height.toDouble(), 1.0)
        // On the axis, half way along the bullet in front of the case mouth, and half way up the case.
        assertEquals(CartridgeDrawing.copper.toArgb(), image.at(o.width / 2, (d.overallLength - d.caseLength) / 2))
        assertEquals(CartridgeDrawing.brass.toArgb(), image.at(o.width / 2, d.overallLength - d.caseLength / 2))
    }

    /** The tip lies on the bullet's nose: it is drawn after the bullet. */
    @Test
    fun aPolymerTipShowsOverItsBullet() {
        val hmr = cartridge(".17 HMR")
        val tip = OutlineGeometry.parse(checkNotNull(hmr.outline.tip)).single()
        assertEquals(CartridgeDrawing.polymer.toArgb(), draw(hmr).at(hmr.outline.width / 2, tip.maxOf { it.y } / 2))
    }

    /** The mouth lies on the bullet's front edge, which the outline draws (Kaliberfinder's testHollowPointCavityIsBold). */
    @Test
    fun theCavitysLineLeavesTheMouthOpen() {
        val triangle = listOf(Point(0.0, 0.0), Point(1.0, 2.0), Point(2.0, 0.0))
        val open = polygonPath(listOf(triangle), scale = 1f, closed = false)
        val length = PathMeasure().apply { setPath(open, forceClosed = false) }.length
        assertEquals("no closing segment along the mouth", 2 * sqrt(5f), length, 0.01f)
    }
}
