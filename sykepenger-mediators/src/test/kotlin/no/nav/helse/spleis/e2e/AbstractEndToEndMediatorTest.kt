package no.nav.helse.spleis.e2e

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.HendelseMediator
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.TestMessageFactory
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.db.LagrePersonDao
import no.nav.helse.spleis.db.PersonPostgresRepository
import no.nav.helse.spleis.meldinger.TestRapid
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.testhelpers.januar
import no.nav.inntektsmeldingkontrakt.OpphoerAvNaturalytelse
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
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal abstract class AbstractEndToEndMediatorTest {
    internal companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
        private const val INNTEKT = 31000.00
    }

    private val meldingsfabrikk = TestMessageFactory(UNG_PERSON_FNR_2018, AKTØRID, ORGNUMMER, INNTEKT)
    protected val testRapid = TestRapid()
    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection
    protected lateinit var dataSource: DataSource
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
            hendelseRepository = HendelseRepository(dataSource),
            lagrePersonDao = LagrePersonDao(dataSource)
        )

        messageMediator = MessageMediator(
            rapidsConnection = testRapid,
            hendelseMediator = hendelseMediator,
            hendelseRepository = HendelseRepository(dataSource)
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

    protected fun sendNySøknad(vararg perioder: SoknadsperiodeDTO) {
        testRapid.sendTestMessage(meldingsfabrikk.lagNySøknad(*perioder))
    }

    protected fun sendSøknad(
        vedtaksperiodeIndeks: Int,
        perioder: List<SoknadsperiodeDTO>,
        egenmeldinger: List<PeriodeDTO> = emptyList()
    ) {
        assertFalse(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, InntekterForSammenligningsgrunnlag))
        assertFalse(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Opptjening))
        testRapid.sendTestMessage(meldingsfabrikk.lagSøknadNav(perioder, egenmeldinger))
    }

    protected fun sendInntektsmelding(
        vedtaksperiodeIndeks: Int,
        arbeidsgiverperiode: List<Periode>,
        førsteFraværsdag: LocalDate,
        opphørAvNaturalytelser: List<OpphoerAvNaturalytelse> = emptyList()
    ) {
        assertFalse(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, InntekterForSammenligningsgrunnlag))
        assertFalse(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Opptjening))
        testRapid.sendTestMessage(meldingsfabrikk.lagInnteksmelding(arbeidsgiverperiode, førsteFraværsdag, opphørAvNaturalytelser))
    }

    protected fun sendNyPåminnelse(vedtaksperiodeIndeks: Int = -1, tilstandType: TilstandType = TilstandType.START) {
        val vedtaksperiodeId = if (vedtaksperiodeIndeks == -1) UUID.randomUUID() else testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks)
        testRapid.sendTestMessage(meldingsfabrikk.lagPåminnelse(vedtaksperiodeId, tilstandType))
    }

    protected fun sendUtbetalingsgodkjenning(
        vedtaksperiodeIndeks: Int,
        godkjent: Boolean = true,
        saksbehandlerIdent: String = "O123456",
        saksbehandlerEpost: String = "jan@banan.no",
        automatiskBehandling: Boolean = false
    ) {
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Godkjenning))
        testRapid.sendTestMessage(
            meldingsfabrikk.lagUtbetalingsgodkjenning(
                vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks),
                tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(vedtaksperiodeIndeks, Godkjenning),
                utbetalingGodkjent = godkjent,
                saksbehandlerIdent = saksbehandlerIdent,
                saksbehandlerEpost = saksbehandlerEpost,
                automatiskBehandling = automatiskBehandling
            )
        )
    }

    protected fun sendYtelser(
        vedtaksperiodeIndeks: Int,
        pleiepenger: List<TestMessageFactory.PleiepengerTestdata> = emptyList(),
        omsorgspenger: List<TestMessageFactory.OmsorgspengerTestdata> = emptyList(),
        opplæringspenger: List<TestMessageFactory.OpplæringspengerTestdata> = emptyList(),
        institusjonsoppholdsperioder: List<TestMessageFactory.InstitusjonsoppholdTestdata> = emptyList()
    ) {
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Sykepengehistorikk))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Foreldrepenger))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Pleiepenger))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Omsorgspenger))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Opplæringspenger))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Institusjonsopphold))
        testRapid.sendTestMessage(
            meldingsfabrikk.lagYtelser(
                vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks),
                tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(vedtaksperiodeIndeks, Sykepengehistorikk),
                pleiepenger = pleiepenger,
                omsorgspenger = omsorgspenger,
                opplæringspenger = opplæringspenger,
                institusjonsoppholdsperioder = institusjonsoppholdsperioder
            )
        )
    }

    protected fun sendVilkårsgrunnlag(
        vedtaksperiodeIndeks: Int,
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
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, InntekterForSammenligningsgrunnlag))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Opptjening))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Dagpenger))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Arbeidsavklaringspenger))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Medlemskap))
        testRapid.sendTestMessage(
            meldingsfabrikk.lagVilkårsgrunnlag(
                vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks),
                tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(
                    vedtaksperiodeIndeks,
                    InntekterForSammenligningsgrunnlag
                ),
                inntekter = inntekter,
                opptjening = opptjening,
                medlemskapstatus = medlemskapstatus
            )
        )
    }

    protected fun sendSimulering(vedtaksperiodeIndeks: Int, status: SimuleringMessage.Simuleringstatus) {
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Simulering))
        testRapid.sendTestMessage(
            meldingsfabrikk.lagSimulering(
                vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks),
                tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(vedtaksperiodeIndeks, Simulering),
                status = status
            )
        )
    }

    protected fun sendUtbetaling(
        vedtaksperiodeIndeks: Int,
        utbetalingOK: Boolean = true,
        saksbehandlerEpost: String = "siri.saksbehanlder@nav.no",
        annullert: Boolean = false
    ) {
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Utbetaling))
        testRapid.sendTestMessage(
            meldingsfabrikk.lagUtbetalingOverført(
                vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks),
                fagsystemId = testRapid.inspektør.etterspurteBehov(Utbetaling).path("fagsystemId").asText(),
                utbetalingId = testRapid.inspektør.etterspurteBehov(Utbetaling).path("utbetalingId").asText(),
                tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(vedtaksperiodeIndeks, Simulering),
                avstemmingsnøkkel = 123456L,
                overføringstidspunkt = LocalDateTime.now()
            )
        )
        testRapid.sendTestMessage(
            meldingsfabrikk.lagUtbetaling(
                vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks),
                fagsystemId = testRapid.inspektør.etterspurteBehov(Utbetaling).path("fagsystemId").asText(),
                utbetalingId = testRapid.inspektør.etterspurteBehov(Utbetaling).path("utbetalingId").asText(),
                tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(vedtaksperiodeIndeks, Simulering),
                saksbehandlerEpost = saksbehandlerEpost,
                annullering = annullert,
                utbetalingOK = utbetalingOK
            )
        )
    }

    protected fun sendRollback(personVersjon: Long) {
        testRapid.sendTestMessage(meldingsfabrikk.lagRollback(personVersjon))
    }

    protected fun sendRollbackDelete() {
        testRapid.sendTestMessage(meldingsfabrikk.lagRollbackDelete())
    }

    protected fun sendAnnullering(fagsystemId: String) {
        testRapid.sendTestMessage(meldingsfabrikk.lagAnnullering(fagsystemId))
    }

    protected fun sendOverstyringTidslinje(dager: List<ManuellOverskrivingDag>) {
        testRapid.sendTestMessage(meldingsfabrikk.lagOverstyringTidslinje(dager))
    }

    protected fun assertTilstander(vedtaksperiodeIndeks: Int, vararg tilstand: String) {
        assertEquals(
            tilstand.toList(),
            testRapid.inspektør.tilstander(testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks))
        )
    }

    protected fun assertIkkeForkastedeTilstander(vedtaksperiodeIndeks: Int, vararg tilstand: String) {
        assertEquals(
            tilstand.toList(),
            testRapid.inspektør.tilstanderUtenForkastede(testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks))
        )
    }

    protected fun assertForkastedeTilstander(vedtaksperiodeIndeks: Int, vararg tilstand: String) {
        assertEquals(
            tilstand.toList(),
            testRapid.inspektør.forkastedeTilstander(testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks))
        )
    }
}

internal fun createHikariConfig(jdbcUrl: String) =
    HikariConfig().apply {
        this.jdbcUrl = jdbcUrl
        maximumPoolSize = 3
        minimumIdle = 1
        idleTimeout = 10001
        connectionTimeout = 1000
        maxLifetime = 30001
    }
