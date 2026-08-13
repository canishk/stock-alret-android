package com.stockpricealert.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stockpricealert.util.TradingWindowConfig
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var intervalExpanded by remember { mutableStateOf(false) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH) }

    LaunchedEffect(state.savedMessage) {
        state.savedMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSavedMessage()
        }
    }

    if (showStartPicker) {
        val pickerState = rememberTimePickerState(
            initialHour = state.startHour,
            initialMinute = state.startMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setStartTime(pickerState.hour, pickerState.minute)
                        showStartPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showStartPicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = pickerState)
            }
        )
    }

    if (showEndPicker) {
        val pickerState = rememberTimePickerState(
            initialHour = state.endHour,
            initialMinute = state.endMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setEndTime(pickerState.hour, pickerState.minute)
                        showEndPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEndPicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = pickerState)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Trading window (IST)",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedButton(
                onClick = { showStartPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                val start = state.toConfig().startTime().format(timeFormatter)
                Text("Start: $start")
            }

            OutlinedButton(
                onClick = { showEndPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                val end = state.toConfig().endTime().format(timeFormatter)
                Text("End: $end")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Weekdays only (Mon–Fri)")
                Switch(
                    checked = state.weekdaysOnly,
                    onCheckedChange = viewModel::setWeekdaysOnly
                )
            }

            Text(
                text = "Background check interval",
                style = MaterialTheme.typography.titleMedium
            )

            ExposedDropdownMenuBox(
                expanded = intervalExpanded,
                onExpandedChange = { intervalExpanded = it }
            ) {
                TextField(
                    value = "${state.checkIntervalMinutes} minutes",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Check every") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                DropdownMenu(
                    expanded = intervalExpanded,
                    onDismissRequest = { intervalExpanded = false }
                ) {
                    TradingWindowConfig.INTERVAL_OPTIONS.forEach { minutes ->
                        DropdownMenuItem(
                            text = { Text("$minutes minutes") },
                            onClick = {
                                viewModel.setCheckIntervalMinutes(minutes)
                                intervalExpanded = false
                            }
                        )
                    }
                }
            }

            Text(
                text = "Summary: ${state.toConfig().let { config ->
                    val days = if (config.weekdaysOnly) "Mon–Fri" else "Every day"
                    val start = config.startTime().format(timeFormatter)
                    val end = config.endTime().format(timeFormatter)
                    "$days, $start – $end IST · every ${config.checkIntervalMinutes} min"
                }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
