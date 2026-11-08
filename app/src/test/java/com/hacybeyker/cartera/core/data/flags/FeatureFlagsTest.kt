package com.hacybeyker.cartera.core.data.flags

import com.hacybeyker.cartera.BuildConfig
import com.hacybeyker.cartera.core.domain.flags.FeatureFlag
import com.hacybeyker.cartera.core.domain.flags.IsFeatureEnabledUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureFlagsTest {

    @Test
    fun `without sources, as in release, every flag stays off`() {
        val flags = BuildConfigFeatureFlags(emptySet())

        FeatureFlag.entries.forEach { assertFalse(flags.isEnabled(it)) }
    }

    @Test
    fun `build config ships every flag off`() {
        assertFalse(BuildConfig.MOVEMENTS_FULL_PAGE)
        assertFalse(BuildConfig.ACCOUNTS_EAGER_REFRESH)
        assertFalse(BuildConfig.BREAKDOWN_RICH_TRANSITION)
    }

    @Test
    fun `the first source that decides wins`() {
        val undecided = FeatureFlagSource { null }
        val scenario = FeatureFlagSource { it == FeatureFlag.ACCOUNTS_EAGER_REFRESH }
        val flags = BuildConfigFeatureFlags(linkedSetOf(undecided, scenario))

        assertTrue(flags.isEnabled(FeatureFlag.ACCOUNTS_EAGER_REFRESH))
        assertFalse(flags.isEnabled(FeatureFlag.MOVEMENTS_FULL_PAGE))
        assertTrue(IsFeatureEnabledUseCase(flags)(FeatureFlag.ACCOUNTS_EAGER_REFRESH))
    }

    @Test
    fun `keys match the adb extras`() {
        assertEquals(
            listOf("movements_full_page", "accounts_eager_refresh", "breakdown_rich_transition"),
            FeatureFlag.entries.map { it.key }
        )
    }
}
