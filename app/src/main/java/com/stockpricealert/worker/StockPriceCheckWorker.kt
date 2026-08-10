package com.stockpricealert.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stockpricealert.StockAlertApp
import com.stockpricealert.util.MarketHoursChecker

class StockPriceCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!MarketHoursChecker.isWithinTradingWindow()) {
            Log.d(TAG, "Outside trading window, skipping")
            return Result.success()
        }

        val app = applicationContext as StockAlertApp
        val repository = app.repository
        val notifier = app.notificationManager

        val watchers = repository.getActiveWatchers()
        if (watchers.isEmpty()) {
            Log.d(TAG, "No active watchers")
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

            repository.updateLastNsePrice(watcher.id, quote.nsePrice)
        }

        return Result.success()
    }

    companion object {
        const val TAG = "StockPriceCheck"
        const val WORK_NAME = "stock_price_check"
    }
}
