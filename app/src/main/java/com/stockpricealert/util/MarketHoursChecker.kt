package com.stockpricealert.util

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object MarketHoursChecker {
    private val IST = ZoneId.of("Asia/Kolkata")
    private val WINDOW_START = LocalTime.of(11, 0)
    private val WINDOW_END = LocalTime.of(15, 0)

    fun isWithinTradingWindow(now: ZonedDateTime = ZonedDateTime.now(IST)): Boolean {
        val day = now.dayOfWeek
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false
        }

        val time = now.toLocalTime()
        return !time.isBefore(WINDOW_START) && !time.isAfter(WINDOW_END)
    }
}
