package com.hacybeyker.cartera.demo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hacybeyker.cartera.demo.backend.CarteraBackend
import com.hacybeyker.cartera.demo.backend.fileFixtures
import java.net.InetAddress
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesDeviceRegistryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val client = OkHttpClient()

    @Test
    fun `a new instance over the same preferences keeps the devices`() {
        context.deviceRegistry().register("dvc_ana", "USR-001")
        context.deviceRegistry().register("dvc_otro", "USR-002")

        val reopened = context.deviceRegistry()

        assertEquals("USR-001", reopened.userOf("dvc_ana"))
        reopened.revokeAll("USR-001")
        assertNull(context.deviceRegistry().userOf("dvc_ana"))
        assertEquals("USR-002", context.deviceRegistry().userOf("dvc_otro"))
        reopened.clear()
        assertNull(context.deviceRegistry().userOf("dvc_otro"))
    }

    @Test
    fun `the device token survives a new embedded backend, like after a cold start`() {
        val deviceToken = backend { enroll() }

        assertEquals(200, backend { biometricLogin(deviceToken) })
        backend { post("_demo/reset", "") }
        assertEquals(401, backend { biometricLogin(deviceToken) })
    }

    private fun <T> backend(block: CarteraBackend.() -> T): T =
        CarteraBackend(fileFixtures, devices = context.deviceRegistry()).use { backend ->
            backend.start(InetAddress.getLoopbackAddress(), port = 0)
            backend.block()
        }

    private fun CarteraBackend.enroll(): String {
        val login = post("auth/login", """{"username":"ana.torres","password":"Cartera2026"}""").body.string()
        val accessToken = Regex("\"accessToken\":\"([^\"]+)\"").find(login)!!.groupValues[1]
        val enroll = post("auth/biometric/enroll", "", accessToken).body.string()
        return Regex("\"deviceToken\":\"([^\"]+)\"").find(enroll)!!.groupValues[1]
    }

    private fun CarteraBackend.biometricLogin(deviceToken: String): Int =
        post("auth/biometric/login", """{"deviceToken":"$deviceToken"}""").use { it.code }

    private fun CarteraBackend.post(path: String, body: String, accessToken: String? = null) = client.newCall(
        Request.Builder()
            .url(baseUrl.resolve(path)!!)
            .post(body.toRequestBody("application/json".toMediaType()))
            .apply { accessToken?.let { header("Authorization", "Bearer $it") } }
            .build()
    ).execute()
}
