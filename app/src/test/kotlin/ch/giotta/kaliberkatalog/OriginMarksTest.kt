package ch.giotta.kaliberkatalog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import ch.giotta.kaliberkatalog.catalog.Cartridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The globe and the flags of the list rows (Kaliberfinder's CalibreTagTests), and the flags the app draws. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "de-rCH-xhdpi")
class OriginMarksTest {
    @get:Rule
    val compose = createComposeRule()

    /** What the row of [name] reads after its name and other names: its marks, in order. */
    private fun marks(name: String): List<String>? {
        compose.onNode(hasSetTextAction()).performTextReplacement(name)
        val row = compose.onNode(hasText(name) and hasClickAction() and !hasSetTextAction()).fetchSemanticsNode()
        return row.config.getOrNull(SemanticsProperties.ContentDescription)
    }

    /** Kaliberfinder's testRowMarksAreTheGlobeThenEachCountryOnce, for the one calibre of a catalogue row. */
    @Test
    fun aRowShowsTheGlobeThenTheFlag() {
        compose.setContent { App((RuntimeEnvironment.getApplication() as KaliberkatalogApp).catalog) }
        assertEquals(listOf("Weltweit verbreitet", "Deutschland"), marks("9x19 mm")) // German and worldwide
        assertEquals(listOf("Weltweit verbreitet"), marks("12/70")) // a gauge: worldwide without a country
        assertEquals(listOf("Sowjetunion"), marks("7.62x39 mm")) // a drawn flag
        assertNull(marks(".36 Rundkugel (.375)"))
    }

    /** Kaliberfinder's testEveryCountryHasAFlagToShow, and each has its name in the app's languages. */
    @Test
    fun everyCountryOfTheCatalogueHasANameAndAFlagToShow() {
        val countries = live.file.cartridges.flatMap { c -> listOfNotNull(c.country) + c.colourCodes.flatMap { it.countries } }
        for (country in countries.toSet()) {
            assertNotNull(country.code, countryTitle(country.code))
            assertTrue("${country.code} has neither an emoji nor a drawn flag", country.flag != null || country.code in drawnFlagCodes)
        }
    }

    /** A country newer than the app is named as the data names it, in German, under a grey flag. */
    @Test
    fun aCountryNewerThanTheAppKeepsTheDataName() {
        assertNull(countryTitle("XX"))
        compose.setContent { CountryFlag(Cartridge.Country("XX", "Neuland")) }
        compose.onNode(hasContentDescription("Neuland")).assertExists()
    }

    /** Each code's flag, 60 × 40 dp; one composition, as a test can set its content once. */
    private fun flags(vararg codes: String): Map<String, ImageBitmap> {
        compose.setContent { Column { for (code in codes) DrawnFlag(code, Modifier.size(60.dp, 40.dp).testTag(code)) } }
        return codes.associateWith { compose.onNodeWithTag(it).captureToImage() }
    }

    /** The colour at [x] and [y], in flag heights from the top left corner. */
    private fun ImageBitmap.at(x: Float, y: Float) = toPixelMap()[(x * height).toInt(), (y * height).toInt()]

    private fun assertColour(expected: Color, actual: Color, where: String) = assertEquals(where, expected.toArgb(), actual.toArgb())

    @Test
    fun theDrawnFlags() {
        val flag = flags("RUE", "AH", "SU", "CS", "OE", "NATO", "XX")
        val gold = Color(0.95f, 0.76f, 0f)
        with(flag.getValue("RUE")) {
            assertColour(Color.Black, at(0.75f, 0.15f), "RUE top")
            assertColour(gold, at(0.75f, 0.5f), "RUE middle")
            assertColour(Color.White, at(0.75f, 0.85f), "RUE bottom")
        }
        with(flag.getValue("AH")) {
            assertColour(Color.Black, at(0.75f, 0.25f), "AH top")
            assertColour(gold, at(0.75f, 0.75f), "AH bottom")
        }
        with(flag.getValue("SU")) {
            assertColour(Color(1f, 0.84f, 0f), at(0.325f, 0.275f), "SU star")
            assertColour(Color(0.8f, 0f, 0f), at(1.3f, 0.8f), "SU field")
        }
        with(flag.getValue("CS")) {
            assertColour(Color(0.07f, 0.27f, 0.49f), at(0.1f, 0.5f), "CS triangle")
            assertColour(Color.White, at(1.3f, 0.25f), "CS top")
            assertColour(Color(0.84f, 0.08f, 0.1f), at(1.3f, 0.75f), "CS bottom")
        }
        with(flag.getValue("OE")) { // the crescent's centre is 0.42 widths (0.63 heights) from the left
            val red = Color(0.89f, 0.04f, 0.09f)
            assertColour(Color.White, at(0.63f - 0.2f, 0.5f), "OE crescent")
            assertColour(red, at(0.63f, 0.5f), "OE inside the crescent")
            assertColour(Color.White, at(0.63f + 0.3f, 0.5f), "OE star")
            assertColour(red, at(1.4f, 0.1f), "OE field")
        }
        with(flag.getValue("NATO")) { // the centre is 0.75 heights from the left
            val blue = Color(0f, 0.29f, 0.56f)
            assertColour(Color.White, at(0.75f, 0.5f), "NATO rose")
            assertColour(Color.White, at(0.75f + 0.39f, 0.5f), "NATO ring")
            assertColour(blue, at(0.75f + 0.18f, 0.5f + 0.18f), "NATO between the rose's points")
            assertColour(blue, at(0.05f, 0.05f), "NATO field")
        }
        assertColour(Color.Gray, flag.getValue("XX").at(0.75f, 0.5f), "a code without a drawing")
    }
}
