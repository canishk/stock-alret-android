package com.stockpricealert.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BullBlue,
    onPrimary = OnBullBlue,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = BearRed,
    onSecondary = OnBearRed,
    secondaryContainer = Color(0xFFFFCDD2),
    onSecondaryContainer = Color(0xFF8E0000),
    background = AppBackground,
    onBackground = Color(0xFF1A1A2E),
    surface = AppSurface,
    onSurface = Color(0xFF1A1A2E),
    onSurfaceVariant = Color(0xFF5C6B7A),
    error = BearRed,
    onError = Color.White
)

@Composable
fun StockPriceAlertTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
