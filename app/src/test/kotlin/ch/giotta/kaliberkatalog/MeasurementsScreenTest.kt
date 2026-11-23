package ch.giotta.kaliberkatalog

import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import ch.giotta.kaliberkatalog.catalog.DimensionLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The dimensioned drawing in Swiss German (Kaliberfinder's MeasurementsView), opened from the calibre page. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE) // the fonts' real line heights, for aLabelsLinesFitIt
@Config(qualifiers = "de-rCH")
class MeasurementsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The app, searched for [query], on the drawing of [name]. */
    private fun open(query: String, name: String) {
        compose.setContent { App((RuntimeEnvironment.getApplication() as KaliberkatalogApp).catalog) }
        compose.onNode(hasSetTextAction()).performTextInput(query)
        compose.onNode(hasText(name) and hasClickAction() and !hasSetTextAction()).performClick()
        compose.onNodeWithText("Masszeichnung anzeigen").performClick()
    }

    /** The labels, each read as one: the calibre page's name of the value, then the value. */
    private fun labels() = compose.onAllNodes(hasContentDescription(" mm", substring = true)).fetchSemanticsNodes()

    private fun spoken() = labels().map { it.config[SemanticsProperties.ContentDescription].single() }

    /** Kaliberfinder's testPartsFollowTheCalibrePage on the screen: the diameters top to bottom, then the lengths. */
    @Test
    fun theGp11sDrawingNamesEveryValue() {
        open("GP 11", "7.5x55 mm Swiss (GP 11)")
        val diameters = listOf("Geschossdurchmesser 7.8 mm", "Halsdurchmesser 8.5 mm", "Schulterdurchmesser 11.6 mm")
        val base = listOf("Bodendurchmesser 12.6 mm", "Randdurchmesser 12.6 mm")
        assertEquals(diameters + base + listOf("Hülsenlänge 55.6 mm", "Gesamtlänge 77.5 mm"), spoken())
        compose.onNodeWithText("7.5x55 mm Swiss (GP 11)").assertExists() // the page's title
        compose.onNodeWithContentDescription("Zurück").performClick()
        compose.onNodeWithText("Masszeichnung anzeigen").assertExists()
    }

    /** A pellet has no case: its diameter and its length. */
    @Test
    fun aPelletHasItsDiameterAndLength() {
        open("Diabolo", "4.5 mm Diabolo (.177)")
        assertEquals(listOf("Geschossdurchmesser 4.5 mm", "Gesamtlänge 5.5 mm"), spoken())
    }

    /** The bar's «Quellen» opens the sources, as the calibre page's last row does; back returns to the drawing. */
    @Test
    fun theSourcesAreInTheBar() {
        open("GP 11", "7.5x55 mm Swiss (GP 11)")
        compose.onNodeWithContentDescription("Quellen").performClick()
        compose.onNode(hasText("Cartridges of the World") and hasText("W. Todd Woodard, Corey Graff · 2026 · S. 573")).assertExists()
        compose.onNodeWithContentDescription("Zurück").performClick()
        assertEquals(7, spoken().size)
    }

    /** A label's two lines fit its height at the default text size, so the value keeps its size: Ø 12.6 mm, under «Hülse». */
    @Test
    fun aLabelsLinesFitIt() {
        var lines = 0.dp
        compose.setContent {
            val measure = rememberTextMeasurer()
            val (name, value) = labelStyles()
            val px = measure.measure("Hülse", name).size.height + measure.measure("Ø 12.6 mm", value).size.height
            lines = with(LocalDensity.current) { px.toDp() }
        }
        compose.waitForIdle()
        assertTrue("$lines", lines + 1.dp <= DimensionLayout.LABEL.height.dp) // and 1 dp between them
    }

    /** A partial entry, as most cannon rounds, has no drawing to open. */
    @Test
    fun aCalibreWithoutADrawingHasNone() {
        compose.setContent {
            CartridgeDetailScreen(cartridge("20x124 mm"), group = null, onBack = {}, onMeasurements = {}, onGroup = {}, onSources = {})
        }
        compose.onNodeWithText("Hülsenlänge").assertExists()
        compose.onNodeWithText("Masszeichnung anzeigen").assertDoesNotExist()
    }

    /** At twice the text size on a small phone the labels stay on the screen and apart: they stop growing, then shrink. */
    @Test
    @Config(qualifiers = "de-rCH-w360dp-h640dp", fontScale = 2f)
    fun largeTextKeepsTheLabelsApart() {
        open("GP 11", "7.5x55 mm Swiss (GP 11)")
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val boxes = labels().map { it.boundsInRoot }
        assertEquals(7, boxes.size)
        for ((i, box) in boxes.withIndex()) {
            assertEquals(box, box.intersect(screen))
            // Less a pixel each side: neighbours touch, and their edges round to whole pixels.
            for (other in boxes.drop(i + 1)) assertFalse("$box, $other", box.deflate(1f).overlaps(other.deflate(1f)))
        }
    }
}
