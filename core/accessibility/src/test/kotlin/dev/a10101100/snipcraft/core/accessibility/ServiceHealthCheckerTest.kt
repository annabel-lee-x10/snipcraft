package dev.a10101100.snipcraft.core.accessibility

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ServiceHealthCheckerTest {

    @Test
    fun `isServiceEnabled returns false in fresh Robolectric context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val checker = ServiceHealthChecker(context)
        assertFalse(checker.isServiceEnabled())
    }

    @Test
    fun `serviceComponentName has correct package and class`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val checker = ServiceHealthChecker(context)
        val name = checker.serviceComponentName()
        assertEquals("dev.a10101100.snipcraft", name.packageName)
        assertTrue(name.className.contains("SnipAccessibilityService"))
    }

    private fun assertTrue(condition: Boolean) = org.junit.Assert.assertTrue(condition)
}
