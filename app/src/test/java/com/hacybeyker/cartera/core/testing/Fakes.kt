package com.hacybeyker.cartera.core.testing

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.config.AppConfig
import com.hacybeyker.cartera.core.domain.config.AppConfigRepository
import com.hacybeyker.cartera.core.domain.firstrun.FirstRunRepository
import com.hacybeyker.cartera.core.domain.idempotency.IdempotencyKeyFactory
import com.hacybeyker.cartera.core.domain.session.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

val TestAppConfig = AppConfig(minSupportedVersion = "0.1.0", termsVersion = "2026-11-01")

class FakeAppConfigRepository(var config: AppConfig = TestAppConfig, var error: AppError? = null) :
    AppConfigRepository {
    var refreshes = 0

    override suspend fun refresh(): Outcome<AppConfig> {
        refreshes++
        return error?.let { Outcome.Failure(it) } ?: Outcome.Success(config)
    }

    override suspend fun config(): Outcome<AppConfig> = error?.let { Outcome.Failure(it) } ?: Outcome.Success(config)
}

class FakeFirstRunRepository(
    var acceptedTermsVersion: String? = null,
    var onboardingSeen: Boolean = false,
    var biometricOfferResolved: Boolean = false
) : FirstRunRepository {
    override suspend fun acceptedTermsVersion(): String? = acceptedTermsVersion

    override suspend fun acceptTerms(version: String) {
        acceptedTermsVersion = version
    }

    override suspend fun isOnboardingSeen(): Boolean = onboardingSeen

    override suspend fun markOnboardingSeen() {
        onboardingSeen = true
    }

    override suspend fun isBiometricOfferResolved(): Boolean = biometricOfferResolved

    override suspend fun markBiometricOfferResolved() {
        biometricOfferResolved = true
    }
}

class FakeSessionRepository : SessionRepository {
    var accessToken: String? = null
    private val expired = MutableStateFlow(false)

    override val isExpired: StateFlow<Boolean> = expired

    override fun start(accessToken: String) {
        this.accessToken = accessToken
    }

    override fun end() {
        accessToken = null
    }

    override fun acknowledgeExpiry() {
        expired.value = false
    }

    fun expire() {
        accessToken = null
        expired.value = true
    }
}

/** Claves `key-1`, `key-2`…: una por cada orden válida. */
class SequentialKeys : IdempotencyKeyFactory {
    private var next = 1

    override fun newKey(): String = "key-${next++}"
}
