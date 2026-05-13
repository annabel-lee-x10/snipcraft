package dev.a10101100.snipcraft.core.accessibility

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import dev.a10101100.snipcraft.core.common.AppLogger
import dev.a10101100.snipcraft.core.domain.ExpansionStrategy

private val DELIMITERS = setOf(' ', '\t', '\n', '.', ',', '!', '?', ';', ':', ')', ']', '}', '\'', '"')

class ExpansionExecutor(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())

    /**
     * Applies the expansion to [node], replacing [shortcut] + its trailing delimiter with [expandedText].
     * Returns true if the expansion was applied, false if it was skipped.
     */
    fun execute(
        node: AccessibilityNodeInfo,
        currentText: String,
        shortcut: String,
        expandedText: String,
        strategy: ExpansionStrategy,
    ): Boolean {
        if (strategy == ExpansionStrategy.DISABLED) return false

        val shortcutIdx = findShortcutIndex(currentText, shortcut) ?: run {
            AppLogger.w("ExpansionExecutor: shortcut '%s' not found at tail of '%s'", shortcut, currentText)
            return false
        }

        val delimIdx = shortcutIdx + shortcut.length
        val delimiter = if (delimIdx < currentText.length) currentText[delimIdx] else null
        // Build: text before shortcut + expanded text + text after delimiter
        val suffix = if (delimiter != null && delimIdx < currentText.length) currentText.substring(delimIdx + 1) else ""
        val newText = currentText.substring(0, shortcutIdx) + expandedText + (if (suffix.isNotEmpty()) delimiter.toString() + suffix else "")

        return when (strategy) {
            ExpansionStrategy.SET_TEXT -> applySetText(node, newText)
            ExpansionStrategy.PASTE -> applyPaste(node, newText.trimEnd(*DELIMITERS.toCharArray()))
            else -> false
        }
    }

    private fun applySetText(node: AccessibilityNodeInfo, newText: String): Boolean {
        val bundle = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
        }
        val ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
        AppLogger.d("ExpansionExecutor: SET_TEXT → '%s', success=%b", newText, ok)
        return ok
    }

    private fun applyPaste(node: AccessibilityNodeInfo, expandedText: String): Boolean {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val previousClip = cm.primaryClip
        cm.setPrimaryClip(ClipData.newPlainText("snipcraft", expandedText))
        val ok = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        // Restore clipboard after a short delay
        handler.postDelayed({
            try {
                if (previousClip != null) cm.setPrimaryClip(previousClip)
            } catch (_: Exception) {}
        }, 600L)
        AppLogger.d("ExpansionExecutor: PASTE → '%s', success=%b", expandedText, ok)
        return ok
    }

    private fun findShortcutIndex(text: String, shortcut: String): Int? {
        val idx = text.lastIndexOf(shortcut)
        if (idx < 0) return null
        val afterIdx = idx + shortcut.length
        if (afterIdx >= text.length) return idx  // shortcut at very end (no delimiter yet - edge case)
        val afterChar = text[afterIdx]
        return if (afterChar in DELIMITERS) idx else null
    }
}
