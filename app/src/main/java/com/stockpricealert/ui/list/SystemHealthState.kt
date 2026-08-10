package com.stockpricealert.ui.list

data class SystemHealthState(
    val notificationsEnabled: Boolean = false,
    val batteryUnrestricted: Boolean = false,
    val lastBackgroundCheckAt: Long? = null,
    val message: String? = null
)
