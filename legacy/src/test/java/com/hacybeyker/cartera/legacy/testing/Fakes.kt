package com.hacybeyker.cartera.legacy.testing

import com.hacybeyker.cartera.legacy.account.domain.Account
import com.hacybeyker.cartera.legacy.account.domain.AccountRepository
import com.hacybeyker.cartera.legacy.billers.domain.Biller
import com.hacybeyker.cartera.legacy.billers.domain.BillerRepository
import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.domain.LegacyException
import com.hacybeyker.cartera.legacy.core.domain.SchedulerProvider
import com.hacybeyker.cartera.legacy.debts.domain.CustomerDebts
import com.hacybeyker.cartera.legacy.debts.domain.DebtRepository
import com.hacybeyker.cartera.legacy.integration.LegacyContentMarker
import com.hacybeyker.cartera.legacy.integration.LegacyFeatureFlags
import com.hacybeyker.cartera.legacy.integration.LegacyPinPadSeed
import com.hacybeyker.cartera.legacy.integration.LegacySessionExpiry
import com.hacybeyker.cartera.legacy.payment.domain.BillPayment
import com.hacybeyker.cartera.legacy.payment.domain.IdempotencyKeyFactory
import com.hacybeyker.cartera.legacy.payment.domain.PaymentOrder
import com.hacybeyker.cartera.legacy.payment.domain.PaymentRepository
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.schedulers.TestScheduler
import java.util.concurrent.CopyOnWriteArrayList

/** Todos los hilos son el mismo reloj virtual: el test decide cuándo corre cada tarea. */
class TestSchedulerProvider(val scheduler: TestScheduler = TestScheduler()) : SchedulerProvider {
    override fun io(): Scheduler = scheduler

    override fun computation(): Scheduler = scheduler

    override fun main(): Scheduler = scheduler
}

/** Las llamadas corren al instante; solo el reloj de las esperas (computation) es virtual. */
class ImmediateSchedulerProvider(val clock: TestScheduler = TestScheduler()) : SchedulerProvider {
    override fun io(): Scheduler = Schedulers.trampoline()

    override fun computation(): Scheduler = clock

    override fun main(): Scheduler = Schedulers.trampoline()
}

class RecordingMarker : LegacyContentMarker {
    val marks = CopyOnWriteArrayList<String>()

    override fun onContentVisible(mark: String) {
        marks += mark
    }
}

class FakeLegacyFeatureFlags(var billersFullRebind: Boolean = false, var debtsMainDecode: Boolean = false) :
    LegacyFeatureFlags {
    override fun billersFullRebind(): Boolean = billersFullRebind

    override fun debtsMainDecode(): Boolean = debtsMainDecode
}

class RecordingSessionExpiry : LegacySessionExpiry {
    var calls = 0

    override fun onSessionExpired() {
        calls++
    }
}

fun failure(error: LegacyError): Throwable = LegacyException(error)

class FakeBillerRepository : BillerRepository {
    val requests = mutableListOf<String>()
    var respond: (String) -> Single<List<Biller>> = { Single.just(emptyList()) }

    override fun search(query: String): Single<List<Biller>> = Single.defer {
        requests += query
        respond(query)
    }
}

class FakeDebtRepository : DebtRepository {
    val requests = mutableListOf<Pair<String, String>>()
    var respond: () -> Single<CustomerDebts> = { Single.never() }

    override fun pendingDebts(billerId: String, customerCode: String): Single<CustomerDebts> = Single.defer {
        requests += billerId to customerCode
        respond()
    }
}

class FakeAccountRepository : AccountRepository {
    var requests = 0
    var respond: () -> Single<List<Account>> = { Single.never() }

    override fun accounts(): Single<List<Account>> = Single.defer {
        requests++
        respond()
    }
}

class FakePaymentRepository : PaymentRepository {
    val orders = mutableListOf<PaymentOrder>()
    val vouchers = mutableListOf<String>()
    var respondPay: (PaymentOrder) -> Single<BillPayment> = { Single.never() }
    var respondVoucher: (String) -> Single<String> = { Single.never() }

    override fun pay(order: PaymentOrder): Single<BillPayment> = Single.defer {
        orders += order
        respondPay(order)
    }

    override fun voucher(paymentId: String): Single<String> = Single.defer {
        vouchers += paymentId
        respondVoucher(paymentId)
    }
}

class FakePinPadSeed(var seed: Int = 0) : LegacyPinPadSeed {
    override fun current(): Int = seed
}

/** Claves de idempotencia predecibles: `key-1`, `key-2`... */
class SequenceIdempotencyKeys : IdempotencyKeyFactory {
    var created = 0
        private set

    override fun create(): String = "key-${++created}"
}
