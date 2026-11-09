package com.hacybeyker.cartera.demo.contract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cada respuesta del backend embebido cumple lo que `api/openapi.yaml` dice de ella. */
class ContractResponsesTest {
    @Test
    fun `every backend response matches the contract`() {
        val failures = ContractScenarios.all.flatMap { scenario ->
            val played = scenario.play(Env())
            ContractCheck.problems(scenario, played)
        }

        assertTrue("\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `the voucher is served as html and the failures as problem json`() {
        val voucher = ContractScenarios.all.first { it.operation.endsWith("/voucher") && it.status == 200 }
            .let { it.play(Env()).response }
        val problem = ContractScenarios.all.first { it.operation == "get /me" && it.status == 401 }
            .let { it.play(Env()).response }

        assertEquals("text/html", voucher.contentType)
        assertTrue(requireNotNull(voucher.body).startsWith("<!DOCTYPE html>"))
        assertEquals("application/problem+json", problem.contentType)
    }
}
