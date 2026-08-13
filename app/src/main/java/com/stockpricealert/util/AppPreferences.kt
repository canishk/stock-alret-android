package com.stockpricealert.util

import android.content.Context

object AppPreferences {
    private const val PREFS_NAME = "stock_alert_prefs"
    private const val KEY_LAST_BACKGROUND_CHECK_AT = "last_background_check_at"
    private const val KEY_LAST_BG_STATUS = "last_bg_status"
    private const val KEY_LAST_BG_MESSAGE = "last_bg_message"
    private const val KEY_LAST_BG_WATCHERS = "last_bg_watchers"
    private const val KEY_LAST_BG_FORCE_RUN = "last_bg_force_run"
    private const val KEY_WINDOW_START_HOUR = "window_start_h"
    private const val KEY_WINDOW_START_MINUTE = "window_start_m"
    private const val KEY_WINDOW_END_HOUR = "window_end_h"
    private const val KEY_WINDOW_END_MINUTE = "window_end_m"
    private const val KEY_WINDOW_WEEKDAYS_ONLY = "window_weekdays_only"
    private const val KEY_CHECK_INTERVAL_MINUTES = "check_interval_minutes"

    fun getTradingWindowConfig(context: Context): TradingWindowConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaults = TradingWindowConfig.DEFAULT
        return TradingWindowConfig(
            startHour = prefs.getInt(KEY_WINDOW_START_HOUR, defaults.startHour),
            startMinute = prefs.getInt(KEY_WINDOW_START_MINUTE, defaults.startMinute),
            endHour = prefs.getInt(KEY_WINDOW_END_HOUR, defaults.endHour),
            endMinute = prefs.getInt(KEY_WINDOW_END_MINUTE, defaults.endMinute),
            weekdaysOnly = prefs.getBoolean(KEY_WINDOW_WEEKDAYS_ONLY, defaults.weekdaysOnly),
            checkIntervalMinutes = prefs.getInt(
                KEY_CHECK_INTERVAL_MINUTES,
                defaults.checkIntervalMinutes
            )
        ).sanitized()
    }

    fun setTradingWindowConfig(context: Context, config: TradingWindowConfig) {
        val sanitized = config.sanitized()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_WINDOW_START_HOUR, sanitized.startHour)
            .putInt(KEY_WINDOW_START_MINUTE, sanitized.startMinute)
            .putInt(KEY_WINDOW_END_HOUR, sanitized.endHour)
            .putInt(KEY_WINDOW_END_MINUTE, sanitized.endMinute)
            .putBoolean(KEY_WINDOW_WEEKDAYS_ONLY, sanitized.weekdaysOnly)
            .putInt(KEY_CHECK_INTERVAL_MINUTES, sanitized.checkIntervalMinutes)
            .apply()
    }

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
