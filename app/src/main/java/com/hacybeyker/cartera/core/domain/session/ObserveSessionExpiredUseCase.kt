package com.hacybeyker.cartera.core.domain.session

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSessionExpiredUseCase @Inject constructor(private val session: SessionRepository) {
    operator fun invoke(): Flow<Boolean> = session.isExpired
}
