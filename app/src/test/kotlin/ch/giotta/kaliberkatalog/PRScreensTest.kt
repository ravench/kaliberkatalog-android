package ch.giotta.kaliberkatalog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.unit.dp
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

    /** The list in English, which every language the app has no texts for gets. */
    @Test
    @Config(qualifiers = "en-w412dp-h915dp-xhdpi")
    fun english() {
        compose.setContent { App((RuntimeEnvironment.getApplication() as KaliberkatalogApp).catalog) }
        screen("04-Englisch")
    }

    /** The launcher icon (tools/app_icon.py): its layers in a round mask, and its themed look on a dark home screen. */
    @Test
    fun launcherIcon() {
        compose.setContent {
            Row(Modifier.fillMaxSize().background(Color(0xFFECECEC)), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                LauncherIcon(listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground))
                LauncherIcon(listOf(R.drawable.ic_launcher_monochrome), Color(0xFFD6E3FF), Modifier.background(Color(0xFF1F2A44)))
            }
        }
        screen("00-App-Symbol")
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

/** An adaptive icon's [layers] as a launcher shows them, twice as large: the middle 72 of their 108 dp, in a circle. */
@Composable
private fun LauncherIcon(layers: List<Int>, tint: Color? = null, modifier: Modifier = Modifier) =
    Box(Modifier.size(144.dp).clip(CircleShape).then(modifier), contentAlignment = Alignment.Center) {
        for (layer in layers) {
            Image(painterResource(layer), null, Modifier.requiredSize(216.dp), colorFilter = tint?.let { ColorFilter.tint(it) })
        }
    }
