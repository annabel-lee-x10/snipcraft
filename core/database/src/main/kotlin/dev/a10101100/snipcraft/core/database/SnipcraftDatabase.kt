package dev.a10101100.snipcraft.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SnippetEntity::class, FolderEntity::class, CompatibilityRuleEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SnipcraftDatabase : RoomDatabase() {
    abstract fun snippetDao(): SnippetDao
    abstract fun folderDao(): FolderDao
    abstract fun compatibilityRuleDao(): CompatibilityRuleDao

    companion object {
        const val DATABASE_NAME = "snipcraft.db"
    }
}
