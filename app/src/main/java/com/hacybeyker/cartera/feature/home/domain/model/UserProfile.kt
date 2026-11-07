package com.hacybeyker.cartera.feature.home.domain.model

data class UserProfile(val id: String, val fullName: String, val documentMasked: String, val email: String) {
    val firstName: String get() = fullName.trim().substringBefore(' ')
}
