package ch.giotta.kaliberkatalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import ch.giotta.kaliberkatalog.catalog.CartridgeCatalog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val catalog = (application as KaliberkatalogApp).catalog
        setContent { App(catalog) }
    }
}

/** The app's screens in its theme. */
@Composable
fun App(catalog: CartridgeCatalog) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        CatalogScreen(catalog)
    }
}
