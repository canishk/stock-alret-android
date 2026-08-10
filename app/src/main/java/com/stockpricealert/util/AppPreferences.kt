package com.stockpricealert.util

import android.content.Context

object AppPreferences {
    private const val PREFS_NAME = "stock_alert_prefs"
    private const val KEY_LAST_BACKGROUND_CHECK_AT = "last_background_check_at"

    fun getLastBackgroundCheckAt(context: Context): Long? {
        val value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_BACKGROUND_CHECK_AT, -1L)
        return value.takeIf { it > 0L }
    }

    fun setLastBackgroundCheckAt(context: Context, epochMillis: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_BACKGROUND_CHECK_AT, epochMillis)
            .apply()
    }
}
