package ch.giotta.kaliberkatalog

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * The switches meant for the developer (Raven, 2026-11-14), behind the last row of the settings so they stay out of the
 * users' way. Ported from Kaliberfinder's DeveloperOptionsView, without the training capture, which this app has not.
 */
@Composable
fun DeveloperOptionsScreen(onBack: () -> Unit) {
    val settings = appSettings
    Scaffold(topBar = { PageBar(stringResource(R.string.developer_options), onBack) }) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding).verticalScroll(rememberScrollState())) {
            // The tags' icons for the calibres they show (Raven, 2026-11-19: the cannon's shell).
            val icon = Modifier.size(24.dp)
            Toggle(R.string.show_unsourced, R.string.show_unsourced_note, settings.showUnsourced, { settings.showUnsourced = it }) {
                TagIcon(CalibreTag.UNSOURCED, icon)
            }
            Toggle(R.string.show_cannon, R.string.show_cannon_note, settings.showCannon, { settings.showCannon = it }) {
                TagIcon(CalibreTag.CANNON, icon)
            }
            Toggle(R.string.show_original, R.string.show_original_note, settings.showOriginal, { settings.showOriginal = it }) {
                Icon(painterResource(R.drawable.ic_history), null)
            }
        }
    }
}

/** A switch with its [icon], which the whole row turns, and the note under it, as a section of Kaliberfinder's list. */
@Composable
private fun Toggle(@StringRes title: Int, @StringRes note: Int, on: Boolean, onChange: (Boolean) -> Unit, icon: @Composable () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        modifier = Modifier.toggleable(on, role = Role.Switch, onValueChange = onChange),
        leadingContent = icon,
        trailingContent = { Switch(on, onCheckedChange = null) },
    )
    Footnote(stringResource(note))
}
