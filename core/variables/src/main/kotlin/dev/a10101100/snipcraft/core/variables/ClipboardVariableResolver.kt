package dev.a10101100.snipcraft.core.variables

import dev.a10101100.snipcraft.core.domain.ExpansionContext

/**
 * Resolves `{{clipboard}}` — returns the clipboard contents at expansion time.
 * The actual clipboard read is injected via [clipboardProvider] to keep this
 * module pure-JVM testable. The accessibility module wires the real implementation.
 */
class ClipboardVariableResolver(
    private val clipboardProvider: suspend () -> String,
) : VariableResolver {

    override fun matches(token: String): Boolean = token == "clipboard"

    override suspend fun resolve(token: String, ctx: ExpansionContext): String =
        clipboardProvider()
}
