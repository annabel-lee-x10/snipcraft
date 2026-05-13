package dev.a10101100.snipcraft.core.compatibility

import dev.a10101100.snipcraft.core.domain.ExpansionStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CompatibilityResolverTest {

    private val resolver = CompatibilityResolver()

    @Test
    fun `Chrome forces PASTE strategy`() {
        assertEquals(ExpansionStrategy.PASTE, resolver.strategyFor("com.android.chrome"))
    }

    @Test
    fun `Firefox forces PASTE strategy`() {
        assertEquals(ExpansionStrategy.PASTE, resolver.strategyFor("org.mozilla.firefox"))
    }

    @Test
    fun `Discord forces PASTE strategy`() {
        assertEquals(ExpansionStrategy.PASTE, resolver.strategyFor("com.discord"))
    }

    @Test
    fun `unknown app defaults to SET_TEXT`() {
        assertEquals(ExpansionStrategy.SET_TEXT, resolver.strategyFor("com.some.random.app"))
    }

    @Test
    fun `blacklisted package returns DISABLED`() {
        val r = CompatibilityResolver(blacklist = setOf("com.bank.app"))
        assertEquals(ExpansionStrategy.DISABLED, r.strategyFor("com.bank.app"))
    }

    @Test
    fun `systemui is DISABLED`() {
        assertEquals(ExpansionStrategy.DISABLED, resolver.strategyFor("com.android.systemui"))
    }

    @Test
    fun `Slack forces PASTE strategy`() {
        assertEquals(ExpansionStrategy.PASTE, resolver.strategyFor("com.Slack"))
    }
}
