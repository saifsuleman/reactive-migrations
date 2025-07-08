package net.saifs.reactivemigrations.data

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.io.File
import java.sql.Connection

@Primary
@Component
class HikariDatabase : Database {
    private val dataSource: HikariDataSource
    override val connection: Connection get() = dataSource.connection

    init {
        val file = File("reactive-migrations.duckdb.db")
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:duckdb:${file.absolutePath}"
        config.maximumPoolSize = 32
        config.driverClassName = "org.duckdb.DuckDBDriver";
        this.dataSource = HikariDataSource(config)
    }
}