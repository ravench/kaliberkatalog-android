package ch.giotta.kaliberkatalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import ch.giotta.kaliberkatalog.catalog.CartridgeCatalog
import java.io.Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val catalog = (application as KaliberkatalogApp).catalog
        setContent { App(catalog) }
    }
}

/** A page opened over the list, of the calibre [id]. Serializable, so the open pages outlive a restore. */
sealed interface Page : Serializable {
    val id: String

    /** The calibre's page. */
    data class Calibre(override val id: String) : Page

    /** Where the calibre's values come from. */
    data class Sources(override val id: String) : Page
}

/** The app's screens in its theme: the list, and the pages opened from it, each over the one it was opened from. */
@Composable
fun App(catalog: CartridgeCatalog) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        var pages by rememberSaveable { mutableStateOf(emptyList<Page>()) } // the last one is shown
        // Keeps each screen's state, as the list's search and place, while a page is open over it; by its depth.
        val saved = rememberSaveableStateHolder()
        val depth = pages.size // as shown: two taps before the next frame, as with two fingers, open one page, not two
        val open = { page: Page -> pages = pages.take(depth) + page }
        val back = {
            saved.removeState(depth)
            pages = pages.take(depth - 1)
        }
        val page = pages.lastOrNull()
        val cartridge = page?.let { catalog.cartridge(it.id) }
        // No calibre after an update that dropped it: the list shows, and back leaves the app from there.
        BackHandler(enabled = cartridge != null, onBack = back)
        saved.SaveableStateProvider(if (cartridge == null) 0 else depth) {
            when {
                cartridge == null -> CatalogScreen(catalog, onOpen = { open(Page.Calibre(it.id)) })
                page is Page.Sources -> SourcesScreen(cartridge, catalog.file.sources, onBack = back)
                else -> CartridgeDetailScreen(cartridge, onBack = back, onSources = { open(Page.Sources(cartridge.id)) })
            }
        }
    }
}

/** The bar of a page over the list: its title and the way back. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageBar(title: String, onBack: () -> Unit) = TopAppBar(
    title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
    },
)
