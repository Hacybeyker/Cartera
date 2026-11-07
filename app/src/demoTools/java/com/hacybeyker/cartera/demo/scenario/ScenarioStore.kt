package com.hacybeyker.cartera.demo.scenario

interface ScenarioStorage {
    fun read(): Map<String, String>

    fun write(values: Map<String, String>)
}

// Sin caché propia: el receptor de adb, los argumentos de arranque y el interceptor comparten el mismo almacenamiento.
class ScenarioStore(private val storage: ScenarioStorage) {

    fun load(): Scenario = ScenarioParser.apply(Scenario(), storage.read()).scenario

    fun update(values: Map<String, Any?>): ScenarioUpdate = ScenarioParser.apply(load(), values).also {
        storage.write(it.scenario.toValues())
    }
}
