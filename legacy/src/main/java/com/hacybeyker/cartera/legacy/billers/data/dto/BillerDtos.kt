package com.hacybeyker.cartera.legacy.billers.data.dto

import com.hacybeyker.cartera.legacy.billers.domain.Biller
import kotlinx.serialization.Serializable

@Serializable
data class BillerDto(val id: String, val name: String, val category: String, val customerCodeFormat: String)

@Serializable
data class BillerListDto(val items: List<BillerDto>)

fun BillerDto.toBiller(): Biller = Biller(id, name, category, customerCodeFormat)
