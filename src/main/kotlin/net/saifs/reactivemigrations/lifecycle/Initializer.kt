package net.saifs.reactivemigrations.lifecycle

interface Initializer {
    suspend fun initialize()
}