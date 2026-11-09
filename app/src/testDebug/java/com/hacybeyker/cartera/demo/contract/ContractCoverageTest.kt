package com.hacybeyker.cartera.demo.contract

import com.hacybeyker.cartera.demo.backend.Endpoint
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** El contrato, el router y las pruebas hablan de las mismas operaciones y de las mismas respuestas. */
class ContractCoverageTest {
    private val documented: Set<String> = ContractSpec.operations.flatMap { (operation, node) ->
        ContractSpec.statuses(node).map { "$operation $it" }
    }.toSet()

    private val exercised: Set<String> = ContractScenarios.all.map { "${it.operation} ${it.status}" }.toSet()

    @Test
    fun `every documented response is exercised against the backend`() {
        assertEquals(emptySet<String>(), documented - exercised)
    }

    @Test
    fun `every exercised response is documented`() {
        assertEquals(emptySet<String>(), exercised - documented)
    }

    @Test
    fun `the router and the contract serve the same operations`() {
        val routed = Endpoint.entries.filter { it != Endpoint.MARKET_STREAM }
            .map { "${it.method.lowercase()} /${it.path}" }.toSet()
        val published = ContractSpec.operations.keys.map { it.replace(Regex("\\{[^}]+}"), "{id}") }.toSet()

        assertEquals(routed, published)
    }

    @Test
    fun `the live channel handshake is described in the asyncapi contract`() {
        val asyncApi = File(ContractSpec.rootDir, "api/asyncapi.yaml").readText()

        assertTrue(asyncApi.contains("address: /${Endpoint.MARKET_STREAM.path}"))
    }
}
