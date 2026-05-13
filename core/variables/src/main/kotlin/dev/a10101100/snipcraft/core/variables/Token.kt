package dev.a10101100.snipcraft.core.variables

sealed interface Token

data class TextToken(val text: String) : Token

data class VariableToken(
    val name: String,
    val argument: String? = null,
) : Token
