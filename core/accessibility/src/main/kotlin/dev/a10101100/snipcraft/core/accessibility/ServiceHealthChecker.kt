package dev.a10101100.snipcraft.core.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceHealthChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isServiceEnabled(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val target = serviceComponentName().flattenToString()
        return enabled.any { it.resolveInfo?.serviceInfo?.let { si ->
            ComponentName(si.packageName, si.name).flattenToString() == target
        } == true }
    }

    fun serviceComponentName(): ComponentName =
        ComponentName(
            "dev.a10101100.snipcraft",
            "dev.a10101100.snipcraft.core.accessibility.SnipAccessibilityService",
        )
}
