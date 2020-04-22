package no.nav.helse.spleis.e2e

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.HendelseMediator
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.TestMessageFactory
import no.nav.helse.spleis.db.HendelseRecorder
import no.nav.helse.spleis.db.LagrePersonDao
import no.nav.helse.spleis.db.PersonPostgresRepository
import no.nav.helse.spleis.meldinger.TestRapid
import no.nav.helse.testhelpers.januar
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.PeriodeDTO
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.sql.Connection
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal abstract class AbstractEndToEndMediatorTest {
    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
        private val INNTEKT = 31000.00
    }

    private val meldingsfabrikk = TestMessageFactory(UNG_PERSON_FNR_2018, AKTØRID, ORGNUMMER, INNTEKT)
    protected val testRapid = TestRapid()
    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection
    private lateinit var dataSource: DataSource
    private lateinit var hendelseMediator: HendelseMediator
    private lateinit var messageMediator: MessageMediator

    @BeforeAll
    internal fun setupAll(@TempDir postgresPath: Path) {
        embeddedPostgres = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(postgresPath.toFile())
            .setDataDirectory(postgresPath.resolve("datadir"))
            .start()
        postgresConnection = embeddedPostgres.postgresDatabase.connection
        val hikariConfig = createHikariConfig(embeddedPostgres.getJdbcUrl("postgres", "postgres"))
        dataSource = HikariDataSource(hikariConfig)

        hendelseMediator = HendelseMediator(
            rapidsConnection = testRapid,
            personRepository = PersonPostgresRepository(dataSource),
            lagrePersonDao = LagrePersonDao(dataSource)
        )

        messageMediator = MessageMediator(
            rapidsConnection = testRapid,
            hendelseMediator = hendelseMediator,
            hendelseRecorder = HendelseRecorder(dataSource)
        )
    }

    @AfterAll
    internal fun teardown() {
        postgresConnection.close()
        embeddedPostgres.close()
    }

    @BeforeEach
    internal fun setupEach() {
        Flyway
            .configure()
            .dataSource(dataSource)
            .load()
            .also {
                it.clean()
                it.migrate()
            }

        testRapid.reset()
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

    protected fun sendNySøknad(vararg perioder: SoknadsperiodeDTO) {
        testRapid.sendTestMessage(meldingsfabrikk.lagNySøknad(*perioder))
    }

    protected fun sendSøknad(
        vedtaksperiodeIndeks: Int,
        perioder: List<SoknadsperiodeDTO>,
        egenmeldinger: List<PeriodeDTO> = emptyList()
    ) {
        assertFalse(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Inntektsberegning))
        assertFalse(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Opptjening))
        assertFalse(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, EgenAnsatt))
        testRapid.sendTestMessage(meldingsfabrikk.lagSøknadNav(perioder, egenmeldinger))
    }

    protected fun sendInnteksmelding(
        vedtaksperiodeIndeks: Int,
        arbeidsgiverperiode: List<Periode>,
        førsteFraværsdag: LocalDate
    ) {
        assertFalse(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Inntektsberegning))
        assertFalse(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Opptjening))
        assertFalse(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, EgenAnsatt))
        testRapid.sendTestMessage(meldingsfabrikk.lagInnteksmelding(arbeidsgiverperiode, førsteFraværsdag))
    }

    protected fun sendNyPåminnelse() {
        testRapid.sendTestMessage(meldingsfabrikk.lagPåminnelse(UUID.randomUUID(), TilstandType.START))
    }

    protected fun sendManuellSaksbehandling(vedtaksperiodeIndeks: Int, godkjent: Boolean = true) {
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Godkjenning))
        testRapid.sendTestMessage(meldingsfabrikk.lagManuellSaksbehandling(
            vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks),
            tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(vedtaksperiodeIndeks, Godkjenning),
            utbetalingGodkjent = godkjent
        ))
    }

    protected fun sendYtelserUtenHistorikk(vedtaksperiodeIndeks: Int) {
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Sykepengehistorikk))
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Foreldrepenger))
        testRapid.sendTestMessage(meldingsfabrikk.lagYtelser(
            vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks),
            tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(vedtaksperiodeIndeks, Sykepengehistorikk)
        ))
    }

    protected fun sendVilkårsgrunnlag(
        vedtaksperiodeIndeks: Int,
        egenAnsatt: Boolean = false,
        inntekter: List<Pair<YearMonth, Double>> = 1.rangeTo(12).map { YearMonth.of(2018, it) to INNTEKT },
        opptjening: List<Triple<String, LocalDate, LocalDate?>> = listOf(
            Triple(
                ORGNUMMER,
                1.januar(2010),
                null
            )
        ),
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja
    ) {
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Inntektsberegning))
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, EgenAnsatt))
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Opptjening))
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Dagpenger))
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Arbeidsavklaringspenger))
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Medlemskap))
        testRapid.sendTestMessage(meldingsfabrikk.lagVilkårsgrunnlag(
            vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks),
            tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(vedtaksperiodeIndeks, Inntektsberegning),
            egenAnsatt = egenAnsatt,
            inntekter = inntekter,
            opptjening = opptjening,
            medlemskapstatus = medlemskapstatus
        ))
    }

    protected fun sendSimulering(vedtaksperiodeIndeks: Int, simuleringOK: Boolean = true) {
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Simulering))
        testRapid.sendTestMessage(meldingsfabrikk.lagSimulering(
            vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks),
            tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(vedtaksperiodeIndeks, Simulering),
            simuleringOK = simuleringOK
        ))
    }

    protected fun sendUtbetaling(vedtaksperiodeIndeks: Int, utbetalingOK: Boolean = true) {
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Utbetaling))
        testRapid.sendTestMessage(meldingsfabrikk.lagUtbetaling(
            vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks),
            tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(vedtaksperiodeIndeks, Simulering),
            utbetalingOK = utbetalingOK
        ))
    }

    protected fun sendKansellerUtbetaling() {
        val fagsystemId = testRapid.inspektør.let {
            it.melding(it.antall() - 1)["fagsystemId"]
        }.asText()
        testRapid.sendTestMessage(meldingsfabrikk.lagKansellerUtbetaling(fagsystemId))
    }

    protected fun assertTilstander(vedtaksperiodeIndeks: Int, vararg tilstand: String) {
        assertEquals(
            tilstand.toList(),
            testRapid.inspektør.tilstander(testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks))
        )
    }
}
