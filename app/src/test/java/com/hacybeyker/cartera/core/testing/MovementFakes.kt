package com.hacybeyker.cartera.core.testing

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.Page
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.domain.account.AccountChanges
import com.hacybeyker.cartera.core.domain.account.AccountRepository
import com.hacybeyker.cartera.core.domain.movement.Movement
import com.hacybeyker.cartera.core.domain.movement.MovementCategory
import com.hacybeyker.cartera.core.domain.movement.MovementRepository
import java.time.OffsetDateTime

fun pen(value: String) = Money(value.toBigDecimal(), "PEN")

val TestAccounts = listOf(
    Account("ACC-001", "Cuenta Sueldo", "SAVINGS", "PEN", "****4821", pen("8450.75"), pen("8450.75")),
    Account("ACC-002", "Cuenta Gastos", "CHECKING", "PEN", "****9034", pen("2300.00"), pen("2300.00"))
)

fun testMovement(number: Int, accountId: String = "ACC-002") = Movement(
    id = "MOV-%s-%04d".format(accountId.takeLast(3), number),
    accountId = accountId,
    date = OffsetDateTime.parse("2026-11-06T14:32:00-05:00").minusHours(number.toLong()),
    description = "Movimiento $number",
    category = MovementCategory.GROCERIES,
    amount = pen("-$number.00")
)

class FakeAccountRepository(var accounts: List<Account> = TestAccounts, var error: AppError? = null) :
    AccountRepository {
    var accountsCalls = 0

    override suspend fun accounts(): Outcome<List<Account>> {
        accountsCalls++
        return error?.let { Outcome.Failure(it) } ?: Outcome.Success(accounts)
    }

    override suspend fun account(accountId: String): Outcome<Account> {
        error?.let { return Outcome.Failure(it) }
        return accounts.firstOrNull { it.id == accountId }?.let { Outcome.Success(it) }
            ?: Outcome.Failure(AppError.NotFound)
    }
}

/** Pagina como el backend: 20 por página y el cursor es el id del último entregado. */
class FakeMovementRepository(var movements: List<Movement> = (1..45).map { testMovement(it) }) : MovementRepository {
    /** Errores para las próximas llamadas, en orden; vacía, todas responden bien. */
    val failures = ArrayDeque<AppError>()
    val requestedCursors = mutableListOf<String?>()
    val requestedAccounts = mutableListOf<String?>()

    override suspend fun page(accountId: String?, cursor: String?): Outcome<Page<Movement>> {
        requestedCursors += cursor
        requestedAccounts += accountId
        failures.removeFirstOrNull()?.let { return Outcome.Failure(it) }
        val all = movements.filter { accountId == null || it.accountId == accountId }
        val start = cursor?.let { id -> all.indexOfFirst { it.id == id } + 1 } ?: 0
        val items = all.drop(start).take(PAGE_SIZE)
        val next = items.lastOrNull()?.id?.takeIf { start + items.size < all.size }
        return Outcome.Success(Page(items, next))
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}

class FakeAccountChanges(override var revision: Int = 0) : AccountChanges {
    override fun notifyChanged() {
        revision++
    }
}
