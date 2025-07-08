package net.saifs.reactivemigrations.storage

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import net.saifs.reactivemigrations.data.Database
import net.saifs.reactivemigrations.data.batch
import net.saifs.reactivemigrations.extension.logger
import net.saifs.reactivemigrations.lifecycle.Initializer
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Order(100)
@Component
@ExperimentalCoroutinesApi
class StorageTable(
    private val database: Database,
) : Initializer {
    private val logger by logger()

    override suspend fun initialize() {
        logger.info { "initializing tables..." }

        database.update(
            """
            CREATE TABLE IF NOT EXISTS storage_files (
                storage_id TEXT NOT NULL,
                path TEXT NOT NULL,
                size BIGINT NOT NULL,
                checksum TEXT NOT NULL,
                last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
                PRIMARY KEY (storage_id, path)
            )
        """.trimIndent()
        ).execute()

        database.update(
            """
            CREATE INDEX IF NOT EXISTS idx_storage_files_storage_id ON storage_files (storage_id)
             """.trimIndent()
        ).execute()

        database.update("""
            TRUNCATE TABLE storage_files
        """.trimIndent()).execute()
    }

    suspend fun ingest(storage: Storage, batch: Int = 500, concurrency: Int = 4) {
        logger.info { "ingesting storage..." }
        storage
            .all()
            .chunked(batch)
            .flatMapMerge(concurrency) { entries ->
                flow {
                    logger.info { "ingesting ${entries.size} entries" }

                    database.batch<StorageFile>(
                        """
                    INSERT INTO storage_files (storage_id, path, size, checksum, last_modified)
                    VALUES (:storage_id, :path, :size, :checksum, NOW())
                    ON CONFLICT (storage_id, path) DO UPDATE SET
                        size = EXCLUDED.size,
                        checksum = EXCLUDED.checksum,
                        last_modified = EXCLUDED.last_modified
                """.trimIndent()
                    )
                        .execute(entries) { file ->
                            bind("storage_id", storage.id)
                            bind("path", file.path)
                            bind("size", file.size)
                            bind("checksum", file.checksum)
                        }
                    emit(Unit)
                }
            }
            .collect()
        logger.info { "storage ingested" }
    }

    fun diff(source: Storage, target: Storage, pageSize: Int): Flow<PaginatedDiff> {
        return database.query(
            """
            SELECT
              COALESCE(s.path, t.path) AS path,
              COALESCE(s.size, t.size) AS size,
              COALESCE(s.checksum, t.checksum) AS checksum,
              CASE
                WHEN s.path IS NOT NULL AND t.path IS NULL THEN 'added'
                WHEN s.path IS NULL AND t.path IS NOT NULL THEN 'removed'
                WHEN s.checksum != t.checksum OR s.size != t.size THEN 'modified'
                ELSE NULL
              END AS diff_type
            FROM
              storage_files s
            FULL OUTER JOIN storage_files t
              ON s.path = t.path
                 AND s.storage_id = :source_id
                 AND t.storage_id = :target_id
            WHERE
              (
                (s.storage_id = :source_id AND t.storage_id IS NULL) -- added (exists in source, not in target)
                OR (s.storage_id IS NULL AND t.storage_id = :target_id) -- removed (exists in target, not in source)
                OR (
                  s.storage_id = :source_id AND
                  t.storage_id = :target_id AND
                  (s.checksum != t.checksum OR s.size != t.size) -- modified
                )
              )
            ORDER BY path;
        """.trimIndent()
        )
            .bind("source_id", source.id)
            .bind("target_id", target.id)
            .map { row ->
                val file = StorageFile(
                    path = row.string("path"),
                    size = row.long("size"),
                    checksum = row.string("checksum")
                )
                row.string("diff_type") to file
            }
            .chunked(pageSize)
            .map { rows ->
                val grouped = rows.groupBy({ it.first }, { it.second })
                logger.info { "row hit" }
                PaginatedDiff(
                    added = grouped["added"] ?: emptyList(),
                    modified = grouped["modified"] ?: emptyList(),
                    removed = grouped["removed"] ?: emptyList()
                )
            }
    }
}