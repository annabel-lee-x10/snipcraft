package dev.a10101100.snipcraft.core.variables

import dev.a10101100.snipcraft.core.domain.ExpansionContext
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class TimeVariableResolverTest {

    private val resolver = TimeVariableResolver()
    private val testContext = ExpansionContext(
        packageName = "test.pkg",
        fieldText = "/now",
        cursorPosition = 4,
        timestampMs = Instant.parse("2026-05-13T14:30:00Z").toEpochMilli(),
    )

    @Test
    fun `matches 'time' token`() {
        assertTrue(resolver.matches("time"))
    }

    @Test
    fun `does not match 'date' token`() {
        assertFalse(resolver.matches("date"))
    }

    @Test
    fun `resolves time — result is non-empty`() = runTest {
        val result = resolver.resolve("time", testContext)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `resolves time with HH-mm format`() = runTest {
        val result = resolver.resolve("time:HH:mm", testContext)
        // Should be a time string in HH:mm format
        assertTrue(result.matches(Regex("\\d{2}:\\d{2}")), "Expected HH:mm but got: $result")
    }
}
