package dev.a10101100.snipcraft.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expansion_history")
data class ExpansionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shortcut: String,
    val packageName: String,
    val timestamp: Long,
    val success: Boolean,
    val errorReason: String?,
)
