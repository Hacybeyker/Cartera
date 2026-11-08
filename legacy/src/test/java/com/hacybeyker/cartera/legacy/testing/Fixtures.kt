package com.hacybeyker.cartera.legacy.testing

import java.io.File

/** Lee un fixture de `api/fixtures`, la misma fuente de datos que sirve el backend embebido. */
fun fixture(path: String): String = File(checkNotNull(System.getProperty("cartera.fixturesDir")), path).readText()
