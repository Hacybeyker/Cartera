package com.hacybeyker.cartera.legacy.testing

import com.hacybeyker.cartera.legacy.account.domain.Account
import com.hacybeyker.cartera.legacy.billers.data.dto.BillerDto
import com.hacybeyker.cartera.legacy.billers.data.dto.toBiller
import com.hacybeyker.cartera.legacy.billers.domain.Biller
import com.hacybeyker.cartera.legacy.core.data.LegacyJson
import com.hacybeyker.cartera.legacy.core.domain.Money
import com.hacybeyker.cartera.legacy.core.domain.PEN
import com.hacybeyker.cartera.legacy.debts.domain.CustomerDebts
import com.hacybeyker.cartera.legacy.debts.domain.Debt
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.serialization.builtins.ListSerializer

fun pen(value: String): Money = Money(BigDecimal(value), PEN)

object Samples {
    val luz = Biller("BIL-LUZ", "Luz del Norte", "Electricidad", "^\\d{6}$")
    val electroSierra = Biller("BIL-ESI", "Electro Sierra", "Electricidad", "^\\d{8}$")
    val agua = Biller("BIL-AGUA", "Agua Trujillo", "Agua", "^\\d{6}$")
    val telefonia = Biller("BIL-TEL", "Telefonía Andina", "Telefonía", "^\\d{6}$")

    val billers = listOf(luz, electroSierra, agua, telefonia)

    val octubre = Debt("DEB-LUZ-202610", YearMonth.of(2026, 10), LocalDate.of(2026, 11, 12), pen("85.40"))
    val noviembre = Debt("DEB-LUZ-202611", YearMonth.of(2026, 11), LocalDate.of(2026, 12, 12), pen("92.10"))

    val luzDebts = CustomerDebts("Ana Torres", listOf(octubre, noviembre))

    val sueldo = Account("ACC-001", "Cuenta Sueldo", "****4821", pen("8450.75"))
    val gastos = Account("ACC-002", "Cuenta Gastos", "****9034", pen("2300.00"))
    val dolares = Account("ACC-003", "Cuenta Dólares", "****1177", Money(BigDecimal("1050.20"), "USD"))
    val poca = Account("ACC-004", "Cuenta Chica", "****0001", pen("50.00"))
}

/** Las 20 empresas de `api/fixtures/billers.json`, en el orden del servidor. */
fun fixtureBillers(): List<Biller> = LegacyJson.instance
    .decodeFromString(ListSerializer(BillerDto.serializer()), fixture("billers.json"))
    .map { it.toBiller() }
