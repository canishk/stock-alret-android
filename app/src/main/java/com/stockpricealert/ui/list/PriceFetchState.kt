package com.stockpricealert.ui.list

data class PriceFetchState(
    val isLoading: Boolean = false,
    val nsePrice: Double? = null,
    val bsePrice: Double? = null,
    val error: String? = null
)
