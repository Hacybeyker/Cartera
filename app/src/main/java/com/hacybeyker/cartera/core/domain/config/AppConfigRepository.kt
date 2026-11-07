package com.hacybeyker.cartera.core.domain.config

import com.hacybeyker.cartera.core.domain.Outcome

interface AppConfigRepository {
    /** Pide la configuración al servidor y la recuerda. */
    suspend fun refresh(): Outcome<AppConfig>

    /** La última configuración conocida; si no hay, la pide. */
    suspend fun config(): Outcome<AppConfig>
}
