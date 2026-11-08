package com.hacybeyker.cartera.demo.backend

import java.time.Clock
import kotlinx.serialization.json.jsonObject

/** Las mesas que atienden cada grupo de endpoints, sobre el mismo estado. */
class BackendDesks(
    val sessions: SessionDesk,
    val transfers: TransferDesk,
    val market: MarketDesk,
    val orders: OrderDesk,
    val fx: FxDesk,
    val biometrics: BiometricDesk
) {
    companion object {
        fun create(state: BackendState, jwt: JwtSigner, clock: Clock): BackendDesks {
            val sessions = SessionDesk(state, jwt)
            return BackendDesks(
                sessions = sessions,
                transfers = TransferDesk(state, clock),
                market = MarketDesk(state, clock),
                orders = OrderDesk(state, clock),
                fx = FxDesk(state, clock),
                biometrics = BiometricDesk(state, sessions)
            )
        }
    }
}

class BackendRouter(
    private val state: BackendState,
    private val desks: BackendDesks,
    private val onReset: () -> Unit = {}
) {
    private class Call(val userId: String, val id: String, val request: BackendRequest) {
        val idempotencyKey: String? get() = request.header(TransferDesk.IDEMPOTENCY_HEADER)
    }

    // Un mapa en vez de un when: cada endpoint nuevo suma una línea y no complejidad.
    private val routes: Map<Endpoint, (Call) -> BackendResponse> = mapOf(
        Endpoint.APP_CONFIG to { _ -> json(OK, state.appConfig) },
        Endpoint.LOGIN to { call -> desks.sessions.login(call.request) },
        Endpoint.ME to { call -> json(OK, ResponseViews.profile(state.user(call.userId) ?: notFound("el usuario"))) },
        Endpoint.ACCOUNTS to { call -> json(OK, ResponseViews.accounts(state.accountsOf(call.userId))) },
        Endpoint.ACCOUNT to { call ->
            json(OK, ResponseViews.account(state.account(call.userId, call.id) ?: notFound("la cuenta ${call.id}")))
        },
        Endpoint.MOVEMENTS to { call ->
            json(OK, ResponseViews.movementPage(movements(call.userId, call.request.query)))
        },
        Endpoint.MOVEMENT to { call -> json(OK, ResponseViews.movementDetail(movement(call.userId, call.id))) },
        Endpoint.BREAKDOWN to { call ->
            json(OK, movement(call.userId, call.id).json.getValue("breakdown").jsonObject)
        },
        Endpoint.TRANSFERS to { call ->
            desks.transfers.transfer(call.userId, call.idempotencyKey, call.request.jsonBody())
        },
        Endpoint.MARKET_STOCKS to { _ -> desks.market.stocks() },
        Endpoint.MARKET_STOCK to { call -> desks.market.stock(call.id) },
        Endpoint.MARKET_HISTORY to { call -> desks.market.history(call.id, call.request.query) },
        Endpoint.MARKET_STREAM to { call -> streamHandshake(call.request) },
        Endpoint.PORTFOLIO to { call -> desks.market.portfolio(call.userId) },
        Endpoint.ORDERS to { call -> desks.orders.order(call.userId, call.idempotencyKey, call.request.jsonBody()) },
        Endpoint.FX_RATES to { call -> desks.fx.rates(call.userId, call.request) },
        Endpoint.FX_EXCHANGES to { call ->
            desks.fx.exchange(call.userId, call.idempotencyKey, call.request.jsonBody())
        },
        Endpoint.BIOMETRIC_ENROLL to { call -> desks.biometrics.enroll(call.userId) },
        Endpoint.BIOMETRIC_LOGIN to { call -> desks.biometrics.login(call.request) },
        Endpoint.BIOMETRIC_DELETE to { call -> desks.biometrics.revoke(call.userId) },
        Endpoint.DEMO_RESET to { _ -> reset() }
    )

    fun route(request: BackendRequest): BackendResponse = try {
        val match = Endpoint.resolve(request.method, request.path)
            ?: throw ProblemException(ProblemCode.NOT_FOUND, "No existe ${request.method} ${request.path}.")
        val userId = if (match.endpoint.authenticated) desks.sessions.authenticate(request) else ""
        routes.getValue(match.endpoint)(Call(userId, match.pathParameter, request))
    } catch (problem: ProblemException) {
        problem(problem.code, problem.detail, request.requestId, problem.errors, extensions = problem.extensions)
    }

    private fun movements(userId: String, query: Map<String, String>): Page {
        val ownAccounts = state.accountsOf(userId).map { it.string("id") }
        val accountId = query["accountId"]
        if (accountId != null && accountId !in ownAccounts) {
            throw validationProblem("accountId", "NOT_OWNED", "La cuenta no existe o no es tuya.")
        }
        val limit = query["limit"]?.let { raw ->
            raw.toIntOrNull()?.takeIf { it in 1..CursorPager.MAX_LIMIT }
                ?: throw validationProblem("limit", "OUT_OF_RANGE", "Usa un valor entre 1 y ${CursorPager.MAX_LIMIT}.")
        } ?: CursorPager.DEFAULT_LIMIT
        val records = state.movements(accountId?.let(::listOf) ?: ownAccounts)
        return CursorPager.page(records, query["cursor"]?.takeIf { it.isNotEmpty() }, limit)
    }

    private fun movement(userId: String, movementId: String): MovementRecord {
        val ownAccounts = state.accountsOf(userId).map { it.string("id") }
        return state.movements(ownAccounts).firstOrNull { it.id == movementId } ?: notFound("el movimiento $movementId")
    }

    // El 101 solo marca que el handshake pasó la sesión: CarteraBackend hace el upgrade y MarketStream lo atiende.
    private fun streamHandshake(request: BackendRequest): BackendResponse {
        val upgrade = request.header("Connection").equals("Upgrade", ignoreCase = true) &&
            request.header("Upgrade").equals("websocket", ignoreCase = true)
        if (!upgrade) {
            throw validationProblem("Upgrade", "REQUIRED", "El canal en vivo se abre con un handshake WebSocket.")
        }
        return BackendResponse(status = SWITCHING_PROTOCOLS)
    }

    private fun reset(): BackendResponse {
        state.reset()
        onReset()
        return BackendResponse(status = NO_CONTENT)
    }

    private fun notFound(what: String): Nothing = throw ProblemException(ProblemCode.NOT_FOUND, "No se encontró $what.")

    companion object {
        const val SWITCHING_PROTOCOLS = 101
        private const val OK = 200
        private const val NO_CONTENT = 204
    }
}
