package no.nav.helse.spleis.e2e

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.spleis.e2e.SpleisDataSource.migratedDb
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

object PostgresContainer {
    val instance by lazy {
        PostgreSQLContainer<Nothing>("postgres:14").apply {
            withReuse(true)
            withLabel("app-navn", "spleis-mediators")
            start()
        }
    }
}

object SpleisDataSource {
    private val instance: HikariDataSource by lazy {
        HikariDataSource().apply {
            initializationFailTimeout = 5000
            username = PostgresContainer.instance.username
            password = PostgresContainer.instance.password
            jdbcUrl = PostgresContainer.instance.jdbcUrl
            connectionTimeout = 1000L
        }
    }

    val migratedDb = instance.also { migrate(it) }
}

private val tabeller = listOf("person", "person_kopi", "melding", "unike_person")
fun resetDatabase() {
    migrate(migratedDb)
}

private fun migrate(dataSource: HikariDataSource, initSql: String = "") =
    Flyway.configure()
        .dataSource(dataSource)
        .baselineOnMigrate(true)
        .cleanDisabled(false)
        .initSql(initSql)
        .load()
        .also { it.clean() }
        .migrate()
