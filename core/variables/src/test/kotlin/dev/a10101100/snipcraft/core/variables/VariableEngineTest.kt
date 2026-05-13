package dev.a10101100.snipcraft.core.variables

import dev.a10101100.snipcraft.core.domain.ExpansionContext
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class VariableEngineTest {

    private val testContext = ExpansionContext(
        packageName = "test.pkg",
        fieldText = "/today",
        cursorPosition = 6,
        timestampMs = Instant.parse("2026-05-13T00:00:00Z").toEpochMilli(),
    )

    @Test
    fun `plain text body passes through unchanged`() = runTest {
        val engine = VariableEngine(resolvers = emptyList())
        assertEquals("Hello world", engine.resolve("Hello world", testContext))
    }

    @Test
    fun `date token is resolved`() = runTest {
        val engine = VariableEngine(resolvers = listOf(DateVariableResolver()))
        val result = engine.resolve("Today is {{date:yyyy-MM-dd}}", testContext)
        // The result should have replaced {{date:yyyy-MM-dd}} with a date string
        assertTrue(!result.contains("{{date"), "Expected date token to be resolved, got: $result")
    }

    @Test
    fun `unknown token is replaced with empty string`() = runTest {
        val engine = VariableEngine(resolvers = emptyList())
        val result = engine.resolve("{{unknown}}", testContext)
        assertEquals("", result)
    }

    @Test
    fun `multiple tokens resolved in one pass`() = runTest {
        val dateResolver = mockk<VariableResolver>()
        val timeResolver = mockk<VariableResolver>()
        coEvery { dateResolver.matches("date") } returns true
        coEvery { timeResolver.matches("date") } returns false
        coEvery { dateResolver.resolve("date", any()) } returns "2026-05-13"
        coEvery { timeResolver.matches("time") } returns true
        coEvery { dateResolver.matches("time") } returns false
        coEvery { timeResolver.resolve("time", any()) } returns "10:00"

        val engine = VariableEngine(resolvers = listOf(dateResolver, timeResolver))
        val result = engine.resolve("{{date}} at {{time}}", testContext)
        assertEquals("2026-05-13 at 10:00", result)
    }
}

private fun assertTrue(condition: Boolean, message: String) {
    org.junit.jupiter.api.Assertions.assertTrue(condition, message)
}
