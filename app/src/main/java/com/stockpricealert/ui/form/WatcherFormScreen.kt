package com.stockpricealert.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.stockpricealert.domain.AlertType
import com.stockpricealert.ui.theme.BearRed
import com.stockpricealert.ui.theme.BullBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatcherFormScreen(
    viewModel: WatcherFormViewModel,
    isEditing: Boolean,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Watcher" else "Add Watcher") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.stockName,
                onValueChange = viewModel::onStockNameChange,
                label = { Text("Stock name") },
                placeholder = { Text("e.g. RELIANCE, Tata Steel") },
                isError = state.stockNameError != null,
                supportingText = state.stockNameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.targetPrice,
                onValueChange = viewModel::onTargetPriceChange,
                label = { Text("Target price (₹)") },
                isError = state.targetPriceError != null,
                supportingText = state.targetPriceError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                text = "Alert when price is",
                style = MaterialTheme.typography.titleSmall
            )

            AlertType.entries.forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = state.alertType == type,
                            onClick = { viewModel.onAlertTypeChange(type) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = state.alertType == type,
                        onClick = null
                    )
                    Text(
                        text = if (type == AlertType.HIGH) {
                            "Bull — HIGH (at or above target)"
                        } else {
                            "Bear — LOW (at or below target)"
                        },
                        color = if (type == AlertType.HIGH) BullBlue else BearRed,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            state.saveError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "Update Watcher" else "Save Watcher")
            }
        }
    }
}
