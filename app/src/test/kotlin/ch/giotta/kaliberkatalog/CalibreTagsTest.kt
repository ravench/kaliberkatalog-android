package ch.giotta.kaliberkatalog

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The calibres' tags in Swiss German (Kaliberfinder's CalibreTag): on their rows, and what they mean on their pages. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "de-rCH")
class CalibreTagsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app get() = RuntimeEnvironment.getApplication() as KaliberkatalogApp

    /** The list, searched for [query]. */
    private fun search(query: String) {
        compose.setContent { App(app.catalog) }
        compose.onNode(hasSetTextAction()).performTextInput(query)
    }

    /** The row of the calibre [name], in the list or on a group's page. */
    private fun row(name: String) = compose.onNode(hasText(name) and hasClickAction() and !hasSetTextAction())

    /**
     * The .44 Henry is rare and obsolete, and in a one-size group, which its row names, read as the group (its rimfire and
     * centrefire versions). Its page says what the tags mean; on the group's page, which the group's name titles, the row
     * has no group tag.
     */
    @Test
    fun theHenrysTags() {
        search(".44 Henry")
        row(".44 Henry").assert(hasText("Selten")).assert(hasText("Veraltet")).assert(hasContentDescription("Gruppe .44 Henry"))
        row(".44 Henry").performClick()
        compose.onNodeWithText("Selten: Ein Treffer ist oft eine ähnliche Patrone, genau nachmessen.").assertExists()
        compose.onNodeWithText("Veraltet: wird nicht mehr regulär hergestellt.").assertExists()
        compose.onNode(hasText("Gruppe") and hasClickAction()).performClick()
        row(".44 Henry").assert(hasText("Selten")).assert(!hasContentDescription("Gruppe .44 Henry"))
    }

    /**
     * With its switch on, an unsourced calibre shows, and says that it is one: the .38-70 Winchester. Its group, told apart
     * by the length, is no tag: only a group of one size is.
     */
    @Test
    fun anUnsourcedCalibre() {
        app.settings.showUnsourced = true
        search(".38-70")
        row(".38-70 Winchester").assert(hasText("Unbelegt")).assert(!hasContentDescription("Gruppe .38-56 WCF")).performClick()
        compose.onNodeWithText(
            "Unbelegt: Masse in calibre-data nicht gut belegt, Teile der Zeichnung geschätzt. Nur mit der Entwickleroption sichtbar.",
        ).assertExists()
    }

    /** With its switch on, a cannon round shows, and says that it is one: the 20x102 mm. */
    @Test
    fun aCannonRound() {
        app.settings.showCannon = true
        search("20x102")
        row("20x102 mm").assert(hasText("Kanone")).performClick()
        compose.onNodeWithText("Kanone: nur mit der Entwickleroption sichtbar.").assertExists()
    }
}
