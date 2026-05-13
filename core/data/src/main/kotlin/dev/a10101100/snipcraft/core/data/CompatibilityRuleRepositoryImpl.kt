package dev.a10101100.snipcraft.core.data

import dev.a10101100.snipcraft.core.database.CompatibilityRuleDao
import dev.a10101100.snipcraft.core.database.CompatibilityRuleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompatibilityRuleRepositoryImpl @Inject constructor(
    private val dao: CompatibilityRuleDao,
) : CompatibilityRuleRepository {

    override fun observeBlacklistedPackages(): Flow<List<String>> =
        dao.observeUserBlacklist().map { entities -> entities.map { it.packageName } }

    override suspend fun addToBlacklist(packageName: String) {
        dao.upsert(
            CompatibilityRuleEntity(
                id = UUID.randomUUID().toString(),
                packageName = packageName,
                strategy = "DISABLED",
                triggerOverride = null,
                delayMs = 0,
                notes = null,
                isBuiltIn = false,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun removeFromBlacklist(packageName: String) {
        dao.deleteByPackage(packageName)
    }
}
