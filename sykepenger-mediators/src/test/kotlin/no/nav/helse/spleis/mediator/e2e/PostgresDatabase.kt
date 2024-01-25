package no.nav.helse.spleis.mediator.e2e

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

object PostgresContainer {
    private val MAX_POOL_SIZE = maxOf(1, System.getProperty("junit.jupiter.execution.parallel.config.fixed.parallelism")?.toInt() ?: 0)

    private val instance by lazy {
        PostgreSQLContainer<Nothing>("postgres:15").apply {
            withCreateContainerCmdModifier { command -> command.withName("spleis-mediators") }
            withReuse(true)
            withLabel("app-navn", "spleis-mediators")
            start()
        }
    }

    private val systemtilkobling by lazy { instance.createConnection("") }
    private val tilgjengeligeTilkoblinger by lazy {
        ArrayBlockingQueue(MAX_POOL_SIZE, false, opprettTilkoblinger())
    }

    fun nyTilkobling(): SpleisDataSource {
        return tilgjengeligeTilkoblinger.poll(20, TimeUnit.SECONDS) ?: throw RuntimeException("Ventet i 20 sekunder uten å få en ledig database")
    }

    private fun opprettTilkoblinger() =
        (1..MAX_POOL_SIZE).map { opprettTilkobling("spleis_$it") }

    private fun opprettTilkobling(dbnavn: String): SpleisDataSource {
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
        println("Tilgjengeliggjør datbasen igjen")
        spleisDataSource.cleanUp()
        tilgjengeligeTilkoblinger.offer(spleisDataSource)
    }

    fun ryddOpp() {
        tilgjengeligeTilkoblinger.forEach {
            it.teardown { dbnavn ->
                println("Dropper databasen $dbnavn")
                systemtilkobling.createStatement().execute("drop database $dbnavn")
            }
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
            .cleanDisabled(false)
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
    }

    fun cleanUp() {
        println("Rydder opp og forbereder gjenbruk i $dbnavn")
        flyway.clean()
        flyway.migrate()
    }
    fun teardown(dropDatabase: (String) -> Unit) {
        migrationDataSource.close()
        dataSource.close()
        dropDatabase(dbnavn)
    }
}