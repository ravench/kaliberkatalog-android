package ch.giotta.kaliberkatalog

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The lookalike group's row on a calibre's page and the group's page (Kaliberfinder's LookalikeGroupView), in Swiss German. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "de-rCH")
class GroupScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val catalog get() = (RuntimeEnvironment.getApplication() as KaliberkatalogApp).catalog
    private val settings get() = (RuntimeEnvironment.getApplication() as KaliberkatalogApp).settings

    /** The app, searched for [query], on the page of [name]. */
    private fun open(query: String, name: String) {
        compose.setContent { App(catalog) }
        compose.onNode(hasSetTextAction()).performTextInput(query)
        compose.onNode(hasText(name) and hasClickAction() and !hasSetTextAction()).performClick()
    }

    /** A member's row, which opens its page. */
    private fun member(name: String) = compose.onNode(hasText(name) and hasClickAction())

    /** The 5.56x45 mm group: four calibres of one case, told apart by their load; a member opens its page and back returns. */
    @Test
    fun theNatoRoundsGroup() {
        open("5.56", "5.56x45 mm NATO")
        compose.onNode(hasText("Gruppe") and hasText("5.56x45 mm") and hasClickAction()).performClick()
        compose.onNodeWithText("Gruppe 5.56x45 mm").assertExists()
        for (name in listOf(".223 Remington", "5.56x45 mm NATO", "5.56 Commercial", "5.6 mm GP 90")) member(name).assertExists()
        compose.onNodeWithText("Unterscheiden sich in: Ladung").assertExists()
        member(".223 Remington").performClick()
        compose.onNodeWithText(".223 Remington").assertExists() // its page's title
        compose.onNodeWithText("Bauart").assertExists()
        compose.onNodeWithContentDescription("Zurück").performClick()
        compose.onNodeWithText("Gruppe 5.56x45 mm").assertExists()
        compose.onNodeWithContentDescription("Zurück").performClick()
        compose.onNodeWithText("5.56x45 mm NATO").assertExists() // the page the group was opened from
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNode(hasSetTextAction()).assert(hasText("5.56"))
    }

    /** A group told apart by measurements names them all, in the app's words. */
    @Test
    fun theSwiss75Group() {
        open("Modell 1890", "7.5x53.5 mm Swiss (Patrone Modell 1890)")
        compose.onNode(hasText("Gruppe") and hasClickAction()).performClick()
        compose.onNodeWithText("Unterscheiden sich in: Laufdurchmesser, Länge, Ladung").assertExists()
        member("7.5 mm Kadettenpatrone").assertExists()
    }

    /** Kaliberfinder's LookalikeGroupView.lists: a group whose other calibres are all hidden is no row. */
    @Test
    fun aGroupOfOneIsNoRow() {
        assertNull(linkedGroup(catalog, cartridge(".38-56 WCF"), settings)) // the .38-70 Winchester is unsourced
        assertNotNull(linkedGroup(catalog, cartridge("9x19 mm"), settings))
        open(".38-56", ".38-56 WCF")
        compose.onNodeWithText("Bauart").assertExists()
        compose.onNodeWithText("Gruppe").assertDoesNotExist()
    }

    /** Every difference of the catalogue has its words. */
    @Test
    fun everyDifferenceHasATitle() {
        val differences = catalog.file.lookalikes.flatMap { it.differs }.toSet()
        assertEquals(setOf("rim", "bore", "length", "seating", "load", "ignition"), differences)
        for (difference in differences) assertNotNull(difference, differenceTitle(difference))
    }
}
