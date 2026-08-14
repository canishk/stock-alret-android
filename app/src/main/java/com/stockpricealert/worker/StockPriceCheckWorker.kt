package com.stockpricealert.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stockpricealert.StockAlertApp
import com.stockpricealert.util.AppPreferences
import com.stockpricealert.util.BackgroundCheckResult
import com.stockpricealert.util.MarketHoursChecker

class StockPriceCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val forceRun = inputData.getBoolean(KEY_FORCE_RUN, false)
        Log.i(TAG, "Background check started (forceRun=$forceRun)")

        return try {
            if (!forceRun && !MarketHoursChecker.isWithinTradingWindow(applicationContext)) {
                Log.d(TAG, "Outside trading window, skipping silently")
                return Result.success()
            }

            val app = applicationContext as StockAlertApp
            val repository = app.repository
            val notifier = app.notificationManager

            val watchers = repository.getActiveWatchers()
            if (watchers.isEmpty()) {
                Log.d(TAG, "No active watchers")
                saveResult(
                    status = BackgroundCheckResult.STATUS_SUCCESS,
                    message = "No active watchers",
                    forceRun = forceRun,
                    watchersChecked = 0
                )
                return Result.success()
            }

            var watchersChecked = 0
            var apiFailures = 0

            for (watcher in watchers) {
                if (!forceRun && !MarketHoursChecker.isWithinTradingWindow(applicationContext)) {
                    Log.d(TAG, "Trading window ended mid-run, stopping")
                    break
                }

                val quoteResult = repository.fetchQuote(watcher.stockName)
                if (quoteResult.isFailure) {
                    apiFailures++
                    Log.w(TAG, "Market/API unavailable for ${watcher.stockName}, skipping")
                    continue
                }

                val quote = quoteResult.getOrThrow()
                watchersChecked++

                if (repository.shouldTriggerAlert(
                        alertType = watcher.alertType,
                        targetPrice = watcher.targetPrice,
                        currentNsePrice = quote.nsePrice,
                        lastNsePrice = watcher.lastNsePrice
                    )
                ) {
                    notifier.showPriceAlert(
                        stockName = watcher.stockName,
                        alertType = watcher.alertType,
                        targetPrice = watcher.targetPrice,
                        nsePrice = quote.nsePrice,
                        bsePrice = quote.bsePrice,
                        notificationId = watcher.id.toInt()
                    )
                    repository.pauseWatcher(watcher.id)
                }

                repository.recordFetchedQuote(watcher.id, quote)
            }

            when {
                watchersChecked == 0 && apiFailures > 0 -> {
                    saveResult(
                        status = BackgroundCheckResult.STATUS_FAILED,
                        message = "API unavailable for all watchers",
                        forceRun = forceRun,
                        watchersChecked = 0
                    )
                }
                else -> {
                    val message = if (watchersChecked == 1) {
                        "Checked 1 watcher"
                    } else {
                        "Checked $watchersChecked watchers"
                    }
                    saveResult(
                        status = BackgroundCheckResult.STATUS_SUCCESS,
                        message = message,
                        forceRun = forceRun,
                        watchersChecked = watchersChecked
                    )
                }
            }

            Log.i(TAG, "Background check completed (forceRun=$forceRun, checked=$watchersChecked)")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background check failed", e)
            saveResult(
                status = BackgroundCheckResult.STATUS_FAILED,
                message = e.message ?: "Background check failed",
                forceRun = forceRun,
                watchersChecked = 0
            )
            Result.failure()
        }
    }

    private fun saveResult(
        status: String,
        message: String,
        forceRun: Boolean,
        watchersChecked: Int
    ) {
        AppPreferences.setBackgroundCheckResult(
            applicationContext,
            BackgroundCheckResult(
                completedAt = System.currentTimeMillis(),
                forceRun = forceRun,
                status = status,
                message = message,
                watchersChecked = watchersChecked
            )
        )
    }

    companion object {
        const val TAG = "StockPriceCheck"
        const val WORK_NAME = "stock_price_check"
        const val TEST_WORK_NAME = "stock_price_test_check"
        const val KEY_FORCE_RUN = "force_run"
    }
}
