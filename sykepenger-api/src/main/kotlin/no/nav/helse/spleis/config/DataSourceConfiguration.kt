package no.nav.helse.spleis.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.time.Duration

// Understands how to create a data source from environment variables
internal class DataSourceConfiguration(
    private val jdbcUrl: String? = null,
    private val gcpProjectId: String? = null,
    private val databaseRegion: String? = null,
    private val databaseInstance: String? = null,
    private val databaseUsername: String? = null,
    private val databasePassword: String? = null,
    private val databaseName: String? = null
) {
    // username and password is only needed when vault is not enabled,
    // since we rotate credentials automatically when vault is enabled
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = this@DataSourceConfiguration.jdbcUrl ?: kotlin.run {
            requireNotNull(gcpProjectId) { "gcp project id must be set if jdbd url is not provided" }
            requireNotNull(databaseRegion) { "database region must be set if jdbd url is not provided" }
            requireNotNull(databaseInstance) { "database instance must be set if jdbd url is not provided" }
            requireNotNull(databaseName) { "database name must be set if jdbc url is not provided" }
            String.format(
                "jdbc:postgresql:///%s?%s&%s",
                databaseName,
                "cloudSqlInstance=$gcpProjectId:$databaseRegion:$databaseInstance",
                "socketFactory=com.google.cloud.sql.postgres.SocketFactory"
            )
        }

        databaseUsername?.let { this.username = it }
        databasePassword?.let { this.password = it }

        maximumPoolSize = 3
        minimumIdle = 1
        connectionTimeout = Duration.ofSeconds(5).toMillis()
        maxLifetime = Duration.ofMinutes(30).toMillis()
        idleTimeout = Duration.ofMinutes(10).toMillis()
        initializationFailTimeout = Duration.ofMinutes(1).toMillis()
    }

    internal fun getDataSource() = HikariDataSource(hikariConfig)
}
