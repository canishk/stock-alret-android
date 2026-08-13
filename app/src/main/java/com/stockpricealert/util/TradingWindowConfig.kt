package com.stockpricealert.util

import java.time.LocalTime

data class TradingWindowConfig(
    val startHour: Int = 11,
    val startMinute: Int = 0,
    val endHour: Int = 15,
    val endMinute: Int = 0,
    val weekdaysOnly: Boolean = true,
    val checkIntervalMinutes: Int = 15
) {
    fun startTime(): LocalTime = LocalTime.of(startHour, startMinute)

    fun endTime(): LocalTime = LocalTime.of(endHour, endMinute)

    fun sanitized(): TradingWindowConfig {
        val interval = checkIntervalMinutes.coerceAtLeast(MIN_CHECK_INTERVAL_MINUTES)
        if (!startTime().isBefore(endTime())) {
            return DEFAULT.copy(checkIntervalMinutes = interval)
        }
        return copy(checkIntervalMinutes = interval)
    }

    companion object {
        const val MIN_CHECK_INTERVAL_MINUTES = 15
        val INTERVAL_OPTIONS = listOf(15, 30, 45, 60)

        val DEFAULT = TradingWindowConfig()
    }
}
