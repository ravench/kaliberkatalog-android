package ch.giotta.kaliberkatalog

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.giotta.kaliberkatalog.catalog.Cartridge
import ch.giotta.kaliberkatalog.catalog.LengthUnit
import ch.giotta.kaliberkatalog.catalog.LookalikeGroup

/**
 * Dimensions of one calibre, under its drawing and its case seen from below, and the ways to its lookalike [group] and its
 * sources. Ported from Kaliberfinder's CartridgeDetailView.swift.
 */
@Composable
fun CartridgeDetailScreen(
    cartridge: Cartridge,
    group: LookalikeGroup?,
    onBack: () -> Unit,
    onGroup: () -> Unit,
    onSources: () -> Unit,
) {
    val unit = LengthUnit.METRIC // the setting comes with the settings
    val locale = LocalConfiguration.current.locales[0]
    fun length(mm: Double) = unit.format(mm, locale)
    val d = cartridge.dimensions
    Scaffold(topBar = { PageBar(cartridge.name, onBack) }) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding).verticalScroll(rememberScrollState())) {
            val scale = thumbnailScale(cartridge, 90.dp, 160.dp)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Bottom,
            ) {
                // Labelled here, where it stands alone, as Kaliberfinder's drawing; in the list the name is beside it.
                CartridgeThumbnail(cartridge, 90.dp, 160.dp, Modifier.clearAndSetSemantics { contentDescription = cartridge.name })
                // Same scale as the side view, so the head's size reads correctly next to it.
                CaseBaseView(cartridge, scale)
            }
            if (group != null) {
                // The calibre's lookalike group, by its name: the group page lists the members (Raven, PR 188).
                ListItem(
                    headlineContent = { Text(stringResource(R.string.group)) },
                    modifier = Modifier.clickable(onClick = onGroup),
                    supportingContent = { Text(group.name) },
                    leadingContent = { Icon(painterResource(R.drawable.ic_group), null) },
                )
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
            if (cartridge.colourCodes.isNotEmpty()) {
                // The painted tips, bases and primer rings of military loads, under the measurements (Raven, 2026-11-11).
                SectionHeader(stringResource(R.string.colour_codes))
                cartridge.colourCodes.forEach { ColourCodeRow(it) }
                if (explainsTipOrder(cartridge.colourCodes)) Footnote(stringResource(R.string.tip_order))
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Value(R.string.category, categoryTitle(cartridge.category)?.let { stringResource(it) } ?: cartridge.category)
            Value(R.string.kind, stringResource(kindTitle(cartridge.kind)))
            cartridge.country?.let { country ->
                // The flag is not read: it would say the country's name a second time.
                Value(R.string.origin, countryName(country)) { Box(Modifier.clearAndSetSemantics {}) { CountryFlag(country) } }
            }
            if (cartridge.worldwide) Value(R.string.prevalence, stringResource(R.string.worldwide))
            if (cartridge.displayAliases.isNotEmpty()) Value(R.string.also_known_as, cartridge.displayAliases.joinToString(", "))
            if (cartridge.sources.isNotEmpty()) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.sources)) },
                    modifier = Modifier.clickable(onClick = onSources),
                    leadingContent = { Icon(painterResource(R.drawable.ic_sources), null) },
                )
            }
            Footnote(stringResource(R.string.rounded_values))
        }
    }
}

/** A note under a section, as iOS's section footer. */
@Composable
internal fun Footnote(text: String) = Text(
    text,
    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    style = MaterialTheme.typography.bodySmall,
)

/**
 * A label and its value, read together, with [after] behind the value (the origin's flag). As iOS's LabeledContent, the
 * value stands beside the label and wraps; only where not even a word of it fits there, it goes below. (A ListItem's
 * trailing text would squeeze the label instead.)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Value(@StringRes label: Int, value: String, after: @Composable () -> Unit = {}) {
    FlowRow(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                value,
                Modifier.weight(1f, fill = false),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyLarge,
            )
            after()
        }
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
