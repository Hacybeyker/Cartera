package com.hacybeyker.cartera.feature.home.domain.usecase

import com.hacybeyker.cartera.core.domain.session.SessionRepository
import javax.inject.Inject

/** Descarta el token en memoria. Términos y onboarding quedan guardados: el próximo login no los repite. */
class LogOutUseCase @Inject constructor(private val session: SessionRepository) {
    operator fun invoke() = session.end()
}
