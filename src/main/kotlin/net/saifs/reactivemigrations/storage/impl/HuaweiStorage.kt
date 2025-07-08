package net.saifs.reactivemigrations.storage.impl

import kotlinx.coroutines.flow.Flow
import net.saifs.reactivemigrations.storage.Storage
import net.saifs.reactivemigrations.storage.StorageFile

class HuaweiStorage : Storage {
    override val id: String
        get() = TODO("Not yet implemented")

    override suspend fun all(): Flow<StorageFile> {
        TODO("Not yet implemented")
    }

    override suspend fun upload(file: StorageFile, data: ByteArray) {
        TODO("Not yet implemented")
    }

    override suspend fun download(file: StorageFile): ByteArray {
        TODO("Not yet implemented")
    }

    override suspend fun delete(file: StorageFile) {
        TODO("Not yet implemented")
    }
}