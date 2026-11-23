package ch.giotta.kaliberkatalog

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The developer's switches in Swiss German (Kaliberfinder's DeveloperOptionsView) and what they change. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE) // drawn pixels, for theOriginalMaterialsAreTheFirstIssues
@Config(qualifiers = "de-rCH")
class DeveloperOptionsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app get() = RuntimeEnvironment.getApplication() as KaliberkatalogApp

    private fun listRow(name: String) = compose.onNode(hasText(name) and hasClickAction() and !hasSetTextAction())

    /** The settings' last row opens the switches; the cannon switch brings the cannon rounds into the list, here a search. */
    @Test
    fun theCannonSwitchShowsTheCannonRounds() {
        compose.setContent { App(app.catalog) }
        compose.onNode(hasSetTextAction()).performTextInput("20x102")
        listRow("20x102 mm").assertDoesNotExist()
        compose.onNodeWithContentDescription("Einstellungen").performClick()
        compose.onNodeWithText("Entwickleroptionen").performScrollTo().performClick() // the last row, under the fold on a small phone
        compose.onNodeWithText("Kanonenkaliber zeigen").performClick()
        repeat(2) { compose.onNodeWithContentDescription("Zurück").performClick() }
        listRow("20x102 mm").assertExists()
    }

    /** Each row turns its own switch, and the settings read again from the app's preferences have it. */
    @Test
    fun eachSwitchIsKept() {
        fun switches(settings: AppSettings) = listOf(settings.showUnsourced, settings.showCannon, settings.showOriginal)
        compose.setContent { DeveloperOptionsScreen(onBack = {}) }
        compose.onNodeWithText("Unbelegte Kaliber zeigen").performClick()
        assertEquals(listOf(true, false, false), switches(app.settings))
        compose.onNodeWithText("Kanonenkaliber zeigen").performClick()
        assertEquals(listOf(true, true, false), switches(app.settings))
        compose.onNodeWithText("Ursprüngliche Materialien zeigen").performScrollTo().performClick()
        assertEquals(listOf(true, true, true), switches(AppSettings(app.getSharedPreferences("settings", Context.MODE_PRIVATE))))
    }

    /** Unsourced calibres show, and with them the .38-56 WCF's group: its only other calibre, the .38-70 Winchester, is one. */
    @Test
    fun theUnsourcedSwitchShowsTheUnsourcedCalibres() {
        assertNull(linkedGroup(app.catalog, cartridge(".38-56 WCF"), app.settings))
        app.settings.showUnsourced = true
        assertNotNull(linkedGroup(app.catalog, cartridge(".38-56 WCF"), app.settings))
        compose.setContent { App(app.catalog) }
        compose.onNode(hasSetTextAction()).performTextInput(".38-70")
        listRow(".38-70 Winchester").assertExists()
    }

    /** The GP 11 is drawn with today's copper jacket, and with the switch in the cupronickel it was first issued with. */
    @Test
    fun theOriginalMaterialsAreTheFirstIssues() {
        compose.setContent { CartridgeView(cartridge("7.5x55 mm Swiss (GP 11)"), 4f) }
        val today = compose.onRoot().captureToImage().asAndroidBitmap()
        app.settings.showOriginal = true
        val first = compose.onRoot().captureToImage().asAndroidBitmap()
        assertFalse(today.sameAs(first))
    }
}
