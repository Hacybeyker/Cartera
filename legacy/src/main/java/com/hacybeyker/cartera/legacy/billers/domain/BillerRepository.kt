package com.hacybeyker.cartera.legacy.billers.domain

import io.reactivex.rxjava3.core.Single

interface BillerRepository {
    /** Las empresas cuyo nombre o rubro coinciden con [query]; todas si está vacío. */
    fun search(query: String): Single<List<Biller>>
}
