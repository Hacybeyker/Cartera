package com.hacybeyker.cartera.legacy.debts.domain

import com.hacybeyker.cartera.legacy.core.domain.Money
import com.hacybeyker.cartera.legacy.core.domain.PEN
import java.time.LocalDate
import java.time.YearMonth

data class Debt(val id: String, val period: YearMonth, val dueDate: LocalDate, val amount: Money)

data class CustomerDebts(val customerName: String, val debts: List<Debt>)

/** La suma exacta de los importes; S/ 0.00 si no hay recibos. */
fun List<Debt>.total(): Money = fold(Money.zero(PEN)) { sum, debt -> sum + debt.amount }
