package dev.a10101100.snipcraft.core.domain

data class ExpansionContext(
    val packageName: String,
    val fieldText: String,
    val cursorPosition: Int,
    val timestampMs: Long = System.currentTimeMillis(),
)
