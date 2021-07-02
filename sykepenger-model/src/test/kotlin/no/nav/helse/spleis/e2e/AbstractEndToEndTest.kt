package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Arbeidsavklaringspenger
import no.nav.helse.hendelser.Dagpenger
import no.nav.helse.hendelser.Dødsinfo
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.Institusjonsopphold.Institusjonsoppholdsperiode
import no.nav.helse.hendelser.Omsorgspenger
import no.nav.helse.hendelser.Opplæringspenger
import no.nav.helse.hendelser.Pleiepenger
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Simulering.*
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.Arbeidskategorikode.Arbeidstaker
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.KodePeriode
import no.nav.helse.person.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.api.HendelseDTO
import no.nav.helse.serde.api.serializePersonForSpeil
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.testhelpers.Inntektperioder
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.*
import kotlin.reflect.KClass

internal abstract class AbstractEndToEndTest : AbstractPersonTest() {

    protected companion object {
        val INNTEKT = 31000.00.månedlig
        val DAGSINNTEKT = INNTEKT.reflection { _, _, _, dagligInt -> dagligInt }
        val MÅNEDLIG_INNTEKT = INNTEKT.reflection { _, månedlig, _, _ -> månedlig.toInt() }
    }

    fun speilApi(hendelser: List<HendelseDTO> = emptyList()) = serializePersonForSpeil(person, hendelser)
    protected lateinit var hendelselogg: IAktivitetslogg
    protected var forventetEndringTeller = 0
    private val sykmeldinger = mutableMapOf<UUID, Array<out Sykmeldingsperiode>>()
    private val søknader = mutableMapOf<UUID, Triple<LocalDate, List<Søknad.Inntektskilde>, Array<out Søknad.Søknadsperiode>>>()
    private val inntektsmeldinger = mutableMapOf<UUID, () -> Inntektsmelding>()

    fun <T> sjekkAt(t: T, init: T.() -> Unit) {
        t.init()
    }

    @BeforeEach
    internal fun abstractSetup() {
        sykmeldinger.clear()
        søknader.clear()
        inntektsmeldinger.clear()
        ikkeBesvarteBehov.clear()
    }

    protected fun assertSisteTilstand(id: UUID, tilstand: TilstandType) {
        assertEquals(tilstand, observatør.tilstandsendringer[id]?.last())
    }

    protected fun assertTilstander(indeks: Int, vararg tilstander: TilstandType) {
        assertTilstander(vedtaksperiodeId(indeks), *tilstander)
    }

    protected fun assertTilstander(id: UUID, vararg tilstander: TilstandType, inspektør: TestArbeidsgiverInspektør = this.inspektør) {
        assertFalse(inspektør.periodeErForkastet(id)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}" }
        assertTrue(inspektør.periodeErIkkeForkastet(id)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}" }
        assertEquals(tilstander.asList(), observatør.tilstandsendringer[id])
    }

    protected fun TestArbeidsgiverInspektør.assertTilstander(id: UUID, vararg tilstander: TilstandType) {
        assertTilstander(id = id, inspektør = this, tilstander = tilstander)
    }

    protected fun assertForkastetPeriodeTilstander(indeks: Int, vararg tilstander: TilstandType) {
        assertForkastetPeriodeTilstander(vedtaksperiodeId(indeks), *tilstander)
    }

    protected fun assertForkastetPeriodeTilstander(id: UUID, vararg tilstander: TilstandType) {
        assertTrue(inspektør.periodeErForkastet(id)) { "Perioden er ikke forkastet" }
        assertFalse(inspektør.periodeErIkkeForkastet(id)) { "Perioden er ikke forkastet" }
        assertEquals(tilstander.asList(), observatør.tilstandsendringer[id])
    }

    protected fun assertForkastetPeriodeTilstander(orgnummer: String, id: UUID, vararg tilstander: TilstandType) {
        assertTrue(inspektør(orgnummer).periodeErForkastet(id)) { "Perioden er ikke forkastet" }
        assertFalse(inspektør(orgnummer).periodeErIkkeForkastet(id)) { "Perioden er ikke forkastet" }
        assertEquals(tilstander.asList(), observatør.tilstandsendringer[id])
    }

    protected fun assertSisteForkastetPeriodeTilstand(orgnummer: String, id: UUID, tilstand: TilstandType) {
        assertTrue(inspektør(orgnummer).periodeErForkastet(id)) { "Perioden er ikke forkastet" }
        assertFalse(inspektør(orgnummer).periodeErIkkeForkastet(id)) { "Perioden er ikke forkastet" }
        assertEquals(tilstand, observatør.tilstandsendringer[id]?.last())

    }

    protected fun assertNoErrors(inspektør: TestArbeidsgiverInspektør) {
        assertFalse(inspektør.personLogg.hasErrorsOrWorse(), inspektør.personLogg.toString())
    }

    protected fun TestArbeidsgiverInspektør.assertHasNoErrors() = assertNoErrors(this)

    protected fun assertNoWarnings(inspektør: TestArbeidsgiverInspektør) {
        assertFalse(inspektør.personLogg.hasWarningsOrWorse(), inspektør.personLogg.toString())
    }

    protected fun assertWarnings(inspektør: TestArbeidsgiverInspektør) {
        assertTrue(inspektør.personLogg.hasWarningsOrWorse(), inspektør.personLogg.toString())
    }

    protected fun assertErrors(inspektør: TestArbeidsgiverInspektør) {
        assertTrue(inspektør.personLogg.hasErrorsOrWorse(), inspektør.personLogg.toString())
    }

    protected fun assertActivities(inspektør: TestArbeidsgiverInspektør) {
        assertTrue(inspektør.personLogg.hasActivities(), inspektør.personLogg.toString())
    }

    protected fun replaySykmelding(hendelseId: UUID) = håndterSykmelding(
        id = hendelseId,
        sykeperioder = requireNotNull(sykmeldinger[hendelseId])
    )

    protected fun replaySøknad(hendelseId: UUID) = håndterSøknad(
        id = hendelseId,
        sendtTilNav = requireNotNull(søknader[hendelseId]).first,
        andreInntektskilder = requireNotNull(søknader[hendelseId]).second,
        perioder = requireNotNull(søknader[hendelseId]).third
    )

    protected fun håndterSykmelding(
        vararg sykeperioder: Sykmeldingsperiode,
        sykmeldingSkrevet: LocalDateTime? = null,
        mottatt: LocalDateTime? = null,
        id: UUID = UUID.randomUUID(),
        orgnummer: String = ORGNUMMER
    ): UUID {
        sykmelding(
            id,
            *sykeperioder,
            sykmeldingSkrevet = sykmeldingSkrevet,
            mottatt = mottatt,
            orgnummer = orgnummer
        ).håndter(Person::håndter)
        sykmeldinger[id] = sykeperioder
        return id
    }

