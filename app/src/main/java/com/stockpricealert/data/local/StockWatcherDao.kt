package com.stockpricealert.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StockWatcherDao {
    @Query("SELECT * FROM stock_watchers ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<StockWatcherEntity>>

    @Query("SELECT * FROM stock_watchers WHERE isActive = 1")
    suspend fun getActiveWatchers(): List<StockWatcherEntity>

    @Query("SELECT * FROM stock_watchers WHERE id = :id")
    suspend fun getById(id: Long): StockWatcherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(watcher: StockWatcherEntity): Long

    @Update
    suspend fun update(watcher: StockWatcherEntity)

    @Delete
    suspend fun delete(watcher: StockWatcherEntity)

    @Query(
        "UPDATE stock_watchers SET lastNsePrice = :nse, lastBsePrice = :bse, lastFetchedAt = :fetchedAt WHERE id = :id"
    )
    suspend fun updateLastFetchedQuote(id: Long, nse: Double, bse: Double?, fetchedAt: Long)
}
