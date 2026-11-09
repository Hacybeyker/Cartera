package com.hacybeyker.cartera.legacy.testing

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/** Lee un fixture de `api/fixtures`, la misma fuente de datos que sirve el backend embebido. */
fun fixture(path: String): String = File(checkNotNull(System.getProperty("cartera.fixturesDir")), path).readText()

/** El cuerpo de Luz del Norte para el cliente 100200 con su histórico de facturación, como lo sirve el backend. */
fun luzBodyWithHistory(): String {
    val customer = Json.parseToJsonElement(fixture("debts/BIL-LUZ.json")).jsonObject
        .getValue("customers").jsonArray.first().jsonObject
    return buildJsonObject {
        put("customerName", customer.getValue("name"))
        put("debts", customer.getValue("debts"))
        put("history", customer.getValue("history"))
    }.toString()
}
