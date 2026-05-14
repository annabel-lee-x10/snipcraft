package dev.a10101100.snipcraft.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import dagger.hilt.android.AndroidEntryPoint
import dev.a10101100.snipcraft.core.common.AppLogger
import dev.a10101100.snipcraft.core.compatibility.CompatibilityResolver
import dev.a10101100.snipcraft.core.data.CompatibilityRuleRepository
import dev.a10101100.snipcraft.core.data.ExpansionHistoryRepository
import dev.a10101100.snipcraft.core.data.SnippetRepository
import dev.a10101100.snipcraft.core.domain.ExpansionContext
import dev.a10101100.snipcraft.core.domain.ExpansionStrategy
import dev.a10101100.snipcraft.core.variables.VariableEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

private val EXPANSION_DELIMITERS = setOf(
    ' ', '\t', '\n', '.', ',', '!', '?', ';', ':'
)

@AndroidEntryPoint
class SnipAccessibilityService : AccessibilityService() {

    @Inject lateinit var snippetCacheManager: SnippetCacheManager
    @Inject lateinit var compatibilityResolver: CompatibilityResolver
    @Inject lateinit var compatibilityRuleRepository: CompatibilityRuleRepository
    @Inject lateinit var variableEngine: VariableEngine
    @Inject lateinit var snippetRepository: SnippetRepository
    @Inject lateinit var expansionHistoryRepository: ExpansionHistoryRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val eventProcessor = AccessibilityEventProcessor()
    private lateinit var executor: ExpansionExecutor

    override fun onServiceConnected() {
        executor = ExpansionExecutor(applicationContext)
        snippetCacheManager.startObserving(serviceScope)
        serviceScope.launch {
            compatibilityRuleRepository.observeBlacklistedPackages().collect { packages ->
                compatibilityResolver.setUserBlacklist(packages.toSet())
            }
        }
        AppLogger.i("SnipAccessibilityService: connected — expansion active")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val snapshot = eventProcessor.process(event) ?: return
        if (snapshot.isPassword) return  // Hard exclusion — non-overridable

        val strategy = compatibilityResolver.strategyFor(snapshot.packageName)
        if (strategy == ExpansionStrategy.DISABLED) return

        val text = snapshot.text
        if (text.length < 2) return

        // Find the last delimiter in the text — text-change events deliver the full field value
        val delimIdx = text.indices.lastOrNull { text[it] in EXPANSION_DELIMITERS } ?: return
        if (delimIdx == 0) return

        val textBeforeDelim = text.substring(0, delimIdx)
        val shortcut = snippetCacheManager.trie.matchSuffix(textBeforeDelim) ?: return
        val snippet = snippetCacheManager.snippets[shortcut] ?: return
        if (!snippet.isEnabled) return

        serviceScope.launch {
            val ctx = ExpansionContext(
                packageName = snapshot.packageName,
                fieldText = text,
                cursorPosition = snapshot.cursorPosition,
            )
            val expandedText = variableEngine.resolve(snippet.body, ctx)
            val node = event.source ?: return@launch
            val now = System.currentTimeMillis()
            val ok = executor.execute(node, text, shortcut, expandedText, strategy)
            if (ok) {
                snippetRepository.incrementUsage(snippet.id, now)
                AppLogger.i("Expanded '%s' in %s", shortcut, snapshot.packageName)
            }
            expansionHistoryRepository.logExpansion(
                shortcut = shortcut,
                packageName = snapshot.packageName,
                timestamp = now,
                success = ok,
                errorReason = if (ok) null else "executor_failed",
            )
        }
    }

    override fun onInterrupt() {
        AppLogger.w("SnipAccessibilityService: interrupted")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        serviceScope.cancel()
        AppLogger.w("SnipAccessibilityService: unbound — scope cancelled")
        return super.onUnbind(intent)
    }
}
