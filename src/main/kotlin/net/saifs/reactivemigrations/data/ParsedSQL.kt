package net.saifs.reactivemigrations.data

data class ParsedSQL(val sql: String, val params: List<String>) {
    companion object {
        fun parse(namedSql: String): ParsedSQL {
            val params = mutableListOf<String>()
            val parsedSql = StringBuilder()
            var i = 0
            while (i < namedSql.length) {
                if (namedSql[i] == ':' && (i + 1 < namedSql.length && namedSql[i + 1].isLetter())) {
                    i++
                    val start = i
                    while (i < namedSql.length && (namedSql[i].isLetterOrDigit() || namedSql[i] == '_')) i++
                    params += namedSql.substring(start, i)
                    parsedSql.append('?')
                } else {
                    parsedSql.append(namedSql[i])
                    i++
                }
            }
            return ParsedSQL(parsedSql.toString(), params)
        }
    }
}