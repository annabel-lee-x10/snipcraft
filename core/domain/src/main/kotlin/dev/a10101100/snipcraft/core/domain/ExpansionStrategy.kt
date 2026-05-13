package dev.a10101100.snipcraft.core.domain

enum class ExpansionStrategy {
    SET_TEXT,    // preferred for native EditText — uses ACTION_SET_TEXT
    PASTE,       // for WebViews and Chrome — clipboard swap
    KEYSTROKE,   // last-resort: synthesize backspaces + character input
    DISABLED,    // password fields, blacklisted apps
}
