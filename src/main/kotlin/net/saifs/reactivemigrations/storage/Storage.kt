package net.saifs.reactivemigrations.storage

import kotlinx.coroutines.flow.Flow

interface Storage {
    val id: String

    suspend fun all(): Flow<StorageFile>
    suspend fun upload(file: StorageFile, data: ByteArray)
    suspend fun download(file: StorageFile): ByteArray
    suspend fun delete(file: StorageFile)
}