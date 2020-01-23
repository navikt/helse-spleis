package no.nav.helse.component

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.spleis.db.HendelseRecorder
import no.nav.helse.spleis.hendelser.JsonMessage
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.Connection
import javax.sql.DataSource

class HendelsePersisteringPostgresTest {

    companion object {
        private lateinit var embeddedPostgres: EmbeddedPostgres
        private lateinit var postgresConnection: Connection

        private lateinit var hikariConfig: HikariConfig

        @BeforeAll
        @JvmStatic
        internal fun `start postgres`() {
            embeddedPostgres = EmbeddedPostgres.builder().start()
            postgresConnection = embeddedPostgres.postgresDatabase.connection
            hikariConfig = createHikariConfig(embeddedPostgres.getJdbcUrl("postgres", "postgres"))

            Flyway.configure()
                .dataSource(HikariDataSource(hikariConfig))
                .load()
                .migrate()
        }

        @AfterAll
        @JvmStatic
        internal fun `stop postgres`() {
            postgresConnection.close()
            embeddedPostgres.close()
        }

        private fun createHikariConfig(jdbcUrl: String) =
            HikariConfig().apply {
                this.jdbcUrl = jdbcUrl
                maximumPoolSize = 3
                minimumIdle = 1
                idleTimeout = 10001
                connectionTimeout = 1000
                maxLifetime = 30001
            }
    }

    @Test
    internal fun `hendelser skal lagres`() {
        val dataSource = HikariDataSource(hikariConfig)
        val dao = HendelseRecorder(dataSource)

        JsonMessage("{}", Aktivitetslogger()).also {
            dao.lagreMelding(it)
            assertEquals(1, meldinger(dataSource).size)
        }
    }

    private fun meldinger(dataSource: DataSource): List<JsonMessage> {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf("SELECT data FROM melding ORDER BY id").map {
                    JsonMessage(it.string("data"), Aktivitetslogger())
                }.asList
            )
        }
    }
}
