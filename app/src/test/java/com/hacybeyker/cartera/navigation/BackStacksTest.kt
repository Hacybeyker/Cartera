package com.hacybeyker.cartera.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.hacybeyker.cartera.feature.home.ui.HomeNavKey
import com.hacybeyker.cartera.feature.login.ui.LoginNavKey
import com.hacybeyker.cartera.feature.terms.ui.TermsNavKey
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
}
