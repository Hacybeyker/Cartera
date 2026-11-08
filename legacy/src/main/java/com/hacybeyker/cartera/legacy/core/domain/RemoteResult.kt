package com.hacybeyker.cartera.legacy.core.domain

import io.reactivex.rxjava3.core.Single

sealed interface RemoteResult<out T> {
    data class Success<T>(val value: T) : RemoteResult<T>

    data class Failure(val error: LegacyError) : RemoteResult<Nothing>
}

/**
 * Convierte el error traducido en un valor, así el flujo de un Presenter sobrevive a un fallo. Cualquier otra
 * excepción es un error de programación y sigue su camino.
 */
fun <T : Any> Single<T>.toResult(): Single<RemoteResult<T>> = map<RemoteResult<T>> { RemoteResult.Success(it) }
    .onErrorResumeNext { error ->
        if (error is LegacyException) Single.just(RemoteResult.Failure(error.error)) else Single.error(error)
    }
