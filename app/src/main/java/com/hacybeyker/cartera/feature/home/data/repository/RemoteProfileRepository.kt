package com.hacybeyker.cartera.feature.home.data.repository

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraApi
import com.hacybeyker.cartera.core.data.network.toOutcome
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.home.domain.model.UserProfile
import com.hacybeyker.cartera.feature.home.domain.repository.ProfileRepository
import javax.inject.Inject

class RemoteProfileRepository @Inject constructor(private val api: CarteraApi, private val call: ApiCaller) :
    ProfileRepository {

    override suspend fun me(): Outcome<UserProfile> = call { api.me() }.toOutcome {
        UserProfile(id = it.id, fullName = it.fullName, documentMasked = it.documentMasked, email = it.email)
    }
}
