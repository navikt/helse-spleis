package no.nav.helse

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.time.Duration

// Understands how to create a data source from environment variables
internal class DataSourceBuilder(env: Map<String, String>) {

    private val gcpProjectId: String? = env["GCP_TEAM_PROJECT_ID"]
    private val databaseRegion: String? = env["DATABASE_REGION"]
    private val databaseInstance: String? = env["DATABASE_INSTANCE"]
    private val databaseUsername: String? = env["DATABASE_SPLEIS_API_USERNAME"]
    private val databasePassword: String? = env["DATABASE_SPLEIS_API_PASSWORD"]
    private val databaseName: String? = env["DATABASE_DATABASE"]

    init {
        requireNotNull(gcpProjectId) { "gcp project id must be set if jdbd url is not provided" }
        requireNotNull(databaseRegion) { "database region must be set if jdbd url is not provided" }
        requireNotNull(databaseInstance) { "database instance must be set if jdbd url is not provided" }
        requireNotNull(databaseName) { "database name must be set if jdbc url is not provided" }
    }

    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = String.format(
            "jdbc:postgresql:///%s?%s&%s",
            databaseName,
            "cloudSqlInstance=$gcpProjectId:$databaseRegion:$databaseInstance",
            "socketFactory=com.google.cloud.sql.postgres.SocketFactory"
        )

        databaseUsername?.let { this.username = it }
        databasePassword?.let { this.password = it }

        maximumPoolSize = 3
        minimumIdle = 1
        connectionTimeout = Duration.ofSeconds(5).toMillis()
        maxLifetime = Duration.ofMinutes(30).toMillis()
        idleTimeout = Duration.ofMinutes(10).toMillis()
    }

    internal fun getDataSource() = HikariDataSource(hikariConfig)
}
