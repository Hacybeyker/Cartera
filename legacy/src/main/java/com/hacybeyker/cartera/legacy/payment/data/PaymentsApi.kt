package com.hacybeyker.cartera.legacy.payment.data

import com.hacybeyker.cartera.legacy.payment.data.dto.BillPaymentDto
import com.hacybeyker.cartera.legacy.payment.data.dto.BillPaymentRequestDto
import io.reactivex.rxjava3.core.Single
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface PaymentsApi {
    @POST("bill-payments")
    fun pay(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: BillPaymentRequestDto
    ): Single<BillPaymentDto>

    @Headers("Accept: text/html")
    @GET("bill-payments/{paymentId}/voucher")
    fun voucher(@Path("paymentId") paymentId: String): Single<ResponseBody>
}
