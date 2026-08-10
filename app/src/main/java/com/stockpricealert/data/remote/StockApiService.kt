package com.stockpricealert.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface StockApiService {
    @GET("stock")
    suspend fun getStock(@Query("name") name: String): StockPriceResponse
}
