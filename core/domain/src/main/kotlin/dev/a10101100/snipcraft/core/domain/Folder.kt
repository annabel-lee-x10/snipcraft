package dev.a10101100.snipcraft.core.domain

data class Folder(
    val id: String,
    val name: String,
    val color: Int? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
