package dev.a10101100.snipcraft.core.accessibility

import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import dev.a10101100.snipcraft.core.domain.ExpansionStrategy
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExpansionExecutorTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `SET_TEXT strategy calls ACTION_SET_TEXT on node`() {
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { node.text } returns "Hello /sig "
        every { node.performAction(any(), any()) } returns true

        val executor = ExpansionExecutor(context)
        val success = executor.execute(
            node = node,
            currentText = "Hello /sig ",
            shortcut = "/sig",
            expandedText = "John Smith",
            strategy = ExpansionStrategy.SET_TEXT,
        )

        assertTrue(success)
        val bundleSlot = slot<Bundle>()
        verify { node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, capture(bundleSlot)) }
        val newText = bundleSlot.captured.getCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE)
        assertEquals("Hello John Smith", newText.toString())
    }

    @Test
    fun `PASTE strategy calls ACTION_PASTE on node`() {
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { node.performAction(AccessibilityNodeInfo.ACTION_PASTE) } returns true

        val executor = ExpansionExecutor(context)
        val success = executor.execute(
            node = node,
            currentText = "/sig ",
            shortcut = "/sig",
            expandedText = "John Smith",
            strategy = ExpansionStrategy.PASTE,
        )

        assertTrue(success)
        verify { node.performAction(AccessibilityNodeInfo.ACTION_PASTE) }
    }

    @Test
    fun `DISABLED strategy does nothing and returns false`() {
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)
        val executor = ExpansionExecutor(context)

        val success = executor.execute(
            node = node,
            currentText = "/sig ",
            shortcut = "/sig",
            expandedText = "John Smith",
            strategy = ExpansionStrategy.DISABLED,
        )

        assertFalse(success)
        verify(exactly = 0) { node.performAction(any()) }
        verify(exactly = 0) { node.performAction(any(), any()) }
    }

    @Test
    fun `shortcut removal leaves surrounding text intact`() {
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { node.performAction(any(), any()) } returns true

        val executor = ExpansionExecutor(context)
        executor.execute(
            node = node,
            currentText = "Dear /sig, hope",
            shortcut = "/sig",
            expandedText = "John",
            strategy = ExpansionStrategy.SET_TEXT,
        )

        val bundleSlot = slot<Bundle>()
        verify { node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, capture(bundleSlot)) }
        val newText = bundleSlot.captured.getCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE)
        // shortcut+delimiter (/sig,) removed, expansion inserted
        assertEquals("Dear John, hope", newText.toString())
    }
}
