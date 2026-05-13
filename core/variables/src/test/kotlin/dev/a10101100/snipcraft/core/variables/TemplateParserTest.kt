package dev.a10101100.snipcraft.core.variables

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TemplateParserTest {

    private val parser = TemplateParser()

    @Test
    fun `plain text with no tokens returns as-is`() {
        assertEquals(listOf(TextToken("hello world")), parser.parse("hello world"))
    }

    @Test
    fun `single variable token is parsed`() {
        assertEquals(listOf(VariableToken("date")), parser.parse("{{date}}"))
    }

    @Test
    fun `variable with format argument`() {
        assertEquals(listOf(VariableToken("date", "yyyy-MM-dd")), parser.parse("{{date:yyyy-MM-dd}}"))
    }

    @Test
    fun `mixed text and token`() {
        val result = parser.parse("Today is {{date}}")
        assertEquals(
            listOf(TextToken("Today is "), VariableToken("date")),
            result,
        )
    }

    @Test
    fun `multiple tokens`() {
        val result = parser.parse("{{date}} at {{time}}")
        assertEquals(
            listOf(VariableToken("date"), TextToken(" at "), VariableToken("time")),
            result,
        )
    }

    @Test
    fun `unclosed brace is treated as literal text`() {
        val result = parser.parse("{{date")
        assertEquals(listOf(TextToken("{{date")), result)
    }

    @Test
    fun `empty template returns empty list`() {
        assertEquals(emptyList<Token>(), parser.parse(""))
    }

    @Test
    fun `token with clipboard`() {
        assertEquals(listOf(VariableToken("clipboard")), parser.parse("{{clipboard}}"))
    }
}
