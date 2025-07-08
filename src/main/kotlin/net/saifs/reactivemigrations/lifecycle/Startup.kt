package net.saifs.reactivemigrations.lifecycle

import com.github.ajalt.clikt.core.main
import kotlinx.coroutines.runBlocking
import net.saifs.reactivemigrations.extension.context
import net.saifs.reactivemigrations.extension.inject
import net.saifs.reactivemigrations.extension.logger
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.stereotype.Component

private val startup by inject<Startup>()

@Component
class Startup(
    private val initializers: List<Initializer>,
    private val shutdowns: List<ShutdownTask>
) {
    private val logger by logger()

    suspend fun startup() {
        Runtime.getRuntime().addShutdownHook(Thread {
            runBlocking {
                shutdowns.forEach { shutdownTask ->
                    try {
                        shutdownTask.shutdown()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })

        logger.info { "Starting ${initializers.size} initializers" }
        for (initializer in initializers) {
            initializer.initialize()
        }

        logger.info { "Finished initializing" }
    }
}

fun main(args: Array<String>) = runBlocking {
    context = AnnotationConfigApplicationContext()
    val command = ReactiveTransferCommand()
    command.main(args)
    context.scan("net.saifs.reactivemigrations")
    context.refresh()
    context.start()
    val start = System.currentTimeMillis()
    startup.startup()
    val elapsed = (System.currentTimeMillis() - start) / 1000
    println("Migration completed in $elapsed seconds")
    Runtime.getRuntime().exit(0)
}