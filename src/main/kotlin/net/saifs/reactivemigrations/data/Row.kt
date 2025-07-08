package net.saifs.reactivemigrations.data

import java.sql.ResultSet

@Suppress("NOTHING_TO_INLINE")
data class Row(@PublishedApi internal val data: Map<String, Any?>) {
    inline fun string(column: String) = get<String>(column)
    inline fun int(column: String) = get<Int>(column)
    inline fun long(column: String) = get<Long>(column)
    inline fun double(column: String) = get<Double>(column)
    inline fun boolean(column: String) = get<Boolean>(column)
    inline fun <reified T> get(column: String): T = data[column] as T
}

internal fun ResultSet.row(): Row {
    val metadata = metaData
    val columnCount = metadata.columnCount
    val data = mutableMapOf<String, Any?>()

    for (i in 1..columnCount) {
        val columnName = metadata.getColumnName(i)
        data[columnName] = getObject(i)
    }

    return Row(data)
}