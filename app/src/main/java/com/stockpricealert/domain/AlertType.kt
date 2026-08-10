package com.stockpricealert.domain

enum class AlertType {
    HIGH,
    LOW;

    companion object {
        fun fromString(value: String): AlertType =
            entries.firstOrNull { it.name == value } ?: HIGH
    }
}
