package dev.a10101100.snipcraft.core.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OnDelimiterTriggerTest {

    private val trigger = OnDelimiterTrigger()

    @Test
    fun `space after shortcut fires trigger`() {
        val result = trigger.evaluate(text = "/today ", cursorPos = 7)
        assertEquals("/today", result)
    }

    @Test
    fun `tab after shortcut fires trigger`() {
        val result = trigger.evaluate(text = "/sig\t", cursorPos = 5)
        assertEquals("/sig", result)
    }

    @Test
    fun `period after shortcut fires trigger`() {
        val result = trigger.evaluate(text = "/sig.", cursorPos = 5)
        assertEquals("/sig", result)
    }

    @Test
    fun `comma after shortcut fires trigger`() {
        val result = trigger.evaluate(text = "/sig,", cursorPos = 5)
        assertEquals("/sig", result)
    }

    @Test
    fun `no delimiter at cursor — no trigger`() {
        val result = trigger.evaluate(text = "/today", cursorPos = 6)
        assertNull(result)
    }

    @Test
    fun `letter at cursor — no trigger`() {
        val result = trigger.evaluate(text = "/todayx", cursorPos = 7)
        assertNull(result)
    }

    @Test
    fun `empty text — no trigger`() {
        val result = trigger.evaluate(text = "", cursorPos = 0)
        assertNull(result)
    }

    @Test
    fun `delimiter at start returns nothing`() {
        val result = trigger.evaluate(text = " ", cursorPos = 1)
        assertNull(result)
    }

    @Test
    fun `shortcut in middle of sentence triggers on space`() {
        val result = trigger.evaluate(text = "Dear /sig ", cursorPos = 10)
        assertEquals("/sig", result)
    }
}
