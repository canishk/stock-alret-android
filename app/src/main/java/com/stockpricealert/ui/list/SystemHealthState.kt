package com.stockpricealert.ui.list

import com.stockpricealert.util.BackgroundCheckResult

enum class BackgroundJobState {
    Idle,
    Queued,
    Running,
    Succeeded,
    Failed
}

data class SystemHealthState(
    val notificationsEnabled: Boolean = false,
    val batteryUnrestricted: Boolean = false,
    val lastBackgroundResult: BackgroundCheckResult? = null,
    val isBackgroundCheckRunning: Boolean = false,
    val backgroundJobState: BackgroundJobState = BackgroundJobState.Idle,
    val message: String? = null
) {
    val isHealthy: Boolean
        get() = notificationsEnabled && batteryUnrestricted

    val issueKey: String
        get() = "n$notificationsEnabled-b$batteryUnrestricted"

    val lastBackgroundCheckAt: Long?
        get() = lastBackgroundResult?.completedAt

    fun issueSummary(): String {
        val issues = buildList {
            if (!notificationsEnabled) add("Notifications blocked")
            if (!batteryUnrestricted) add("Battery restricted")
        }
        return issues.joinToString(" · ")
    }
}
