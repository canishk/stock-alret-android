package com.stockpricealert.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stockpricealert.data.repository.StockRepository
import com.stockpricealert.domain.AlertType
import com.stockpricealert.domain.StockWatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WatcherFormState(
    val stockName: String = "",
    val targetPrice: String = "",
    val alertType: AlertType = AlertType.HIGH,
    val isLoading: Boolean = false,
    val stockNameError: String? = null,
    val targetPriceError: String? = null,
    val saveError: String? = null
)

class WatcherFormViewModel(
    private val repository: StockRepository,
    private val watcherId: Long?,
    private val onDataChanged: () -> Unit,
    private val onSaved: () -> Unit
) : ViewModel() {

    private val _state = MutableStateFlow(WatcherFormState())
    val state: StateFlow<WatcherFormState> = _state.asStateFlow()

    init {
        if (watcherId != null) {
            viewModelScope.launch {
                val watcher = repository.getWatcher(watcherId)
                if (watcher != null) {
                    _state.update {
                        it.copy(
                            stockName = watcher.stockName,
                            targetPrice = watcher.targetPrice.toString(),
                            alertType = watcher.alertType
                        )
                    }
                }
            }
        }
    }

    fun onStockNameChange(value: String) {
        _state.update { it.copy(stockName = value, stockNameError = null, saveError = null) }
    }

    fun onTargetPriceChange(value: String) {
        _state.update { it.copy(targetPrice = value, targetPriceError = null, saveError = null) }
    }

    fun onAlertTypeChange(value: AlertType) {
        _state.update { it.copy(alertType = value) }
    }

    fun save() {
        val current = _state.value
        val stockName = current.stockName.trim()
        val price = current.targetPrice.trim().toDoubleOrNull()

        var hasError = false
        if (stockName.isEmpty()) {
            _state.update { it.copy(stockNameError = "Stock name is required") }
            hasError = true
        }
        if (price == null || price <= 0) {
            _state.update { it.copy(targetPriceError = "Enter a valid price") }
            hasError = true
        }
        if (hasError || price == null) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, saveError = null) }
            try {
                val existing = watcherId?.let { repository.getWatcher(it) }
                val targetChanged = existing != null && existing.targetPrice != price
                val typeChanged = existing != null && existing.alertType != current.alertType
                val wasPaused = existing != null && !existing.isActive
                val shouldRearm = existing == null || targetChanged || typeChanged || wasPaused

                val watcher = StockWatcher(
                    id = watcherId ?: 0L,
                    stockName = stockName,
                    targetPrice = price,
                    alertType = current.alertType,
                    isActive = true,
                    lastNsePrice = if (shouldRearm) null else existing?.lastNsePrice,
                    lastBsePrice = existing?.lastBsePrice,
                    lastFetchedAt = existing?.lastFetchedAt,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
                val savedId = repository.saveWatcher(watcher)
                if (shouldRearm && watcherId != null) {
                    repository.rearmWatcher(savedId)
                }
                onDataChanged()
                onSaved()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        saveError = e.message ?: "Failed to save watcher"
                    )
                }
            }
        }
    }

    class Factory(
        private val repository: StockRepository,
        private val watcherId: Long?,
        private val onDataChanged: () -> Unit,
        private val onSaved: () -> Unit
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WatcherFormViewModel(repository, watcherId, onDataChanged, onSaved) as T
        }
    }
}
