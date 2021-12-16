package no.nav.helse.spleis.e2e

import com.zaxxer.hikari.HikariConfig
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.spleis.HendelseMediator
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.TestMessageFactory
import no.nav.helse.spleis.TestMessageFactory.*
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.db.LagrePersonDao
import no.nav.helse.spleis.db.PersonPostgresRepository
import no.nav.helse.spleis.e2e.SpleisDataSource.migratedDb
import no.nav.helse.spleis.meldinger.TestRapid
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.testhelpers.januar
import no.nav.inntektsmeldingkontrakt.OpphoerAvNaturalytelse
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.FravarDTO
import no.nav.syfo.kafka.felles.InntektskildeDTO
import no.nav.syfo.kafka.felles.PeriodeDTO
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal abstract class AbstractEndToEndMediatorTest {
    internal companion object {
        internal const val UNG_PERSON_FNR_2018 = "12029240045"
        internal const val AKTØRID = "42"
        internal const val ORGNUMMER = "987654321"
        internal const val INNTEKT = 31000.00
    }

    private val meldingsfabrikk = TestMessageFactory(UNG_PERSON_FNR_2018, AKTØRID, ORGNUMMER, INNTEKT)
    protected val testRapid = TestRapid()
    private lateinit var dataSource: DataSource
    private lateinit var hendelseMediator: HendelseMediator
    private lateinit var messageMediator: MessageMediator

    @BeforeAll
    internal fun setupAll() {
        dataSource = migratedDb

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

    @BeforeEach
    internal fun setupEach() {
        resetDatabase()
        testRapid.reset()
    }

    protected fun sendNySøknad(
        vararg perioder: SoknadsperiodeDTO,
        orgnummer: String = ORGNUMMER,
        meldingOpprettet: LocalDateTime = perioder.minOfOrNull { it.fom!! }!!.atStartOfDay(),
        meldingId: String = UUID.randomUUID().toString()
    ) {
        testRapid.sendTestMessage(meldingsfabrikk.lagNySøknad(*perioder, orgnummer = orgnummer, opprettet = meldingOpprettet, meldingId = meldingId))
    }

    protected fun sendSøknad(
        vedtaksperiodeIndeks: Int,
        perioder: List<SoknadsperiodeDTO>,
        orgnummer: String = ORGNUMMER,
        fravær: List<FravarDTO> = emptyList(),
        egenmeldinger: List<PeriodeDTO> = emptyList(),
        andreInntektskilder: List<InntektskildeDTO>? = null,
        sendtNav: LocalDateTime? = perioder.maxOfOrNull { it.tom!! }?.atStartOfDay()
    ) {
        assertFalse(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Foreldrepenger))

        testRapid.sendTestMessage(
            meldingsfabrikk.lagSøknadNav(
                perioder = perioder,
                orgnummer = orgnummer,
                fravær = fravær,
                egenmeldinger = egenmeldinger,
                andreInntektskilder = andreInntektskilder,
                sendtNav = sendtNav
            )
        )
    }

    protected fun sendSøknadUtenVedtaksperiode(perioder: List<SoknadsperiodeDTO>) {
        testRapid.sendTestMessage(
            meldingsfabrikk.lagSøknadNav(
                perioder = perioder,
                orgnummer = ORGNUMMER,
                fravær = emptyList(),
                egenmeldinger = emptyList(),
                andreInntektskilder = null,
                sendtNav = perioder.maxOfOrNull { it.tom!! }?.atStartOfDay()
            )
        )
    }

    protected fun sendKorrigerendeSøknad(
        perioder: List<SoknadsperiodeDTO>,
        fravær: List<FravarDTO> = emptyList(),
        egenmeldinger: List<PeriodeDTO> = emptyList()
    ) {
        testRapid.sendTestMessage(meldingsfabrikk.lagSøknadNav(perioder = perioder, fravær = fravær, egenmeldinger = egenmeldinger))
    }

    protected fun sendSøknadArbeidsgiver(
        vedtaksperiodeIndeks: Int,
        perioder: List<SoknadsperiodeDTO>,
        egenmeldinger: List<PeriodeDTO> = emptyList()
    ) {
        assertFalse(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Foreldrepenger))
        testRapid.sendTestMessage(meldingsfabrikk.lagSøknadArbeidsgiver(perioder, egenmeldinger))
    }

    protected fun sendInntektsmelding(
        vedtaksperiodeIndeks: Int,
        arbeidsgiverperiode: List<Periode>,
        førsteFraværsdag: LocalDate,
        opphørAvNaturalytelser: List<OpphoerAvNaturalytelse> = emptyList(),
        beregnetInntekt: Double = INNTEKT,
        opphørsdatoForRefusjon: LocalDate? = null,
        meldingId: String = UUID.randomUUID().toString()
    ) {
        assertFalse(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Foreldrepenger))
        sendInntektsmelding(arbeidsgiverperiode, førsteFraværsdag, opphørAvNaturalytelser, beregnetInntekt, opphørsdatoForRefusjon, meldingId)
    }

    protected fun sendInntektsmelding(
        arbeidsgiverperiode: List<Periode>,
        førsteFraværsdag: LocalDate,
        opphørAvNaturalytelser: List<OpphoerAvNaturalytelse> = emptyList(),
        beregnetInntekt: Double = INNTEKT,
        opphørsdatoForRefusjon: LocalDate? = null,
        meldingId: String = UUID.randomUUID().toString()
    ) {
        testRapid.sendTestMessage(
            meldingsfabrikk.lagInnteksmelding(
                arbeidsgiverperiode,
                førsteFraværsdag,
                opphørAvNaturalytelser,
                beregnetInntekt,
                opphørsdatoForRefusjon,
                meldingId
            )
        )
    }

    protected fun sendInntektsmeldingReplay(
        vedtaksperiodeIndeks: Int = 0,
        arbeidsgiverperiode: List<Periode>,
        førsteFraværsdag: LocalDate,
        meldingId: String = UUID.randomUUID().toString()
    ) {
        testRapid.sendTestMessage(
            meldingsfabrikk.lagInnteksmeldingReplay(
                testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks),
                arbeidsgiverperiode,
                førsteFraværsdag,
                meldingId
            )
        )
    }

    protected fun sendNyUtbetalingpåminnelse(utbetalingIndeks: Int, status: Utbetalingstatus = Utbetalingstatus.IKKE_UTBETALT) {
        val utbetalingId = testRapid.inspektør.utbetalingId(utbetalingIndeks)
        testRapid.sendTestMessage(meldingsfabrikk.lagUtbetalingpåminnelse(utbetalingId, status))
    }

    protected fun sendNyPåminnelse(
        vedtaksperiodeIndeks: Int = -1,
        tilstandType: TilstandType = TilstandType.START,
        orgnummer: String = ORGNUMMER,
        tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now()
    ): UUID {
        val vedtaksperiodeId = if (vedtaksperiodeIndeks == -1) UUID.randomUUID() else testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks)
        testRapid.sendTestMessage(meldingsfabrikk.lagPåminnelse(vedtaksperiodeId, tilstandType, orgnummer, tilstandsendringstidspunkt))
        return vedtaksperiodeId
    }

    protected fun sendUtbetalingsgodkjenning(
        vedtaksperiodeIndeks: Int,
        godkjent: Boolean = true,
        saksbehandlerIdent: String = "O123456",
        saksbehandlerEpost: String = "jan@banan.no",
        automatiskBehandling: Boolean = false,
        makstidOppnådd: Boolean = false,
        godkjenttidspunkt: LocalDateTime = LocalDateTime.now()
    ) {
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Godkjenning))
        testRapid.sendTestMessage(
            meldingsfabrikk.lagUtbetalingsgodkjenning(
                vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks),
                utbetalingId = UUID.fromString(testRapid.inspektør.etterspurteBehov(Godkjenning).path("utbetalingId").asText()),
                tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(vedtaksperiodeIndeks, Godkjenning),
                utbetalingGodkjent = godkjent,
                saksbehandlerIdent = saksbehandlerIdent,
                saksbehandlerEpost = saksbehandlerEpost,
                automatiskBehandling = automatiskBehandling,
                makstidOppnådd = makstidOppnådd,
                godkjenttidspunkt = godkjenttidspunkt
            )
        )
    }

    protected fun sendYtelserUtenSykepengehistorikk(
        vedtaksperiodeIndeks: Int,
        pleiepenger: List<PleiepengerTestdata> = emptyList(),
        omsorgspenger: List<OmsorgspengerTestdata> = emptyList(),
        opplæringspenger: List<OpplæringspengerTestdata> = emptyList(),
        institusjonsoppholdsperioder: List<InstitusjonsoppholdTestdata> = emptyList(),
        arbeidsavklaringspenger: List<ArbeidsavklaringspengerTestdata> = emptyList(),
        dagpenger: List<DagpengerTestdata> = emptyList()
    ) {
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Foreldrepenger))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Pleiepenger))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Omsorgspenger))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Opplæringspenger))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Arbeidsavklaringspenger))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Dagpenger))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Institusjonsopphold))
        testRapid.sendTestMessage(
            meldingsfabrikk.lagYtelser(
                vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks),
                tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(vedtaksperiodeIndeks, Foreldrepenger),
                pleiepenger = pleiepenger,
                omsorgspenger = omsorgspenger,
                opplæringspenger = opplæringspenger,
                institusjonsoppholdsperioder = institusjonsoppholdsperioder,
                arbeidsavklaringspenger = arbeidsavklaringspenger,
                dagpenger = dagpenger,
                sykepengehistorikk = null
            )
        )
    }

    protected fun sendYtelser(
        vedtaksperiodeIndeks: Int,
        pleiepenger: List<PleiepengerTestdata> = emptyList(),
        omsorgspenger: List<OmsorgspengerTestdata> = emptyList(),
        opplæringspenger: List<OpplæringspengerTestdata> = emptyList(),
        institusjonsoppholdsperioder: List<InstitusjonsoppholdTestdata> = emptyList(),
        arbeidsavklaringspenger: List<ArbeidsavklaringspengerTestdata> = emptyList(),
        dagpenger: List<DagpengerTestdata> = emptyList(),
        sykepengehistorikk: List<UtbetalingshistorikkTestdata> = emptyList()
    ) {
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Sykepengehistorikk))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Foreldrepenger))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Pleiepenger))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Omsorgspenger))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Opplæringspenger))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Arbeidsavklaringspenger))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Dagpenger))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Institusjonsopphold))
        testRapid.sendTestMessage(
            meldingsfabrikk.lagYtelser(
                vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks),
                tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(vedtaksperiodeIndeks, Sykepengehistorikk),
                pleiepenger = pleiepenger,
                omsorgspenger = omsorgspenger,
                opplæringspenger = opplæringspenger,
                institusjonsoppholdsperioder = institusjonsoppholdsperioder,
                arbeidsavklaringspenger = arbeidsavklaringspenger,
                dagpenger = dagpenger,
                sykepengehistorikk = sykepengehistorikk
            )
        )
    }

    protected fun sendUtbetalingshistorikk(
        vedtaksperiodeIndeks: Int,
        sykepengehistorikk: List<UtbetalingshistorikkTestdata> = emptyList()
    ) {
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Sykepengehistorikk))
        testRapid.sendTestMessage(
            meldingsfabrikk.lagUtbetalingshistorikk(
                testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks),
                TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
                sykepengehistorikk
            )
        )
    }

    protected fun sendUtbetalingshistorikkForFeriepenger(testdata: UtbetalingshistorikkForFeriepengerTestdata) {
        testRapid.sendTestMessage(
            meldingsfabrikk.lagUtbetalingshistorikkForFeriepenger(testdata)
        )
    }

    protected fun sendVilkårsgrunnlag(
        vedtaksperiodeIndeks: Int,
        inntekter: List<Pair<YearMonth, Double>> = 1.rangeTo(12).map { YearMonth.of(2017, it) to INNTEKT },
        arbeidsforhold: List<Arbeidsforhold> = listOf(
            Arbeidsforhold(
                ORGNUMMER,
                1.januar(2010),
                null
            )
        ),
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        inntekterForSykepengegrunnlag: List<InntekterForSykepengegrunnlagFraLøsning> = 1.rangeTo(12).map {
            InntekterForSykepengegrunnlagFraLøsning(
                måned = YearMonth.of(2017, it),
                inntekter = listOf(
                    InntekterForSykepengegrunnlagFraLøsning.Inntekt(INNTEKT, ORGNUMMER)
                ),
                arbeidsforhold = emptyList()
            )
        },
    ) {
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, InntekterForSammenligningsgrunnlag))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Medlemskap))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, ArbeidsforholdV2))
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, InntekterForSykepengegrunnlag))
        testRapid.sendTestMessage(
            meldingsfabrikk.lagVilkårsgrunnlag(
                vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks),
                tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(
                    vedtaksperiodeIndeks,
                    InntekterForSammenligningsgrunnlag
                ),
                inntekter = inntekter,
                arbeidsforhold = (arbeidsforhold),
                medlemskapstatus = medlemskapstatus,
                inntekterForSykepengegrunnlag = inntekterForSykepengegrunnlag,
            )
        )
    }

    protected fun sendSimulering(
        vedtaksperiodeIndeks: Int,
        status: SimuleringMessage.Simuleringstatus,
        forventedeFagområder: Set<String> = setOf("SPREF")
    ) {
        val fagområder = mutableSetOf<String>()
        assertTrue(testRapid.inspektør.harEtterspurteBehov(vedtaksperiodeIndeks, Simulering))
        testRapid.inspektør.alleEtterspurteBehov(Simulering).forEach { behov ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagSimulering(
                    vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks),
                    tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(vedtaksperiodeIndeks, Simulering),
                    status = status,
                    utbetalingId = UUID.fromString(behov.path("utbetalingId").asText()),
                    fagsystemId = behov.path("Simulering").path("fagsystemId").asText(),
                    fagområde = behov.path("Simulering").path("fagområde").asText().also {
                        fagområder.add(it)
                    }
                )
            )
        }
        assertEquals(forventedeFagområder, fagområder)
    }

    protected fun sendEtterbetaling(
        fagsystemId: String = testRapid.inspektør.etterspurteBehov(Utbetaling).path(Utbetaling.name).path("fagsystemId").asText(),
        gyldighetsdato: LocalDate
    ) {
        testRapid.sendTestMessage(
            meldingsfabrikk.lagEtterbetaling(
                fagsystemId = fagsystemId,
                gyldighetsdato = gyldighetsdato
            )
        )
    }

    protected fun sendEtterbetalingMedHistorikk(
        fagsystemId: String = testRapid.inspektør.etterspurteBehov(Utbetaling).path(Utbetaling.name).path("fagsystemId").asText(),
        gyldighetsdato: LocalDate
    ) {
        testRapid.sendTestMessage(
            meldingsfabrikk.lagEtterbetalingMedHistorikk(
                fagsystemId = fagsystemId,
                gyldighetsdato = gyldighetsdato
            )
        )
    }

    protected fun sendUtbetaling(utbetalingOK: Boolean = true) {
        val etterspurteBehov = testRapid.inspektør.alleEtterspurteBehov(Utbetaling)
        etterspurteBehov.forEach { behov ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagUtbetalingOverført(
                    fagsystemId = behov.path("fagsystemId").asText(),
                    utbetalingId = behov.path("utbetalingId").asText(),
                    avstemmingsnøkkel = 123456L,
                    overføringstidspunkt = LocalDateTime.now()
                )
            )
        }
        etterspurteBehov.forEach { behov ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagUtbetaling(
                    fagsystemId = behov.path("fagsystemId").asText(),
                    utbetalingId = behov.path("utbetalingId").asText(),
                    utbetalingOK = utbetalingOK
                )
            )
        }
    }

    protected fun sendAvstemming() {
        testRapid.sendTestMessage(meldingsfabrikk.lagAvstemming())
    }

    protected fun sendAnnullering(fagsystemId: String) {
        testRapid.sendTestMessage(meldingsfabrikk.lagAnnullering(fagsystemId))
    }

    protected fun sendOverstyringTidslinje(dager: List<ManuellOverskrivingDag>) {
        testRapid.sendTestMessage(meldingsfabrikk.lagOverstyringTidslinje(dager))
    }

    protected fun sendOverstyringInntekt(inntekt: Double, skjæringstidspunkt: LocalDate) {
        testRapid.sendTestMessage(meldingsfabrikk.lagOverstyringInntekt(inntekt, skjæringstidspunkt))
    }

    protected fun assertUtbetalingtype(utbetalingIndeks: Int, type: String) {
        assertEquals(
            type,
            testRapid.inspektør.utbetalingtype(utbetalingIndeks)
        )
    }

    protected fun assertTilstander(vedtaksperiodeIndeks: Int, vararg tilstand: String) {
        assertEquals(
            tilstand.toList(),
            testRapid.inspektør.tilstander(testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks))
        )
    }

    protected fun assertUtbetalingTilstander(utbetalingIndeks: Int, vararg tilstand: String) {
        assertEquals(
            tilstand.toList(),
            testRapid.inspektør.utbetalingtilstander(utbetalingIndeks)
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
