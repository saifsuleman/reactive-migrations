package net.saifs.reactivemigrations.config

import net.saifs.reactivemigrations.storage.Storage

data class TransferConfig(
    val source: Storage,
    val target: Storage,
    val allowDelete: Boolean = false,
    val syncMetadata: Boolean = false,
    val concurrency: Int = 200,
    val tps: Int = 2000,
    val batchSize: Int = 1000
)