package net.saifs.reactivemigrations.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.saifs.reactivemigrations.concurrency.RateLimiter
import net.saifs.reactivemigrations.config.TransferConfig
import net.saifs.reactivemigrations.extension.logger
import net.saifs.reactivemigrations.lifecycle.Initializer
import net.saifs.reactivemigrations.metrics.Metrics
import net.saifs.reactivemigrations.storage.PaginatedDiff
import net.saifs.reactivemigrations.storage.StorageTable
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
@ExperimentalCoroutinesApi
class ReactiveTransfer(
    private val config: TransferConfig,
    private val metrics: Metrics,
    private val table: StorageTable,
) : Initializer {
    private val source = config.source
    private val target = config.target
    private val logger by logger()
    private val downloads = RateLimiter(config.tps)
    private val uploads = RateLimiter(config.tps)

    private val operations = AtomicLong(0)
    private val errors = AtomicLong(0)

    override suspend fun initialize() {
        do {
            errors.set(0)
            migrate()
        } while (errors.get() > 0 || operations.get() > 0)
    }

    suspend fun migrate() {
        logger.info { "starting transfer" }
        coroutineScope {
            listOf(
                async { table.ingest(source, batch = config.batchSize, concurrency = config.concurrency) },
                async { table.ingest(target, batch = config.batchSize, concurrency = config.concurrency) },
            ).awaitAll()
        }

        metrics.gauge("operations.pending") { operations.get() }

        logger.info { "starting to diff..." }
        var n = 0
        table.diff(source, target, config.batchSize)
            .collect { diff ->
                n += 1
                logger.info { "Processing diff: ${diff.added.size} added, ${diff.modified.size} modified, ${diff.removed.size} removed" }
                transfer(diff)
            }
        logger.info { "Finished processing $n diffs" }
    }

    @ExperimentalCoroutinesApi
    suspend fun transfer(diff: PaginatedDiff) {
        val toUpload = diff.added + diff.modified
        val toDelete = diff.removed

        operations.addAndGet(toUpload.size.toLong() + toDelete.size.toLong())

        logger.info { "Files to upload: ${toUpload.size}" }
        logger.info { "Files to delete: ${toDelete.size}" }

        toUpload
            .asFlow()
            .flatMapMerge(concurrency = config.concurrency) { file ->
                flow {
                    runCatching {
                        downloads.acquire()
                        val data = metrics.timer("objects.download.duration") { source.download(file) }
                        uploads.acquire()
                        metrics.timer("objects.upload.duration") { target.upload(file, data) }
                        metrics.increment("objects.transferred.total")
                        metrics.increment("bytes.transferred.total", amount = data.size.toDouble())
                    }
                        .onFailure { error ->
                            logger.error(error) { "Failed to transfer file: ${file.path}" }
                            metrics.increment("objects.transfer.failed")
                            errors.incrementAndGet()
                        }
                    operations.decrementAndGet()
                    emit(Unit)
                }
            }
            .flowOn(Dispatchers.IO)
            .collect()

        if (config.allowDelete) {
            toDelete
                .asFlow()
                .flatMapMerge(concurrency = config.concurrency) { file ->
                    flow {
                        runCatching {
                            uploads.acquire() // this is an operation on our target node only so this is technically an upload
                            target.delete(file)
                            metrics.increment("objects.deleted.total")
                        }
                            .onFailure {
                                logger.error(it) { "Failed to delete file: ${file.path}" }
                                metrics.increment("objects.delete.failed")
                                errors.incrementAndGet()
                            }
                        operations.decrementAndGet()
                        emit(Unit)
                    }
                }
                .flowOn(Dispatchers.IO)
                .collect()
        } else {
            logger.warn { "Deletion is not allowed, skipping deletion ${toDelete.size} of files" }
            metrics.increment("objects.delete.skipped", amount = toDelete.size.toDouble())
        }
    }
}