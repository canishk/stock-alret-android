package com.stockpricealert.util

import android.content.Context
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object MarketHoursChecker {
    private val IST = ZoneId.of("Asia/Kolkata")
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    fun isWithinTradingWindow(
        context: Context,
        now: ZonedDateTime = ZonedDateTime.now(IST)
    ): Boolean {
        val config = AppPreferences.getTradingWindowConfig(context).sanitized()

        if (config.weekdaysOnly) {
            val day = now.dayOfWeek
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                return false
            }
        }

        val time = now.toLocalTime()
        val start = config.startTime()
        val end = config.endTime()
        return !time.isBefore(start) && !time.isAfter(end)
    }

    fun formatWindowSummary(context: Context): String {
        val config = AppPreferences.getTradingWindowConfig(context).sanitized()
        val days = if (config.weekdaysOnly) "Mon–Fri" else "Every day"
        val start = config.startTime().format(timeFormatter)
        val end = config.endTime().format(timeFormatter)
        return "$days, $start – $end IST"
    }

    fun formatIntervalSummary(context: Context): String {
        val minutes = AppPreferences.getTradingWindowConfig(context).sanitized().checkIntervalMinutes
        return "every $minutes min"
    }

    fun formatOutsideWindowMessage(context: Context): String {
        return "Outside trading window (${formatWindowSummary(context)})"
    }
}
