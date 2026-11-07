package com.hacybeyker.cartera.core.data.network

import javax.inject.Qualifier

/** Interceptores que otras variantes suman al cliente HTTP. En release el conjunto queda vacío. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ExtraInterceptors
