package no.nav.helse.spleis.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import java.time.Duration
import javax.sql.DataSource

// Understands how to create a data source from environment variables
internal class DataSourceConfiguration(
    private val jdbcUrl: String? = null,
    private val databaseHost: String? = null,
    private val databasePort: String? = null,
    private val databaseUsername: String? = null,
    private val databasePassword: String? = null,
    private val databaseName: String? = null,
    private val vaultMountPath: String? = null
) {
    private val shouldGetCredentialsFromVault = vaultMountPath != null

    // username and password is only needed when vault is not enabled,
    // since we rotate credentials automatically when vault is enabled
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = this@DataSourceConfiguration.jdbcUrl ?: String.format(
            "jdbc:postgresql://%s:%s/%s%s",
            requireNotNull(databaseHost) { "database host must be set if jdbc url is not provided" },
            requireNotNull(databasePort) { "database port must be set if jdbc url is not provided" },
            requireNotNull(databaseName) { "database name must be set if jdbc url is not provided" },
            databaseUsername?.let { "?user=$it" } ?: "")

        databaseUsername?.let { this.username = it }
        databasePassword?.let { this.password = it }

        maximumPoolSize = 3
        minimumIdle = 1
        connectionTimeout = Duration.ofSeconds(5).toMillis()
        maxLifetime = Duration.ofMinutes(30).toMillis()
        idleTimeout = Duration.ofMinutes(10).toMillis()
    }

    init {
        if (!shouldGetCredentialsFromVault) {
            if (jdbcUrl == null) {
                checkNotNull(databaseUsername) { "username must be set when vault is disabled" }
                checkNotNull(databasePassword) { "password must be set when vault is disabled" }
            }
        } else {
            check(null == databaseUsername) { "username must not be set when vault is enabled" }
            check(null == databasePassword) { "password must not be set when vault is enabled" }
            checkNotNull(databaseName) { "database name must be set when vault is enabled" }
        }
    }

    fun getDataSource(role: Role = Role.User): DataSource {
        if (!shouldGetCredentialsFromVault) return HikariDataSource(hikariConfig)
        return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(hikariConfig, vaultMountPath, "$databaseName-$role")
    }

    enum class Role {
        Admin, User, ReadOnly;
        override fun toString() = name.toLowerCase()
    }
}
