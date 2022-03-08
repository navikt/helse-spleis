package no.nav.helse

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway
import java.time.Duration

// Understands how to create a data source from environment variables
internal class OnPremDataSourceBuilder(env: Map<String, String>): DataSourceBuilder {
    private val databaseName = env["DATABASE_NAME"]

    private val vaultMountPath = env["VAULT_MOUNTPATH"]
    private val shouldGetCredentialsFromVault = vaultMountPath != null

    // username and password is only needed when vault is not enabled,
    // since we rotate credentials automatically when vault is enabled
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = env["DATABASE_JDBC_URL"] ?: String.format(
            "jdbc:postgresql://%s:%s/%s%s",
            requireNotNull(env["DATABASE_HOST"]) { "database host must be set if jdbc url is not provided" },
            requireNotNull(env["DATABASE_PORT"]) { "database port must be set if jdbc url is not provided" },
            requireNotNull(databaseName) { "database name must be set if jdbc url is not provided" },
            env["DATABASE_USERNAME"]?.let { "?user=$it" } ?: "")

        env["DATABASE_USERNAME"]?.let { this.username = it }
        env["DATABASE_PASSWORD"]?.let { this.password = it }

        maximumPoolSize = 1
        connectionTimeout = Duration.ofSeconds(5).toMillis()
        maxLifetime = Duration.ofMinutes(30).toMillis()
    }

    init {
        if (!shouldGetCredentialsFromVault) {
            if (!env.containsKey("DATABASE_JDBC_URL")) {
                checkNotNull(env["DATABASE_USERNAME"]) { "username must be set when vault is disabled" }
                checkNotNull(env["DATABASE_PASSWORD"]) { "password must be set when vault is disabled" }
            }
        } else {
            check(null == env["DATABASE_USERNAME"]) { "username must not be set when vault is enabled" }
            check(null == env["DATABASE_PASSWORD"]) { "password must not be set when vault is enabled" }
            checkNotNull(env["DATABASE_NAME"]) { "database name must be set when vault is enabled" }
        }
    }

    private fun getDataSource(role: Role): HikariDataSource {
        if (!shouldGetCredentialsFromVault) return HikariDataSource(hikariConfig)
        return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
            hikariConfig,
            vaultMountPath,
            "$databaseName-$role"
        )
    }

    override fun getDataSource(): HikariDataSource {
        return getDataSource(Role.User)
    }

    override fun migrate() {
        val initSql = if (shouldGetCredentialsFromVault) "SET ROLE \"$databaseName-${Role.Admin}\"" else null

        getDataSource(Role.Admin).use { dataSource ->
            Flyway.configure()
                .dataSource(dataSource)
                .initSql(initSql)
                .load()
                .migrate()
        }
    }

    enum class Role {
        Admin, User;

        override fun toString() = name.lowercase()
    }
}
