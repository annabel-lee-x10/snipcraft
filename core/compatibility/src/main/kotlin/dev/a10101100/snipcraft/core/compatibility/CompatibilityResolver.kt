package dev.a10101100.snipcraft.core.compatibility

import dev.a10101100.snipcraft.core.domain.ExpansionStrategy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompatibilityResolver @Inject constructor(
    private val blacklist: Set<String> = emptySet(),
) {
    @Volatile private var userBlacklist: Set<String> = emptySet()

    fun setUserBlacklist(packages: Set<String>) {
        userBlacklist = packages
    }

    fun strategyFor(packageName: String): ExpansionStrategy {
        if (packageName in blacklist || packageName in userBlacklist) return ExpansionStrategy.DISABLED
        return defaultStrategyFor(packageName)
    }
}
