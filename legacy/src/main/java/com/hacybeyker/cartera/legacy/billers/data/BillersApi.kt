package com.hacybeyker.cartera.legacy.billers.data

import com.hacybeyker.cartera.legacy.billers.data.dto.BillerListDto
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface BillersApi {
    @GET("billers")
    fun billers(@Query("query") query: String): Single<BillerListDto>
}
