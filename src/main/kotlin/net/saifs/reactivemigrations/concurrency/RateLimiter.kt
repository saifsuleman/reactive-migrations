package net.saifs.reactivemigrations.concurrency

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RateLimiter(maxPermitsPerSecond: Int) {
    private val interval = 1000L / maxPermitsPerSecond
    private var nextPermitTime = System.currentTimeMillis()
    private val mutex = Mutex()

    suspend fun acquire() {
        val waitTime = mutex.withLock {
            val now = System.currentTimeMillis()
            val timeToWait = nextPermitTime - now
            if (timeToWait > 0) {
                nextPermitTime += interval
                timeToWait
            } else {
                nextPermitTime = now + interval
                0L
            }
        }

        if (waitTime > 0) {
            delay(waitTime)
        }
    }
}