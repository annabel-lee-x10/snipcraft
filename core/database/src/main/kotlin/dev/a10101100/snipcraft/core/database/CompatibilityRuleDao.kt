package dev.a10101100.snipcraft.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CompatibilityRuleDao {

    @Query("SELECT * FROM compatibility_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): CompatibilityRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: CompatibilityRuleEntity)
}
