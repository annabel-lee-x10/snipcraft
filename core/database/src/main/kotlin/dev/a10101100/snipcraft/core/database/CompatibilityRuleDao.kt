package dev.a10101100.snipcraft.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompatibilityRuleDao {

    @Query("SELECT * FROM compatibility_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): CompatibilityRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: CompatibilityRuleEntity)

    @Query("SELECT * FROM compatibility_rules WHERE isBuiltIn = 0 ORDER BY packageName ASC")
    fun observeUserBlacklist(): Flow<List<CompatibilityRuleEntity>>

    @Query("DELETE FROM compatibility_rules WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
}
