package com.hacybeyker.cartera.feature.biometric.domain.usecase

import com.hacybeyker.cartera.core.domain.firstrun.FirstRunRepository
import javax.inject.Inject

/** Omitir resuelve la oferta: no se vuelve a mostrar, y el ingreso con huella queda disponible en Seguridad. */
class SkipBiometricOfferUseCase @Inject constructor(private val firstRun: FirstRunRepository) {
    suspend operator fun invoke() = firstRun.markBiometricOfferResolved()
}
