package dev.a10101100.snipcraft.core.accessibility

import dev.a10101100.snipcraft.core.common.AppLogger
import dev.a10101100.snipcraft.core.data.SnippetRepository
import dev.a10101100.snipcraft.core.domain.Snippet
import dev.a10101100.snipcraft.core.engine.TrieMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnippetCacheManager @Inject constructor(
    private val repository: SnippetRepository,
) {
    val trie = TrieMatcher()
    private val _snippets = mutableMapOf<String, Snippet>()
    val snippets: Map<String, Snippet> get() = _snippets

    fun startObserving(scope: CoroutineScope) {
        repository.observeEnabled()
            .onEach { list ->
                _snippets.clear()
                list.forEach { _snippets[it.shortcut] = it }
                trie.rebuild(list.map { it.shortcut })
                AppLogger.i("SnippetCacheManager: rebuilt trie with %d snippets", list.size)
            }
            .launchIn(scope)
    }
}
