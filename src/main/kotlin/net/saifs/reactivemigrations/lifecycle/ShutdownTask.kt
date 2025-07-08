package net.saifs.reactivemigrations.lifecycle

interface ShutdownTask {
    suspend fun shutdown()
}