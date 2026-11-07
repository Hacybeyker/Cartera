package com.hacybeyker.cartera.feature.login.domain.usecase

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.entry.EntryStep
import com.hacybeyker.cartera.core.domain.entry.ResolveEntryStepUseCase
import com.hacybeyker.cartera.core.domain.flatMap
import com.hacybeyker.cartera.core.domain.session.SessionRepository
import com.hacybeyker.cartera.feature.login.domain.model.Credentials
import com.hacybeyker.cartera.feature.login.domain.repository.AuthRepository
import javax.inject.Inject

/** Inicia la sesión en memoria y dice a qué pantalla seguir. */
class LogInUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val session: SessionRepository,
    private val resolveEntryStep: ResolveEntryStepUseCase
) {
    suspend operator fun invoke(credentials: Credentials): Outcome<EntryStep> =
        auth.logIn(credentials).flatMap { authSession ->
            session.start(authSession.accessToken)
            resolveEntryStep()
        }
}
