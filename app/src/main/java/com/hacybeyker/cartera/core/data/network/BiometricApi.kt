package com.hacybeyker.cartera.core.data.network

import com.hacybeyker.cartera.core.data.network.dto.BiometricEnrollResponseDto
import com.hacybeyker.cartera.core.data.network.dto.BiometricLoginRequestDto
import com.hacybeyker.cartera.core.data.network.dto.LoginResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

/** Ingreso con huella: la misma API y el mismo cliente que [CarteraApi], en su propia interfaz. */
interface BiometricApi {
    @POST("auth/biometric/enroll")
    suspend fun enroll(): BiometricEnrollResponseDto

    @POST("auth/biometric/login")
    suspend fun login(@Body body: BiometricLoginRequestDto): LoginResponseDto

    /** 204 sin cuerpo: quien llama revisa el código. */
    @DELETE("auth/biometric")
    suspend fun delete(): Response<Unit>
}
