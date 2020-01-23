package no.nav.helse.component

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.hendelser.ModelSendtSøknad
import no.nav.helse.hendelser.ModelSendtSøknad.Periode
import no.nav.helse.oktober
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Person
import no.nav.helse.september
import no.nav.helse.spleis.db.LagrePersonDao
import no.nav.helse.spleis.db.PersonPostgresRepository
import no.nav.helse.toJsonNode
import no.nav.syfo.kafka.sykepengesoknad.dto.*
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
        private val objectMapper = jacksonObjectMapper()

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
        val parsedHentetPerson = objectMapper.readTree(hentetPerson!!.memento().state())
        assertEquals("fnr", parsedHentetPerson["fødselsnummer"].textValue())
    }

    @Test
    internal fun `hver endring av person fører til at ny versjon lagres`() {
        val dataSource = HikariDataSource(hikariConfig)

        val aktørId = "3"
        val person = Person(aktørId, "fnr")
        person.addObserver(LagrePersonDao(dataSource))
        person.håndter(nySøknad(aktørId))
        person.håndter(ModelSendtSøknad(
            UUID.randomUUID(),
            "fnr",
            aktørId,
            "123456789",
            LocalDateTime.now(),
            listOf(Periode.Sykdom(16.september, 5.oktober, 100)),
            Aktivitetslogger(),
            SykepengesoknadDTO(
                id = "123",
                type = SoknadstypeDTO.ARBEIDSTAKERE,
                status = SoknadsstatusDTO.SENDT,
                aktorId = aktørId,
                fnr = "fnr",
                sykmeldingId = UUID.randomUUID().toString(),
                arbeidsgiver = ArbeidsgiverDTO(
                    "Hello world",
                    "123456789"
                ),
                fom = 16.september,
                tom = 5.oktober,
                opprettet = LocalDateTime.now(),
                sendtNav = LocalDateTime.now(),
                egenmeldinger = emptyList(),
                soknadsperioder = listOf(
                    SoknadsperiodeDTO(16.september, 5.oktober,100)
                ),
                fravar = emptyList()
            ).toJsonNode().toString()
        ))

        val alleVersjoner = using(sessionOf(dataSource)) { session ->
            session.run(queryOf("SELECT data FROM person WHERE aktor_id = ? ORDER BY id", aktørId).map {
                Person.restore(Person.Memento.fromString(it.string("data")))
            }.asList)
        }
        assertEquals(2, alleVersjoner.size, "Antall versjoner av personaggregat skal være 2, men var ${alleVersjoner.size}")
    }

    private fun nySøknad(aktørId: String) = ModelNySøknad(
        UUID.randomUUID(),
        "fnr",
        aktørId,
        "123456789",
        LocalDateTime.now(),
        listOf(Triple(16.september, 5.oktober, 100)),
        Aktivitetslogger(),
        SykepengesoknadDTO(
            id = "123",
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            status = SoknadsstatusDTO.NY,
            aktorId = aktørId,
            fnr = "fnr",
            sykmeldingId = UUID.randomUUID().toString(),
            arbeidsgiver = ArbeidsgiverDTO(
                "Hello world",
                "123456789"
            ),
            fom = 16.september,
            tom = 5.oktober,
            opprettet = LocalDateTime.now(),
            egenmeldinger = emptyList(),
            soknadsperioder = listOf(
                SoknadsperiodeDTO(16.september, 5.oktober,100)
            ),
            fravar = emptyList()
        ).toJsonNode().toString()
    )

}
