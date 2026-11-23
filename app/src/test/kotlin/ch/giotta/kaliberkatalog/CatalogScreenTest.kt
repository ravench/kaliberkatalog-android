package ch.giotta.kaliberkatalog

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CatalogScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val catalog get() = (RuntimeEnvironment.getApplication() as KaliberkatalogApp).catalog

    private fun show() = compose.setContent { App(catalog) }

    private fun search(query: String) = compose.onNode(hasSetTextAction()).performTextInput(query)

    @Test
    fun theListStartsWithTheFirstCategory() {
        show()
        compose.onNodeWithText("Randfeuer").assertIsDisplayed()
        compose.onNodeWithText("Kaliber, z. B. 9 mm Luger").assertIsDisplayed()
    }

    @Test
    fun searchFindsACalibreByAnotherName() {
        show()
        search("9 mm Luger")
        compose.onNodeWithText("Kurzwaffe").assertIsDisplayed()
        compose.onNodeWithText("9x19 mm").assertIsDisplayed()
    }

    @Test
    fun searchFindsTheGroupsByTheirWords() {
        show()
        search("Platzpatrone") // a word of the blanks' group, not a name of the 6.8/11
        compose.onNodeWithText("6.8/11").assertIsDisplayed()
    }

    @Test
    fun aSearchWithoutResultsSaysSoAndClearingShowsTheListAgain() {
        show()
        search("Kein Kaliber")
        compose.onNodeWithText("Keine Ergebnisse für «Kein Kaliber»").assertIsDisplayed()
        compose.onNodeWithContentDescription("Suche löschen").performClick()
        compose.onNodeWithText("Randfeuer").assertIsDisplayed()
    }

    /** Where the list was scrolled to doesn't matter: a new search shows its first results. */
    @Test
    fun aNewSearchStartsAtTheTop() {
        show()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(300) // among the rifles
        compose.onNodeWithText("Randfeuer").assertDoesNotExist()
        search("x") // hundreds of calibres, the first ones rimfire
        compose.onNodeWithText("Randfeuer").assertIsDisplayed()
    }

    /** Kaliberfinder's developer switches are off: no cannon round, no calibre without good sources. */
    @Test
    fun cannonRoundsAndUnsourcedCalibresStayHidden() {
        show()
        val cannon = catalog.all.first { it.isCannon && it.wellSourced }
        val unsourced = catalog.all.first { !it.wellSourced && !it.isCannon }
        for (hidden in listOf(cannon, unsourced)) {
            compose.onNode(hasSetTextAction()).performTextReplacement(hidden.name)
            compose.onAllNodes(hasText(hidden.name) and !hasSetTextAction()).assertCountEquals(0)
        }
    }
}
