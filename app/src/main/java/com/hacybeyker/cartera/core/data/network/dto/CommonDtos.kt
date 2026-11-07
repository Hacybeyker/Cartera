package com.hacybeyker.cartera.core.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class MoneyDto(val value: String, val currency: String)

@Serializable
data class ProblemDto(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String,
    val code: String,
    val traceId: String,
    val errors: List<FieldErrorDto> = emptyList()
)

@Serializable
data class FieldErrorDto(val field: String, val code: String, val message: String)
