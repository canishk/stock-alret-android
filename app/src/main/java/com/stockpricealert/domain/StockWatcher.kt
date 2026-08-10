package com.stockpricealert.domain

data class StockWatcher(
    val id: Long = 0,
    val stockName: String,
    val targetPrice: Double,
    val alertType: AlertType,
    val isActive: Boolean = true,
    val lastNsePrice: Double? = null,
    val lastBsePrice: Double? = null,
    val lastFetchedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
