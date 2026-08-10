package com.stockpricealert.util

data class BackgroundCheckResult(
    val completedAt: Long,
    val forceRun: Boolean,
    val status: String,
    val message: String,
    val watchersChecked: Int
) {
    companion object {
        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_SKIPPED = "SKIPPED"
        const val STATUS_FAILED = "FAILED"
    }
}
