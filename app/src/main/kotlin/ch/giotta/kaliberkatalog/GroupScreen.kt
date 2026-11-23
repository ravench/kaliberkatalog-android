package ch.giotta.kaliberkatalog

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.giotta.kaliberkatalog.catalog.Cartridge
import ch.giotta.kaliberkatalog.catalog.CartridgeCatalog
import ch.giotta.kaliberkatalog.catalog.LookalikeGroup

/**
 * The calibres of a lookalike group, each opening its page, and what tells them apart. Ported from Kaliberfinder's
 * LookalikeGroupView.swift, without its 1:1 comparison.
 */
@Composable
fun GroupScreen(group: LookalikeGroup, members: List<Cartridge>, onBack: () -> Unit, onOpen: (Cartridge) -> Unit) {
    Scaffold(topBar = { PageBar(stringResource(R.string.group_title, group.name), onBack) }) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding).verticalScroll(rememberScrollState())) {
            members.forEach { CartridgeRow(it, onOpen) }
            Footnote(stringResource(R.string.differ_in, group.differs.map { title(differenceTitle(it), it) }.joinToString(", ")))
        }
    }
}

/** The members the list shows, in the group's order, the parent first. */
fun shownMembers(catalog: CartridgeCatalog, group: LookalikeGroup) =
    CartridgeCatalog.visible(catalog.members(group), showUnsourced = false, showCannon = false) // the switches come with the settings

/**
 * The group a calibre's page links: only one with more to list than that calibre. Some calibres' only siblings are
 * unsourced and hidden, and the page of one would name a calibre nobody finds (Kaliberfinder's LookalikeGroupView.lists).
 */
fun linkedGroup(catalog: CartridgeCatalog, cartridge: Cartridge) =
    catalog.lookalikeGroup(cartridge)?.takeIf { shownMembers(catalog, it).size > 1 }

/** What tells a group's calibres apart, as Kaliberfinder's LookalikeGroup.title: lower-case mid-line, except in German. */
@StringRes
fun differenceTitle(difference: String) = when (difference) {
    "rim" -> R.string.differs_rim
    "bore" -> R.string.differs_bore
    "length" -> R.string.differs_length
    "seating" -> R.string.differs_seating
    "load" -> R.string.differs_load
    "ignition" -> R.string.differs_ignition
    else -> null
}
