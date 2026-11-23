package ch.giotta.kaliberkatalog

import androidx.compose.ui.graphics.Color
import ch.giotta.kaliberkatalog.CartridgeDrawing.brass
import ch.giotta.kaliberkatalog.CartridgeDrawing.bulletFill
import ch.giotta.kaliberkatalog.CartridgeDrawing.caseFill
import ch.giotta.kaliberkatalog.CartridgeDrawing.copper
import ch.giotta.kaliberkatalog.CartridgeDrawing.headFill
import ch.giotta.kaliberkatalog.CartridgeDrawing.lacqueredSteel
import ch.giotta.kaliberkatalog.CartridgeDrawing.lead
import ch.giotta.kaliberkatalog.CartridgeDrawing.nickel
import ch.giotta.kaliberkatalog.CartridgeDrawing.paper
import ch.giotta.kaliberkatalog.CartridgeDrawing.plastic
import ch.giotta.kaliberkatalog.CartridgeDrawing.steel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ported from Kaliberfinder's BulletMaterialTests.swift: each calibre is drawn with its bullet and case as most
 * commonly found today, or as first issued ([original], a developer switch to come).
 */
class CartridgeDrawingTest {
    @Test
    fun leadBulletsAreDrawnInLead() {
        for (id in listOf(".22 LR", ".38 Special", ".45-70 Government")) assertEquals(id, lead, bulletFill(cartridge(id)))
        assertEquals(copper, bulletFill(cartridge("9x19 mm")))
        assertEquals(steel, bulletFill(cartridge("7.5x53.5 mm Swiss (Patrone Modell 1890)")))
    }

    /** A jacket that is not copper-coloured is drawn in its metal: cupronickel on early service rounds, steel on the Argentine. */
    @Test
    fun silverAndSteelJackets() {
        for (id in listOf("7.5x55 mm Swiss (GP 11)", "8x57 mm IS", "6 mm Lee Navy")) assertEquals(id, nickel, bulletFill(cartridge(id), original = true))
        assertEquals(steel, bulletFill(cartridge("7.65x53 mm Argentine"), original = true))
        // No longer made: the original jacket is what is found today too.
        assertEquals(nickel, bulletFill(cartridge("6 mm Lee Navy")))
    }

    @Test
    fun todaysColoursUnlessTheOriginalIsAskedFor() {
        for (id in listOf("7.5x55 mm Swiss (GP 11)", "8x57 mm IS", "7.65x53 mm Argentine")) assertEquals(id, copper, bulletFill(cartridge(id)))
        val r54 = cartridge("7.62x54 mm R")
        assertEquals(lacqueredSteel, caseFill(r54))
        assertEquals(brass, caseFill(r54, original = true))
        assertEquals(lacqueredSteel, headFill(r54))
        val kurz = cartridge("7.92x33 mm Kurz")
        assertEquals(brass, caseFill(kurz))
        assertEquals(lacqueredSteel, caseFill(kurz, original = true))
        val govt = cartridge(".45-70 Government")
        assertEquals(brass, caseFill(govt))
        assertEquals(copper, caseFill(govt, original = true))
        assertEquals(copper, bulletFill(cartridge("9x19 mm"), original = true))
    }

    @Test
    fun polymerTipAndHybridHead() {
        val hmr = cartridge(".17 HMR")
        assertNotNull(hmr.outline.tip)
        assertEquals("polymer", hmr.outline.tipMaterial)
        assertNull("a soft point's tip is lead", cartridge("9.3x62 mm").outline.tipMaterial)
        assertEquals("the jacket stays copper", copper, bulletFill(cartridge("9.3x62 mm")))
        assertNull("military ball is a full metal jacket", cartridge("7.62x51 mm NATO").outline.tip)
        val fury = cartridge(".277 SIG Fury")
        assertEquals("steel", fury.outline.headMaterial)
        assertNotNull(fury.outline.head)
    }

    @Test
    fun caseMaterials() {
        assertEquals(copper, caseFill(cartridge(".44 Henry"), original = true))
        assertEquals(lacqueredSteel, caseFill(cartridge("5.45x39 mm"), original = true))
        assertEquals(nickel, caseFill(cartridge("7 mm Backcountry"), original = true))
        assertEquals(paper, caseFill(cartridge(".577 Snider"), original = true))
        assertEquals(brass, caseFill(cartridge("9x19 mm"), original = true))
    }

    /** The head has its own colour where it is a part of its own; otherwise the case's colour reaches the rim. */
    @Test
    fun headColours() {
        assertEquals(lacqueredSteel, headFill(cartridge("5.45x39 mm"), original = true))
        assertEquals(copper, headFill(cartridge(".44 Henry"), original = true))
        assertEquals(nickel, headFill(cartridge("7 mm Backcountry"), original = true))
        assertEquals(brass, headFill(cartridge("9x19 mm"), original = true))
        assertEquals("brass cup, paper body", brass, headFill(cartridge(".577 Snider"), original = true))
        assertEquals("steel head, brass body", steel, headFill(cartridge(".277 SIG Fury"), original = true))
        assertEquals("brass head, plastic hull", brass, headFill(cartridge("12/70"), original = true))
        for (c in live.all.filter { it.hasCase && it.outline.head == null }) {
            for (original in listOf(false, true)) assertEquals(c.name, caseFill(c, original), headFill(c, original))
        }
    }

    @Test
    fun steelBBIsSteelAndAirsoftBBsArePlastic() {
        val steelBB = cartridge("4.4 mm Stahl-BB (.177 BB)")
        assertEquals("ball", steelBB.kind)
        assertEquals(steel, bulletFill(steelBB))
        for (id in listOf("6 mm Airsoft-BB", "8 mm Airsoft-BB")) assertEquals(id, plastic, bulletFill(cartridge(id)))
        assertEquals(lead, bulletFill(cartridge(".44 Rundkugel (.454)")))
    }

    /**
     * A hollow point's cavity reads at the size of a calibre page: it is shaded, and its line is bolder than the
     * outline, with several dashes. CartridgeViewTest checks that the line leaves the mouth open.
     */
    @Test
    fun hollowPointCavityIsBold() {
        assertNotNull(cartridge(".357 SIG").outline.cavity)
        for (dpPerMm in listOf(1.5f, 3f, 7f, 12f)) {
            val (width, dash, gap) = CartridgeDrawing.cavityStroke(dpPerMm)
            assertTrue("$dpPerMm", width > CartridgeDrawing.lineWidth(dpPerMm))
            // On a calibre page (7 dp per mm and up), a 2 mm side shows at least three dashes.
            if (dpPerMm >= 7) assertTrue("$dpPerMm", 2 * dpPerMm / (dash + gap) >= 3)
        }
        assertEquals(Color.Black.copy(alpha = 0.35f), CartridgeDrawing.cavityShade)
    }
}
