package net.saifs.reactivemigrations.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReactiveDatabaseBatch<T>(
    private val sql: String,
    private val database: Database,
) : DatabaseBatch<T> {
    private val bindings: BatchBindings = BatchBindings()

    override suspend fun execute(
        values: Collection<T>,
        builder: QueryBuilder<DatabaseBatch<T>>.(T) -> Unit
    ) = withContext(Dispatchers.IO) {
        val parsed = ParsedSQL.parse(sql)
        database.connection.use { connection ->
            connection.prepareStatement(parsed.sql).use { statement ->
                values.forEach { value ->
                    bindings.apply {
                        builder(value)
                        parsed.params.forEachIndexed { paramIndex, paramName ->
                            statement.setObject(paramIndex + 1, this@ReactiveDatabaseBatch.bindings.bindings[paramName] ?: error("missing binding: $paramName"))
                        }
                        // Clear bindings for the next iteration
                        this@ReactiveDatabaseBatch.bindings.bindings.clear()
                    }
                    statement.addBatch()
                }
                return@withContext statement.executeBatch()
            }
        }
    }

    inner class BatchBindings : QueryBuilder<DatabaseBatch<T>> {
        internal val bindings: MutableMap<String, Any> = mutableMapOf()

        override fun bind(key: String, value: Any): DatabaseBatch<T> {
            bindings[key] = value
            return this@ReactiveDatabaseBatch
        }
    }
}