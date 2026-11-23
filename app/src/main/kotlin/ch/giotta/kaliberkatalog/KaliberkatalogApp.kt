package ch.giotta.kaliberkatalog

import android.app.Application
import ch.giotta.kaliberkatalog.catalog.CartridgeCatalog

/** The app process: holds the catalogue it ships, decoded once, and the settings. */
class KaliberkatalogApp : Application() {
    // shortcut: decoded on first use, on the main thread (1.9 MB, about 0.1 s); move it off if the cold start gets slow.
    val catalog by lazy {
        checkNotNull(CartridgeCatalog.decode(assets.open("Cartridges.json").reader().use { it.readText() }))
    }

    val settings by lazy { AppSettings(getSharedPreferences("settings", MODE_PRIVATE)) }
}
