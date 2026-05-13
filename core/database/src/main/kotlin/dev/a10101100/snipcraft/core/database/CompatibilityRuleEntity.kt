package dev.a10101100.snipcraft.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "compatibility_rules",
    indices = [Index("packageName", unique = true)],
)
data class CompatibilityRuleEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val strategy: String,              // ExpansionStrategy name
    val triggerOverride: String?,      // TriggerMode name or null
    val delayMs: Int,
    val notes: String?,
    val isBuiltIn: Boolean,
    val updatedAt: Long,
)
