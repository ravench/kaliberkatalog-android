package ch.giotta.kaliberkatalog

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import ch.giotta.kaliberkatalog.catalog.LengthUnit

/** The settings, kept in [prefs]. Compose state: the screens that show one change with it. */
class AppSettings(private val prefs: SharedPreferences) {
    private var unitState by mutableStateOf(LengthUnit.entries.find { it.name == prefs.getString(UNIT, null) } ?: LengthUnit.METRIC)

    /** The unit of the lengths shown, millimetres until one is chosen (Kaliberfinder's LengthUnit.storageKey). */
    var unit: LengthUnit
        get() = unitState
        set(value) {
            unitState = value
            prefs.edit().putString(UNIT, value.name).apply()
        }

    private companion object {
        const val UNIT = "lengthUnit"
    }
}

/** The app's settings, [KaliberkatalogApp.settings]. */
val appSettings: AppSettings
    @Composable get() = (LocalContext.current.applicationContext as KaliberkatalogApp).settings
