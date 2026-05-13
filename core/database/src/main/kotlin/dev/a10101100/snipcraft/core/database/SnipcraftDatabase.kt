package dev.a10101100.snipcraft.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SnippetEntity::class, FolderEntity::class, CompatibilityRuleEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class SnipcraftDatabase : RoomDatabase() {
    abstract fun snippetDao(): SnippetDao
    abstract fun folderDao(): FolderDao
    abstract fun compatibilityRuleDao(): CompatibilityRuleDao

    companion object {
        const val DATABASE_NAME = "snipcraft.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE snippets ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE folders ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
