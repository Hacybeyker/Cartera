package com.hacybeyker.cartera.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.hacybeyker.cartera.feature.home.ui.HomeNavKey
import com.hacybeyker.cartera.feature.login.ui.LoginNavKey
import com.hacybeyker.cartera.feature.terms.ui.TermsNavKey
import com.hacybeyker.cartera.feature.transfer.ui.TransferConfirmNavKey
import com.hacybeyker.cartera.feature.transfer.ui.TransferFormNavKey
import com.hacybeyker.cartera.feature.transfer.ui.TransferOrderArgs
import com.hacybeyker.cartera.feature.transfer.ui.TransferResultNavKey
import org.junit.Assert.assertEquals
import org.junit.Test

class BackStacksTest {

    @Test
    fun `reset leaves only the given key`() {
        val backStack = NavBackStack<NavKey>(LoginNavKey, TermsNavKey)

        backStack.resetTo(HomeNavKey)

        assertEquals(listOf<NavKey>(HomeNavKey), backStack.toList())
    }

    @Test
    fun `reset to the same key keeps a single entry`() {
        val backStack = NavBackStack<NavKey>(LoginNavKey)

        backStack.resetTo(LoginNavKey)

        assertEquals(listOf<NavKey>(LoginNavKey), backStack.toList())
    }

    @Test
    fun `pop to home keeps the existing home entry on top`() {
        val backStack = NavBackStack<NavKey>(
            HomeNavKey,
            TransferFormNavKey,
            TransferConfirmNavKey(order),
            TransferResultNavKey()
        )

        backStack.popTo(HomeNavKey)

        assertEquals(listOf<NavKey>(HomeNavKey), backStack.toList())
    }

    @Test
    fun `pop to a key that is not in the stack resets to it`() {
        val backStack = NavBackStack<NavKey>(LoginNavKey, TermsNavKey)

        backStack.popTo(HomeNavKey)

        assertEquals(listOf<NavKey>(HomeNavKey), backStack.toList())
    }

    private val order =
        TransferOrderArgs("ACC-002", "Cuenta Gastos", "****9034", "999-0000001-0-01", "150.00", "PEN", null, "k")
}
