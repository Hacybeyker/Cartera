package com.hacybeyker.cartera.legacy.debts.data

import com.hacybeyker.cartera.legacy.debts.data.dto.CustomerDebtsDto
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DebtsApi {
    @GET("billers/{billerId}/debts")
    fun debts(@Path("billerId") billerId: String, @Query("customerCode") customerCode: String): Single<CustomerDebtsDto>
}
