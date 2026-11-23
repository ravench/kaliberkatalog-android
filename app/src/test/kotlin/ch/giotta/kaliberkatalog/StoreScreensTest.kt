package ch.giotta.kaliberkatalog

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.github.takahirom.roborazzi.captureRoboImage
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 1080 x 1920 px, a phone's 9:16, which Google Play asks of the screenshots it recommends apps with. */
private const val PHONE = "w360dp-h640dp-xxhdpi"

/**
 * Google Play's phone screenshots in each of the listing's languages (store/), numbered in the order the listing shows
 * them, in app/build/store-screens/<language>/. Taken only where STORE_SCREENS is set: .github/workflows/store-screens.yml.
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

    /** A search and a lookalike group, then the GP 11: its page, its dimensioned drawing, colour codes and sources.
     * The drawing comes first in the listing: no other screen shows at a glance what the app is for. */
    private fun tour(language: String) {
        val app = RuntimeEnvironment.getApplication() as KaliberkatalogApp
        val back = app.getString(R.string.back)
        compose.setContent { App(app.catalog) }
        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("9 mm")
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
        screen(language, "5-colour-codes")
        compose.onNodeWithText(app.getString(R.string.sources)).performScrollTo().performClick()
        screen(language, "6-sources")
    }

    /** Opens the page of the calibre [name] from the list. */
    private fun open(name: String) = compose.onNode(hasText(name) and hasClickAction() and !hasSetTextAction()).performClick()

    /** The screen as Google Play takes it: 1080 x 1920 px, a PNG without an alpha channel. */
    private fun screen(language: String, name: String) {
        val file = File("build/store-screens/$language/$name.png")
        compose.onRoot().captureRoboImage(file.path)
        val image = ImageIO.read(file)
        assertEquals(1080 to 1920, image.width to image.height)
        val opaque = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        opaque.createGraphics().run {
            drawImage(image, 0, 0, null)
            dispose()
        }
        ImageIO.write(opaque, "png", file)
    }
}
