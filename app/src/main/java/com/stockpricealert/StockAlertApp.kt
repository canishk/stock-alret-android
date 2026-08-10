package com.stockpricealert

import android.app.Application
import com.stockpricealert.data.local.AppDatabase
import com.stockpricealert.data.repository.StockRepository
import com.stockpricealert.notification.AlertNotificationManager
import com.stockpricealert.worker.WorkScheduler

class StockAlertApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { StockRepository(database.stockWatcherDao()) }
    val notificationManager by lazy { AlertNotificationManager(this) }

    override fun onCreate() {
        super.onCreate()
        WorkScheduler.schedule(this)
    }
}
