package com.stockpricealert.ui.list

data class SystemHealthState(
    val notificationsEnabled: Boolean = false,
    val batteryUnrestricted: Boolean = false,
    val lastBackgroundCheckAt: Long? = null,
    val message: String? = null
) {
    val isHealthy: Boolean
        get() = notificationsEnabled && batteryUnrestricted

    val issueKey: String
        get() = "n$notificationsEnabled-b$batteryUnrestricted"

    fun issueSummary(): String {
        val issues = buildList {
            if (!notificationsEnabled) add("Notifications blocked")
            if (!batteryUnrestricted) add("Battery restricted")
        }
        return issues.joinToString(" · ")
    }
}
