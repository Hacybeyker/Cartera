package com.hacybeyker.cartera.demo.backend

enum class Endpoint(val id: String, val method: String, path: String, val authenticated: Boolean) {
    APP_CONFIG("app_config", "GET", "app/config", authenticated = false),
    LOGIN("login", "POST", "auth/login", authenticated = false),
    ME("me", "GET", "me", authenticated = true),
    ACCOUNTS("accounts", "GET", "accounts", authenticated = true),
    ACCOUNT("account", "GET", "accounts/{id}", authenticated = true),
    MOVEMENTS("movements", "GET", "movements", authenticated = true),
    MOVEMENT("movement", "GET", "movements/{id}", authenticated = true),
    BREAKDOWN("breakdown", "GET", "movements/{id}/breakdown", authenticated = true),
    TRANSFERS("transfers", "POST", "transfers", authenticated = true),
    DEMO_RESET("demo_reset", "POST", "_demo/reset", authenticated = false);

    private val segments = path.split('/')

    private fun pathParameter(method: String, requested: List<String>): String? {
        if (method != this.method || requested.size != segments.size) return null
        val matches = segments.zip(requested).all { (expected, actual) -> expected == PARAMETER || expected == actual }
        return if (matches) segments.indexOf(PARAMETER).let { requested.getOrNull(it).orEmpty() } else null
    }

    companion object {
        const val BASE_PATH = "/api/v1/"
        private const val PARAMETER = "{id}"

        fun resolve(method: String, path: String): Match? {
            if (!path.startsWith(BASE_PATH)) return null
            val requested = path.removePrefix(BASE_PATH).trimEnd('/').split('/')
            return entries.firstNotNullOfOrNull { endpoint ->
                endpoint.pathParameter(method, requested)?.let { Match(endpoint, it) }
            }
        }
    }

    data class Match(val endpoint: Endpoint, val pathParameter: String)
}
