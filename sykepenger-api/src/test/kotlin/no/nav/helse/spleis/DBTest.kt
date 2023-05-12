package no.nav.helse.spleis

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.PostgreSQLContainer

internal abstract class DBTest {
    protected lateinit var dataSource: DataSource
    companion object {
        private val psqlContainer = PostgreSQLContainer<Nothing>("postgres:14").apply {
            withCreateContainerCmdModifier { command -> command.withName("spleis-api") }
            withReuse(true)
            withLabel("app-navn", "spleis-api")
            start()
        }
    }

    @BeforeEach
    fun setupDB() {
        dataSource = runMigration(psqlContainer)
    }

    internal fun clean() = Flyway.configure().dataSource(HikariDataSource(createHikariConfig(psqlContainer))).cleanDisabled(false).load().clean()
    internal fun migrate() = runMigration(psqlContainer)
    private fun runMigration(psql: PostgreSQLContainer<Nothing>): DataSource {
        val dataSource = HikariDataSource(createHikariConfig(psql))
        Flyway.configure()
            .cleanDisabled(false)
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .also { it.clean() }
            .migrate()
        return dataSource
    }


    private fun createHikariConfig(psql: PostgreSQLContainer<Nothing>) =
        HikariConfig().apply {
            this.jdbcUrl = psql.jdbcUrl
            this.username = psql.username
            this.password = psql.password
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            initializationFailTimeout = 5000
            maxLifetime = 30001
        }
}