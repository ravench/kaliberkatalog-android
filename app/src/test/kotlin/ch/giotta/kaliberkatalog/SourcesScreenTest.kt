package ch.giotta.kaliberkatalog

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The sources page in Swiss German: Kaliberfinder's SourcesTests as the page shows them. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "de-rCH")
class SourcesScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The web pages the rows opened. */
    private val opened = mutableListOf<String>()

    private fun show(id: String) = compose.setContent {
        val browser = object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }
        CompositionLocalProvider(LocalUriHandler provides browser) { SourcesScreen(cartridge(id), live.file.sources, onBack = {}) }
    }

    /** The rows of a work, each read together: its title, then its detail line. */
    private fun rows(title: String, detail: String) = compose.onAllNodes(hasText(title) and hasText(detail))

    /** The sections' titles, in order. */
    private fun headers() =
        compose.onAllNodes(isHeading()).fetchSemanticsNodes().map { node -> node.config[SemanticsProperties.Text].joinToString { it.text } }

    /** Kaliberfinder's testGP11ListsItsMeasurementSourcesFirst and testHeaderNamesTheMergedParts. */
    @Test
    fun theGp11ListsItsMeasurementSourcesFirst() {
        show("7.5x55 mm Swiss (GP 11)")
        assertEquals(listOf("Masse", "Schulterwinkel · Hülsenboden", "Zündhütchen", "Geschoss", "Herkunft", "Farbcodes"), headers())
        rows("Cartridges of the World", "W. Todd Woodard, Corey Graff · 2026 · S. 573").assertCountEquals(1)
        rows("Hornady Handbook of Cartridge Reloading", "Neal Emery · 2016 · S. 469").assertCountEquals(1)
        rows("C.I.P.", "Tab. I · tabical-en-page46").assertCountEquals(3) // the measurements, the shoulder and head, the origin
    }

    /** Kaliberfinder's testABookShowsAuthorsYearAndPages: a book is no link. */
    @Test
    fun aBookShowsAuthorsYearAndPages() {
        show("7.5x55 mm Swiss (GP 11)")
        rows("Military Small Arms Ammunition of the World, 1945-1980", "Peter Labbett · 1980 · S. 76, 77, 86")
            .assertCountEquals(1)[0]
            .assert(!hasClickAction())
    }

    /** A web page opens in the browser and is read with Kaliberfinder's hint. */
    @Test
    fun aWebPageOpensInTheBrowser() {
        show("7.5x55 mm Swiss (GP 11)")
        rows("C.I.P.", "Tab. I · tabical-en-page46")[0].assert(hasContentDescription("Öffnet die Website")).performClick()
        assertEquals(listOf("https://bobp.cip-bobp.org/uploads/tdcc/tab-i/tabical-en-page46.pdf"), opened)
    }

    /** Kaliberfinder's testAWebPageShowsItsHostOnly: without year or pages the title says it (the 6 mm ARC's shoulder). */
    @Test
    fun aWebPageWithoutDetailShowsItsTitleOnly() {
        show("6 mm ARC")
        val row = compose.onAllNodes(hasText("bulletin.accurateshooter.com") and hasClickAction())[0].fetchSemanticsNode()
        assertEquals(listOf("bulletin.accurateshooter.com"), row.config[SemanticsProperties.Text].map { it.text })
    }

    /** Kaliberfinder's testACaliperMeasurementShowsItsDay and testNoseIsTheHeadLength…: Raven's caliper, 8 November 2026. */
    @Test
    fun aCaliperMeasurementShowsItsDay() {
        show("7.5 mm Kadettenpatrone")
        assertEquals(listOf("Masse", "Schulterwinkel · Hülsenboden", "Kopflänge", "Zündhütchen · Herkunft"), headers())
        rows("Eigene Messung", "8.11.2026").assertCountEquals(2) // the measurements and the nose
        rows("Swiss Magazine Loading Rifles 1869 to 1958", "Joe Poyer · 2010 · S. 50").assertCountEquals(1)
    }

    @Test
    fun theDayIsThePhonesNumericDate() {
        assertEquals("8.11.2026", day("2026-11-08", Locale.forLanguageTag("de-CH")))
        assertEquals("11/8/2026", day("2026-11-08", Locale.US))
        assertEquals("2026-11", day("2026-11", Locale.US)) // no day to format: as the data has it
    }

    /** Kaliberfinder's testEveryPartHasATitle and testCasePartNamesTheMaterial: «Hülsenmaterial», not «Hülse». */
    @Test
    fun everyPartHasATitle() {
        for (part in live.file.cartridges.flatMap { c -> c.sources.flatMap { it.parts } }.toSet()) {
            assertNotNull(part, sourcePartTitle(part))
        }
        assertEquals("Hülsenmaterial", RuntimeEnvironment.getApplication().getString(sourcePartTitle("case")!!))
    }

    /** The calibre page's last row opens its sources; back returns to the page where it was, then to the list. */
    @Test
    fun theCalibrePageOpensItsSourcesAndBackReturnsToIt() {
        val restore = StateRestorationTester(compose)
        restore.setContent { App((RuntimeEnvironment.getApplication() as KaliberkatalogApp).catalog) }
        compose.onNode(hasSetTextAction()).performTextInput("GP 11")
        compose.onNode(hasText("7.5x55 mm Swiss (GP 11)") and hasClickAction() and !hasSetTextAction()).performClick()
        compose.onNodeWithText("Quellen").performScrollTo().performClick()
        rows("Cartridges of the World", "W. Todd Woodard, Corey Graff · 2026 · S. 573").assertCountEquals(1)
        restore.emulateSavedInstanceStateRestore() // turning the phone keeps the open pages
        rows("Cartridges of the World", "W. Todd Woodard, Corey Graff · 2026 · S. 573").assertCountEquals(1)
        compose.onNodeWithContentDescription("Zurück").performClick()
        compose.onNodeWithText("Quellen").assertIsDisplayed() // the calibre page, still at its end
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNode(hasSetTextAction()).assert(hasText("GP 11"))
    }

    /** Two taps on «Quellen» before the next frame, as with two fingers, open the sources once: back is the calibre page. */
    @Test
    fun twoTapsInOneFrameOpenOnePage() {
        compose.setContent { App((RuntimeEnvironment.getApplication() as KaliberkatalogApp).catalog) }
        compose.onNode(hasSetTextAction()).performTextInput("GP 11")
        compose.onNode(hasText("7.5x55 mm Swiss (GP 11)") and hasClickAction() and !hasSetTextAction()).performClick()
        val sources = compose.onNode(hasText("Quellen") and hasClickAction()).fetchSemanticsNode()
        compose.runOnUiThread { repeat(2) { sources.config[SemanticsActions.OnClick].action!!() } }
        compose.onNodeWithContentDescription("Zurück").performClick()
        compose.onNodeWithText("Bauart").assertExists() // the calibre page, not the sources again
    }
}
