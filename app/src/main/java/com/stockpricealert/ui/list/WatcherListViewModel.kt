package com.stockpricealert.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stockpricealert.data.repository.StockRepository
import com.stockpricealert.domain.StockWatcher
import com.stockpricealert.worker.WorkScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WatcherListViewModel(
    private val repository: StockRepository,
    private val onDataChanged: () -> Unit
) : ViewModel() {

    val watchers: StateFlow<List<StockWatcher>> = repository.observeWatchers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteWatcher(watcher: StockWatcher) {
        viewModelScope.launch {
            repository.deleteWatcher(watcher)
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
