package ch.giotta.kaliberkatalog

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import ch.giotta.kaliberkatalog.catalog.LengthUnit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

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

    /** Developer switch: also the calibres calibre-data does not source well (Kaliberfinder's showUnsourcedKey). */
    var showUnsourced by Flag("showUnsourcedCalibres")

    /** Developer switch: also the cannon rounds (Kaliberfinder's showCannonKey). */
    var showCannon by Flag("showCannonCalibres")

    /** Developer switch: the drawings in the materials of the first issue, not today's (Kaliberfinder's showOriginalKey). */
    var showOriginal by Flag("showOriginalMaterials")

    /** A switch kept under [key], off until turned on. */
    private inner class Flag(private val key: String) : ReadWriteProperty<Any?, Boolean> {
        private var state by mutableStateOf(prefs.getBoolean(key, false))

        override fun getValue(thisRef: Any?, property: KProperty<*>) = state

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            state = value
            prefs.edit().putBoolean(key, value).apply()
        }
    }

    private companion object {
        const val UNIT = "lengthUnit"
    }
}

/** The app's settings, [KaliberkatalogApp.settings]. */
val appSettings: AppSettings
    @Composable get() = (LocalContext.current.applicationContext as KaliberkatalogApp).settings
