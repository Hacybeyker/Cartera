package com.hacybeyker.cartera.legacy.debts.data

import com.hacybeyker.cartera.legacy.debts.data.dto.CustomerDebtsDto
import com.hacybeyker.cartera.legacy.debts.data.dto.toCustomerDebts
import com.hacybeyker.cartera.legacy.debts.domain.CustomerDebts
import kotlinx.serialization.json.Json

fun interface DebtsBodyDecoder {
    /** Un cuerpo o un valor mal formado lanza una excepción que `LegacyApiCaller` traduce a un error del servidor. */
    fun decode(body: String): CustomerDebts
}

class JsonDebtsBodyDecoder(private val json: Json) : DebtsBodyDecoder {
    override fun decode(body: String): CustomerDebts =
        json.decodeFromString(CustomerDebtsDto.serializer(), body).toCustomerDebts()
}
