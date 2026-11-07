package com.hacybeyker.cartera.feature.home

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.home.domain.model.UserProfile
import com.hacybeyker.cartera.feature.home.domain.repository.ProfileRepository

val TestProfile = UserProfile("USR-001", "Ana Torres", "*****678", "ana.torres@cartera.invalid")

class FakeProfileRepository(var profile: UserProfile = TestProfile, var error: AppError? = null) : ProfileRepository {
    var calls = 0

    override suspend fun me(): Outcome<UserProfile> {
        calls++
        return error?.let { Outcome.Failure(it) } ?: Outcome.Success(profile)
    }
}
