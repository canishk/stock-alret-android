package com.stockpricealert.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {
    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<StockPriceCheckWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            StockPriceCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun runTestBackgroundCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val input = Data.Builder()
            .putBoolean(StockPriceCheckWorker.KEY_FORCE_RUN, true)
            .build()

        val request = OneTimeWorkRequestBuilder<StockPriceCheckWorker>()
            .setConstraints(constraints)
            .setInputData(input)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            StockPriceCheckWorker.TEST_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
