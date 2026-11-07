package com.hacybeyker.cartera.feature.home.domain.usecase

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.home.domain.model.UserProfile
import com.hacybeyker.cartera.feature.home.domain.repository.ProfileRepository
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(private val profile: ProfileRepository) {
    suspend operator fun invoke(): Outcome<UserProfile> = profile.me()
}
