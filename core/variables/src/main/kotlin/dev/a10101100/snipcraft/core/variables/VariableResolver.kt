package dev.a10101100.snipcraft.core.variables

import dev.a10101100.snipcraft.core.domain.ExpansionContext

interface VariableResolver {
    fun matches(token: String): Boolean
    suspend fun resolve(token: String, ctx: ExpansionContext): String
}
