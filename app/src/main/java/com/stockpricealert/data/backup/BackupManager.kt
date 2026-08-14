package com.stockpricealert.data.backup

import android.content.Context
import com.stockpricealert.data.repository.StockRepository
import com.stockpricealert.util.AppPreferences

class BackupManager(
    private val repository: StockRepository
) {
    suspend fun createBackupJson(context: Context): String {
        val watchers = repository.getAllWatchers()
        val config = AppPreferences.getTradingWindowConfig(context)
        val backup = BackupData(
            exportedAt = System.currentTimeMillis(),
            watchers = watchers.map { watcher ->
                WatcherBackup(
                    stockName = watcher.stockName,
                    targetPrice = watcher.targetPrice,
                    alertType = watcher.alertType.name,
                    isActive = watcher.isActive,
                    lastNsePrice = watcher.lastNsePrice,
                    lastBsePrice = watcher.lastBsePrice,
                    lastFetchedAt = watcher.lastFetchedAt,
                    createdAt = watcher.createdAt
                )
            },
            tradingWindow = TradingWindowBackup.fromConfig(config)
        )
        return BackupJson.encode(backup)
    }

    suspend fun importBackupJson(
        context: Context,
        json: String,
        replaceExisting: Boolean
    ): ImportResult {
        val backup = BackupJson.decode(json)
        if (backup.version > BackupJson.CURRENT_VERSION) {
            throw IllegalArgumentException("Backup version ${backup.version} is newer than this app supports")
        }

        val importedCount = if (replaceExisting) {
            repository.replaceAllWatchers(backup.watchers)
        } else {
            repository.mergeWatchers(backup.watchers)
        }

        backup.tradingWindow?.let { window ->
            AppPreferences.setTradingWindowConfig(context, window.toConfig().sanitized())
        }

        return ImportResult(
            watchersImported = importedCount,
            settingsRestored = backup.tradingWindow != null
        )
    }
}

data class ImportResult(
    val watchersImported: Int,
    val settingsRestored: Boolean
)
