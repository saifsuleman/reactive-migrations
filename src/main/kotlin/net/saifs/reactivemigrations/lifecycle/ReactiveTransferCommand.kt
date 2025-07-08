package net.saifs.reactivemigrations.lifecycle

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.runBlocking
import net.saifs.reactivemigrations.config.TransferConfig
import net.saifs.reactivemigrations.extension.context
import net.saifs.reactivemigrations.storage.StorageFactory

class ReactiveTransferCommand : CliktCommand("reactivetransfer"){
    private val source by option("--source", help = "Source storage path").required()
    private val target by option("--target", help = "Target storage path").required()
    private val allowDelete by option("--allow-delete", help = "Allow deletion of files on target").flag(default = false)
    private val syncMetadata by option("--sync-metadata", help = "Sync metadata between source and target").flag(default = false)
    private val concurrency by option("--concurrency", help = "Number of concurrent transfers").int().default(200)
    private val tps by option("--tps", help = "Transfers per second").int().default(2000)

    override fun run() = runBlocking {
        val config = TransferConfig(
            source = StorageFactory.resolve(source),
            target = StorageFactory.resolve(target),
            allowDelete = allowDelete,
            syncMetadata = syncMetadata,
            concurrency = concurrency,
            tps = tps
        )

        context.beanFactory.registerSingleton("transferConfig", config)
    }
}