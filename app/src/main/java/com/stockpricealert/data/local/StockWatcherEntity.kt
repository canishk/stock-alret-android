package com.stockpricealert.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_watchers")
data class StockWatcherEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stockName: String,
    val targetPrice: Double,
    val alertType: String,
    val isActive: Boolean = true,
    val lastNsePrice: Double? = null,
    val lastBsePrice: Double? = null,
    val lastFetchedAt: Long? = null,
    val createdAt: Long
)
