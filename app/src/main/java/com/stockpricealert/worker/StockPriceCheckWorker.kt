package com.stockpricealert.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stockpricealert.StockAlertApp
import com.stockpricealert.util.AppPreferences
import com.stockpricealert.util.MarketHoursChecker

class StockPriceCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val forceRun = inputData.getBoolean(KEY_FORCE_RUN, false)

        if (!forceRun && !MarketHoursChecker.isWithinTradingWindow()) {
            Log.d(TAG, "Outside trading window, skipping")
            return Result.success()
        }

        val app = applicationContext as StockAlertApp
        val repository = app.repository
        val notifier = app.notificationManager

        val watchers = repository.getActiveWatchers()
        if (watchers.isEmpty()) {
            Log.d(TAG, "No active watchers")
            AppPreferences.setLastBackgroundCheckAt(applicationContext, System.currentTimeMillis())
            return Result.success()
        }

        for (watcher in watchers) {
            val quoteResult = repository.fetchQuote(watcher.stockName)
            if (quoteResult.isFailure) {
                Log.w(TAG, "Market/API unavailable for ${watcher.stockName}, skipping")
                continue
            }

            val quote = quoteResult.getOrThrow()
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
            }

            repository.recordFetchedQuote(watcher.id, quote)
        }

        AppPreferences.setLastBackgroundCheckAt(applicationContext, System.currentTimeMillis())
        Log.d(TAG, "Background check completed (forceRun=$forceRun)")

        return Result.success()
    }

    companion object {
        const val TAG = "StockPriceCheck"
        const val WORK_NAME = "stock_price_check"
        const val TEST_WORK_NAME = "stock_price_test_check"
        const val KEY_FORCE_RUN = "force_run"
    }
}
