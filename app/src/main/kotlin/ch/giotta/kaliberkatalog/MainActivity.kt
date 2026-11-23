package ch.giotta.kaliberkatalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import ch.giotta.kaliberkatalog.catalog.CartridgeCatalog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val catalog = (application as KaliberkatalogApp).catalog
        setContent { App(catalog) }
    }
}

/** The app's screens in its theme: the list, or the calibre page a row opened. */
@Composable
fun App(catalog: CartridgeCatalog) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        var open by rememberSaveable { mutableStateOf<String?>(null) } // the open calibre's id
        val cartridge = open?.let(catalog::cartridge)
        // Keeps the list's search and place while a page is open.
        val saved = rememberSaveableStateHolder()
        if (cartridge == null) {
            saved.SaveableStateProvider("list") { CatalogScreen(catalog, onOpen = { open = it.id }) }
        } else {
            BackHandler { open = null }
            CartridgeDetailScreen(cartridge, onBack = { open = null })
        }
    }
}
