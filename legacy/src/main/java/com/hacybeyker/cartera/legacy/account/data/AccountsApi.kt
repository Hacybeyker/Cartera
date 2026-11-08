package com.hacybeyker.cartera.legacy.account.data

import com.hacybeyker.cartera.legacy.account.data.dto.AccountListDto
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET

interface AccountsApi {
    @GET("accounts")
    fun accounts(): Single<AccountListDto>
}
