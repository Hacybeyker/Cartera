package com.hacybeyker.cartera.core.data.firstrun

import android.content.SharedPreferences
import androidx.core.content.edit
import com.hacybeyker.cartera.core.common.IoDispatcher
import com.hacybeyker.cartera.core.domain.firstrun.FirstRunRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class SharedPreferencesFirstRunRepository @Inject constructor(
    private val preferences: SharedPreferences,
    @IoDispatcher private val io: CoroutineDispatcher
) : FirstRunRepository {

    override suspend fun acceptedTermsVersion(): String? = withContext(io) {
        preferences.getString(KEY_ACCEPTED_TERMS_VERSION, null)
    }

    override suspend fun acceptTerms(version: String) = withContext(io) {
        preferences.edit { putString(KEY_ACCEPTED_TERMS_VERSION, version) }
    }

    override suspend fun isOnboardingSeen(): Boolean = withContext(io) {
        preferences.getBoolean(KEY_ONBOARDING_SEEN, false)
    }

    override suspend fun markOnboardingSeen() = withContext(io) {
        preferences.edit { putBoolean(KEY_ONBOARDING_SEEN, true) }
    }

    override suspend fun isBiometricOfferResolved(): Boolean = withContext(io) {
        preferences.getBoolean(KEY_BIOMETRIC_OFFER_RESOLVED, false)
    }

    override suspend fun markBiometricOfferResolved() = withContext(io) {
        preferences.edit { putBoolean(KEY_BIOMETRIC_OFFER_RESOLVED, true) }
    }

    companion object {
        const val FILE_NAME = "cartera_first_run"
        private const val KEY_ACCEPTED_TERMS_VERSION = "accepted_terms_version"
        private const val KEY_ONBOARDING_SEEN = "onboarding_seen"
        private const val KEY_BIOMETRIC_OFFER_RESOLVED = "biometric_offer_resolved"
    }
}
