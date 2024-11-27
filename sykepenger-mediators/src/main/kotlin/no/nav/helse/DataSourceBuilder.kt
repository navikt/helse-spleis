package no.nav.helse

import com.github.navikt.tbd_libs.naisful.postgres.ConnectionConfigFactory
import com.github.navikt.tbd_libs.naisful.postgres.defaultJdbcUrl
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

// Understands how to create a data source from environment variables
internal class DataSourceBuilder(
    env: Map<String, String>,
) {
    private val baseConnectionConfig =
        HikariConfig().apply {
            jdbcUrl = defaultJdbcUrl(ConnectionConfigFactory.Env(env = env, envVarPrefix = "DATABASE"))
        }

    private val migrationConfig =
        HikariConfig().apply {
            baseConnectionConfig.copyStateTo(this)
            poolName = "flyway"
            maximumPoolSize = 2
            metricRegistry = meterRegistry
        }
    private val appConfig =
        HikariConfig().apply {
            baseConnectionConfig.copyStateTo(this)
            poolName = "app"
            maximumPoolSize = 2
            metricRegistry = meterRegistry
        }

    val dataSource by lazy { HikariDataSource(appConfig) }

    internal fun migrate() {
        logger.info("Migrerer database")
        HikariDataSource(migrationConfig).use { flywayDataSource ->
            Flyway
                .configure()
                .dataSource(flywayDataSource)
                .lockRetryCount(-1)
                .load()
                .migrate()
        }
        logger.info("Migrering ferdig!")
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(DataSourceBuilder::class.java)
    }
}
