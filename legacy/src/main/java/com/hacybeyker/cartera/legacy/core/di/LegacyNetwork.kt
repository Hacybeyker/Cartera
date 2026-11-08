package com.hacybeyker.cartera.legacy.core.di

import javax.inject.Qualifier

/** El Retrofit de `:legacy`: `app` ya tiene el suyo sin calificador y los dos conviven en el mismo grafo. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LegacyNetwork
