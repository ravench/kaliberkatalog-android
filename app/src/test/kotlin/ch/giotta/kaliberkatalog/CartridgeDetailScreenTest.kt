package ch.giotta.kaliberkatalog

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The calibre page in Swiss German, with Kaliberfinder's rows (CartridgeDetailView.swift) and PrimerTests' titles. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "de-rCH")
class CartridgeDetailScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The calibre's page; with [largeText], at twice the text size, where it lists the measurements. */
    private fun show(id: String, largeText: Boolean = false) = compose.setContent {
        val screen = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(screen.density, if (largeText) 2f else 1f)) {
            CartridgeDetailScreen(cartridge(id), group = null, onBack = {}, onMeasurements = {}, onGroup = {}, onSources = {})
        }
    }

    private fun row(label: String, value: String) = compose.onNode(hasText(label) and hasText(value)).assertExists()

    private fun noRow(label: String) = compose.onNodeWithText(label).assertDoesNotExist()

    private fun listRow(name: String) = compose.onNode(hasText(name) and hasClickAction() and !hasSetTextAction())

    @Test
    fun aRowOpensItsCalibreAndBackReturnsToTheSearch() {
        compose.setContent { App((RuntimeEnvironment.getApplication() as KaliberkatalogApp).catalog) }
        compose.onNode(hasSetTextAction()).performTextInput("9x19")
        listRow("9x19 mm").performClick()
        row("Bauart", "Randlos")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNode(hasSetTextAction()).assert(hasText("9x19"))
        listRow("9x19 mm").performClick()
        compose.onNodeWithContentDescription("Zurück").performClick()
        listRow("9x19 mm").assertIsDisplayed()
    }

    /** Turning the phone, or Android ending the app in the background, keeps the open page and the search behind it. */
    @Test
    fun theOpenPageAndTheSearchSurviveARestore() {
        val restore = StateRestorationTester(compose)
        restore.setContent { App((RuntimeEnvironment.getApplication() as KaliberkatalogApp).catalog) }
        compose.onNode(hasSetTextAction()).performTextInput("9x19")
        listRow("9x19 mm").performClick()
        restore.emulateSavedInstanceStateRestore()
        row("Bauart", "Randlos")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNode(hasSetTextAction()).assert(hasText("9x19"))
    }

    @Test
    fun aStraightPistolCase() {
        show("9x19 mm", largeText = true)
        row("Gesamtlänge", "29.7 mm")
        row("Hülsenlänge", "19.2 mm") // 19.15 mm, rounded as on iOS
        row("Randdurchmesser", "10.0 mm")
        row("Bodendurchmesser", "9.9 mm")
        row("Halsdurchmesser", "9.6 mm")
        row("Geschossdurchmesser", "9.0 mm")
        row("Zündhütchen", "Pistole klein, 4.4 mm")
        noRow("Schulterdurchmesser")
        noRow("Gürteldurchmesser")
        row("Kategorie", "Kurzwaffe")
        row("Bauart", "Randlos")
        row("Herkunft", "Deutschland")
        row("Verbreitung", "Weltweit verbreitet")
        row("Auch bekannt als", "9 mm Luger, 9 mm Para")
        compose.onNode(hasContentDescription("Boden mit Zündhütchen Pistole klein")).assertExists()
        compose.onNode(hasContentDescription("9x19 mm")).assertExists() // the drawing, as Kaliberfinder labels it
    }

    @Test
    fun aBeltedBottleneckCase() {
        show(".378 Weatherby Magnum", largeText = true)
        row("Gürteldurchmesser", "15.3 mm")
        row("Schulterdurchmesser", "14.2 mm")
        row("Bauart", "Gürtelhülse")
        noRow("Verbreitung")
        noRow("Auch bekannt als")
    }

    @Test
    fun aShotshellShowsItsOpenLengthAndNoBullet() {
        show("12/70", largeText = true)
        row("Hülsenlänge (offen)", "70.0 mm")
        row("Zündhütchen", "Schrot (209), 6.2 mm")
        noRow("Halsdurchmesser")
        noRow("Geschossdurchmesser")
        noRow("Herkunft") // a gauge
    }

    /** The origin row names the country once: its flag is not read. */
    @Test
    fun theOriginIsReadOnce() {
        show("7.5x55 mm Swiss (GP 11)")
        row("Herkunft", "Schweiz").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }

    /** A former state's flag is drawn, and not read either. */
    @Test
    fun aFormerStatesOrigin() {
        show("7.62x54 mm R")
        row("Herkunft", "Russisches Kaiserreich").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }

    @Test
    fun aRoundBallHasNoCase() {
        show(".44 Rundkugel (.454)", largeText = true)
        row("Gesamtlänge", "11.5 mm")
        row("Geschossdurchmesser", "11.5 mm")
        noRow("Hülsenlänge")
        noRow("Randdurchmesser")
        noRow("Zündhütchen")
        row("Bauart", "Rundkugel")
    }

    /**
     * Beside a dimensioned drawing the page lists no measurements, only the primer (Raven, 2026-11-24); they are listed
     * where the drawing's labels stop growing, and for a calibre without a drawing.
     */
    @Test
    fun theMeasurementsAreListedOnlyWithoutADrawing() {
        show("9x19 mm")
        compose.onNodeWithText("Masszeichnung anzeigen").assertExists()
        noRow("Masse")
        noRow("Gesamtlänge")
        noRow("Geschossdurchmesser")
        row("Zündhütchen", "Pistole klein, 4.4 mm")
        noRow("Hülsenlänge (offen)")
        assertFalse(listsMeasurements(isDrawn = true, fontScale = MAX_FONT_SCALE))
        assertTrue(listsMeasurements(isDrawn = true, fontScale = 2f))
        assertTrue(listsMeasurements(isDrawn = false, fontScale = 1f))
    }

    /** A shot shell is drawn crimped: its open length, not on the drawing, keeps its row. */
    @Test
    fun aShotshellKeepsItsOpenLengthBesideTheDrawing() {
        show("12/70")
        row("Hülsenlänge (offen)", "70.0 mm")
        noRow("Gesamtlänge")
    }

    @Test
    fun aCalibreWithoutADrawingListsItsMeasurements() {
        show("30x114 mm")
        row("Gesamtlänge", "190.0 mm")
        row("Geschossdurchmesser", "30.0 mm")
    }

    @Test
    fun rimfireHasNoPrimer() {
        show(".22 LR")
        row("Zündhütchen", "Randfeuer, ohne Zündhütchen")
        compose.onNode(hasContentDescription("Randfeuer, ohne Zündhütchen")).assertExists() // the case from below
    }

    /** Kaliberfinder's testHeadGeometry: the head face ends at the chamfer, or at the step to the body of a rimmed case. */
    @Test
    fun headGeometry() {
        val nine = cartridge("9x19 mm")
        assertTrue(faceDiameter(nine) < nine.headDiameter)
        val special = cartridge(".38 Special")
        assertEquals(special.dimensions.base, faceDiameter(special), 0.0)
    }

    @Test
    fun everyKindAndPrimerOfTheCatalogueHasItsTitle() {
        for (c in live.file.cartridges) {
            assertNotEquals(c.kind, R.string.kind_other, kindTitle(c.kind))
            c.primer?.let { assertNotNull(it.type, primerTitle(it.type)) }
        }
    }

    /** The row shows the calibre's own diameter after the title, so a type of several sizes names no number in its title. */
    @Test
    fun aPrimerTypeOfSeveralSizesNamesNoneInItsTitle() {
        val primers = live.all.mapNotNull { it.primer }
        for ((type, sizes) in primers.groupBy { it.type }.filterValues { s -> s.map { it.diameter }.toSet().size > 1 }) {
            val title = RuntimeEnvironment.getApplication().getString(checkNotNull(primerTitle(type)))
            assertFalse("$type: $title (${sizes.size})", title.any(Char::isDigit))
        }
    }
}
