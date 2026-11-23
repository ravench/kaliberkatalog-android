package ch.giotta.kaliberkatalog

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.giotta.kaliberkatalog.catalog.Cartridge
import ch.giotta.kaliberkatalog.catalog.CartridgeCatalog

/** All catalogue calibres by category, with search. Ported from Kaliberfinder's CatalogView.swift. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CatalogScreen(catalog: CartridgeCatalog) {
    var query by rememberSaveable { mutableStateOf("") }
    val visible = remember(catalog) { CartridgeCatalog.visible(catalog.all, showUnsourced = false, showCannon = false) }
    val results = remember(visible, query) { CartridgeCatalog.search(query, visible) }
    val focus = LocalFocusManager.current
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.calibres)) }) }) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_prompt)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, stringResource(R.string.clear_search)) }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            )
            if (results.isEmpty()) {
                NoResults(query)
            } else {
                LazyColumn {
                    for ((category, cartridges) in CartridgeCatalog.grouped(results)) {
                        stickyHeader { CategoryHeader(category) }
                        items(cartridges, key = { it.id }) { CartridgeRow(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(category: String) {
    Text(
        category,
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
    )
}

/** The calibre's name and its other names. */
@Composable
private fun CartridgeRow(cartridge: Cartridge) {
    val aliases = cartridge.displayAliases.joinToString(", ")
    ListItem(
        headlineContent = { Text(cartridge.name) },
        supportingContent = if (aliases.isEmpty()) null else ({ Text(aliases) }),
    )
}

@Composable
private fun NoResults(query: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.no_results, query), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.no_results_hint),
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
