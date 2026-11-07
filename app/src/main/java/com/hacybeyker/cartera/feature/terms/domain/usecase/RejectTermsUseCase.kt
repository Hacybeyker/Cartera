package com.hacybeyker.cartera.feature.terms.domain.usecase

import com.hacybeyker.cartera.core.domain.session.SessionRepository
import javax.inject.Inject

/** Sin términos aceptados no hay sesión: se descarta el token y se vuelve al login. */
class RejectTermsUseCase @Inject constructor(private val session: SessionRepository) {
    operator fun invoke() = session.end()
}
