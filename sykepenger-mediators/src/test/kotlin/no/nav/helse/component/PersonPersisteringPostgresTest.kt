package no.nav.helse.component

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.hendelser.NySøknad
import no.nav.helse.hendelser.SendtSøknad
import no.nav.helse.hendelser.SendtSøknad.Periode
import no.nav.helse.oktober
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Person
import no.nav.helse.september
import no.nav.helse.spleis.db.LagrePersonDao
import no.nav.helse.spleis.db.PersonPostgresRepository
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.LocalDateTime
import java.util.*

class PersonPersisteringPostgresTest {

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
    internal fun `skal gi null når person ikke finnes`() {
        val repo = PersonPostgresRepository(HikariDataSource(hikariConfig))

        assertNull(repo.hentPerson("1"))
    }

    @Test
    internal fun `skal returnere person når person blir lagret etter tilstandsendring`() {
        val dataSource = HikariDataSource(hikariConfig)
        val repo = PersonPostgresRepository(dataSource)

        val person = Person("2", "fnr")
        person.addObserver(LagrePersonDao(dataSource))
        person.håndter(nySøknad("2"))

        val hentetPerson = repo.hentPerson("2")
        assertNotNull(hentetPerson)
    }

    @Test
    internal fun `hver endring av person fører til at ny versjon lagres`() {
        val dataSource = HikariDataSource(hikariConfig)

        val aktørId = "3"
        val person = Person(aktørId, "fnr")
        person.addObserver(LagrePersonDao(dataSource))
        person.håndter(nySøknad(aktørId))
        person.håndter(
            SendtSøknad(
                hendelseId = UUID.randomUUID(),
                fnr = "fnr",
                aktørId = aktørId,
                orgnummer = "123456789",
                sendtNav = LocalDateTime.now(),
                perioder = listOf(Periode.Sykdom(16.september, 5.oktober, 100)),
                aktivitetslogger = Aktivitetslogger(),
                aktivitetslogg = Aktivitetslogg(),
                harAndreInntektskilder = false
            )
        )

        val antallVersjoner = using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT count(data) as rowCount FROM person WHERE aktor_id = ?",
                    aktørId
                )
                    .map { it.int("rowCount") }
                    .asSingle
            )
        }
        assertEquals(2, antallVersjoner, "Antall versjoner av personaggregat skal være 2, men var $antallVersjoner")
    }

    @Test
    internal fun `ignorerer en skjema versjoner når man henter ut personer`() {
        val dataSource = HikariDataSource(hikariConfig)
        val personRepository = PersonPostgresRepository(dataSource)
        val aktørId = "1234"
        val fnr = "4567"

        using(sessionOf(dataSource)) { session ->
            session.run(queryOf(
                "INSERT INTO person (aktor_id, fnr, skjema_versjon, data) VALUES (?, ?, -1, '{}')",
                aktørId, fnr
            ).asExecute)
        }

        assertNull(personRepository.hentPerson(aktørId))
    }

    private fun nySøknad(aktørId: String) = NySøknad(
        hendelseId = UUID.randomUUID(),
        fnr = "fnr",
        aktørId = aktørId,
        orgnummer = "123456789",
        rapportertdato = LocalDateTime.now(),
        sykeperioder = listOf(Triple(16.september, 5.oktober, 100)),
        aktivitetslogger = Aktivitetslogger(),
        aktivitetslogg = Aktivitetslogg()
    )

}
