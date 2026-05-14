package dev.a10101100.snipcraft.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpansionHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExpansionHistoryEntity)

    @Query("SELECT * FROM expansion_history ORDER BY timestamp DESC LIMIT 50")
    fun observeLast50(): Flow<List<ExpansionHistoryEntity>>
}
