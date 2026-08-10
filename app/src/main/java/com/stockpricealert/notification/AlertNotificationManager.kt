package com.stockpricealert.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.stockpricealert.MainActivity
import com.stockpricealert.R
import com.stockpricealert.domain.AlertType

class AlertNotificationManager(private val context: Context) {
    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Stock Price Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when stock prices cross your targets"
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
            setSound(null, AudioAttributes.Builder().build())
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun showPriceAlert(
        stockName: String,
        alertType: AlertType,
        targetPrice: Double,
        nsePrice: Double,
        bsePrice: Double?,
        notificationId: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bseText = bsePrice?.let { "₹%.2f".format(it) } ?: "N/A"
        val title = "$stockName crossed ${alertType.name} alert (₹%.2f)".format(targetPrice)
        val text = "NSE: ₹%.2f | BSE: $bseText".format(nsePrice)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$text\nTarget: ₹%.2f".format(targetPrice))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(VIBRATION_PATTERN)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "stock_price_alerts"
        private val VIBRATION_PATTERN = longArrayOf(0, 300, 150, 300)
    }
}
