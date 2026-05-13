package dev.a10101100.snipcraft.core.sync

interface SyncConfigStore {
    fun load(): SyncConfig?
    fun save(config: SyncConfig)
    fun clear()
}
