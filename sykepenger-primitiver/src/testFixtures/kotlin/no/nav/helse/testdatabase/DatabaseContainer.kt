package no.nav.helse.testdatabase

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Én delt, gjenbrukt Testcontainers-database per modul (merket med [appnavn]), migrert én gang
 * med Flyway — samme oppsett som brukes i spesialist-repoet
 * (`TestcontainersDatabase`/`ModuleIsolatedDBTestFixture`).
 *
 * Testene bruker unike fødselsnumre (se `nyttFødselsnummer()`) i stedet for å truncate mellom
 * hver test, så vi trenger verken en pool av databaser eller en cleanup-strategi — én database
 * og én Hikari-pool holder, så lenge poolen er stor nok til å dekke JUnit-parallellismen.
 *
 * Containeren gjenbrukes på tvers av testkjøringer (`.withReuse(true)`) dersom
 * Testcontainers er satt opp med `testcontainers.reuse.enable=true`.
 */
class DatabaseContainer(
    appnavn: String,
    postgresVersjon: Int = 17,
    maxHikariPoolSize: Int = 8,
) {
    private val postgres = PostgreSQLContainer("postgres:$postgresVersjon")
        .withReuse(true)
        .withLabel("app-navn", appnavn)
        .apply { start() }

    private val dataSource: HikariDataSource by lazy {
        HikariDataSource(
            HikariConfig().apply {
                poolName = appnavn
                jdbcUrl = postgres.jdbcUrl
                username = postgres.username
                password = postgres.password
                maximumPoolSize = maxHikariPoolSize
            }
        )
    }

    private val delt by lazy { TestDataSource(dataSource) }

    init {
        Flyway.configure()
            .dataSource(dataSource)
            .lockRetryCount(-1)
            .load()
            .migrate()
    }

    // API-kompatibilitetsshim for eksisterende testkode: siden alle tester nå bruker unike
    // fødselsnumre holder det med én delt datakilde — ingen truncate eller retur av tilkobling
    // trengs mellom testene.
    fun nyTilkobling(): TestDataSource = delt
}
