package com.stockpricealert.util

import android.content.Context

object AppPreferences {
    private const val PREFS_NAME = "stock_alert_prefs"
    private const val KEY_LAST_BACKGROUND_CHECK_AT = "last_background_check_at"
    private const val KEY_LAST_BG_STATUS = "last_bg_status"
    private const val KEY_LAST_BG_MESSAGE = "last_bg_message"
    private const val KEY_LAST_BG_WATCHERS = "last_bg_watchers"
    private const val KEY_LAST_BG_FORCE_RUN = "last_bg_force_run"

    fun getLastBackgroundCheckAt(context: Context): Long? {
        val value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_BACKGROUND_CHECK_AT, -1L)
        return value.takeIf { it > 0L }
    }

    fun getBackgroundCheckResult(context: Context): BackgroundCheckResult? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val completedAt = prefs.getLong(KEY_LAST_BACKGROUND_CHECK_AT, -1L)
        if (completedAt <= 0L) return null

        val status = prefs.getString(KEY_LAST_BG_STATUS, null) ?: return null
        val message = prefs.getString(KEY_LAST_BG_MESSAGE, null) ?: return null

        return BackgroundCheckResult(
            completedAt = completedAt,
            forceRun = prefs.getBoolean(KEY_LAST_BG_FORCE_RUN, false),
            status = status,
            message = message,
            watchersChecked = prefs.getInt(KEY_LAST_BG_WATCHERS, 0)
        )
    }

    fun setBackgroundCheckResult(context: Context, result: BackgroundCheckResult) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_BACKGROUND_CHECK_AT, result.completedAt)
            .putString(KEY_LAST_BG_STATUS, result.status)
            .putString(KEY_LAST_BG_MESSAGE, result.message)
            .putInt(KEY_LAST_BG_WATCHERS, result.watchersChecked)
            .putBoolean(KEY_LAST_BG_FORCE_RUN, result.forceRun)
            .apply()
    }

}
