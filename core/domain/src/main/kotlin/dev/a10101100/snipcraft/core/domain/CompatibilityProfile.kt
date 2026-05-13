package dev.a10101100.snipcraft.core.domain

data class CompatibilityProfile(
    val packageName: String,
    val strategy: ExpansionStrategy,
    val triggerOverride: TriggerMode? = null,
    val postExpansionDelayMs: Int = 50,
    val isBuiltIn: Boolean = false,
)
