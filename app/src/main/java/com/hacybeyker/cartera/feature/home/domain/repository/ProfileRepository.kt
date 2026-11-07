package com.hacybeyker.cartera.feature.home.domain.repository

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.home.domain.model.UserProfile

interface ProfileRepository {
    suspend fun me(): Outcome<UserProfile>
}
