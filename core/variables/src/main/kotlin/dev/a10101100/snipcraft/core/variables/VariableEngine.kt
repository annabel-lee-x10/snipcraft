package dev.a10101100.snipcraft.core.variables

import dev.a10101100.snipcraft.core.domain.ExpansionContext

class VariableEngine(private val resolvers: List<VariableResolver>) {

    private val parser = TemplateParser()

    suspend fun resolve(template: String, ctx: ExpansionContext): String {
        val tokens = parser.parse(template)
        return buildString {
            for (token in tokens) {
                when (token) {
                    is TextToken -> append(token.text)
                    is VariableToken -> {
                        val full = if (token.argument != null) "${token.name}:${token.argument}" else token.name
                        val resolver = resolvers.firstOrNull { it.matches(full) }
                            ?: resolvers.firstOrNull { it.matches(token.name) }
                        if (resolver != null) {
                            append(resolver.resolve(full, ctx))
                        }
                        // Unknown tokens resolve to empty string (no append)
                    }
                }
            }
        }
    }
}
