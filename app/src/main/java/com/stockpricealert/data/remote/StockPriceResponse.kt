package com.stockpricealert.data.remote

import com.squareup.moshi.Json

data class StockPriceResponse(
    @Json(name = "companyName") val companyName: String? = null,
    @Json(name = "currentPrice") val currentPrice: CurrentPrice? = null
)

data class CurrentPrice(
    @Json(name = "NSE") val nse: String? = null,
    @Json(name = "BSE") val bse: String? = null
)
