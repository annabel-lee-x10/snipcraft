package dev.a10101100.snipcraft.core.sync

data class SyncResult(
    val pushed: Int = 0,
    val pulled: Int = 0,
    val conflicts: Int = 0,
    val errors: List<String> = emptyList(),
    val timestampMs: Long = System.currentTimeMillis(),
) {
    val isSuccess: Boolean get() = errors.isEmpty()

    fun summary(): String = when {
        !isSuccess -> "Sync failed: ${errors.first()}"
        pushed == 0 && pulled == 0 -> "Already up to date"
        else -> "↑ $pushed  ↓ $pulled${if (conflicts > 0) "  ⚠ $conflicts conflicts" else ""}"
    }
}