    protected fun håndterSøknadMedValidering(
        vedtaksperiodeId: UUID,
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(),
        orgnummer: String = ORGNUMMER
    ) {
        assertIkkeEtterspurt(Søknad::class, InntekterForSammenligningsgrunnlag, vedtaksperiodeId, ORGNUMMER)
        håndterSøknad(*perioder, andreInntektskilder = andreInntektskilder, orgnummer = orgnummer)
    }

    protected fun håndterSøknad(
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(),
        sendtTilNav: LocalDate = Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
        id: UUID = UUID.randomUUID(),
        orgnummer: String = ORGNUMMER,
        sykmeldingSkrevet: LocalDateTime? = null
    ): UUID {
        søknad(
            id,
            *perioder,
            andreInntektskilder = andreInntektskilder,
            sendtTilNav = sendtTilNav,
            orgnummer = orgnummer,
            sykmeldingSkrevet = sykmeldingSkrevet
        ).håndter(Person::håndter)
        søknader[id] = Triple(sendtTilNav, andreInntektskilder, perioder)
        return id
    }

    protected fun håndterSøknadArbeidsgiver(
        vararg perioder: SøknadArbeidsgiver.Søknadsperiode,
        orgnummer: String = ORGNUMMER
    ) = søknadArbeidsgiver(*perioder, orgnummer = orgnummer).håndter(Person::håndter)

    protected fun håndterInntektsmeldingMedValidering(
        vedtaksperiodeId: UUID,
        arbeidsgiverperioder: List<Periode>,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
        refusjon: Refusjon = Refusjon(null, INNTEKT, emptyList()),
        beregnetInntekt: Inntekt = refusjon.inntekt,
        orgnummer: String = ORGNUMMER
    ): UUID {
        assertIkkeEtterspurt(Inntektsmelding::class, InntekterForSammenligningsgrunnlag, vedtaksperiodeId, orgnummer)
        return håndterInntektsmelding(arbeidsgiverperioder, førsteFraværsdag, refusjon, beregnetInntekt = beregnetInntekt, orgnummer = orgnummer)
    }

