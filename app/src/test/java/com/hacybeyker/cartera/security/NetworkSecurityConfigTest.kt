package com.hacybeyker.cartera.security

import com.hacybeyker.cartera.BuildConfig
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** El tráfico sin TLS solo se permite hacia el loopback y, en debug, hacia la máquina anfitriona del emulador. */
class NetworkSecurityConfigTest {
    private val sourceRoot = File(requireNotNull(System.getProperty("cartera.rootDir")), "app/src")

    private fun config(sourceSet: String): Element? {
        val file = File(sourceRoot, "$sourceSet/res/xml/network_security_config.xml")
        if (!file.exists()) return null
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
    }

    private fun cleartextDomains(root: Element): List<String> {
        val groups = root.getElementsByTagName("domain-config")
        return (0 until groups.length).map { groups.item(it) as Element }
            .filter { it.getAttribute("cleartextTrafficPermitted") == "true" }
            .flatMap { group ->
                val domains = group.getElementsByTagName("domain")
                (0 until domains.length).map { domains.item(it).textContent.trim() }
            }
    }

    private fun baseAllowsCleartext(root: Element) =
        (root.getElementsByTagName("base-config").item(0) as Element).getAttribute("cleartextTrafficPermitted")

    @Test
    fun `release and main never allow cleartext`() {
        val main = requireNotNull(config("main"))

        assertEquals("false", baseAllowsCleartext(main))
        assertEquals(emptyList<String>(), cleartextDomains(main))
    }

    @Test
    fun `demo allows cleartext only to the embedded backend`() {
        val demo = requireNotNull(config("demo"))

        assertEquals("false", baseAllowsCleartext(demo))
        assertEquals(listOf("127.0.0.1"), cleartextDomains(demo))
    }

    @Test
    fun `debug also allows the emulator host`() {
        val debug = requireNotNull(config("debug"))

        assertEquals("false", baseAllowsCleartext(debug))
        assertEquals(listOf("127.0.0.1", "10.0.2.2"), cleartextDomains(debug))
    }

    @Test
    fun `the shared demo tools do not carry a copy that would clash with the variants`() {
        assertEquals(null, config("demoTools"))
    }

    @Test
    fun `the host address is allowed in debug and nowhere else`() {
        val others = listOf("main", "demo").map { requireNotNull(config(it)) }

        assertTrue(cleartextDomains(requireNotNull(config("debug"))).contains("10.0.2.2"))
        others.forEach { assertFalse(cleartextDomains(it).contains("10.0.2.2")) }
    }

    @Test
    fun `the base url of the build ends with a slash`() {
        assertTrue(BuildConfig.BASE_URL, BuildConfig.BASE_URL.matches(Regex("https?://\\S+/")))
    }
}
