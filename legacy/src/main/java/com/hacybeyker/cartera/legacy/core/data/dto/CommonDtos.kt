package com.hacybeyker.cartera.legacy.core.data.dto

import com.hacybeyker.cartera.legacy.core.domain.Money
import java.math.BigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class MoneyDto(val value: String, val currency: String)

/** Un valor mal formado lanza [NumberFormatException]; `LegacyApiCaller` lo traduce a un error del servidor. */
fun MoneyDto.toMoney(): Money = Money(BigDecimal(value), currency)

@Serializable
data class ProblemDto(val detail: String = "", val code: String, val errors: List<FieldErrorDto> = emptyList())

@Serializable
data class FieldErrorDto(val field: String, val code: String, val message: String = "")
