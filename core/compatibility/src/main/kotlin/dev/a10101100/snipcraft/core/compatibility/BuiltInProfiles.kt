package dev.a10101100.snipcraft.core.compatibility

import dev.a10101100.snipcraft.core.domain.ExpansionStrategy

internal val BUILT_IN_PASTE_PACKAGES = setOf(
    "com.android.chrome",
    "org.mozilla.firefox",
    "com.microsoft.edgemobile",
    "com.opera.browser",
    "com.brave.browser",
    "com.discord",
    "com.Slack",
    "com.whatsapp",
    "com.facebook.katana",     // Facebook
    "com.instagram.android",
    "com.twitter.android",
    "com.linkedin.android",
)

internal val BUILT_IN_DISABLED_PACKAGES = setOf(
    "com.android.systemui",
    "com.android.settings",
)

internal val WEBVIEW_PACKAGE_SUBSTRINGS = listOf("webview", "browser")

fun defaultStrategyFor(packageName: String): ExpansionStrategy {
    if (packageName in BUILT_IN_DISABLED_PACKAGES) return ExpansionStrategy.DISABLED
    if (packageName in BUILT_IN_PASTE_PACKAGES) return ExpansionStrategy.PASTE
    if (WEBVIEW_PACKAGE_SUBSTRINGS.any { packageName.contains(it, ignoreCase = true) }) {
        return ExpansionStrategy.PASTE
    }
    return ExpansionStrategy.SET_TEXT
}