    protected fun håndterInntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
        refusjon: Refusjon = Refusjon(null, INNTEKT, emptyList()),
        orgnummer: String = ORGNUMMER,
        id: UUID = UUID.randomUUID(),
        beregnetInntekt: Inntekt = refusjon.inntekt,
        harOpphørAvNaturalytelser: Boolean = false,
        arbeidsforholdId: String? = null,
    ): UUID {
        inntektsmelding(
            id,
            arbeidsgiverperioder,
            beregnetInntekt = beregnetInntekt,
            førsteFraværsdag = førsteFraværsdag,
            refusjon = refusjon,
            orgnummer = orgnummer,
            harOpphørAvNaturalytelser = harOpphørAvNaturalytelser,
            arbeidsforholdId = arbeidsforholdId
        ).håndter(Person::håndter)
        return id
    }

    data class Refusjon(
        internal val opphørsdato: LocalDate?,
        internal val inntekt: Inntekt,
        internal val endringerIRefusjon: List<LocalDate> = emptyList()
    )

    protected fun håndterInntektsmeldingReplay(
        inntektsmeldingId: UUID,
        vedtaksperiodeId: UUID
    ) {
        val inntektsmeldinggenerator = inntektsmeldinger[inntektsmeldingId] ?: fail { "Fant ikke inntektsmelding med id $inntektsmeldingId" }
        assertTrue(observatør.bedtOmInntektsmeldingReplay(vedtaksperiodeId)) { "Vedtaksperioden har ikke bedt om replay av inntektsmelding" }
        inntektsmeldingReplay(inntektsmeldinggenerator(), vedtaksperiodeId)
            .håndter(Person::håndter)
    }

    protected fun håndterVilkårsgrunnlag(
        vedtaksperiodeId: UUID,
        inntekt: Inntekt = INNTEKT,
        arbeidsforhold: List<Arbeidsforhold> = emptyList(),
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        orgnummer: String = ORGNUMMER,
        inntektsvurdering: Inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    orgnummer inntekt inntekt
                }
            }
        )
    ) {
        fun assertEtterspurt(behovtype: Behovtype) =
            assertEtterspurt(Vilkårsgrunnlag::class, behovtype, vedtaksperiodeId, orgnummer)

        assertEtterspurt(InntekterForSammenligningsgrunnlag)
        assertEtterspurt(Medlemskap)
        vilkårsgrunnlag(
            vedtaksperiodeId,
            arbeidsforhold,
            medlemskapstatus,
            orgnummer,
            inntektsvurdering,
        ).håndter(Person::håndter)
    }

    protected fun håndterSimulering(
        vedtaksperiodeId: UUID = 1.vedtaksperiode,
        simuleringOK: Boolean = true,
        orgnummer: String = ORGNUMMER
    ) {
        assertEtterspurt(Simulering::class, Behovtype.Simulering, vedtaksperiodeId, orgnummer)
        simulering(vedtaksperiodeId, simuleringOK, orgnummer).håndter(Person::håndter)
    }

    protected fun håndterUtbetalingshistorikk(
        vedtaksperiodeId: UUID,
        vararg utbetalinger: Infotrygdperiode,
        inntektshistorikk: List<Inntektsopplysning>? = null,
        orgnummer: String = ORGNUMMER,
        besvart: LocalDateTime = LocalDateTime.now()
    ) {
        val bedtOmSykepengehistorikk = inspektør(orgnummer).etterspurteBehov(vedtaksperiodeId, Sykepengehistorikk)
        if (bedtOmSykepengehistorikk) assertEtterspurt(Utbetalingshistorikk::class, Sykepengehistorikk, vedtaksperiodeId, orgnummer)
        utbetalingshistorikk(
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalinger = utbetalinger.toList(),
            inntektshistorikk = inntektshistorikk(
                inntektshistorikk = inntektshistorikk,
                orgnummer = orgnummer
            ),
            orgnummer = orgnummer,
            besvart = besvart
        ).håndter(Person::håndter)
    }

    protected fun håndterUtbetalingshistorikkForFeriepenger(
        opptjeningsår: Year,
        utbetalinger: List<UtbetalingshistorikkForFeriepenger.Utbetalingsperiode> = listOf(),
        feriepengehistorikk: List<UtbetalingshistorikkForFeriepenger.Feriepenger> = listOf(),
        skalBeregnesManuelt: Boolean = false
    ) {
        utbetalingshistorikkForFeriepenger(
            opptjeningsår = opptjeningsår,
            utbetalinger = utbetalinger,
            feriepengehistorikk = feriepengehistorikk,
            skalBeregnesManuelt = skalBeregnesManuelt
        ).håndter(Person::håndter)
    }

    protected fun Inntekt.repeat(antall: Int) = (0.until(antall)).map { this }

    protected fun håndterUtbetalingsgrunnlag(
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        orgnummer: String = ORGNUMMER,
        inntekter: List<ArbeidsgiverInntekt> = listOf(
            ArbeidsgiverInntekt(orgnummer, (0..3).map {
                val yearMonth = YearMonth.from(inspektør(orgnummer).skjæringstidspunkt(vedtaksperiodeId)).minusMonths(3L - it)
                ArbeidsgiverInntekt.MånedligInntekt.Sykepengegrunnlag(
                    yearMonth = yearMonth,
                    type = ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT,
                    inntekt = INNTEKT,
                    fordel = "juidy fordel",
                    beskrivelse = "juicy beskrivelse"
                )
            })
        ),
        arbeidsforhold: List<Arbeidsforhold> = listOf(Arbeidsforhold(orgnummer, 1.januar, null))
    ) {
        assertEtterspurt(Utbetalingsgrunnlag::class, InntekterForSykepengegrunnlag, vedtaksperiodeId, orgnummer)
        assertEtterspurt(Utbetalingsgrunnlag::class, ArbeidsforholdV2, vedtaksperiodeId, orgnummer)

        Utbetalingsgrunnlag(
            UUID.randomUUID(),
            AKTØRID, UNG_PERSON_FNR_2018,
            orgnummer,
            vedtaksperiodeId,
            InntektForSykepengegrunnlag(inntekter),
            arbeidsforhold
        ).håndter(Person::håndter)
    }

    protected fun håndterYtelser(
        vedtaksperiodeId: UUID = 1.vedtaksperiode,
        vararg utbetalinger: Infotrygdperiode,
        inntektshistorikk: List<Inntektsopplysning>? = null,
        foreldrepenger: Periode? = null,
        pleiepenger: List<Periode> = emptyList(),
        omsorgspenger: List<Periode> = emptyList(),
        opplæringspenger: List<Periode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsoppholdsperiode> = emptyList(),
        orgnummer: String = ORGNUMMER,
        dødsdato: LocalDate? = null,
        statslønn: Boolean = false,
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList(),
        besvart: LocalDateTime = LocalDateTime.now()
    ) {
        fun assertEtterspurt(behovtype: Behovtype) =
            assertEtterspurt(Ytelser::class, behovtype, vedtaksperiodeId, orgnummer)

        assertEtterspurt(Foreldrepenger)
        assertEtterspurt(Behovtype.Pleiepenger)
        assertEtterspurt(Behovtype.Omsorgspenger)
        assertEtterspurt(Behovtype.Opplæringspenger)
        assertEtterspurt(Behovtype.Arbeidsavklaringspenger)
        assertEtterspurt(Behovtype.Dagpenger)
        assertEtterspurt(Behovtype.Institusjonsopphold)
        assertEtterspurt(Behovtype.Dødsinfo)

        ytelser(
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalinger = utbetalinger.toList(),
            inntektshistorikk = inntektshistorikk(inntektshistorikk, orgnummer),
            foreldrepenger = foreldrepenger,
            pleiepenger = pleiepenger,
            omsorgspenger = omsorgspenger,
            opplæringspenger = opplæringspenger,
            institusjonsoppholdsperioder = institusjonsoppholdsperioder,
            orgnummer = orgnummer,
            dødsdato = dødsdato,
            statslønn = statslønn,
            arbeidskategorikoder = arbeidskategorikoder,
            arbeidsavklaringspenger = arbeidsavklaringspenger,
            dagpenger = dagpenger,
            besvart = besvart
        ).håndter(Person::håndter)
    }

    protected fun håndterUtbetalingpåminnelse(
        utbetalingIndeks: Int,
        status: Utbetalingstatus,
        tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now()
    ) {
        utbetalingpåminnelse(inspektør.utbetalingId(utbetalingIndeks), status, tilstandsendringstidspunkt).håndter(Person::håndter)
    }

    protected fun håndterPåminnelse(
        vedtaksperiodeId: UUID,
        påminnetTilstand: TilstandType,
        tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now(),
        orgnummer: String = ORGNUMMER
    ) {
        påminnelse(
            vedtaksperiodeId = vedtaksperiodeId,
            påminnetTilstand = påminnetTilstand,
            tilstandsendringstidspunkt = tilstandsendringstidspunkt,
            orgnummer = orgnummer
        ).håndter(Person::håndter)
    }

    protected fun håndterUtbetalingsgodkjenning(
        vedtaksperiodeId: UUID = 1.vedtaksperiode,
        utbetalingGodkjent: Boolean = true,
        orgnummer: String = ORGNUMMER,
        automatiskBehandling: Boolean = false,
        utbetalingId: UUID = UUID.fromString(
            inspektør.sisteBehov(Godkjenning).kontekst()["utbetalingId"]
                ?: throw IllegalStateException("Finner ikke utbetalingId i: ${inspektør.sisteBehov(Godkjenning).kontekst()}")
        ),
    ) {
        assertEtterspurt(Utbetalingsgodkjenning::class, Godkjenning, vedtaksperiodeId, orgnummer)
        utbetalingsgodkjenning(vedtaksperiodeId, utbetalingGodkjent, orgnummer, automatiskBehandling, utbetalingId).håndter(Person::håndter)
    }

    protected fun håndterUtbetalt(
        vedtaksperiodeId: UUID = 1.vedtaksperiode,
        status: UtbetalingHendelse.Oppdragstatus = UtbetalingHendelse.Oppdragstatus.AKSEPTERT,
        sendOverførtKvittering: Boolean = true,
        orgnummer: String = ORGNUMMER,
        fagsystemId: String = inspektør(orgnummer).fagsystemId(vedtaksperiodeId),
        meldingsreferanseId: UUID = UUID.randomUUID()
    ): UtbetalingHendelse {
        if (sendOverførtKvittering) {
            UtbetalingOverført(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = AKTØRID,
                fødselsnummer = UNG_PERSON_FNR_2018,
                orgnummer = orgnummer,
                fagsystemId = fagsystemId,
                utbetalingId = inspektør.sisteBehov(Utbetaling).kontekst()["utbetalingId"]
                    ?: throw IllegalStateException("Finner ikke utbetalingId i: ${inspektør.sisteBehov(Utbetaling).kontekst()}"),
                avstemmingsnøkkel = 123456L,
                overføringstidspunkt = LocalDateTime.now()
            ).håndter(Person::håndter)
        }
        return utbetaling(
            fagsystemId = fagsystemId,
            status = status,
            orgnummer = orgnummer,
            meldingsreferanseId = meldingsreferanseId
        ).håndter(Person::håndter)
    }

    protected fun håndterGrunnbeløpsregulering(
        orgnummer: String = ORGNUMMER,
        fagsystemId: String = inspektør.arbeidsgiverOppdrag.last().fagsystemId(),
        gyldighetsdato: LocalDate
    ) {
        Grunnbeløpsregulering(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            gyldighetsdato = gyldighetsdato,
            fagsystemId = fagsystemId,
            aktivitetslogg = Aktivitetslogg()
        ).håndter(Person::håndter)
    }

    protected fun håndterAnnullerUtbetaling(
        orgnummer: String = ORGNUMMER,
        fagsystemId: String = inspektør.arbeidsgiverOppdrag.last().fagsystemId(),
        opprettet: LocalDateTime = LocalDateTime.now()
    ) {
        AnnullerUtbetaling(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            fagsystemId = fagsystemId,
            saksbehandlerIdent = "Ola Nordmann",
            saksbehandlerEpost = "tbd@nav.no",
            opprettet = opprettet
        ).håndter(Person::håndter)
    }

    protected fun håndterOverstyring(
        overstyringsdager: List<ManuellOverskrivingDag> = listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag, 100)),
        orgnummer: String = ORGNUMMER
    ) {
        OverstyrTidslinje(
            meldingsreferanseId = UUID.randomUUID(),
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            organisasjonsnummer = orgnummer,
            dager = overstyringsdager,
            opprettet = LocalDateTime.now()
        ).håndter(Person::håndter)
    }

    protected fun utbetaling(
        fagsystemId: String,
        status: UtbetalingHendelse.Oppdragstatus,
        orgnummer: String = ORGNUMMER,
        meldingsreferanseId: UUID = UUID.randomUUID()
    ) =
        UtbetalingHendelse(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            fagsystemId = fagsystemId,
            utbetalingId = inspektør.sisteBehov(Utbetaling).kontekst().getValue("utbetalingId"),
            status = status,
            melding = "hei",
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        ).apply {
            hendelselogg = this
        }

    protected fun håndterFeriepengerUtbetalt(
        status: UtbetalingHendelse.Oppdragstatus = UtbetalingHendelse.Oppdragstatus.AKSEPTERT,
        orgnummer: String = ORGNUMMER,
        fagsystemId: String,
        meldingsreferanseId: UUID = UUID.randomUUID()
    ): UtbetalingHendelse {
        return feriepengeutbetaling(
            fagsystemId = fagsystemId,
            status = status,
            orgnummer = orgnummer,
            meldingsreferanseId = meldingsreferanseId
        ).håndter(Person::håndter)
    }

    private fun feriepengeutbetaling(
        fagsystemId: String,
        status: UtbetalingHendelse.Oppdragstatus,
        orgnummer: String = ORGNUMMER,
        meldingsreferanseId: UUID = UUID.randomUUID()
    ) =
        UtbetalingHendelse(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            fagsystemId = fagsystemId,
            utbetalingId = "",
            status = status,
            melding = "hey",
            avstemmingsnøkkel = 654321L,
            overføringstidspunkt = LocalDateTime.now()
        ).apply {
            hendelselogg = this
        }

    protected fun sykmelding(
        id: UUID,
        vararg sykeperioder: Sykmeldingsperiode,
        orgnummer: String = ORGNUMMER,
        sykmeldingSkrevet: LocalDateTime? = null,
        mottatt: LocalDateTime? = null
    ): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = id,
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = orgnummer,
            sykeperioder = listOf(*sykeperioder),
            sykmeldingSkrevet = sykmeldingSkrevet ?: Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.atStartOfDay() ?: LocalDateTime.now(),
            mottatt = mottatt ?: Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.atStartOfDay() ?: LocalDateTime.now()
        ).apply {
            hendelselogg = this
        }
    }

    internal fun sentSykmelding(vararg sykeperioder: Sykmeldingsperiode, orgnummer: String = ORGNUMMER): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = orgnummer,
            sykeperioder = sykeperioder.toList(),
            sykmeldingSkrevet = Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.atStartOfDay() ?: LocalDateTime.now(),
            mottatt = Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.plusMonths(7)?.atStartOfDay() ?: LocalDateTime.now()
        ).apply {
            hendelselogg = this
        }
    }

    protected fun søknad(
        id: UUID,
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(),
        sendtTilNav: LocalDate = Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
        orgnummer: String = ORGNUMMER,
        sykmeldingSkrevet: LocalDateTime? = null
    ): Søknad {
        return Søknad(
            meldingsreferanseId = id,
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = orgnummer,
            perioder = listOf(*perioder),
            andreInntektskilder = andreInntektskilder,
            sendtTilNAV = sendtTilNav.atStartOfDay(),
            permittert = false,
            merknaderFraSykmelding = emptyList(),
            sykmeldingSkrevet = sykmeldingSkrevet ?: Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.start.atStartOfDay()
        ).apply {
            hendelselogg = this
        }
    }

    private fun søknadArbeidsgiver(
        vararg perioder: SøknadArbeidsgiver.Søknadsperiode,
        orgnummer: String
    ): SøknadArbeidsgiver {
        return SøknadArbeidsgiver(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = orgnummer,
            perioder = listOf(*perioder),
            sykmeldingSkrevet = LocalDateTime.now()
        ).apply {
            hendelselogg = this
        }
    }

    private fun inntektsmeldingReplay(
        inntektsmelding: Inntektsmelding,
        vedtaksperiodeId: UUID
    ): InntektsmeldingReplay {
        return InntektsmeldingReplay(
            wrapped = inntektsmelding,
            vedtaksperiodeId = vedtaksperiodeId
        ).apply {
            hendelselogg = this
        }
    }

    protected fun inntektsmelding(
        id: UUID,
        arbeidsgiverperioder: List<Periode>,
        beregnetInntekt: Inntekt = INNTEKT,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
        refusjon: Refusjon = Refusjon(null, beregnetInntekt, emptyList()),
        orgnummer: String = ORGNUMMER,
        harOpphørAvNaturalytelser: Boolean = false,
        arbeidsforholdId: String? = null
    ): Inntektsmelding {
        val inntektsmeldinggenerator = {
            Inntektsmelding(
                meldingsreferanseId = id,
                refusjon = Inntektsmelding.Refusjon(refusjon.opphørsdato, refusjon.inntekt, refusjon.endringerIRefusjon),
                orgnummer = orgnummer,
                fødselsnummer = UNG_PERSON_FNR_2018,
                aktørId = AKTØRID,
                førsteFraværsdag = førsteFraværsdag,
                beregnetInntekt = beregnetInntekt,
                arbeidsgiverperioder = arbeidsgiverperioder,
                arbeidsforholdId = arbeidsforholdId,
                begrunnelseForReduksjonEllerIkkeUtbetalt = null,
                harOpphørAvNaturalytelser = harOpphørAvNaturalytelser,
                mottatt = LocalDateTime.now()
            )
        }
        inntektsmeldinger[id] = inntektsmeldinggenerator
        EtterspurtBehov.fjern(ikkeBesvarteBehov, orgnummer, Sykepengehistorikk)
        return inntektsmeldinggenerator().apply { hendelselogg = this }
    }

    protected fun vilkårsgrunnlag(
        vedtaksperiodeId: UUID,
        arbeidsforhold: List<Arbeidsforhold> = emptyList(),
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        orgnummer: String = ORGNUMMER,
        inntektsvurdering: Inntektsvurdering,
    ): Vilkårsgrunnlag {
        return Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            inntektsvurdering = inntektsvurdering,
            medlemskapsvurdering = Medlemskapsvurdering(medlemskapstatus),
            opptjeningvurdering = Opptjeningvurdering(
                if (arbeidsforhold.isEmpty()) listOf(
                    Arbeidsforhold(orgnummer, 1.januar(2017))
                )
                else arbeidsforhold
            )
        ).apply {
            hendelselogg = this
        }
    }

    private fun utbetalingpåminnelse(
        utbetalingId: UUID,
        status: Utbetalingstatus,
        tilstandsendringstidspunkt: LocalDateTime,
        orgnummer: String = ORGNUMMER
    ): Utbetalingpåminnelse {
        return Utbetalingpåminnelse(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            utbetalingId = utbetalingId,
            antallGangerPåminnet = 0,
            status = status,
            endringstidspunkt = tilstandsendringstidspunkt,
            påminnelsestidspunkt = LocalDateTime.now()
        )
    }

    private fun påminnelse(
        vedtaksperiodeId: UUID,
        påminnetTilstand: TilstandType,
        tilstandsendringstidspunkt: LocalDateTime,
        orgnummer: String = ORGNUMMER
    ): Påminnelse {
        return Påminnelse(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            antallGangerPåminnet = 0,
            tilstand = påminnetTilstand,
            tilstandsendringstidspunkt = tilstandsendringstidspunkt,
            påminnelsestidspunkt = LocalDateTime.now(),
            nestePåminnelsestidspunkt = LocalDateTime.now()
        )
    }

    private fun utbetalingshistorikk(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Infotrygdperiode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning>? = null,
        orgnummer: String = ORGNUMMER,
        harStatslønn: Boolean = false,
        besvart: LocalDateTime = LocalDateTime.now(),
    ): Utbetalingshistorikk {
        return Utbetalingshistorikk(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            arbeidskategorikoder = emptyMap(),
            harStatslønn = harStatslønn,
            perioder = utbetalinger,
            inntektshistorikk = inntektshistorikk(inntektshistorikk, orgnummer),
            ugyldigePerioder = emptyList(),
            besvart = besvart
        ).apply {
            hendelselogg = this
        }
    }

    private fun utbetalingshistorikkForFeriepenger(
        utbetalinger: List<UtbetalingshistorikkForFeriepenger.Utbetalingsperiode> = listOf(),
        feriepengehistorikk: List<UtbetalingshistorikkForFeriepenger.Feriepenger> = listOf(),
        arbeidskategorikoder: UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder =
            UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder(listOf(KodePeriode(LocalDate.MIN til LocalDate.MAX, Arbeidstaker))),
        opptjeningsår: Year = Year.of(2017),
        skalBeregnesManuelt: Boolean
    ): UtbetalingshistorikkForFeriepenger {
        return UtbetalingshistorikkForFeriepenger(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            utbetalinger = utbetalinger,
            feriepengehistorikk = feriepengehistorikk,
            arbeidskategorikoder = arbeidskategorikoder,
            opptjeningsår = opptjeningsår,
            skalBeregnesManuelt = skalBeregnesManuelt
        ).apply {
            hendelselogg = this
        }
    }

    protected fun ytelser(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Infotrygdperiode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning>? = null,
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null,
        pleiepenger: List<Periode> = emptyList(),
        omsorgspenger: List<Periode> = emptyList(),
        opplæringspenger: List<Periode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsoppholdsperiode> = emptyList(),
        orgnummer: String = ORGNUMMER,
        dødsdato: LocalDate? = null,
        statslønn: Boolean = false,
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList(),
        besvart: LocalDateTime = LocalDateTime.now()
    ): Ytelser {
        val aktivitetslogg = Aktivitetslogg()
        val meldingsreferanseId = UUID.randomUUID()

        val bedtOmSykepengehistorikk = erEtterspurt(Sykepengehistorikk, vedtaksperiodeId, orgnummer, AVVENTER_HISTORIKK)
            || erEtterspurt(Sykepengehistorikk, vedtaksperiodeId, orgnummer, TilstandType.AVVENTER_HISTORIKK_REVURDERING)
        if (bedtOmSykepengehistorikk) assertEtterspurt(Ytelser::class, Sykepengehistorikk, vedtaksperiodeId, orgnummer)
        val harSpesifisertSykepengehistorikk = utbetalinger.isNotEmpty() || arbeidskategorikoder.isNotEmpty()

        if (!bedtOmSykepengehistorikk && harSpesifisertSykepengehistorikk) {
            fail(
                "Vedtaksperiode $vedtaksperiodeId har ikke bedt om Sykepengehistorikk" +
                    "\nfordi den har gjenbrukt Infotrygdhistorikk-cache." +
                    "\nTrenger ikke sende inn utbetalinger og inntektsopplysninger da." +
                    "\nEnten ta bort overflødig historikk, eller sett 'besvart'-tidspunktet tilbake i tid " +
                    "på forrige Ytelser-innsending" +
                    "\n\n${inspektør.personLogg}"
            )
        }

        val utbetalingshistorikk = if (!bedtOmSykepengehistorikk)
            null
        else
            Utbetalingshistorikk(
                meldingsreferanseId = meldingsreferanseId,
                aktørId = AKTØRID,
                fødselsnummer = UNG_PERSON_FNR_2018,
                organisasjonsnummer = orgnummer,
                vedtaksperiodeId = vedtaksperiodeId.toString(),
                arbeidskategorikoder = arbeidskategorikoder,
                harStatslønn = statslønn,
                perioder = utbetalinger,
                inntektshistorikk = inntektshistorikk(inntektshistorikk, orgnummer),
                ugyldigePerioder = emptyList(),
                besvart = besvart
            )
        return Ytelser(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            utbetalingshistorikk = utbetalingshistorikk,
            foreldrepermisjon = Foreldrepermisjon(
                foreldrepengeytelse = foreldrepenger,
                svangerskapsytelse = svangerskapspenger,
                aktivitetslogg = aktivitetslogg
            ),
            pleiepenger = Pleiepenger(
                perioder = pleiepenger,
                aktivitetslogg = aktivitetslogg
            ),
            omsorgspenger = Omsorgspenger(
                perioder = omsorgspenger,
                aktivitetslogg = aktivitetslogg
            ),
            opplæringspenger = Opplæringspenger(
                perioder = opplæringspenger,
                aktivitetslogg = aktivitetslogg
            ),
            institusjonsopphold = Institusjonsopphold(
                perioder = institusjonsoppholdsperioder,
                aktivitetslogg = aktivitetslogg
            ),
            dødsinfo = Dødsinfo(dødsdato),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(arbeidsavklaringspenger),
            dagpenger = Dagpenger(dagpenger),
            aktivitetslogg = aktivitetslogg
        ).apply {
            hendelselogg = this
        }
    }

    private fun inntektshistorikk(
        inntektshistorikk: List<Inntektsopplysning>?,
        orgnummer: String
    ) = inntektshistorikk
        ?: listOf(Inntektsopplysning(orgnummer, 1.desember(2017), INNTEKT, true))


    protected fun nyttVedtak(
        fom: LocalDate,
        tom: LocalDate,
        grad: Prosentdel = 100.prosent,
        førsteFraværsdag: LocalDate = fom,
        orgnummer: String = ORGNUMMER,
        inntekterBlock: Inntektperioder.() -> Unit = { defaultInntekter(orgnummer, fom) }
    ) {
        val id = tilGodkjent(fom, tom, grad, førsteFraværsdag, orgnummer = orgnummer, inntekterBlock = inntekterBlock)
        håndterUtbetalt(id, status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT, orgnummer = orgnummer)
    }

    protected fun tilGodkjent(
        fom: LocalDate,
        tom: LocalDate,
        grad: Prosentdel,
        førsteFraværsdag: LocalDate,
        orgnummer: String = ORGNUMMER,
        inntekterBlock: Inntektperioder.() -> Unit = { defaultInntekter(orgnummer, fom) }
    ): UUID {
        val id = tilGodkjenning(fom, tom, grad, førsteFraværsdag, orgnummer = orgnummer, inntekterBlock = inntekterBlock)
        håndterUtbetalingsgodkjenning(id, true, orgnummer = orgnummer)
        return id
    }

    protected fun tilGodkjenning(
        fom: LocalDate,
        tom: LocalDate,
        grad: Prosentdel,
        førsteFraværsdag: LocalDate,
        orgnummer: String = ORGNUMMER,
        inntekterBlock: Inntektperioder.() -> Unit = { defaultInntekter(orgnummer, fom) }
    ): UUID {
        val id = tilYtelser(fom, tom, grad, førsteFraværsdag, orgnummer = orgnummer, inntekterBlock = inntekterBlock)
        håndterSimulering(id, orgnummer = orgnummer)
        return id
    }

    private fun Inntektperioder.defaultInntekter(orgnummer: String, fom: LocalDate) =
        fom.minusYears(1) til fom.minusMonths(1) inntekter {
            orgnummer inntekt INNTEKT
        }

    protected fun tilYtelser(
        fom: LocalDate,
        tom: LocalDate,
        grad: Prosentdel,
        førsteFraværsdag: LocalDate,
        orgnummer: String = ORGNUMMER,
        inntekterBlock: Inntektperioder.() -> Unit = { defaultInntekter(orgnummer, fom) }
    ): UUID {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, grad), orgnummer = orgnummer)
        val id = observatør.sisteVedtaksperiode()
        håndterInntektsmeldingMedValidering(
            id,
            listOf(Periode(fom, fom.plusDays(15))),
            førsteFraværsdag = førsteFraværsdag,
            orgnummer = orgnummer
        )
        håndterSøknadMedValidering(id, Søknad.Søknadsperiode.Sykdom(fom, tom, grad), orgnummer = orgnummer)
        håndterUtbetalingsgrunnlag(id, orgnummer = orgnummer)
        håndterYtelser(id, orgnummer = orgnummer)
        håndterVilkårsgrunnlag(
            id, INNTEKT, orgnummer = orgnummer, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag(inntekterBlock)
            )
        )
        håndterYtelser(id, orgnummer = orgnummer)
        return id
    }

    protected fun forlengVedtak(fom: LocalDate, tom: LocalDate, grad: Prosentdel = 100.prosent, orgnummer: String = ORGNUMMER) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, grad), orgnummer = orgnummer)
        val id = observatør.sisteVedtaksperiode()
        håndterSøknadMedValidering(id, Søknad.Søknadsperiode.Sykdom(fom, tom, grad), orgnummer = orgnummer)
        håndterUtbetalingsgrunnlag(id, orgnummer = orgnummer)
        håndterYtelser(id, orgnummer = orgnummer)
        håndterSimulering(id, orgnummer = orgnummer)
        håndterUtbetalingsgodkjenning(id, true, orgnummer = orgnummer)
        håndterUtbetalt(id, status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT, orgnummer = orgnummer)
    }

    protected fun forlengPeriode(fom: LocalDate, tom: LocalDate, grad: Prosentdel = 100.prosent, orgnummer: String = ORGNUMMER) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, grad), orgnummer = orgnummer)
        val id = observatør.sisteVedtaksperiode()
        håndterSøknadMedValidering(id, Søknad.Søknadsperiode.Sykdom(fom, tom, grad), orgnummer = orgnummer)
    }

    protected fun simulering(
        vedtaksperiodeId: UUID,
        simuleringOK: Boolean = true,
        orgnummer: String = ORGNUMMER
    ) =
        Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            simuleringOK = simuleringOK,
            melding = "",
            simuleringResultat = SimuleringResultat(
                totalbeløp = 2000,
                perioder = listOf(
                    SimulertPeriode(
                        periode = Periode(17.januar, 20.januar),
                        utbetalinger = listOf(
                            SimulertUtbetaling(
                                forfallsdato = 21.januar,
                                utbetalesTil = Mottaker(
                                    id = orgnummer,
                                    navn = "Org Orgesen AS"
                                ),
                                feilkonto = false,
                                detaljer = listOf(
                                    Detaljer(
                                        periode = Periode(17.januar, 20.januar),
                                        konto = "81549300",
                                        beløp = 2000,
                                        klassekode = Klassekode(
                                            kode = "SPREFAG-IOP",
                                            beskrivelse = "Sykepenger, Refusjon arbeidsgiver"
                                        ),
                                        uføregrad = 100,
                                        utbetalingstype = "YTEL",
                                        tilbakeføring = false,
                                        sats = Sats(
                                            sats = 1000,
                                            antall = 2,
                                            type = "DAG"
                                        ),
                                        refunderesOrgnummer = orgnummer
                                    )
                                )
                            )
                        )
                    )
                )
            )
        ).apply {
            hendelselogg = this
        }

    protected fun utbetalingsgodkjenning(
        vedtaksperiodeId: UUID,
        utbetalingGodkjent: Boolean,
        orgnummer: String,
        automatiskBehandling: Boolean,
        utbetalingId: UUID = UUID.fromString(
            inspektør.sisteBehov(Godkjenning).kontekst()["utbetalingId"]
                ?: throw IllegalStateException("Finner ikke utbetalingId i: ${inspektør.sisteBehov(Godkjenning).kontekst()}")
        ),
    ) = Utbetalingsgodkjenning(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = AKTØRID,
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        utbetalingId = utbetalingId,
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        saksbehandler = "Ola Nordmann",
        saksbehandlerEpost = "ola.nordmann@nav.no",
        utbetalingGodkjent = utbetalingGodkjent,
        godkjenttidspunkt = LocalDateTime.now(),
        automatiskBehandling = automatiskBehandling,
    ).apply {
        hendelselogg = this
    }

    protected fun assertInntektForDato(forventetInntekt: Inntekt?, dato: LocalDate, inspektør: TestArbeidsgiverInspektør) {
        assertEquals(forventetInntekt, inspektør.inntektInspektør.grunnlagForSykepengegrunnlag(dato))
    }

    private val vedtaksperioderIder = mutableMapOf<Pair<String, Int>, UUID>()

    private inner class VedtaksperioderFinder(person: Person) : PersonVisitor {
        private lateinit var orgnummer: String
        private var indeks = 0

        init {
            person.accept(this)
        }

        override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
            this.orgnummer = organisasjonsnummer
            indeks = 0
        }

        override fun postVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            periode: Periode,
            opprinneligPeriode: Periode,
            skjæringstidspunkt: LocalDate,
            periodetype: Periodetype,
            forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
            hendelseIder: Set<UUID>,
            inntektsmeldingInfo: InntektsmeldingInfo?,
            inntektskilde: Inntektskilde
        ) {
            vedtaksperioderIder[orgnummer to indeks] = id
            indeks++
        }
    }

    internal fun String.id(indeks: Int): UUID {
        if (vedtaksperioderIder[this to indeks] == null) VedtaksperioderFinder(person)
        return requireNotNull(vedtaksperioderIder[this to indeks])
    }

    protected fun <T : PersonHendelse> T.håndter(håndter: Person.(T) -> Unit): T {
        hendelselogg = this
        person.håndter(this)
        ikkeBesvarteBehov += EtterspurtBehov.finnEtterspurteBehov(behov())
        return this
    }

    private fun erEtterspurt(type: Behovtype, vedtaksperiodeId: UUID, orgnummer: String, tilstand: TilstandType): Boolean {
        return EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeId, orgnummer, tilstand) != null
    }

    private fun <T : ArbeidstakerHendelse> assertEtterspurt(løsning: KClass<T>, type: Behovtype, vedtaksperiodeId: UUID, orgnummer: String) {
        val etterspurtBehov = EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeId, orgnummer)
        assertTrue(ikkeBesvarteBehov.remove(etterspurtBehov)) {
            "Forventer at $type skal være etterspurt før ${løsning.simpleName} håndteres. Perioden er i ${
                observatør.tilstandsendringer[vedtaksperiodeId]?.last()
            }.\nAktivitetsloggen:\n${inspektør.personLogg}"
        }
    }

    private fun <T : ArbeidstakerHendelse> assertIkkeEtterspurt(løsning: KClass<T>, type: Behovtype, vedtaksperiodeId: UUID, orgnummer: String) {
        val etterspurtBehov = EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeId, orgnummer)
        assertFalse(etterspurtBehov in ikkeBesvarteBehov) {
            "Forventer ikke at $type skal være etterspurt før ${løsning.simpleName} håndteres. Perioden er i ${
                observatør.tilstandsendringer[vedtaksperiodeId]?.last()
            }"
        }
    }

    protected fun assertAlleBehovBesvart() {
        assertTrue(ikkeBesvarteBehov.isEmpty()) {
            "Ikke alle behov er besvart. Mangler fortsatt svar på behovene $ikkeBesvarteBehov"
        }
    }

    protected fun grunnlag(
        vedtaksperiodeId: UUID,
        orgnummer: String,
        inntekter: List<Inntekt>
    ) = lagMånedsinntekter(vedtaksperiodeId, orgnummer, inntekter, creator = ArbeidsgiverInntekt.MånedligInntekt::Sykepengegrunnlag)

    protected fun sammenligningsgrunnlag(
        vedtaksperiodeId: UUID,
        orgnummer: String,
        inntekter: List<Inntekt>
    ) = lagMånedsinntekter(vedtaksperiodeId, orgnummer, inntekter, creator = ArbeidsgiverInntekt.MånedligInntekt::Sammenligningsgrunnlag)

    private fun lagMånedsinntekter(
        vedtaksperiodeId: UUID,
        orgnummer: String,
        inntekter: List<Inntekt>,
        sluttMnd: YearMonth = YearMonth.from(inspektør(orgnummer).skjæringstidspunkt(vedtaksperiodeId)).minusMonths(1),
        creator: InntektCreator
    ) = ArbeidsgiverInntekt(
        orgnummer, inntekter.mapIndexed { index, inntekt ->
            creator(
                sluttMnd.minusMonths((inntekter.size - index - 1).toLong()),
                inntekt,
                ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT,
                "Juidy inntekt",
                "Juidy fordel"
            )
        }
    )

    protected fun assertHendelseIder(
        vararg hendelseIder: UUID,
        orgnummer: String,
        vedtaksperiodeIndeks: Int = 2,
    ) {
        assertEquals(
            hendelseIder.toSet(),
            inspektør(orgnummer).hendelseIder(vedtaksperiodeIndeks.vedtaksperiode(orgnummer))
        )
    }

    protected fun assertTilstand(
        orgnummer: String,
        tilstand: TilstandType,
        vedtaksperiodeIndeks: Int = 1
    ) {
        assertEquals(tilstand, inspektør(orgnummer).sisteTilstand(vedtaksperiodeIndeks.vedtaksperiode(orgnummer))) {
            inspektør.personLogg.toString()
        }
    }

    protected fun assertInntektskilde(
        orgnummer: String,
        inntektskilde: Inntektskilde,
        vedtaksperiodeIndeks: Int = 1
    ) {
        assertEquals(inntektskilde, inspektør(orgnummer).inntektskilde(vedtaksperiodeIndeks.vedtaksperiode(orgnummer)))
    }

    protected fun prosessperiode(periode: Periode, orgnummer: String, sykedagstelling: Int = 0) {
        gapPeriode(periode, orgnummer, sykedagstelling)
        historikk(orgnummer, sykedagstelling)
        betale(orgnummer)
    }

    protected fun gapPeriode(periode: Periode, orgnummer: String, sykedagstelling: Int = 0) {
        nyPeriode(periode, orgnummer)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(periode.start, periode.start.plusDays(15))),
            førsteFraværsdag = periode.start,
            orgnummer = orgnummer
        )
        håndterUtbetalingsgrunnlag(1.vedtaksperiode(orgnummer), orgnummer = orgnummer)
        historikk(orgnummer, sykedagstelling)
        person.håndter(vilkårsgrunnlag(
            orgnummer.id(0), orgnummer = orgnummer,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.desember(2017) inntekter {
                        orgnummer inntekt INNTEKT
                    }
                }
            ),
        )
        )
    }

    protected fun nyPeriode(periode: Periode, orgnummer: String) {
        person.håndter(
            sykmelding(
                UUID.randomUUID(),
                Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent),
                orgnummer = orgnummer
            )
        )
        person.håndter(
            søknad(
                UUID.randomUUID(),
                Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent),
                orgnummer = orgnummer
            )
        )
    }

    protected fun historikk(orgnummer: String, sykedagstelling: Int = 0) {
        person.håndter(
            ytelser(
                orgnummer.id(0),
                utbetalinger = utbetalinger(sykedagstelling, orgnummer),
                orgnummer = orgnummer
            )
        )
    }

    protected fun betale(orgnummer: String) {
        person.håndter(simulering(orgnummer.id(0), orgnummer = orgnummer))
        person.håndter(
            utbetalingsgodkjenning(
                orgnummer.id(0),
                true,
                orgnummer = orgnummer,
                automatiskBehandling = false
            )
        )
        person.håndter(
            UtbetalingOverført(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = AKTØRID,
                fødselsnummer = UNG_PERSON_FNR_2018,
                orgnummer = orgnummer,
                fagsystemId = inspektør(orgnummer).fagsystemId(orgnummer.id(0)),
                utbetalingId = hendelselogg.behov().first { it.type == Behovtype.Utbetaling }.kontekst().getValue("utbetalingId"),
                avstemmingsnøkkel = 123456L,
                overføringstidspunkt = LocalDateTime.now()
            )
        )
        person.håndter(
            utbetaling(
                inspektør(orgnummer).fagsystemId(orgnummer.id(0)),
                status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT,
                orgnummer = orgnummer
            )
        )
    }

    private fun utbetalinger(dagTeller: Int, orgnummer: String): List<ArbeidsgiverUtbetalingsperiode> {
        if (dagTeller == 0) return emptyList()
        val førsteDato = 2.desember(2017).minusDays(
            (
                (dagTeller / 5 * 7) + dagTeller % 5
                ).toLong()
        )
        return listOf(ArbeidsgiverUtbetalingsperiode(orgnummer, førsteDato, 1.desember(2017), 100.prosent, 100.daglig))
    }

    private val ikkeBesvarteBehov = mutableListOf<EtterspurtBehov>()

    private class EtterspurtBehov(
        private val type: Behovtype,
        private val tilstand: TilstandType,
        private val orgnummer: String,
        private val vedtaksperiodeId: UUID
    ) {
        companion object {
            internal fun fjern(liste: MutableList<EtterspurtBehov>, orgnummer: String, type: Behovtype) {
                liste.removeIf { it.orgnummer == orgnummer && it.type == type }
            }

            internal fun finnEtterspurteBehov(behovsliste: List<Aktivitetslogg.Aktivitet.Behov>) =
                behovsliste
                    .filter { "tilstand" in it.kontekst() }
                    .filter { "organisasjonsnummer" in it.kontekst() }
                    .filter { "vedtaksperiodeId" in it.kontekst() }
                    .map {
                        EtterspurtBehov(
                            type = it.type,
                            tilstand = enumValueOf(it.kontekst()["tilstand"] as String),
                            orgnummer = it.kontekst()["organisasjonsnummer"] as String,
                            vedtaksperiodeId = UUID.fromString(it.kontekst()["vedtaksperiodeId"] as String)
                        )
                    }

            internal fun finnEtterspurtBehov(
                ikkeBesvarteBehov: MutableList<EtterspurtBehov>,
                type: Behovtype,
                vedtaksperiodeId: UUID,
                orgnummer: String
            ) =
                ikkeBesvarteBehov.firstOrNull { it.type == type && it.orgnummer == orgnummer && it.vedtaksperiodeId == vedtaksperiodeId }

            internal fun finnEtterspurtBehov(
                ikkeBesvarteBehov: MutableList<EtterspurtBehov>,
                type: Behovtype,
                vedtaksperiodeId: UUID,
                orgnummer: String,
                tilstand: TilstandType
            ) =
                ikkeBesvarteBehov.firstOrNull {
                    it.type == type && it.orgnummer == orgnummer && it.vedtaksperiodeId == vedtaksperiodeId && it.tilstand == tilstand
                }
        }

        override fun toString() = "$type ($tilstand)"
    }

    fun assertWarn(message: String, aktivitetslogg: Aktivitetslogg) {
        var fant = false
        aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitWarn(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Warn, melding: String, tidsstempel: String) {
                if (message == melding) fant = true
            }
        })
        assertTrue(fant)
    }
}

infix fun <T> T?.er(expected: T?) =
    assertEquals(expected, this)

infix fun <T> T?.skalVære(expected: T?) =
    if (expected == null) {
        this == null
    } else {
        expected == this
    }

infix fun Boolean.ellers(message: String) {
    if (!this) fail(message)
}
