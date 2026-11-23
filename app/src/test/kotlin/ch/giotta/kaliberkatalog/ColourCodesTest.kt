package ch.giotta.kaliberkatalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The colour codes on the calibre page, as Kaliberfinder's ColourCodeRow and ColourSwatch (its CatalogTests). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "de-rCH-xhdpi")
class ColourCodesTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(id: String) = compose.setContent { CartridgeDetailScreen(cartridge(id), group = null, onBack = {}, onGroup = {}, onSources = {}) }

    /** The row of a code, read together: what it means, then where it is and the loads. */
    private fun code(meaning: String, where: String) = compose.onNode(hasText(meaning) and hasText(where)).assertExists()

    /** Kaliberfinder's testColourCodesComeFromCalibreData: every part, meaning and colour used has a title and a colour. */
    @Test
    fun everyPartMeaningAndColourHasItsTitle() {
        val codes = live.file.cartridges.flatMap { it.colourCodes }
        assertTrue(codes.size > 30)
        for (part in codes.map { it.part }.toSet()) assertNotNull(part, partTitle(part))
        for (meaning in codes.flatMap { it.meaning }.toSet()) assertNotNull(meaning, meaningTitle(meaning))
        for (colour in codes.flatMap { it.colours }.toSet()) {
            assertNotNull(colour, colourTitle(colour))
            assertNotNull(colour, swatchColours[colour])
        }
    }

    /** The GP 11: the red tracer tip, then the older red (tracer) and purple (armour-piercing) bases, all Swiss. */
    @Test
    fun theGp11sTipAndBases() {
        show("7.5x55 mm Swiss (GP 11)")
        compose.onNodeWithText("Farbcodes").assertExists()
        code("Leuchtspur", "Geschossspitze · Leuchtspurpatrone M.1911")
            .assert(hasContentDescription("Geschossspitze: Rot"))
            .assert(hasContentDescription("Schweiz"))
        code("Leuchtspur", "Hülsenboden · Leuchtspur, ältere Kennzeichnung: ganzer Boden lackiert")
            .assert(hasContentDescription("Hülsenboden: Rot"))
        code("Panzerbrechend", "Hülsenboden · Panzerbrechend, nicht mehr im Dienst (1980)")
            .assert(hasContentDescription("Hülsenboden: Violett"))
        compose.onNodeWithText("Spitze: Farben von der Spitze abwärts.").assertExists() // with a tip, the bands' order
        assertTrue(explainsTipOrder(cartridge("7.5x55 mm Swiss (GP 11)").colourCodes))
    }

    /** Primer rings only, here the former Czechoslovak codes under their drawn flag: no note on a tip's bands. */
    @Test
    fun primerRingsNeedNoNoteOnTheBands() {
        show("8x57 mm IS")
        code("Standardgeschoss", "Zündhütchenring · Leichtes Standardgeschoss, früherer tschechoslowakischer Code (vor den sowjetischen Codes)")
            .assert(hasContentDescription("Zündhütchenring: Schwarz"))
            .assert(hasContentDescription("Tschechoslowakei"))
        compose.onNodeWithText("Spitze: Farben von der Spitze abwärts.").assertDoesNotExist()
        assertFalse(explainsTipOrder(cartridge("8x57 mm IS").colourCodes))
    }

    @Test
    fun mostCalibresHaveNone() {
        show("9x19 mm")
        compose.onNodeWithText("Farbcodes").assertDoesNotExist()
    }

    /** The loads are described in the app's language, as calibre-data translates them. */
    @Test
    @Config(qualifiers = "fr")
    fun theLoadsAreInTheAppsLanguage() {
        show("7.5x55 mm Swiss (GP 11)")
        code("Traçant", "Culot de la douille · Traçant, ancien marquage : tout le culot laqué")
    }

    /** A phone language the app lacks gets the app's English, and the loads in English too (Kaliberfinder's "ja" case). */
    @Test
    @Config(qualifiers = "nl")
    fun aLanguageTheAppLacksGetsEnglishLoads() {
        show("7.5x55 mm Swiss (GP 11)")
        code("Tracer", "Case base · Tracer, the older marking: the whole base lacquered")
    }

    /** The colour [x] and [y] px from the swatch's top left corner. */
    private fun ImageBitmap.at(x: Int, y: Int) = toPixelMap()[x, y].toArgb()

    /** At 2 px per dp a swatch is 48 × 60 px: a 30 px wide capsule, a disc of 24 px radius, a ring from 11 to 18 px. */
    @Test
    fun theSwatches() {
        compose.setContent {
            Column {
                Box(Modifier.testTag("tip")) { ColourSwatch("tip", listOf("red", "silver")) }
                Box(Modifier.testTag("base")) { ColourSwatch("base", listOf("purple")) }
                Box(Modifier.testTag("annulus")) { ColourSwatch("annulus", listOf("green")) }
            }
        }
        val (tip, base, annulus) = listOf("tip", "base", "annulus").map { compose.onNodeWithTag(it).captureToImage() }
        assertEquals(48, tip.width)
        assertEquals(60, tip.height)
        assertEquals("the tip's band", swatchColours.getValue("red").toArgb(), tip.at(24, 12))
        assertEquals("the band below", swatchColours.getValue("silver").toArgb(), tip.at(24, 48))
        assertEquals("the base", swatchColours.getValue("purple").toArgb(), base.at(24, 30))
        assertEquals("the brass primer", CartridgeDrawing.brass.toArgb(), annulus.at(24, 30))
        assertEquals("the ring", swatchColours.getValue("green").toArgb(), annulus.at(24 + 14, 30))
        assertTrue("no paint beside the capsule", tip.at(2, 30) != swatchColours.getValue("red").toArgb())
    }
}
