package com.hacybeyker.cartera.core.ui.format

import com.hacybeyker.cartera.core.domain.Money
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Formato fijo, como en los datos de referencia ("S/ 8,450.75"): no depende del idioma del dispositivo, así los
// flows de E2E y los goldens leen lo mismo en cualquier equipo.
private val currencySymbols = mapOf("PEN" to "S/", "USD" to "US$")
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT)

private fun amountFormat() = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

/** Saldo o monto sin signo: `S/ 8,450.75`. */
fun Money.formatted(): String = "${symbol()} ${amountFormat().format(amount.abs())}"

/** Monto de un movimiento con su signo: `-S/ 1,379.23` o `+US$ 201.29`. */
fun Money.formattedSigned(): String = amount.sign() + formatted()

/** Variación de un precio con su signo y 2 decimales, sin moneda: `+0.34`, `-1,250.00` o `0.00`. */
fun BigDecimal.formattedChange(): String = sign() + amountFormat().format(abs())

/** Porcentaje con su signo y 2 decimales: `+0.81 %`, `-1.20 %` o `0.00 %`. */
fun BigDecimal.formattedPercent(): String = "${formattedChange()} %"

/** Cantidad entera con separador de miles: `1,250,300`. */
fun Long.formattedCount(): String = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US)).format(this)

private fun BigDecimal.sign(): String = when (signum()) {
    -1 -> "-"
    1 -> "+"
    else -> ""
}

fun OffsetDateTime.formatted(): String = format(dateFormatter)

private fun Money.symbol(): String = currencySymbols[currency] ?: currency
