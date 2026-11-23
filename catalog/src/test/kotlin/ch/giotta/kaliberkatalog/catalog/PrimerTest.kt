package ch.giotta.kaliberkatalog.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Ported from Kaliberfinder's PrimerTests.swift; the titles' part is in the app's CartridgeDetailScreenTest. */
class PrimerTest {
    private fun cartridge(id: String) = assertNotNull(live.cartridge(id), id)

    @Test
    fun rimfireAndCaselessHaveNoPrimer() {
        for (id in listOf(".22 LR", ".17 HMR", ".44 Rundkugel (.454)", "4.5 mm Diabolo (.177)")) assertNull(cartridge(id).primer, id)
    }

    @Test
    fun everyCentrefireCaseHasAPrimerThatFits() {
        // calibre-data has no primer sizes for the cannon rounds yet (generate_data.primer excepts them).
        for (c in live.all.filter { it.hasCase && it.kind != "rimfire" && !it.isCannon }) {
            val primer = assertNotNull(c.primer, "${c.name} has no primer")
            assertTrue(primer.diameter < c.dimensions.base * 0.75, c.name)
        }
    }

    @Test
    fun headDiameter() {
        assertEquals(10.01, cartridge("9x19 mm").headDiameter) // the SAAMI rim
        val special = cartridge(".38 Special")
        assertEquals(special.dimensions.rim, special.headDiameter)
    }
}
