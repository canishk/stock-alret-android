package com.stockpricealert.ui.list

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.stockpricealert.domain.StockWatcher
import com.stockpricealert.util.BackgroundCheckResult
import com.stockpricealert.util.DateTimeFormatterUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatcherListScreen(
    viewModel: WatcherListViewModel,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onSettingsClick: () -> Unit
) {
    val watchers by viewModel.watchers.collectAsState()
    val priceStates by viewModel.priceStates.collectAsState()
    val systemHealth by viewModel.systemHealthState.collectAsState()
    var watcherToDelete by remember { mutableStateOf<StockWatcher?>(null) }
    var healthExpanded by remember { mutableStateOf(false) }
    var lastNotifiedIssueKey by remember { mutableStateOf<String?>(null) }
    var topBarMenuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshSystemHealth()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemHealth()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(systemHealth.message) {
        systemHealth.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(systemHealth.issueKey) {
        if (!systemHealth.isHealthy && systemHealth.issueKey != lastNotifiedIssueKey) {
            val result = snackbarHostState.showSnackbar(
                message = "App health needs attention. Check settings at top.",
                actionLabel = "View"
            )
            if (result == SnackbarResult.ActionPerformed) {
                healthExpanded = true
            }
            lastNotifiedIssueKey = systemHealth.issueKey
        }
        if (systemHealth.isHealthy) {
            lastNotifiedIssueKey = null
            healthExpanded = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock Watchers") },
                actions = {
                    IconButton(onClick = { topBarMenuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = topBarMenuExpanded,
                        onDismissRequest = { topBarMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                topBarMenuExpanded = false
                                onSettingsClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Test Background Check") },
                            onClick = {
                                topBarMenuExpanded = false
                                viewModel.runTestBackgroundCheck()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Refresh status") },
                            onClick = {
                                topBarMenuExpanded = false
                                viewModel.refreshSystemHealth()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add watcher")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BackgroundCheckStatusRow(health = systemHealth)
            }

            if (!systemHealth.isHealthy) {
                item {
                    AppHealthSection(
                        health = systemHealth,
                        expanded = healthExpanded,
                        onToggleExpanded = { healthExpanded = !healthExpanded },
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.openNotificationSettings()
                            }
                        },
                        onOpenNotificationSettings = viewModel::openNotificationSettings,
                        onTestNotification = viewModel::testNotification,
                        onOpenBatterySettings = viewModel::openBatterySettings
                    )
                }
            }

            if (watchers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No watchers yet.\nTap + to add a stock alert.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
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
private fun BackgroundCheckStatusRow(health: SystemHealthState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (health.isBackgroundCheckRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Background: Running...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val result = health.lastBackgroundResult
                val text = if (result == null) {
                    "Background: No check run yet"
                } else {
                    val time = DateTimeFormatterUtil.formatEpochMillis(result.completedAt)
                    "Background: Last run $time — ${result.message}"
                }
                val color = when (result?.status) {
                    BackgroundCheckResult.STATUS_SKIPPED,
                    BackgroundCheckResult.STATUS_FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            }
        }
        if (health.tradingWindowSummary.isNotBlank()) {
            Text(
                text = "Window: ${health.tradingWindowSummary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (health.checkIntervalSummary.isNotBlank()) {
            Text(
                text = "Interval: ${health.checkIntervalSummary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppHealthSection(
    health: SystemHealthState,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onTestNotification: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "App Health: ${health.issueSummary()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            if (expanded) {
                if (!health.notificationsEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onRequestNotificationPermission) {
                            Text("Allow Notifications")
                        }
                        OutlinedButton(onClick = onOpenNotificationSettings) {
                            Text("Notification Settings")
                        }
                    }
                }

                if (!health.batteryUnrestricted) {
                    OutlinedButton(onClick = onOpenBatterySettings) {
                        Text("Battery Settings")
                    }
                }

                TextButton(onClick = onTestNotification) {
                    Text("Test Notification")
                }
            }
        }
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
            val displayBse = priceState?.bsePrice ?: watcher.lastBsePrice
            val displayFetchedAt = priceState?.fetchedAt ?: watcher.lastFetchedAt

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
            displayFetchedAt?.let { fetchedAt ->
                Text(
                    text = "Last fetched: ${DateTimeFormatterUtil.formatEpochMillis(fetchedAt)}",
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
