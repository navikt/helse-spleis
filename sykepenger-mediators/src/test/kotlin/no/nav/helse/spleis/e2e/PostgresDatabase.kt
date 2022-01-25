package no.nav.helse.spleis.e2e

import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.spleis.e2e.SpleisDataSource.migratedDb
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy

object PostgresContainer {
    val instance by lazy {
        PostgreSQLContainer<Nothing>("postgres:12.1").apply {
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

private val tabeller = listOf("person", "melding", "unike_person")
fun resetDatabase() {
    using(sessionOf(migratedDb)) { session -> tabeller.forEach { table -> session.run(queryOf("truncate $table cascade").asExecute) } }
}

private fun migrate(dataSource: HikariDataSource, initSql: String = "") =
    Flyway.configure()
        .dataSource(dataSource)
        .baselineOnMigrate(true)
        .initSql(initSql)
        .load()
        .migrate()
