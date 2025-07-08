package net.saifs.reactivemigrations.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import java.sql.Connection
import java.sql.ResultSet

interface Database {
    val connection: Connection

    fun query(sql: String): DatabaseQuery {
        return ReactiveDatabaseQuery(sql, this)
    }

    fun update(sql: String): DatabaseUpdate {
        return ReactiveDatabaseUpdate(sql, this)
    }
}

inline fun <reified T> Database.batch(sql: String) = ReactiveDatabaseBatch<T>(sql, this)

interface DatabaseQuery : QueryBuilder<DatabaseQuery>, Flow<Row> {
    suspend fun flow(): Flow<Row>
    override suspend fun collect(collector: FlowCollector<Row>) = flow().collect(collector)
}

interface DatabaseUpdate : QueryBuilder<DatabaseUpdate> {
    suspend fun execute(): Int
}

interface DatabaseBatch<T> {
    suspend fun execute(values: Collection<T>, builder: QueryBuilder<DatabaseBatch<T>>.(T) -> Unit): IntArray
}


interface QueryBuilder<T> {
    fun bind(key: String, value: Any): T
}