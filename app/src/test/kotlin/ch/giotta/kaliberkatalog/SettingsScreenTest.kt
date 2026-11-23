package ch.giotta.kaliberkatalog

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import ch.giotta.kaliberkatalog.catalog.LengthUnit
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The settings in Swiss German: Kaliberfinder's SettingsView, without the 1:1 comparison and the photo. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "de-rCH")
class SettingsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app get() = RuntimeEnvironment.getApplication() as KaliberkatalogApp

    /**
     * The list's gear opens the settings; in inches the calibre page and its dimensioned drawing show inches, to 0.001 in as
     * US load data.
     */
    @Test
    fun inchesShowOnTheCalibrePageAndItsDrawing() {
        compose.setContent { App(app.catalog) }
        compose.onNodeWithContentDescription("Einstellungen").performClick()
        compose.onNodeWithText("Imperial (Zoll)").performClick()
        compose.onNodeWithContentDescription("Zurück").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("9x19")
        compose.onNode(hasText("9x19 mm") and hasClickAction() and !hasSetTextAction()).performClick()
        compose.onNode(hasText("Gesamtlänge") and hasText("1.169 in")).assertExists()
        compose.onNodeWithText("Masszeichnung anzeigen").performClick()
        compose.onNodeWithContentDescription("Gesamtlänge 1.169 in").assertExists() // its label, as it is read
    }

    /** The unit is kept: the settings read again from the app's preferences have it. */
    @Test
    fun theUnitIsKept() {
        assertEquals(LengthUnit.METRIC, app.settings.unit)
        app.settings.unit = LengthUnit.IMPERIAL
        assertEquals(LengthUnit.IMPERIAL, AppSettings(app.getSharedPreferences("settings", Context.MODE_PRIVATE)).unit)
    }

    /** The app's version and build, the calibre-data commit the catalogue was made from, and the licence lines. */
    @Test
    fun theInfoNamesTheVersionAndTheCalibreData() {
        compose.setContent { SettingsScreen(app.catalog, onBack = {}) }
        compose.onNode(hasText("Version") and hasText("1.0 (1)")).assertExists()
        val source = checkNotNull(app.catalog.file.calibreData)
        compose.onNode(hasText("Kaliberdaten") and hasText(source.commit.take(7), substring = true)).assertExists()
        compose.onNodeWithText("© 2026 Simon Giotta").assertExists()
        compose.onNodeWithText("Open Database License (ODbL)", substring = true).assertExists()
    }
}
