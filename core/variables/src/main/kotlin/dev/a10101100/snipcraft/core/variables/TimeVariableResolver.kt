package dev.a10101100.snipcraft.core.variables

import dev.a10101100.snipcraft.core.domain.ExpansionContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class TimeVariableResolver : VariableResolver {

    override fun matches(token: String): Boolean =
        token == "time" || token.startsWith("time:")

    override suspend fun resolve(token: String, ctx: ExpansionContext): String {
        val format = token.substringAfter("time:", missingDelimiterValue = "")
        val instant = Instant.ofEpochMilli(ctx.timestampMs)
        return if (format.isEmpty()) {
            DateTimeFormatter
                .ofLocalizedTime(FormatStyle.SHORT)
                .withZone(ZoneId.systemDefault())
                .format(instant)
        } else {
            DateTimeFormatter
                .ofPattern(format)
                .withZone(ZoneId.systemDefault())
                .format(instant)
        }
    }
}
