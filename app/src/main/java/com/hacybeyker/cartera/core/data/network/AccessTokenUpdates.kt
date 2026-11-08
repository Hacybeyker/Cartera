package com.hacybeyker.cartera.core.data.network

import kotlinx.coroutines.flow.StateFlow

/** El token vigente y cada cambio: un login, el cierre de sesión o la expiración (`null`). */
interface AccessTokenUpdates {
    val accessTokens: StateFlow<String?>
}
