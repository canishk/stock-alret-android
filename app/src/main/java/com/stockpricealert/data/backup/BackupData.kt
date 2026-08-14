package com.stockpricealert.data.backup

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.stockpricealert.util.TradingWindowConfig

data class BackupData(
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long,
    val watchers: List<WatcherBackup>,
    val tradingWindow: TradingWindowBackup?
)

data class WatcherBackup(
    val stockName: String,
    val targetPrice: Double,
    val alertType: String,
    val isActive: Boolean,
    val lastNsePrice: Double? = null,
    val lastBsePrice: Double? = null,
    val lastFetchedAt: Long? = null,
    val createdAt: Long
)

data class TradingWindowBackup(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val weekdaysOnly: Boolean,
    val checkIntervalMinutes: Int
) {
    fun toConfig(): TradingWindowConfig = TradingWindowConfig(
        startHour = startHour,
        startMinute = startMinute,
        endHour = endHour,
        endMinute = endMinute,
        weekdaysOnly = weekdaysOnly,
        checkIntervalMinutes = checkIntervalMinutes
    )

    companion object {
        fun fromConfig(config: TradingWindowConfig) = TradingWindowBackup(
            startHour = config.startHour,
            startMinute = config.startMinute,
            endHour = config.endHour,
            endMinute = config.endMinute,
            weekdaysOnly = config.weekdaysOnly,
            checkIntervalMinutes = config.checkIntervalMinutes
        )
    }
}

object BackupJson {
    const val CURRENT_VERSION = 1
    const val MIME_TYPE = "application/json"
    const val DEFAULT_FILENAME = "stock-watchers-backup.json"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(BackupData::class.java)

    fun encode(data: BackupData): String = adapter.toJson(data)

    fun decode(json: String): BackupData {
        return adapter.fromJson(json)
            ?: throw IllegalArgumentException("Invalid backup file")
    }
}
