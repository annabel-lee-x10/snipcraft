package dev.a10101100.snipcraft.core.variables

class TemplateParser {

    fun parse(template: String): List<Token> {
        if (template.isEmpty()) return emptyList()

        val tokens = mutableListOf<Token>()
        var i = 0

        while (i < template.length) {
            val openIdx = template.indexOf("{{", i)
            if (openIdx == -1) {
                tokens += TextToken(template.substring(i))
                break
            }
            val closeIdx = template.indexOf("}}", openIdx + 2)
            if (closeIdx == -1) {
                // Unclosed brace — treat rest as literal
                tokens += TextToken(template.substring(i))
                break
            }
            if (openIdx > i) {
                tokens += TextToken(template.substring(i, openIdx))
            }
            val inner = template.substring(openIdx + 2, closeIdx)
            val colonIdx = inner.indexOf(':')
            if (colonIdx == -1) {
                tokens += VariableToken(inner)
            } else {
                tokens += VariableToken(
                    name = inner.substring(0, colonIdx),
                    argument = inner.substring(colonIdx + 1),
                )
            }
            i = closeIdx + 2
        }
        return tokens
    }
}
