package ch.giotta.kaliberkatalog

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.giotta.kaliberkatalog.catalog.Cartridge
import ch.giotta.kaliberkatalog.catalog.LengthUnit

/** Dimensions of one calibre, under its drawing and its case seen from below. Ported from Kaliberfinder's CartridgeDetailView.swift. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartridgeDetailScreen(cartridge: Cartridge, onBack: () -> Unit) {
    val unit = LengthUnit.METRIC // the setting comes with the settings
    val locale = LocalConfiguration.current.locales[0]
    fun length(mm: Double) = unit.format(mm, locale)
    val d = cartridge.dimensions
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cartridge.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding).verticalScroll(rememberScrollState())) {
            val scale = thumbnailScale(cartridge, 90.dp, 160.dp)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Bottom,
            ) {
                CartridgeThumbnail(cartridge, 90.dp, 160.dp)
                // Same scale as the side view, so the head's size reads correctly next to it.
                CaseBaseView(cartridge, scale)
            }
            SectionHeader(stringResource(R.string.dimensions))
            // A partial entry's measurement no source gives is 0: no row.
            if (d.overallLength > 0) Value(R.string.overall_length, length(d.overallLength))
            if (cartridge.hasCase) {
                Value(if (cartridge.kind == "shot") R.string.shell_length_open else R.string.case_length, length(d.caseLength))
                Value(R.string.rim_diameter, length(d.rim))
                d.belt?.let { Value(R.string.belt_diameter, length(it)) }
                if (d.base > 0) Value(R.string.base_diameter, length(d.base))
                d.shoulder?.let { Value(R.string.shoulder_diameter, length(it)) }
                if (cartridge.kind != "shot" && d.neck > 0) Value(R.string.neck_diameter, length(d.neck))
            }
            if (cartridge.kind != "shot" && !cartridge.blank) Value(R.string.bullet_diameter, length(d.bullet))
            val primer = cartridge.primer
            if (primer != null) {
                val title = primerTitle(primer.type)?.let { stringResource(it) } ?: primer.type
                Value(R.string.primer, "$title, ${length(primer.diameter)}")
            } else if (cartridge.kind == "rimfire") {
                Value(R.string.primer, stringResource(R.string.primer_rimfire))
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Value(R.string.category, categoryTitle(cartridge.category)?.let { stringResource(it) } ?: cartridge.category)
            Value(R.string.kind, stringResource(kindTitle(cartridge.kind)))
            if (cartridge.worldwide) Value(R.string.prevalence, stringResource(R.string.worldwide))
            if (cartridge.displayAliases.isNotEmpty()) Value(R.string.also_known_as, cartridge.displayAliases.joinToString(", "))
            Text(
                stringResource(R.string.rounded_values),
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * A label and its value, read together. As iOS's LabeledContent, the value stands beside the label and wraps; only where
 * not even a word of it fits there, it goes below. (A ListItem's trailing text would squeeze the label instead.)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Value(@StringRes label: Int, value: String) {
    FlowRow(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** The title of a catalogue kind, as Kaliberfinder's CartridgeDetailView.kindTitle. */
@StringRes
fun kindTitle(kind: String) = when (kind) {
    "rimless" -> R.string.kind_rimless
    "rimmed" -> R.string.kind_rimmed
    "semi-rimmed" -> R.string.kind_semi_rimmed
    "belted" -> R.string.kind_belted
    "rimfire" -> R.string.kind_rimfire
    "shot" -> R.string.kind_shot
    "ball" -> R.string.kind_ball
    "minie" -> R.string.kind_minie
    "pellet" -> R.string.kind_pellet
    else -> R.string.kind_other
}
