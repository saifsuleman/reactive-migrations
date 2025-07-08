package net.saifs.reactivemigrations.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ReactiveDatabaseQuery(
    private val sql: String,
    private val database: Database,
) : DatabaseQuery {
    private val bindings: MutableMap<String, Any> = mutableMapOf()

    override suspend fun flow() = flow {
        val parsed = ParsedSQL.parse(sql)
        database.connection.use { connection ->
            connection.prepareStatement(parsed.sql).use { statement ->
                parsed.params.forEachIndexed { index, paramName ->
                    statement.setObject(index + 1, bindings[paramName] ?: error("missing binding: $paramName"))
                }
                val result = statement.executeQuery()
                while (result.next()) {
                    emit(result.row())
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun bind(key: String, value: Any): DatabaseQuery {
        bindings[key] = value
        return this
    }
}