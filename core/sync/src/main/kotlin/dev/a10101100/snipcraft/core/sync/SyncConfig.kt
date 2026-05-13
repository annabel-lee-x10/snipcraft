package dev.a10101100.snipcraft.core.sync

data class SyncConfig(
    val serverUrl: String,
    val remotePath: String = "/snipcraft/snippets.json",
    val username: String,
    val password: String,
    val allowHttp: Boolean = false,
    val intervalHours: Int = 6,
) {
    fun remoteFileUrl(): String {
        val base = serverUrl.trimEnd('/')
        val path = if (remotePath.startsWith("/")) remotePath else "/$remotePath"
        return "$base$path"
    }

    fun remoteDirUrl(): String {
        val fileUrl = remoteFileUrl()
        return fileUrl.substringBeforeLast('/')
    }

    fun requiresHttps(): Boolean = !allowHttp
}
