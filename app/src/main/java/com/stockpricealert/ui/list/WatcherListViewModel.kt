package com.stockpricealert.ui.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stockpricealert.data.repository.StockRepository
import com.stockpricealert.domain.StockWatcher
import com.stockpricealert.notification.AlertNotificationManager
import com.stockpricealert.util.AppPreferences
import com.stockpricealert.util.BackgroundHealthHelper
import com.stockpricealert.util.NotificationPermissionHelper
import com.stockpricealert.worker.WorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WatcherListViewModel(
    application: Application,
    private val repository: StockRepository,
    private val notificationManager: AlertNotificationManager,
    private val onDataChanged: () -> Unit
) : AndroidViewModel(application) {

    val watchers: StateFlow<List<StockWatcher>> = repository.observeWatchers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _priceStates = MutableStateFlow<Map<Long, PriceFetchState>>(emptyMap())
    val priceStates: StateFlow<Map<Long, PriceFetchState>> = _priceStates.asStateFlow()

    private val _systemHealthState = MutableStateFlow(SystemHealthState())
    val systemHealthState: StateFlow<SystemHealthState> = _systemHealthState.asStateFlow()

    init {
        refreshSystemHealth()
    }

    fun refreshSystemHealth() {
        val context = getApplication<Application>()
        _systemHealthState.value = SystemHealthState(
            notificationsEnabled = NotificationPermissionHelper.areNotificationsEnabled(context),
            batteryUnrestricted = BackgroundHealthHelper.isIgnoringBatteryOptimizations(context),
            lastBackgroundCheckAt = AppPreferences.getLastBackgroundCheckAt(context)
        )
    }

    fun fetchCurrentPrice(watcher: StockWatcher) {
        viewModelScope.launch {
            _priceStates.update {
                it + (watcher.id to PriceFetchState(isLoading = true))
            }

            repository.fetchQuote(watcher.stockName)
                .onSuccess { quote ->
                    repository.recordFetchedQuote(watcher.id, quote)
                    _priceStates.update {
                        it + (
                            watcher.id to PriceFetchState(
                                nsePrice = quote.nsePrice,
                                bsePrice = quote.bsePrice,
                                fetchedAt = System.currentTimeMillis()
                            )
                            )
                    }
                }
                .onFailure { error ->
                    _priceStates.update {
                        it + (
                            watcher.id to PriceFetchState(
                                error = error.message ?: "Failed to fetch price"
                            )
                            )
                    }
                }
        }
    }

    fun testNotification() {
        notificationManager.showTestAlert()
            .onSuccess {
                _systemHealthState.update { it.copy(message = "Test notification sent.") }
            }
            .onFailure { error ->
                _systemHealthState.update {
                    it.copy(message = error.message ?: "Failed to send test notification.")
                }
            }
        refreshSystemHealth()
    }

    fun openNotificationSettings() {
        NotificationPermissionHelper.openNotificationSettings(getApplication())
    }

    fun openBatterySettings() {
        BackgroundHealthHelper.openBatteryOptimizationSettings(getApplication())
    }

    fun runTestBackgroundCheck() {
        WorkScheduler.runTestBackgroundCheck(getApplication())
        _systemHealthState.update { it.copy(message = "Background check queued.") }
    }

    fun clearMessage() {
        _systemHealthState.update { it.copy(message = null) }
    }

    fun deleteWatcher(watcher: StockWatcher) {
        viewModelScope.launch {
            repository.deleteWatcher(watcher)
            _priceStates.update { it - watcher.id }
            onDataChanged()
        }
    }

    class Factory(
        private val application: Application,
        private val repository: StockRepository,
        private val notificationManager: AlertNotificationManager,
        private val onDataChanged: () -> Unit
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WatcherListViewModel(
                application,
                repository,
                notificationManager,
                onDataChanged
            ) as T
        }
    }
}
