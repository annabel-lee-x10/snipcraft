package dev.a10101100.snipcraft.core.data

import kotlinx.coroutines.flow.Flow

interface CompatibilityRuleRepository {
    fun observeBlacklistedPackages(): Flow<List<String>>
    suspend fun addToBlacklist(packageName: String)
    suspend fun removeFromBlacklist(packageName: String)
}
