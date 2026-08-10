package com.stockpricealert.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stockpricealert.domain.StockWatcher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatcherListScreen(
    viewModel: WatcherListViewModel,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit
) {
    val watchers by viewModel.watchers.collectAsState()
    val priceStates by viewModel.priceStates.collectAsState()
    var watcherToDelete by remember { mutableStateOf<StockWatcher?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Stock Watchers") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add watcher")
            }
        }
    ) { padding ->
        if (watchers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No watchers yet.\nTap + to add a stock alert.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(watchers, key = { it.id }) { watcher ->
                    WatcherCard(
                        watcher = watcher,
                        priceState = priceStates[watcher.id],
                        onClick = { onEditClick(watcher.id) },
                        onFetchPriceClick = { viewModel.fetchCurrentPrice(watcher) },
                        onDeleteClick = { watcherToDelete = watcher }
                    )
                }
            }
        }
    }

    watcherToDelete?.let { watcher ->
        AlertDialog(
            onDismissRequest = { watcherToDelete = null },
            title = { Text("Delete watcher?") },
            text = { Text("Remove alert for ${watcher.stockName}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWatcher(watcher)
                        watcherToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { watcherToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun WatcherCard(
    watcher: StockWatcher,
    priceState: PriceFetchState?,
    onClick: () -> Unit,
    onFetchPriceClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = watcher.stockName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onClick)
                )
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
            Text(
                text = "Target: ₹%.2f (${watcher.alertType.name})".format(watcher.targetPrice),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable(onClick = onClick)
            )

            val displayNse = priceState?.nsePrice ?: watcher.lastNsePrice
            val displayBse = priceState?.bsePrice

            displayNse?.let { price ->
                Text(
                    text = "NSE: ₹%.2f".format(price),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            displayBse?.let { price ->
                Text(
                    text = "BSE: ₹%.2f".format(price),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            priceState?.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedButton(
                onClick = onFetchPriceClick,
                enabled = priceState?.isLoading != true,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                if (priceState?.isLoading == true) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 4.dp)
                    )
                }
                Text("Fetch Price")
            }

            Text(
                text = if (watcher.isActive) "Active" else "Inactive",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
