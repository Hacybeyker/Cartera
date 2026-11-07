package com.hacybeyker.cartera.demo.backend

import kotlinx.serialization.json.jsonObject

class BackendRouter(
    private val state: BackendState,
    private val sessions: SessionDesk,
    private val transfers: TransferDesk,
    private val onReset: () -> Unit = {}
) {

    fun route(request: BackendRequest): BackendResponse = try {
        val match = Endpoint.resolve(request.method, request.path)
            ?: throw ProblemException(ProblemCode.NOT_FOUND, "No existe ${request.method} ${request.path}.")
        val userId = if (match.endpoint.authenticated) sessions.authenticate(request) else ""
        handle(match, userId, request)
    } catch (problem: ProblemException) {
        problem(problem.code, problem.detail, request.requestId, problem.errors)
    }

    private fun handle(match: Endpoint.Match, userId: String, request: BackendRequest): BackendResponse {
        val id = match.pathParameter
        return when (match.endpoint) {
            Endpoint.APP_CONFIG -> json(OK, state.appConfig)
            Endpoint.LOGIN -> sessions.login(request)
            Endpoint.ME -> json(OK, ResponseViews.profile(state.user(userId) ?: notFound("el usuario")))
            Endpoint.ACCOUNTS -> json(OK, ResponseViews.accounts(state.accountsOf(userId)))
            Endpoint.ACCOUNT -> json(OK, ResponseViews.account(state.account(userId, id) ?: notFound("la cuenta $id")))
            Endpoint.MOVEMENTS -> json(OK, ResponseViews.movementPage(movements(userId, request.query)))
            Endpoint.MOVEMENT -> json(OK, ResponseViews.movementDetail(movement(userId, id)))
            Endpoint.BREAKDOWN -> json(OK, movement(userId, id).json.getValue("breakdown").jsonObject)
            Endpoint.TRANSFERS ->
                transfers.transfer(userId, request.header(TransferDesk.IDEMPOTENCY_HEADER), request.jsonBody())
            Endpoint.DEMO_RESET -> reset()
        }
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

    private fun reset(): BackendResponse {
        state.reset()
        onReset()
        return BackendResponse(status = NO_CONTENT)
    }

    private fun notFound(what: String): Nothing = throw ProblemException(ProblemCode.NOT_FOUND, "No se encontró $what.")

    private companion object {
        const val OK = 200
        const val NO_CONTENT = 204
    }
}
