package dev.a10101100.snipcraft.core.accessibility

import android.text.InputType
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccessibilityEventProcessorTest {

    private lateinit var processor: AccessibilityEventProcessor

    @Before
    fun setUp() {
        processor = AccessibilityEventProcessor()
    }

    @Test
    fun `text-changed event on normal field produces snapshot`() {
        val event = mockk<AccessibilityEvent>(relaxed = true)
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        every { event.packageName } returns "com.example.app"
        every { event.source } returns node
        every { node.isPassword } returns false
        every { node.inputType } returns InputType.TYPE_CLASS_TEXT
        every { node.text } returns "hello /today"

        val snapshot = processor.process(event)
        assertNotNull(snapshot)
        assertFalse(snapshot!!.isPassword)
    }

    @Test
    fun `password node is detected and snapshot is marked`() {
        val event = mockk<AccessibilityEvent>(relaxed = true)
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        every { event.packageName } returns "com.example.app"
        every { event.source } returns node
        every { node.isPassword } returns true
        every { node.inputType } returns InputType.TYPE_TEXT_VARIATION_PASSWORD
        every { node.text } returns "secret"

        val snapshot = processor.process(event)
        assertNotNull(snapshot)
        assertTrue(snapshot!!.isPassword)
    }

    @Test
    fun `non-text-change event returns null`() {
        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_VIEW_FOCUSED

        assertNull(processor.process(event))
    }

    @Test
    fun `null source node returns null`() {
        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        every { event.source } returns null

        assertNull(processor.process(event))
    }

    @Test
    fun `inputType password variation is detected`() {
        val event = mockk<AccessibilityEvent>(relaxed = true)
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        every { event.packageName } returns "com.example.app"
        every { event.source } returns node
        every { node.isPassword } returns false
        // TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD = 0x81
        every { node.inputType } returns (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        every { node.text } returns "typed"

        val snapshot = processor.process(event)
        assertNotNull(snapshot)
        assertTrue(snapshot!!.isPassword)
    }
}
