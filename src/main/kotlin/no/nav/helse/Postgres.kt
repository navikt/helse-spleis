package no.nav.helse

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway

@KtorExperimentalAPI
fun ApplicationConfig.isVaultEnabled() =
        propertyOrNull("database.vault.mountpath") != null

enum class Role {
    Admin, User, ReadOnly;

    override fun toString() = name.toLowerCase()
}

@KtorExperimentalAPI
fun Application.getDataSource(hikariConfig: HikariConfig) =
        if (environment.config.isVaultEnabled()) {
            dataSourceFromVault(hikariConfig, Role.User)
        } else {
            HikariDataSource(hikariConfig)
        }

@KtorExperimentalAPI
fun Application.dataSourceFromVault(hikariConfig: HikariConfig, role: Role) =
        HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
                hikariConfig,
                environment.config.property("database.vault.mountpath").getString(),
                "${environment.config.property("database.name").getString()}-$role"
        )

@KtorExperimentalAPI
fun Application.migrate(hikariConfig: HikariConfig) =
    if (environment.config.isVaultEnabled()) {
        runMigration(dataSourceFromVault(hikariConfig, Role.Admin), "SET ROLE \"${environment.config.property("database.name").getString()}-${Role.Admin}\"")
    } else {
        runMigration(HikariDataSource(hikariConfig))
    }

fun runMigration(dataSource: HikariDataSource, initSql: String? = null) =
        Flyway.configure()
                .dataSource(dataSource)
                .initSql(initSql)
                .load()
                .migrate()
