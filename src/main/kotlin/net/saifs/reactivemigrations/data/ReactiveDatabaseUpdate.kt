package net.saifs.reactivemigrations.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReactiveDatabaseUpdate(
    private val sql: String,
    private val database: Database
) : DatabaseUpdate {
    private val bindings: MutableMap<String, Any> = mutableMapOf()

    override fun bind(key: String, value: Any): DatabaseUpdate {
        bindings[key] = value
        return this
    }

    override suspend fun execute(): Int = withContext(Dispatchers.IO) {
        val parsed = ParsedSQL.parse(sql)
        database.connection.use { connection ->
            connection.prepareStatement(parsed.sql).use { statement ->
                parsed.params.forEachIndexed { index, paramName ->
                    statement.setObject(index + 1, bindings[paramName] ?: error("missing binding: $paramName"))
                }
                return@withContext statement.executeUpdate()
            }
        }
    }
}