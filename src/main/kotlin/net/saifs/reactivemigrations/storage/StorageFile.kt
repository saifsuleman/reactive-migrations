package net.saifs.reactivemigrations.storage

data class StorageFile(
    val path: String,
    val size: Long,
    val checksum: String
)