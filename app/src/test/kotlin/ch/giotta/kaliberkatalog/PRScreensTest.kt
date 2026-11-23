package ch.giotta.kaliberkatalog

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performImeAction
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
@Config(qualifiers = "de-w412dp-h915dp-xhdpi")
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

    /** The list in English, which every language the app has no texts for gets. */
    @Test
    @Config(qualifiers = "en-w412dp-h915dp-xhdpi")
    fun english() {
        compose.setContent { App((RuntimeEnvironment.getApplication() as KaliberkatalogApp).catalog) }
        screen("04-Englisch")
    }

    private fun screen(name: String) = compose.onRoot().captureRoboImage("build/screens/$name.png")
}
