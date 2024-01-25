package no.nav.helse.spleis.mediator.e2e

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.time.Duration
import kotliquery.queryOf
import kotliquery.sessionOf
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

object PostgresContainer {
    private val instance by lazy {
        PostgreSQLContainer<Nothing>("postgres:15").apply {
            withCreateContainerCmdModifier { command -> command.withName("spleis-mediators") }
            withReuse(true)
            withLabel("app-navn", "spleis-mediators")
            start()
        }
    }

    private val systemtilkobling by lazy { instance.createConnection("") }

    fun nyTilkobling(dbnavn: String = "spleis_${kotlin.random.Random.nextInt(from = 0, until = 999999)}"): SpleisDataSource {
        opprettDatabase(dbnavn)
        instance.withDatabaseName(dbnavn)
        return SpleisDataSource(dbnavn, HikariConfig().apply {
            username = instance.username
            password = instance.password
            jdbcUrl = instance.jdbcUrl
        })
    }

    private fun opprettDatabase(dbnavn: String) {
        println("Oppretter databasen $dbnavn")
        systemtilkobling.createStatement().execute("create database $dbnavn")
    }

    fun droppTilkobling(spleisDataSource: SpleisDataSource) {
        spleisDataSource.teardown { dbnavn ->
            println("Dropper databasen $dbnavn")
            systemtilkobling.createStatement().execute("drop database $dbnavn")
        }
    }
}

class SpleisDataSource(
    private val dbnavn: String,
    config: HikariConfig
) {
    private val migrationConfig = HikariConfig()
    private val spleisConfig = HikariConfig()
    private val dataSource: HikariDataSource by lazy { HikariDataSource(spleisConfig) }
    private val migrationDataSource: HikariDataSource by lazy { HikariDataSource(migrationConfig) }

    private val flyway by lazy {
        Flyway.configure()
            .dataSource(migrationDataSource)
            .validateMigrationNaming(true)
            .load()
    }

    init {
        println("Oppretter datasource med dbnavn=$dbnavn")
        config.copyStateTo(migrationConfig)
        config.copyStateTo(spleisConfig)

        migrationConfig.maximumPoolSize = 2 // flyway klarer seg ikke med én connection visstnok
        migrationConfig.initializationFailTimeout = Duration.ofSeconds(10).toMillis()

        spleisConfig.maximumPoolSize = 2 // spleis bruker to connections ved håndtering av en melding (replay av IM tar en tilkobling)
    }

    val ds: HikariDataSource by lazy {
        migrate()
        dataSource
    }

    private fun migrate() {
        println("Migrerer dbnavn=$dbnavn")
        flyway.migrate()
        migrationDataSource.close()
    }

    fun teardown(dropDatabase: (String) -> Unit) {
        dataSource.close()
        dropDatabase(dbnavn)
    }
}