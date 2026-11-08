package com.hacybeyker.cartera.legacy.core.data

import kotlinx.serialization.json.Json

object LegacyJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
}
