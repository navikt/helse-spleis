package no.nav.helse.component

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.hendelser.ModelSendtSøknad
import no.nav.helse.hendelser.ModelSendtSøknad.Periode
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.løsBehov
import no.nav.helse.oktober
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.september
import no.nav.helse.spleis.HendelseRecorder
import no.nav.helse.toJsonNode
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
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

        nySøknad().also {
            dao.onNySøknad(it, Aktivitetslogger())
            assertHendelse(dataSource, it)
        }

        ModelSendtSøknad(
            hendelseId = UUID.randomUUID(),
            fnr = "1234",
            aktørId = "56789",
            orgnummer = "orgnummer",
            rapportertdato = LocalDateTime.now(),
            perioder = listOf(Periode.Sykdom(LocalDate.now(), LocalDate.now(), 100)),
            aktivitetslogger = Aktivitetslogger(),
            originalJson = "{}"
        ).also {
            dao.onSendtSøknad(it)
            assertHendelse(dataSource, it)
        }

        ModelInntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = ModelInntektsmelding.Refusjon(LocalDate.now(), 1000.00, emptyList()),
            orgnummer = "123456789",
            fødselsnummer = "01018712345",
            aktørId = "aktør",
            mottattDato = LocalDateTime.now(),
            førsteFraværsdag = LocalDate.now(),
            beregnetInntekt = 1000.00,
            aktivitetslogger = Aktivitetslogger(),
            originalJson = "{}",
            arbeidsgiverperioder = listOf(LocalDate.now().minusDays(1)..LocalDate.now()),
            ferieperioder = emptyList()
        ).also {
            dao.onInntektsmelding(it)
            assertHendelse(dataSource, it)
        }

        Vilkårsgrunnlag.Builder().build(
            generiskBehov().løsBehov(
                mapOf(
                    "EgenAnsatt" to false
                )
            ).toJson()
        )!!.also {
            dao.onVilkårsgrunnlag(it)
            assertHendelse(dataSource, it)
        }
    }

    private fun generiskBehov() = Behov.nyttBehov(
        hendelsestype = ArbeidstakerHendelse.Hendelsestype.Vilkårsgrunnlag,
        behov = listOf(),
        aktørId = "aktørId",
        fødselsnummer = "fødselsnummer",
        organisasjonsnummer = "organisasjonsnummer",
        vedtaksperiodeId = UUID.randomUUID(),
        additionalParams = mapOf()
    )

    private fun assertHendelse(dataSource: DataSource, hendelse: ArbeidstakerHendelse) {
        val alleHendelser = using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT data FROM hendelse WHERE aktor_id = ? AND type = ? ORDER BY id",
                    hendelse.aktørId(),
                    hendelse.hendelsetype().name
                ).map {
                    it.string("data")
                }.asList
            )
        }
        assertEquals(1, alleHendelser.size, "Antall hendelser skal være 1, men var ${alleHendelser.size}")
    }

    private fun nySøknad(
        orgnummer: String = "organisasjonsnummer",
        perioder: List<Triple<LocalDate, LocalDate, Int>> = listOf(Triple(16.september, 5.oktober, 100))
    ) = ModelNySøknad(
        UUID.randomUUID(),
        "fødselsnummer",
        "aktørId",
        orgnummer,
        LocalDateTime.now(),
        perioder,
        Aktivitetslogger(),
        SykepengesoknadDTO(
            id = "123",
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            status = SoknadsstatusDTO.NY,
            aktorId = "aktørId",
            fnr = "fødselsnummer",
            sykmeldingId = UUID.randomUUID().toString(),
            arbeidsgiver = ArbeidsgiverDTO(
                "Hello world",
                orgnummer
            ),
            fom = 16.september,
            tom = 5.oktober,
            opprettet = LocalDateTime.now(),
            egenmeldinger = emptyList(),
            soknadsperioder = perioder.map { SoknadsperiodeDTO(it.first, it.second, it.third) }
        ).toJsonNode().toString()
    )
}
