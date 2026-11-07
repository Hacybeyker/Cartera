package com.hacybeyker.cartera.core.domain.session

import javax.inject.Inject

class AcknowledgeSessionExpiryUseCase @Inject constructor(private val session: SessionRepository) {
    operator fun invoke() = session.acknowledgeExpiry()
}
