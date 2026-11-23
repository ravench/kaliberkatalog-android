package ch.giotta.kaliberkatalog

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import ch.giotta.kaliberkatalog.catalog.Cartridge
import ch.giotta.kaliberkatalog.catalog.CartridgeCatalog
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Date
import java.util.Locale

/**
 * Where a calibre's values come from (Raven, 2026-11-15): calibre-data's sources, grouped by the parts of the calibre
 * page they back, a book with its pages, a web page as a link. Opened from the calibre page. Ported from Kaliberfinder's
 * SourcesView.swift.
 */
@Composable
fun SourcesScreen(cartridge: Cartridge, works: List<CartridgeCatalog.Work>, onBack: () -> Unit) {
    Scaffold(topBar = { PageBar(stringResource(R.string.sources), onBack) }) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding).verticalScroll(rememberScrollState())) {
            for (group in cartridge.sources) {
                // The group's parts, which cite the same works: «Schulterwinkel · Hülsenboden».
                SectionHeader(group.parts.map { title(sourcePartTitle(it), it) }.joinToString(" · "))
                for (ref in group.refs) works.getOrNull(ref.work)?.let { SourceRow(it, ref.pages.orEmpty()) }
            }
        }
    }
}

/** One work: its title over its detail line; a web page opens in the browser. */
@Composable
private fun SourceRow(work: CartridgeCatalog.Work, pages: List<String>) {
    val detail = sourceDetail(work, pages)
    val url = work.url
    val browser = LocalUriHandler.current
    ListItem(
        headlineContent = { Text(if (work.measured != null) stringResource(R.string.own_measurement) else work.title.orEmpty()) },
        // A phone without a browser has nothing to open it with: the tap does nothing.
        modifier = if (url == null) Modifier else Modifier.clickable { runCatching { browser.openUri(url) } },
        supportingContent = if (detail.isEmpty()) null else ({ Text(detail) }),
        // Read with the row, as Kaliberfinder's hint: «C.I.P., Öffnet die Website».
        trailingContent = if (url == null) null else ({ Icon(painterResource(R.drawable.ic_link), stringResource(R.string.opens_website)) }),
    )
}

/**
 * The line under the title: authors or host, year and pages («W. Todd Woodard · 2026 · S. 573»), or a measurement's
 * day. Empty for a web page without either: its title says it.
 */
@Composable
private fun sourceDetail(work: CartridgeCatalog.Work, pages: List<String>): String {
    val measured = work.measured
    if (measured != null) return day(measured, LocalConfiguration.current.locales[0])
    val page = if (pages.isEmpty()) null else stringResource(R.string.pages, pages.joinToString(", "))
    return listOfNotNull(work.detail, work.year?.toString(), page).joinToString(" · ")
}

/** "2026-11-08" in the phone's numeric date ("8.11.2026" in Switzerland), the same day in every time zone. */
internal fun day(iso: String, locale: Locale): String = try {
    val format = DateFormat.getInstanceForSkeleton("yMd", locale).apply { timeZone = TimeZone.GMT_ZONE }
    format.format(Date(LocalDate.parse(iso).toEpochDay() * 86_400_000))
} catch (e: DateTimeParseException) {
    iso
}

/** What a part of the calibre page is called in a section's title, as Kaliberfinder's Cartridge.SourceGroup.title. */
@StringRes
fun sourcePartTitle(part: String) = when (part) {
    "mm" -> R.string.dimensions
    "shoulder_angle" -> R.string.source_shoulder_angle
    // The curved part in front of the straight one, round or pointed (Raven, 2026-11-20).
    "nose" -> R.string.source_nose
    "flat" -> R.string.source_flat
    "mouth" -> R.string.neck_diameter // a crimped blank's mouth, shown as its neck
    "crimp_length" -> R.string.source_crimp_length
    "case_head" -> R.string.part_base
    "primer" -> R.string.primer
    "bullet" -> R.string.source_bullet
    "case" -> R.string.source_case // the material: the page has no «Hülse» row (Raven, 2026-11-19)
    "origin" -> R.string.origin
    "colour_codes" -> R.string.colour_codes
    else -> null
}
