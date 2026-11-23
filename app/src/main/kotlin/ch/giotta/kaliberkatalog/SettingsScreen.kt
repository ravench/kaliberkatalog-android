package ch.giotta.kaliberkatalog

import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.giotta.kaliberkatalog.catalog.CartridgeCatalog
import ch.giotta.kaliberkatalog.catalog.LengthUnit

/**
 * The settings, opened by the list's gear: the unit, then the app's version, its calibre data and the licence lines. Ported
 * from Kaliberfinder's SettingsView, without the 1:1 comparison and the photo, which this app has not.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(catalog: CartridgeCatalog, onBack: () -> Unit) {
    val settings = appSettings
    val locale = LocalConfiguration.current.locales[0]
    Scaffold(topBar = { PageBar(stringResource(R.string.settings), onBack) }) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding).verticalScroll(rememberScrollState())) {
            SectionHeader(stringResource(R.string.units))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                for ((i, unit) in LengthUnit.entries.withIndex()) {
                    SegmentedButton(
                        selected = settings.unit == unit,
                        onClick = { settings.unit = unit },
                        shape = SegmentedButtonDefaults.itemShape(i, LengthUnit.entries.size),
                    ) { Text(stringResource(unitTitle(unit))) }
                }
            }
            SectionHeader(stringResource(R.string.about))
            val context = LocalContext.current
            Value(R.string.version, remember(context) { versionText(context) }) // asked once, not at each unit tap
            catalog.file.calibreData?.let { Value(R.string.calibre_data, "${it.commit.take(7)} · ${day(it.date.take(10), locale)}") }
            Footnote("© 2026 Simon Giotta") // the same in every language
            Footnote(stringResource(R.string.odbl_notice))
        }
    }
}

/** "1.0 (1)": the app's version and build, as Kaliberfinder's Bundle.versionText. */
private fun versionText(context: Context): String {
    @Suppress("DEPRECATION") // the overload with flags is API 33
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    @Suppress("DEPRECATION") // longVersionCode is API 28
    val build = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    return "${info.versionName} ($build)"
}

/** A unit's name, as Kaliberfinder's LengthUnit.title. */
@StringRes
fun unitTitle(unit: LengthUnit) = when (unit) {
    LengthUnit.METRIC -> R.string.unit_metric
    LengthUnit.IMPERIAL -> R.string.unit_imperial
}
