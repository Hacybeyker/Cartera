package com.hacybeyker.cartera.legacy.debts.domain

import com.hacybeyker.cartera.legacy.core.domain.Money

/** Los recibos que la persona marcó para pagar y su total exacto. */
class DebtSelection private constructor(val debts: List<Debt>, private val selectedIds: Set<String>) {

    constructor(debts: List<Debt>) : this(debts, emptySet())

    val selected: List<Debt> = debts.filter { it.id in selectedIds }

    val total: Money = selected.total()

    fun isSelected(id: String): Boolean = id in selectedIds

    fun toggle(id: String): DebtSelection {
        if (debts.none { it.id == id }) return this
        val ids = if (id in selectedIds) selectedIds - id else selectedIds + id
        return DebtSelection(debts, ids)
    }
}
