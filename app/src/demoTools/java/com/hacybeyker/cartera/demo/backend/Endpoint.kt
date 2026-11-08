package com.hacybeyker.cartera.demo.backend

enum class Endpoint(val id: String, val method: String, val path: String, val authenticated: Boolean) {
    APP_CONFIG("app_config", "GET", "app/config", authenticated = false),
    LOGIN("login", "POST", "auth/login", authenticated = false),
    ME("me", "GET", "me", authenticated = true),
    ACCOUNTS("accounts", "GET", "accounts", authenticated = true),
    ACCOUNT("account", "GET", "accounts/{id}", authenticated = true),
    MOVEMENTS("movements", "GET", "movements", authenticated = true),
    MOVEMENT("movement", "GET", "movements/{id}", authenticated = true),
    BREAKDOWN("breakdown", "GET", "movements/{id}/breakdown", authenticated = true),
    TRANSFERS("transfers", "POST", "transfers", authenticated = true),
    MARKET_STOCKS("market_stocks", "GET", "market/stocks", authenticated = true),
    MARKET_STOCK("market_stock", "GET", "market/stocks/{id}", authenticated = true),
    MARKET_HISTORY("market_history", "GET", "market/stocks/{id}/history", authenticated = true),
    MARKET_STREAM("market_stream", "GET", "market/stream", authenticated = true),
    PORTFOLIO("portfolio", "GET", "portfolio", authenticated = true),
    ORDERS("orders", "POST", "orders", authenticated = true),
    FX_RATES("fx_rates", "GET", "fx/rates", authenticated = true),
    FX_EXCHANGES("fx_exchanges", "POST", "fx/exchanges", authenticated = true),
    BILLERS("billers", "GET", "billers", authenticated = true),
    BILLER_DEBTS("biller_debts", "GET", "billers/{id}/debts", authenticated = true),
    BILL_PAYMENTS("bill_payments", "POST", "bill-payments", authenticated = true),
    BILL_VOUCHER("bill_voucher", "GET", "bill-payments/{id}/voucher", authenticated = true),
    BIOMETRIC_ENROLL("biometric_enroll", "POST", "auth/biometric/enroll", authenticated = true),
    BIOMETRIC_LOGIN("biometric_login", "POST", "auth/biometric/login", authenticated = false),
    BIOMETRIC_DELETE("biometric_delete", "DELETE", "auth/biometric", authenticated = true),
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
