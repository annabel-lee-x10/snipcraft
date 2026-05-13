package dev.a10101100.snipcraft.core.accessibility

import android.text.InputType
import android.view.accessibility.AccessibilityEvent
import dev.a10101100.snipcraft.core.common.AppLogger

private val PASSWORD_INPUT_VARIATIONS = setOf(
    InputType.TYPE_TEXT_VARIATION_PASSWORD,
    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
    InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
    InputType.TYPE_NUMBER_VARIATION_PASSWORD,
)

class AccessibilityEventProcessor {

    fun process(event: AccessibilityEvent): AccessibilitySnapshot? {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return null

        val node = event.source ?: return null

        val inputType = node.inputType
        val isPasswordNode = node.isPassword || isPasswordInputType(inputType)

        val text = node.text?.toString() ?: ""
        val pkg = event.packageName?.toString() ?: ""

        AppLogger.d("AccessibilityEvent: pkg=%s password=%b text=%s", pkg, isPasswordNode, if (isPasswordNode) "[REDACTED]" else text)

        return AccessibilitySnapshot(
            packageName = pkg,
            text = if (isPasswordNode) "" else text,
            cursorPosition = text.length,
            isPassword = isPasswordNode,
            fieldClassName = node.className?.toString(),
        )
    }

    private fun isPasswordInputType(inputType: Int): Boolean {
        val typeClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return when (typeClass) {
            InputType.TYPE_CLASS_TEXT -> variation in PASSWORD_INPUT_VARIATIONS
            InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
    }
}
