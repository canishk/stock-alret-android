package com.stockpricealert.data.repository

data class StockQuote(
    val nsePrice: Double,
    val bsePrice: Double?,
    val companyName: String?
)
