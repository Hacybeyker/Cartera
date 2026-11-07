package com.hacybeyker.cartera.core.domain

/** Una página por cursor: [nextCursor] es `null` en la última. */
data class Page<T>(val items: List<T>, val nextCursor: String?)
