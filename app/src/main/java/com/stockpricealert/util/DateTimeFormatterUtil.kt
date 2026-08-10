package com.stockpricealert.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeFormatterUtil {
    private val IST = ZoneId.of("Asia/Kolkata")
    private val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH)

    fun formatEpochMillis(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(IST)
            .format(formatter)
    }
}
