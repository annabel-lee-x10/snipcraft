package dev.a10101100.snipcraft.core.domain

enum class TriggerMode {
    ON_DELIMITER,  // MVP: expands when user types space/punctuation after shortcut
    INSTANT,       // Phase 2: expands immediately on last character typed
    MANUAL,        // Phase 2: only expands on explicit user action
}
