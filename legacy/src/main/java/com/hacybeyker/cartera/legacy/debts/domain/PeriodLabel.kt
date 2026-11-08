package com.hacybeyker.cartera.legacy.debts.domain

import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

private val MONTHS = listOf(
    "Enero",
    "Febrero",
    "Marzo",
    "Abril",
    "Mayo",
    "Junio",
    "Julio",
    "Agosto",
    "Septiembre",
    "Octubre",
    "Noviembre",
    "Diciembre"
)

/** "Octubre 2026": los nombres van fijos para que no cambien con la configuración regional del dispositivo. */
fun YearMonth.label(): String = "${MONTHS[monthValue - 1]} $year"

/** "12/11/2026". */
fun LocalDate.label(): String = "%02d/%02d/%d".format(Locale.US, dayOfMonth, monthValue, year)
