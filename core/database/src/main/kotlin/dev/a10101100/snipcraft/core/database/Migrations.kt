package dev.a10101100.snipcraft.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `expansion_history` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `shortcut` TEXT NOT NULL,
                `packageName` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `success` INTEGER NOT NULL,
                `errorReason` TEXT
            )
            """.trimIndent()
        )
    }
}
