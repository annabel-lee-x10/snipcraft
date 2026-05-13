package dev.a10101100.snipcraft.core.data

import dev.a10101100.snipcraft.core.domain.Snippet
import dev.a10101100.snipcraft.core.domain.SnippetType
import dev.a10101100.snipcraft.core.domain.TriggerMode
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedDataPopulator @Inject constructor(
    private val snippetRepository: SnippetRepository,
) {
    suspend fun seedIfEmpty() {
        if (snippetRepository.count() > 0) return
        val now = System.currentTimeMillis()
        snippetRepository.upsertAll(SEED_SNIPPETS.mapIndexed { i, (shortcut, body, desc) ->
            Snippet(
                id = UUID.randomUUID().toString(),
                shortcut = shortcut,
                body = body,
                type = SnippetType.PLAIN,
                triggerMode = TriggerMode.ON_DELIMITER,
                description = desc,
                createdAt = now + i,
                updatedAt = now + i,
            )
        })
    }

    private companion object {
        val SEED_SNIPPETS = listOf(
            Triple(";sig",   "Best regards,\n[Your Name]",                         "Closing signature"),
            Triple(";em",    "your@email.com",                                      "Your email address"),
            Triple(";addr",  "123 Main St\nCity, State 00000",                      "Mailing address"),
            Triple(";today", "{{date:yyyy-MM-dd}}",                                 "Today's date (ISO)"),
            Triple(";now",   "{{time:HH:mm}}",                                      "Current time (24h)"),
            Triple(";phone", "+1 (555) 000-0000",                                   "Phone number"),
            Triple(";lh",    "http://localhost:3000",                               "Localhost dev URL"),
            Triple(";lorem", "Lorem ipsum dolor sit amet, consectetur adipiscing elit.", "Lorem ipsum"),
        )
    }
}
