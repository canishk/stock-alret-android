package com.stockpricealert.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stockpricealert.data.repository.StockRepository
import com.stockpricealert.domain.StockWatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WatcherListViewModel(
    private val repository: StockRepository,
    private val onDataChanged: () -> Unit
) : ViewModel() {

    val watchers: StateFlow<List<StockWatcher>> = repository.observeWatchers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _priceStates = MutableStateFlow<Map<Long, PriceFetchState>>(emptyMap())
    val priceStates: StateFlow<Map<Long, PriceFetchState>> = _priceStates.asStateFlow()

    fun fetchCurrentPrice(watcher: StockWatcher) {
        viewModelScope.launch {
            _priceStates.update {
                it + (watcher.id to PriceFetchState(isLoading = true))
            }

            repository.fetchQuote(watcher.stockName)
                .onSuccess { quote ->
                    repository.updateLastNsePrice(watcher.id, quote.nsePrice)
                    _priceStates.update {
                        it + (
                            watcher.id to PriceFetchState(
                                nsePrice = quote.nsePrice,
                                bsePrice = quote.bsePrice
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

    fun deleteWatcher(watcher: StockWatcher) {
        viewModelScope.launch {
            repository.deleteWatcher(watcher)
            _priceStates.update { it - watcher.id }
            onDataChanged()
        }
    }

    class Factory(
        private val repository: StockRepository,
        private val onDataChanged: () -> Unit
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WatcherListViewModel(repository, onDataChanged) as T
        }
    }
}
