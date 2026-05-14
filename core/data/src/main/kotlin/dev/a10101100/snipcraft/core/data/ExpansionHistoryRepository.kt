package dev.a10101100.snipcraft.core.data

import dev.a10101100.snipcraft.core.database.ExpansionHistoryDao
import dev.a10101100.snipcraft.core.database.ExpansionHistoryEntity
import dev.a10101100.snipcraft.core.domain.ExpansionEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface ExpansionHistoryRepository {
    suspend fun logExpansion(
        shortcut: String,
        packageName: String,
        timestamp: Long,
        success: Boolean,
        errorReason: String?,
    )
    fun observeRecent(): Flow<List<ExpansionEvent>>
}

class ExpansionHistoryRepositoryImpl @Inject constructor(
    private val dao: ExpansionHistoryDao,
) : ExpansionHistoryRepository {

    override suspend fun logExpansion(
        shortcut: String,
        packageName: String,
        timestamp: Long,
        success: Boolean,
        errorReason: String?,
    ) {
        dao.insert(
            ExpansionHistoryEntity(
                shortcut = shortcut,
                packageName = packageName,
                timestamp = timestamp,
                success = success,
                errorReason = errorReason,
            )
        )
    }

    override fun observeRecent(): Flow<List<ExpansionEvent>> =
        dao.observeLast50().map { entities ->
            entities.map { e ->
                ExpansionEvent(
                    id = e.id,
                    shortcut = e.shortcut,
                    packageName = e.packageName,
                    timestamp = e.timestamp,
                    success = e.success,
                    errorReason = e.errorReason,
                )
            }
        }
}
