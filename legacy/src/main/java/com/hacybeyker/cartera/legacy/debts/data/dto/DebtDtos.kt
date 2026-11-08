package com.hacybeyker.cartera.legacy.debts.data.dto

import com.hacybeyker.cartera.legacy.core.data.dto.MoneyDto
import com.hacybeyker.cartera.legacy.core.data.dto.toMoney
import com.hacybeyker.cartera.legacy.debts.domain.CustomerDebts
import com.hacybeyker.cartera.legacy.debts.domain.Debt
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.serialization.Serializable

@Serializable
data class DebtDto(val id: String, val period: String, val dueDate: String, val amount: MoneyDto)

@Serializable
data class CustomerDebtsDto(val customerName: String, val debts: List<DebtDto>)

fun DebtDto.toDebt(): Debt = Debt(id, YearMonth.parse(period), LocalDate.parse(dueDate), amount.toMoney())

fun CustomerDebtsDto.toCustomerDebts(): CustomerDebts = CustomerDebts(customerName, debts.map { it.toDebt() })
