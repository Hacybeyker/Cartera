package com.hacybeyker.cartera.core.data.network

import com.hacybeyker.cartera.core.data.network.dto.AccountDto
import com.hacybeyker.cartera.core.data.network.dto.AccountListDto
import com.hacybeyker.cartera.core.data.network.dto.AppConfigDto
import com.hacybeyker.cartera.core.data.network.dto.BreakdownDto
import com.hacybeyker.cartera.core.data.network.dto.LoginRequestDto
import com.hacybeyker.cartera.core.data.network.dto.LoginResponseDto
import com.hacybeyker.cartera.core.data.network.dto.MeDto
import com.hacybeyker.cartera.core.data.network.dto.MovementDetailDto
import com.hacybeyker.cartera.core.data.network.dto.MovementPageDto
import com.hacybeyker.cartera.core.data.network.dto.TransferRequestDto
import com.hacybeyker.cartera.core.data.network.dto.TransferResponseDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CarteraApi {
    @GET("app/config")
    suspend fun appConfig(): AppConfigDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): LoginResponseDto

    @GET("me")
    suspend fun me(): MeDto

    @GET("accounts")
    suspend fun accounts(): AccountListDto

    @GET("accounts/{accountId}")
    suspend fun account(@Path("accountId") accountId: String): AccountDto

    @GET("movements")
    suspend fun movements(
        @Query("accountId") accountId: String?,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int?
    ): MovementPageDto

    /** Los movimientos como cuerpo crudo, sin pasar por el converter: quien llama decide dónde decodificarlos. */
    @GET("movements")
    suspend fun movementsBody(
        @Query("accountId") accountId: String?,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int?
    ): ResponseBody

    @GET("movements/{movementId}")
    suspend fun movement(@Path("movementId") movementId: String): MovementDetailDto

    @GET("movements/{movementId}/breakdown")
    suspend fun breakdown(@Path("movementId") movementId: String): BreakdownDto

    @POST("transfers")
    suspend fun transfer(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: TransferRequestDto
    ): TransferResponseDto
}
