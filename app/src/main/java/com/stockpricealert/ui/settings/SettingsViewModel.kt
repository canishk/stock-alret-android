package com.stockpricealert.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stockpricealert.data.backup.BackupManager
import com.stockpricealert.data.backup.ImportResult
import com.stockpricealert.data.repository.StockRepository
import com.stockpricealert.util.AppPreferences
import com.stockpricealert.util.TradingWindowConfig
import com.stockpricealert.worker.WorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val startHour: Int = 11,
    val startMinute: Int = 0,
    val endHour: Int = 15,
    val endMinute: Int = 0,
    val weekdaysOnly: Boolean = true,
    val checkIntervalMinutes: Int = 15,
    val savedMessage: String? = null,
    val isBackupBusy: Boolean = false,
    val pendingImportJson: String? = null
) {
    fun toConfig(): TradingWindowConfig = TradingWindowConfig(
        startHour = startHour,
        startMinute = startMinute,
        endHour = endHour,
        endMinute = endMinute,
        weekdaysOnly = weekdaysOnly,
        checkIntervalMinutes = checkIntervalMinutes
    )

    companion object {
        fun fromConfig(config: TradingWindowConfig) = SettingsUiState(
            startHour = config.startHour,
            startMinute = config.startMinute,
            endHour = config.endHour,
            endMinute = config.endMinute,
            weekdaysOnly = config.weekdaysOnly,
            checkIntervalMinutes = config.checkIntervalMinutes
        )
    }
}

class SettingsViewModel(
    application: Application,
    private val repository: StockRepository,
    private val onScheduleUpdated: () -> Unit
) : AndroidViewModel(application) {

    private val backupManager = BackupManager(repository)

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        val config = AppPreferences.getTradingWindowConfig(getApplication())
        _state.value = SettingsUiState.fromConfig(config)
    }

    fun setStartTime(hour: Int, minute: Int) {
        _state.update { it.copy(startHour = hour, startMinute = minute) }
    }

    fun setEndTime(hour: Int, minute: Int) {
        _state.update { it.copy(endHour = hour, endMinute = minute) }
    }

    fun setWeekdaysOnly(enabled: Boolean) {
        _state.update { it.copy(weekdaysOnly = enabled) }
    }

    fun setCheckIntervalMinutes(minutes: Int) {
        _state.update {
            it.copy(
                checkIntervalMinutes = minutes.coerceAtLeast(
                    TradingWindowConfig.MIN_CHECK_INTERVAL_MINUTES
                )
            )
        }
    }

    fun save() {
        val context = getApplication<Application>()
        val config = _state.value.toConfig().sanitized()
        AppPreferences.setTradingWindowConfig(context, config)
        _state.value = SettingsUiState.fromConfig(config)
        WorkScheduler.schedule(context)
        onScheduleUpdated()
        _state.update { it.copy(savedMessage = "Settings saved") }
    }

    fun exportBackup(onJsonReady: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isBackupBusy = true) }
            try {
                val json = backupManager.createBackupJson(getApplication())
                onJsonReady(json)
            } catch (e: Exception) {
                _state.update {
                    it.copy(savedMessage = e.message ?: "Export failed")
                }
            } finally {
                _state.update { it.copy(isBackupBusy = false) }
            }
        }
    }

    fun writeExportToUri(uri: Uri, json: String) {
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                try {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: return@withContext "Failed to write backup file"
                    "Data exported"
                } catch (_: Exception) {
                    "Failed to write backup file"
                }
            }
            _state.update { it.copy(savedMessage = message) }
        }
    }

    fun prepareImport(json: String) {
        _state.update { it.copy(pendingImportJson = json) }
    }

    fun cancelImport() {
        _state.update { it.copy(pendingImportJson = null) }
    }

    fun confirmImport() {
        val json = _state.value.pendingImportJson ?: return
        viewModelScope.launch {
            _state.update { it.copy(isBackupBusy = true, pendingImportJson = null) }
            try {
                val result = backupManager.importBackupJson(
                    context = getApplication(),
                    json = json,
                    replaceExisting = true
                )
                load()
                WorkScheduler.schedule(getApplication())
                onScheduleUpdated()
                _state.update {
                    it.copy(savedMessage = formatImportMessage(result))
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(savedMessage = e.message ?: "Import failed")
                }
            } finally {
                _state.update { it.copy(isBackupBusy = false) }
            }
        }
    }

    fun readImportFromUri(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isBackupBusy = true) }
            val json = withContext(Dispatchers.IO) {
                try {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().readText()
                    }
                } catch (_: Exception) {
                    null
                }
            }
            _state.update { it.copy(isBackupBusy = false) }
            if (json.isNullOrBlank()) {
                _state.update { it.copy(savedMessage = "Failed to read backup file") }
            } else {
                prepareImport(json)
            }
        }
    }

    fun clearSavedMessage() {
        _state.update { it.copy(savedMessage = null) }
    }

    private fun formatImportMessage(result: ImportResult): String {
        val settingsPart = if (result.settingsRestored) " Settings restored." else ""
        return "Imported ${result.watchersImported} watcher(s).$settingsPart"
    }

    class Factory(
        private val application: Application,
        private val repository: StockRepository,
        private val onScheduleUpdated: () -> Unit
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(application, repository, onScheduleUpdated) as T
        }
    }
}
