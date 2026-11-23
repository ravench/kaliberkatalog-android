package ch.giotta.kaliberkatalog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CatalogScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show() = compose.setContent { App((RuntimeEnvironment.getApplication() as KaliberkatalogApp).catalog) }

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
}
