package dev.a10101100.snipcraft.core.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TrieMatcherTest {

    private lateinit var matcher: TrieMatcher

    @BeforeEach
    fun setUp() {
        matcher = TrieMatcher()
        matcher.insert("/today")
        matcher.insert("/sig")
        matcher.insert("/email")
        matcher.insert("/lh")
    }

    @Test
    fun `exact match returns shortcut`() {
        assertEquals("/today", matcher.matchSuffix("/today"))
    }

    @Test
    fun `match within longer text`() {
        assertEquals("/today", matcher.matchSuffix("Hello /today"))
    }

    @Test
    fun `match at end of field text`() {
        assertEquals("/sig", matcher.matchSuffix("Best regards /sig"))
    }

    @Test
    fun `no match returns null`() {
        assertNull(matcher.matchSuffix("hello world"))
    }

    @Test
    fun `partial shortcut does not match`() {
        assertNull(matcher.matchSuffix("/tod"))
    }

    @Test
    fun `empty input returns null`() {
        assertNull(matcher.matchSuffix(""))
    }

    @Test
    fun `match short two-char shortcut`() {
        assertEquals("/lh", matcher.matchSuffix("check /lh"))
    }

    @Test
    fun `removing a shortcut stops matching`() {
        matcher.remove("/sig")
        assertNull(matcher.matchSuffix("test /sig"))
    }

    @Test
    fun `trie handles unicode characters`() {
        matcher.insert("/café")
        assertEquals("/café", matcher.matchSuffix("/café"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["/today", "/sig", "/email", "/lh"])
    fun `all inserted shortcuts are matchable`(shortcut: String) {
        assertEquals(shortcut, matcher.matchSuffix(shortcut))
    }

    @Test
    fun `rebuild clears previous entries and inserts new ones`() {
        matcher.rebuild(listOf("/new1", "/new2"))
        assertNull(matcher.matchSuffix("/today"))
        assertEquals("/new1", matcher.matchSuffix("/new1"))
        assertEquals("/new2", matcher.matchSuffix("/new2"))
    }
}
