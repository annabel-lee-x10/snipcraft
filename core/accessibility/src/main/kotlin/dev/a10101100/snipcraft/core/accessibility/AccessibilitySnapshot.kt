package dev.a10101100.snipcraft.core.accessibility

data class AccessibilitySnapshot(
    val packageName: String,
    val text: String,
    val cursorPosition: Int,
    val isPassword: Boolean,
    val fieldClassName: String?,
)
