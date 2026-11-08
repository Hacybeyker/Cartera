package com.hacybeyker.cartera.core.data.network

import com.hacybeyker.cartera.core.data.network.dto.OrderRequestDto
import com.hacybeyker.cartera.core.data.network.dto.OrderResponseDto
import com.hacybeyker.cartera.core.data.network.dto.PortfolioDto
import com.hacybeyker.cartera.core.data.network.dto.PriceHistoryDto
import com.hacybeyker.cartera.core.data.network.dto.StockDetailDto
import com.hacybeyker.cartera.core.data.network.dto.StockListDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Mercado, portafolio y órdenes: la misma API y el mismo cliente que [CarteraApi], en su propia interfaz. */
interface MarketApi {
    @GET("market/stocks")
    suspend fun stocks(): StockListDto

    @GET("market/stocks/{symbol}")
    suspend fun stock(@Path("symbol") symbol: String): StockDetailDto

    @GET("market/stocks/{symbol}/history")
    suspend fun stockHistory(@Path("symbol") symbol: String, @Query("range") range: String): PriceHistoryDto

    @GET("portfolio")
    suspend fun portfolio(): PortfolioDto

    @POST("orders")
    suspend fun order(@Header("Idempotency-Key") idempotencyKey: String, @Body body: OrderRequestDto): OrderResponseDto
}
