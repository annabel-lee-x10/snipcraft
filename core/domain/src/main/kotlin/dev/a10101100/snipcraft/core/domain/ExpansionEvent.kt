package dev.a10101100.snipcraft.core.domain

data class ExpansionEvent(
    val id: Long,
    val shortcut: String,
    val packageName: String,
    val timestamp: Long,
    val success: Boolean,
    val errorReason: String?,
)
