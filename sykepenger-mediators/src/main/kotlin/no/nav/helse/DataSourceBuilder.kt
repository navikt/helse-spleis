package no.nav.helse

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.time.Duration

// Understands how to create a data source from environment variables
internal class DataSourceBuilder(env: Map<String, String>) {

    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = env["DATABASE_JDBC_URL"] ?: String.format(
            "jdbc:postgresql://%s:%s/%s",
            requireNotNull(env["DATABASE_HOST"]) { "database host must be set if jdbc url is not provided" },
            requireNotNull(env["DATABASE_PORT"]) { "database port must be set if jdbc url is not provided" },
            requireNotNull(env["DATABASE_DATABASE"]) { "database name must be set if jdbc url is not provided" })
        username = requireNotNull(env["DATABASE_USERNAME"]) { "databasebrukernavn må settes" }
        password = requireNotNull(env["DATABASE_PASSWORD"]) { "databasepassord må settes" }
        maximumPoolSize = 1
        connectionTimeout = Duration.ofSeconds(5).toMillis()
        maxLifetime = Duration.ofMinutes(30).toMillis()
    }

    internal fun getDataSource() = HikariDataSource(hikariConfig)

    internal fun migrate() {
        getDataSource().use { dataSource ->
            Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate()
        }
    }
}
