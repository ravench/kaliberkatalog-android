package ch.giotta.kaliberkatalog.catalog

import java.io.File
import kotlin.test.assertNotNull

/** The catalogue the app ships (tools/generate_data.py); Gradle runs the tests in catalog/. */
val live = assertNotNull(CartridgeCatalog.decode(File("../app/src/main/assets/Cartridges.json").readText()))

/**
 * Calibres frozen from calibre-data (catalogue.json, written by tools/catalogue_fixture.py, the same file as
 * Kaliberfinder's): the search and switch tests run on them, so a data bump changes no test. Rewrite the file on
 * purpose only, when a test needs a calibre or a value that moved.
 */
object Fixture {
    val catalogue = assertNotNull(CartridgeCatalog.decode(Fixture::class.java.getResource("/catalogue.json")!!.readText())).file.cartridges

    fun cartridge(id: String) = assertNotNull(catalogue.firstOrNull { it.id == id }, "$id is not in catalogue.json")
}
