package ch.giotta.kaliberkatalog

import ch.giotta.kaliberkatalog.catalog.CartridgeCatalog
import java.io.File

/** The catalogue the app ships, for tests without Robolectric (Gradle runs them in app/). */
val live = checkNotNull(CartridgeCatalog.decode(File("src/main/assets/Cartridges.json").readText()))

fun cartridge(id: String) = checkNotNull(live.cartridge(id)) { "$id is not in the catalogue" }
