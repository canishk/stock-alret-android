package com.stockpricealert

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.stockpricealert.ui.navigation.AppNavGraph
import com.stockpricealert.ui.theme.StockPriceAlertTheme
import com.stockpricealert.worker.WorkScheduler

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        val app = application as StockAlertApp

        setContent {
            StockPriceAlertTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(
                        repository = app.repository,
                        onDataChanged = { WorkScheduler.schedule(this@MainActivity) }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
