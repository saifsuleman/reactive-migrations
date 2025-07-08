package net.saifs.reactivemigrations.storage

/**
 * For a given page on the source, this class represents:
 * a) if the data does not exist on the target [added]
 * b) if the checksums or filesize differ [modified]
 * c) if the data does not exist on the source yet does on the target [removed]
 */
data class PaginatedDiff(
    val added: List<StorageFile> = emptyList(),
    val modified: List<StorageFile> = emptyList(),
    val removed: List<StorageFile> = emptyList(),
)