package com.stockpricealert.data.repository

import com.stockpricealert.data.backup.WatcherBackup
import com.stockpricealert.data.local.StockWatcherDao
import com.stockpricealert.data.local.StockWatcherEntity
import com.stockpricealert.data.remote.ApiClient
import com.stockpricealert.data.remote.StockPriceResponse
import com.stockpricealert.domain.AlertType
import com.stockpricealert.domain.StockWatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StockRepository(
    private val dao: StockWatcherDao
) {
    private val api = ApiClient.stockApiService

    fun observeWatchers(): Flow<List<StockWatcher>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getWatcher(id: Long): StockWatcher? =
        dao.getById(id)?.toDomain()

    suspend fun getActiveWatchers(): List<StockWatcher> =
        dao.getActiveWatchers().map { it.toDomain() }

    suspend fun getAllWatchers(): List<StockWatcher> =
        dao.getAll().map { it.toDomain() }

    suspend fun replaceAllWatchers(backups: List<WatcherBackup>): Int {
        dao.deleteAll()
        backups.forEach { backup ->
            dao.insert(backup.toEntity())
        }
        return backups.size
    }

    suspend fun mergeWatchers(backups: List<WatcherBackup>): Int {
        var imported = 0
        backups.forEach { backup ->
            dao.insert(backup.toEntity())
            imported++
        }
        return imported
    }

    suspend fun saveWatcher(watcher: StockWatcher): Long {
        val entity = watcher.toEntity()
        return if (watcher.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            watcher.id
        }
    }

    suspend fun deleteWatcher(watcher: StockWatcher) {
        dao.delete(watcher.toEntity())
    }

    suspend fun recordFetchedQuote(watcherId: Long, quote: StockQuote) {
        dao.updateLastFetchedQuote(
            id = watcherId,
            nse = quote.nsePrice,
            bse = quote.bsePrice,
            fetchedAt = System.currentTimeMillis()
        )
    }

    suspend fun pauseWatcher(id: Long) {
        dao.updateActive(id, false)
    }

    suspend fun resumeWatcher(id: Long) {
        dao.updateActive(id, true)
        dao.clearLastNsePrice(id)
    }

    suspend fun rearmWatcher(id: Long) {
        dao.clearLastNsePrice(id)
    }

    suspend fun fetchQuote(stockName: String): Result<StockQuote> {
        return try {
            val response = api.getStock(stockName)
            parseQuote(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseQuote(response: StockPriceResponse): Result<StockQuote> {
        val nseRaw = response.currentPrice?.nse
        val nsePrice = nseRaw?.replace(",", "")?.toDoubleOrNull()
            ?: return Result.failure(MarketUnavailableException("NSE price unavailable"))

        val bsePrice = response.currentPrice?.bse
            ?.replace(",", "")
            ?.toDoubleOrNull()

        return Result.success(
            StockQuote(
                nsePrice = nsePrice,
                bsePrice = bsePrice,
                companyName = response.companyName
            )
        )
    }

    fun shouldTriggerAlert(
        alertType: AlertType,
        targetPrice: Double,
        currentNsePrice: Double,
        lastNsePrice: Double?
    ): Boolean {
        return when (alertType) {
            AlertType.HIGH -> {
                val breached = currentNsePrice >= targetPrice
                if (lastNsePrice == null) breached
                else lastNsePrice < targetPrice && breached
            }
            AlertType.LOW -> {
                val breached = currentNsePrice <= targetPrice
                if (lastNsePrice == null) breached
                else lastNsePrice > targetPrice && breached
            }
        }
    }

    private fun StockWatcherEntity.toDomain() = StockWatcher(
        id = id,
        stockName = stockName,
        targetPrice = targetPrice,
        alertType = AlertType.fromString(alertType),
        isActive = isActive,
        lastNsePrice = lastNsePrice,
        lastBsePrice = lastBsePrice,
        lastFetchedAt = lastFetchedAt,
        createdAt = createdAt
    )

    private fun StockWatcher.toEntity() = StockWatcherEntity(
        id = id,
        stockName = stockName,
        targetPrice = targetPrice,
        alertType = alertType.name,
        isActive = isActive,
        lastNsePrice = lastNsePrice,
        lastBsePrice = lastBsePrice,
        lastFetchedAt = lastFetchedAt,
        createdAt = createdAt
    )

    private fun WatcherBackup.toEntity() = StockWatcherEntity(
        id = 0,
        stockName = stockName,
        targetPrice = targetPrice,
        alertType = alertType,
        isActive = isActive,
        lastNsePrice = lastNsePrice,
        lastBsePrice = lastBsePrice,
        lastFetchedAt = lastFetchedAt,
        createdAt = createdAt
    )
}

class MarketUnavailableException(message: String) : Exception(message)
