package dev.a10101100.snipcraft.core.variables

import dev.a10101100.snipcraft.core.domain.ExpansionContext
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DateVariableResolverTest {

    private val resolver = DateVariableResolver()
    private val testContext = ExpansionContext(
        packageName = "test.pkg",
        fieldText = "/today",
        cursorPosition = 6,
        timestampMs = Instant.parse("2026-05-13T10:00:00Z").toEpochMilli(),
    )

    @Test
    fun `matches 'date' token`() {
        assertTrue(resolver.matches("date"))
    }

    @Test
    fun `does not match 'time' token`() {
        assertFalse(resolver.matches("time"))
    }

    @Test
    fun `resolves date with default ISO format`() = runTest {
        val result = resolver.resolve("date", testContext)
        // Default format is system-locale dependent but should be non-empty
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `resolves date with custom format`() = runTest {
        val result = resolver.resolve("date:yyyy-MM-dd", testContext)
        // Given the test timestamp of 2026-05-13T10:00:00Z
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())
        val expected = formatter.format(Instant.ofEpochMilli(testContext.timestampMs))
        assertEquals(expected, result)
    }

    @Test
    fun `resolves date with MM-dd-yyyy format`() = runTest {
        val result = resolver.resolve("date:MM/dd/yyyy", testContext)
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy").withZone(ZoneId.systemDefault())
        val expected = formatter.format(Instant.ofEpochMilli(testContext.timestampMs))
        assertEquals(expected, result)
    }
}
