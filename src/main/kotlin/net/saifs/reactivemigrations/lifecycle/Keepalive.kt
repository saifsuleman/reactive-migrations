package net.saifs.reactivemigrations.lifecycle

import net.saifs.reactivemigrations.extension.logger
import org.springframework.stereotype.Component

//@Component
class Keepalive : Initializer, ShutdownTask {
    private val logger by logger()
    private val thread = Thread({
        try {
            while (true) Thread.sleep(Long.MAX_VALUE)
        } catch (e: InterruptedException) {
            logger.info { "keepalive thread death" }
        }
    }, "keepalive").apply { isDaemon = false }

    override suspend fun initialize() {
        thread.start()
    }

    override suspend fun shutdown() {
        thread.interrupt()
    }
}