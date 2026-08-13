package com.stockpricealert.ui.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.stockpricealert.data.repository.StockRepository
import com.stockpricealert.domain.StockWatcher
import com.stockpricealert.notification.AlertNotificationManager
import com.stockpricealert.util.AppPreferences
import com.stockpricealert.util.BackgroundHealthHelper
import com.stockpricealert.util.MarketHoursChecker
import com.stockpricealert.util.NotificationPermissionHelper
import com.stockpricealert.worker.StockPriceCheckWorker
import com.stockpricealert.worker.WorkScheduler
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
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
        observeTestBackgroundWork()
    }

    private fun observeTestBackgroundWork() {
        viewModelScope.launch {
            val workManager = WorkManager.getInstance(getApplication())
            callbackFlow {
                val liveData = workManager.getWorkInfosForUniqueWorkLiveData(
                    StockPriceCheckWorker.TEST_WORK_NAME
                )
                val observer = Observer<List<WorkInfo>> { workInfos ->
                    trySend(workInfos)
                }
                liveData.observeForever(observer)
                awaitClose { liveData.removeObserver(observer) }
            }.collect { workInfos ->
                handleTestWorkState(workInfos)
            }
        }
    }

    private fun handleTestWorkState(workInfos: List<WorkInfo>) {
        val workInfo = workInfos.firstOrNull()
        val state = workInfo?.state

        val isRunning = state == WorkInfo.State.ENQUEUED ||
            state == WorkInfo.State.RUNNING ||
            state == WorkInfo.State.BLOCKED

        val jobState = when (state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> BackgroundJobState.Queued
            WorkInfo.State.RUNNING -> BackgroundJobState.Running
            WorkInfo.State.SUCCEEDED -> BackgroundJobState.Succeeded
            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> BackgroundJobState.Failed
            else -> BackgroundJobState.Idle
        }

        val wasRunning = _systemHealthState.value.isBackgroundCheckRunning

        _systemHealthState.update {
            it.copy(
                isBackgroundCheckRunning = isRunning,
                backgroundJobState = jobState
            )
        }

        if (!isRunning && wasRunning &&
            (state == WorkInfo.State.SUCCEEDED || state == WorkInfo.State.FAILED)
        ) {
            refreshSystemHealth()
            val result = AppPreferences.getBackgroundCheckResult(getApplication())
            val message = result?.message ?: when (state) {
                WorkInfo.State.SUCCEEDED -> "Background check completed"
                else -> "Background check failed"
            }
            _systemHealthState.update { it.copy(message = message) }
        }
    }

    fun refreshSystemHealth() {
        val context = getApplication<Application>()
        _systemHealthState.update {
            it.copy(
                notificationsEnabled = NotificationPermissionHelper.areNotificationsEnabled(context),
                batteryUnrestricted = BackgroundHealthHelper.isIgnoringBatteryOptimizations(context),
                lastBackgroundResult = AppPreferences.getBackgroundCheckResult(context),
                tradingWindowSummary = MarketHoursChecker.formatWindowSummary(context),
                checkIntervalSummary = MarketHoursChecker.formatIntervalSummary(context)
            )
        }
    }

    fun fetchCurrentPrice(watcher: StockWatcher) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            if (!MarketHoursChecker.isWithinTradingWindow(context)) {
                val message = MarketHoursChecker.formatOutsideWindowMessage(context)
                _priceStates.update {
                    it + (watcher.id to PriceFetchState(error = message))
                }
                return@launch
            }

            _priceStates.update {
                it + (watcher.id to PriceFetchState(isLoading = true))
            }

            repository.fetchQuote(watcher.stockName)
                .onSuccess { quote ->
                    if (watcher.isActive && repository.shouldTriggerAlert(
                            alertType = watcher.alertType,
                            targetPrice = watcher.targetPrice,
                            currentNsePrice = quote.nsePrice,
                            lastNsePrice = watcher.lastNsePrice
                        )
                    ) {
                        notificationManager.showPriceAlert(
                            stockName = watcher.stockName,
                            alertType = watcher.alertType,
                            targetPrice = watcher.targetPrice,
                            nsePrice = quote.nsePrice,
                            bsePrice = quote.bsePrice,
                            notificationId = watcher.id.toInt()
                        )
                        repository.pauseWatcher(watcher.id)
                    }
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
        _systemHealthState.update {
            it.copy(
                message = "Background check queued.",
                isBackgroundCheckRunning = true,
                backgroundJobState = BackgroundJobState.Queued
            )
        }
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

    fun resumeWatcher(watcher: StockWatcher) {
        viewModelScope.launch {
            repository.resumeWatcher(watcher.id)
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
