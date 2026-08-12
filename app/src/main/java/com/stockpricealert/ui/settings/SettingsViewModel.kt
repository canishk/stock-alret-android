package com.stockpricealert.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stockpricealert.util.AppPreferences
import com.stockpricealert.util.TradingWindowConfig
import com.stockpricealert.worker.WorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val startHour: Int = 11,
    val startMinute: Int = 0,
    val endHour: Int = 15,
    val endMinute: Int = 0,
    val weekdaysOnly: Boolean = true,
    val checkIntervalMinutes: Int = 15,
    val savedMessage: String? = null
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
    private val onScheduleUpdated: () -> Unit
) : AndroidViewModel(application) {

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

    fun clearSavedMessage() {
        _state.update { it.copy(savedMessage = null) }
    }

    class Factory(
        private val application: Application,
        private val onScheduleUpdated: () -> Unit
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(application, onScheduleUpdated) as T
        }
    }
}
