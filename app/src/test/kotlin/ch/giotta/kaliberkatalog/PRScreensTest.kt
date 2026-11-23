package ch.giotta.kaliberkatalog

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The screen tour of the PR screens (.github/workflows/build.yml): build.yml runs it on a pull request and on its base
 * branch and comments with the screens that look different. Each screen a pull request can change needs a stop here.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "de-rCH-w412dp-h915dp-xhdpi") // Swiss: 29.7 mm
class PRScreensTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tour() {
        compose.setContent { App((RuntimeEnvironment.getApplication() as KaliberkatalogApp).catalog) }
        screen("01-Kaliber")
        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("Vetterli")
        field.performImeAction() // closes the keyboard, so no cursor blinks in the screen
        screen("02-Suche")
        field.performTextReplacement("Kein Kaliber")
        field.performImeAction()
        screen("03-Keine-Treffer")
    }

    /** A row opens its calibre's page. */
    @Test
    fun calibrePage() {
        compose.setContent { App((RuntimeEnvironment.getApplication() as KaliberkatalogApp).catalog) }
        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("9x19")
        field.performImeAction()
        compose.onNode(hasText("9x19 mm") and hasClickAction() and !hasSetTextAction()).performClick()
        screen("05-Kaliber")
    }

    /** A NATO round: the alliance's flag is drawn, as no emoji exists, in the list and on the page. */
    @Test
    fun natoFlag() {
        compose.setContent { App((RuntimeEnvironment.getApplication() as KaliberkatalogApp).catalog) }
        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("5.56")
        field.performImeAction()
        screen("06-NATO")
        compose.onNode(hasText("5.56x45 mm NATO") and hasClickAction() and !hasSetTextAction()).performClick()
        compose.onNodeWithText("Gerundete Richtwerte, keine Wiederladedaten.").performScrollTo()
        screen("06-NATO-Kaliber-unten") // the Herkunft row with the drawn flag
    }

    /** The colour codes under the measurements (Raven, 2026-11-11): the GP 11's tracer tip and bases. */
    @Test
    fun gp11ColourCodes() {
        open("GP 11", "7.5x55 mm Swiss (GP 11)")
        compose.onNodeWithText("Kategorie").performScrollTo() // the row under the colour codes
        screen("07-GP11-Farbcodes")
    }

    /** Primer-ring colour codes: the .303 British's pre-NATO British code (Raven, 2026-11-11). */
    @Test
    fun primerRingColourCodes() {
        open(".303 British", ".303 British")
        compose.onNodeWithText("Kategorie").performScrollTo()
        screen("08-303-Zuendhuetchenringe")
    }

    /** The sources page (Raven, 2026-11-15): the GP 11's, opened from the calibre page's last row. */
    @Test
    fun gp11Sources() {
        open("GP 11", "7.5x55 mm Swiss (GP 11)")
        compose.onNodeWithText("Quellen").performScrollTo().performClick()
        screen("09-GP11-Quellen")
    }

    /** A lookalike group's page, from its row on a member's page: the four 5.56x45 mm rounds, told apart by their load. */
    @Test
    fun natoRoundsGroup() {
        open("5.56", "5.56x45 mm NATO")
        compose.onNode(hasText("Gruppe") and hasClickAction()).performClick()
        screen("10-Gruppe")
    }

    /** The dimensioned drawing (Kaliberfinder's MeasurementsView): the GP 11's, from the row under its drawing. */
    @Test
    fun gp11Measurements() {
        open("GP 11", "7.5x55 mm Swiss (GP 11)")
        compose.onNodeWithText("Masszeichnung anzeigen").performClick()
        screen("11-Masszeichnung")
    }

    /** The list in English, which every language the app has no texts for gets. */
    @Test
    @Config(qualifiers = "en-w412dp-h915dp-xhdpi")
    fun english() {
        compose.setContent { App((RuntimeEnvironment.getApplication() as KaliberkatalogApp).catalog) }
        screen("04-Englisch")
    }

    /** The app, searched for [query], on the page of [name]. */
    private fun open(query: String, name: String) {
        compose.setContent { App((RuntimeEnvironment.getApplication() as KaliberkatalogApp).catalog) }
        val field = compose.onNode(hasSetTextAction())
        field.performTextInput(query)
        field.performImeAction()
        compose.onNode(hasText(name) and hasClickAction() and !hasSetTextAction()).performClick()
    }

    private fun screen(name: String) = compose.onRoot().captureRoboImage("build/screens/$name.png")
}
