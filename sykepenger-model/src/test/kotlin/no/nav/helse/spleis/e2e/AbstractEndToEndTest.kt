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
import no.nav.helse.hendelser.Utbetalingshistorikk.Inntektsopplysning
import no.nav.helse.person.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.serde.api.serializePersonForSpeil
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.inntektperioder
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass

internal abstract class AbstractEndToEndTest : AbstractPersonTest() {

    protected companion object {
        val INNTEKT = 31000.00.månedlig
    }

    fun speilApi() = serializePersonForSpeil(person)
    protected lateinit var hendelselogg: ArbeidstakerHendelse
    protected var forventetEndringTeller = 0
    private val sykmeldinger = mutableMapOf<UUID, Array<out Sykmeldingsperiode>>()
    private val søknader = mutableMapOf<UUID, Triple<LocalDate, List<Søknad.Inntektskilde>, Array<out Søknad.Søknadsperiode>>>()
    private val inntektsmeldinger = mutableMapOf<UUID, InntektsmeldingData>()

    private data class InntektsmeldingData(
        val arbeidsgiverperioder: List<Periode>,
        val førsteFraværsdag: LocalDate,
        val ferieperioder: List<Periode>,
        val refusjon: Triple<LocalDate?, Inntekt, List<LocalDate>>
    )

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
        assertEquals(tilstand, observatør.tilstander[id]?.last())
    }

    protected fun assertTilstander(indeks: Int, vararg tilstander: TilstandType) {
        assertTilstander(vedtaksperiodeId(indeks), *tilstander)
    }

    protected fun assertTilstander(id: UUID, vararg tilstander: TilstandType) {
        assertFalse(inspektør.periodeErForkastet(id)) { "Perioden er forkastet" }
        assertTrue(inspektør.periodeErIkkeForkastet(id)) { "Perioden er forkastet" }
        assertEquals(tilstander.asList(), observatør.tilstander[id])
    }

    protected fun assertForkastetPeriodeTilstander(indeks: Int, vararg tilstander: TilstandType) {
        assertForkastetPeriodeTilstander(vedtaksperiodeId(indeks), *tilstander)
    }

    protected fun assertForkastetPeriodeTilstander(id: UUID, vararg tilstander: TilstandType) {
        assertTrue(inspektør.periodeErForkastet(id)) { "Perioden er ikke forkastet" }
        assertFalse(inspektør.periodeErIkkeForkastet(id)) { "Perioden er ikke forkastet" }
        assertEquals(tilstander.asList(), observatør.tilstander[id])
    }

    protected fun assertForkastetPeriodeTilstander(orgnummer: String, id: UUID, vararg tilstander: TilstandType) {
        assertTrue(inspektør(orgnummer).periodeErForkastet(id)) { "Perioden er ikke forkastet" }
        assertFalse(inspektør(orgnummer).periodeErIkkeForkastet(id)) { "Perioden er ikke forkastet" }
        assertEquals(tilstander.asList(), observatør.tilstander[id])
    }

    protected fun assertSisteForkastetPeriodeTilstand(orgnummer: String, id: UUID, tilstand: TilstandType){
        assertTrue(inspektør(orgnummer).periodeErForkastet(id)) { "Perioden er ikke forkastet" }
        assertFalse(inspektør(orgnummer).periodeErIkkeForkastet(id)) { "Perioden er ikke forkastet" }
        assertEquals(tilstand, observatør.tilstander[id]?.last())

    }

    protected fun assertReplayAv(vararg ids: UUID) {
        assertEquals(
            ids.map(this::vedtaksperiodeIndeks).toSet(),
            observatør.hendelserTilReplay.keys.map(this::vedtaksperiodeIndeks).toSet()
        )
        ids.forEachIndexed { index, id ->
            assertEquals(inspektør.hendelseIder(id), observatør.hendelserTilReplay[id], "index: $index")
        }
    }

    protected fun assertAntallReplays(antall: Int) {
        assertEquals(antall, observatør.hendelserTilReplay.size)
    }

    private fun vedtaksperiodeIndeks(id: UUID): String {
        val index = observatør.vedtaksperiodeIndeks(ORGNUMMER, id)
        return "${index + 1}.vedtaksperiode"
    }

    protected fun assertNoErrors(inspektør: TestArbeidsgiverInspektør) {
        assertFalse(inspektør.personLogg.hasErrorsOrWorse(), inspektør.personLogg.toString())
    }

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

    protected fun replayInntektsmelding(hendelseId: UUID): UUID {
        return håndterInntektsmelding(
            arbeidsgiverperioder = requireNotNull(inntektsmeldinger[hendelseId]).arbeidsgiverperioder,
            førsteFraværsdag = requireNotNull(inntektsmeldinger[hendelseId]).førsteFraværsdag,
            ferieperioder = requireNotNull(inntektsmeldinger[hendelseId]).ferieperioder,
            refusjon = requireNotNull(inntektsmeldinger[hendelseId]).refusjon,
            id = hendelseId
        )
    }

    protected fun håndterSykmelding(
        vararg sykeperioder: Sykmeldingsperiode,
        mottatt: LocalDateTime? = null,
        id: UUID = UUID.randomUUID(),
        orgnummer: String = ORGNUMMER
    ): UUID {
        sykmelding(
            id,
            *sykeperioder,
            mottatt = mottatt,
            orgnummer = orgnummer
        ).håndter(Person::håndter)
        sykmeldinger[id] = sykeperioder
        return id
    }

    protected fun håndterSøknadMedValidering(
        vedtaksperiodeId: UUID,
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: List<Søknad.Inntektskilde> = emptyList()
    ) {
        assertIkkeEtterspurt(Søknad::class, InntekterForSammenligningsgrunnlag, vedtaksperiodeId, ORGNUMMER)
        håndterSøknad(*perioder, andreInntektskilder = andreInntektskilder)
    }

    protected fun håndterSøknad(
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(),
        sendtTilNav: LocalDate = Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
        id: UUID = UUID.randomUUID(),
        orgnummer: String = ORGNUMMER
    ): UUID {
        søknad(
            id,
            *perioder,
            andreInntektskilder = andreInntektskilder,
            sendtTilNav = sendtTilNav,
            orgnummer = orgnummer
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
        ferieperioder: List<Periode> = emptyList(),
        refusjon: Triple<LocalDate?, Inntekt, List<LocalDate>> = Triple(null, INNTEKT, emptyList()),
        beregnetInntekt: Inntekt = refusjon.second
    ): UUID {
        assertIkkeEtterspurt(Inntektsmelding::class, InntekterForSammenligningsgrunnlag, vedtaksperiodeId, ORGNUMMER)
        return håndterInntektsmelding(arbeidsgiverperioder, førsteFraværsdag, ferieperioder, refusjon, beregnetInntekt = beregnetInntekt)
    }

    protected fun håndterInntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
        ferieperioder: List<Periode> = emptyList(),
        refusjon: Triple<LocalDate?, Inntekt, List<LocalDate>> = Triple(null, INNTEKT, emptyList()),
        id: UUID = UUID.randomUUID(),
        beregnetInntekt: Inntekt = refusjon.second,
        harOpphørAvNaturalytelser: Boolean = false
    ): UUID {
        inntektsmelding(
            id,
            arbeidsgiverperioder,
            ferieperioder = ferieperioder,
            beregnetInntekt = beregnetInntekt,
            førsteFraværsdag = førsteFraværsdag,
            refusjon = refusjon,
            harOpphørAvNaturalytelser = harOpphørAvNaturalytelser
        ).håndter(Person::håndter)
        inntektsmeldinger[id] = InntektsmeldingData(
            arbeidsgiverperioder,
            førsteFraværsdag,
            ferieperioder,
            refusjon
        )
        return id
    }

    protected fun håndterInntektsmeldingReplay(
        inntektsmelding: Inntektsmelding,
        vedtaksperiodeId: UUID
    ) {
        assertTrue(vedtaksperiodeId in observatør.inntektsmeldingReplayEventer)
        inntektsmeldingReplay(
            inntektsmelding,
            vedtaksperiodeId
        ).håndter(Person::håndter)
    }

    protected fun håndterVilkårsgrunnlag(
        vedtaksperiodeId: UUID,
        inntekt: Inntekt = INNTEKT,
        arbeidsforhold: List<Opptjeningvurdering.Arbeidsforhold> = emptyList(),
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        orgnummer: String = ORGNUMMER,
        inntektsvurdering: Inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
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
            inntektsvurdering
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
        vararg utbetalinger: Utbetalingshistorikk.Infotrygdperiode,
        inntektshistorikk: List<Inntektsopplysning>? = null,
        orgnummer: String = ORGNUMMER
    ) {
        assertEtterspurt(Utbetalingshistorikk::class, Sykepengehistorikk, vedtaksperiodeId, orgnummer)

        utbetalingshistorikk(
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalinger = utbetalinger.toList(),
            inntektshistorikk = inntektshistorikk(inntektshistorikk, orgnummer),
            orgnummer = orgnummer
        ).håndter(Person::håndter)
    }

    protected fun håndterYtelser(
        vedtaksperiodeId: UUID = 1.vedtaksperiode,
        vararg utbetalinger: Utbetalingshistorikk.Infotrygdperiode,
        inntektshistorikk: List<Inntektsopplysning>? = null,
        foreldrepenger: Periode? = null,
        pleiepenger: List<Periode> = emptyList(),
        omsorgspenger: List<Periode> = emptyList(),
        opplæringspenger: List<Periode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsoppholdsperiode> = emptyList(),
        orgnummer: String = ORGNUMMER,
        dødsdato: LocalDate? = null,
        statslønn: Boolean = false,
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList()
    ) {
        fun assertEtterspurt(behovtype: Behovtype) =
            assertEtterspurt(Ytelser::class, behovtype, vedtaksperiodeId, orgnummer)

        assertEtterspurt(Sykepengehistorikk)
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
            arbeidsavklaringspenger = arbeidsavklaringspenger,
            dagpenger = dagpenger
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
        automatiskBehandling: Boolean = false
    ) {
        assertEtterspurt(Utbetalingsgodkjenning::class, Godkjenning, vedtaksperiodeId, orgnummer)
        utbetalingsgodkjenning(vedtaksperiodeId, utbetalingGodkjent, orgnummer, automatiskBehandling).håndter(Person::håndter)
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

    protected fun håndterOverstyring(overstyringsdager: List<ManuellOverskrivingDag>) {
        OverstyrTidslinje(
            meldingsreferanseId = UUID.randomUUID(),
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            organisasjonsnummer = ORGNUMMER,
            dager = overstyringsdager
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

    protected fun sykmelding(
        id: UUID,
        vararg sykeperioder: Sykmeldingsperiode,
        orgnummer: String = ORGNUMMER,
        mottatt: LocalDateTime? = null
    ): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = id,
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = orgnummer,
            sykeperioder = listOf(*sykeperioder),
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
            mottatt = Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.plusYears(2)?.atStartOfDay() ?: LocalDateTime.now()
        ).apply {
            hendelselogg = this
        }
    }

    protected fun søknad(
        id: UUID,
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(),
        sendtTilNav: LocalDate = Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
        orgnummer: String = ORGNUMMER
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
            merknaderFraSykmelding = emptyList()
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
            perioder = listOf(*perioder)
        ).apply {
            hendelselogg = this
        }
    }

    private fun inntektsmeldingReplay(
        inntektsmelding: Inntektsmelding,
        vedtaksperiodeId: UUID
    ): InntektsmeldingReplay {
        return InntektsmeldingReplay(
            meldingsreferanseId = UUID.randomUUID(),
            wrapped = inntektsmelding,
            vedtaksperiodeId = vedtaksperiodeId
        ).apply {
            hendelselogg = this
        }
    }

    protected fun inntektsmelding(
        id: UUID,
        arbeidsgiverperioder: List<Periode>,
        ferieperioder: List<Periode> = emptyList(),
        beregnetInntekt: Inntekt = INNTEKT,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
        refusjon: Triple<LocalDate?, Inntekt, List<LocalDate>> = Triple(null, beregnetInntekt, emptyList()),
        orgnummer: String = ORGNUMMER,
        harOpphørAvNaturalytelser: Boolean = false
    ): Inntektsmelding {
        return Inntektsmelding(
            meldingsreferanseId = id,
            refusjon = Inntektsmelding.Refusjon(refusjon.first, refusjon.second, refusjon.third),
            orgnummer = orgnummer,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = arbeidsgiverperioder,
            ferieperioder = ferieperioder,
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            harOpphørAvNaturalytelser = harOpphørAvNaturalytelser
        ).apply {
            hendelselogg = this
        }
    }

    protected fun vilkårsgrunnlag(
        vedtaksperiodeId: UUID,
        arbeidsforhold: List<Opptjeningvurdering.Arbeidsforhold> = emptyList(),
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        orgnummer: String = ORGNUMMER,
        inntektsvurdering: Inntektsvurdering
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
                    Opptjeningvurdering.Arbeidsforhold(orgnummer, 1.januar(2017))
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
        utbetalinger: List<Utbetalingshistorikk.Infotrygdperiode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning>? = null,
        orgnummer: String = ORGNUMMER
    ): Utbetalingshistorikk {
        return Utbetalingshistorikk(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            utbetalinger = utbetalinger,
            inntektshistorikk =
            inntektshistorikk(inntektshistorikk, orgnummer)
        ).apply {
            hendelselogg = this
        }
    }

    protected fun ytelser(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Utbetalingshistorikk.Infotrygdperiode> = listOf(),
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
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList()
    ): Ytelser {
        val aktivitetslogg = Aktivitetslogg()
        val meldingsreferanseId = UUID.randomUUID()
        return Ytelser(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            statslønn = statslønn,
            utbetalingshistorikk = Utbetalingshistorikk(
                meldingsreferanseId = meldingsreferanseId,
                aktørId = AKTØRID,
                fødselsnummer = UNG_PERSON_FNR_2018,
                organisasjonsnummer = orgnummer,
                vedtaksperiodeId = vedtaksperiodeId.toString(),
                utbetalinger = utbetalinger,
                inntektshistorikk = inntektshistorikk(inntektshistorikk, orgnummer),
                aktivitetslogg = aktivitetslogg
            ),
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
            aktivitetslogg = aktivitetslogg,
            dødsinfo = Dødsinfo(dødsdato),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(arbeidsavklaringspenger),
            dagpenger = Dagpenger(dagpenger)
        ).apply {
            hendelselogg = this
        }
    }

    private fun inntektshistorikk(
        inntektshistorikk: List<Inntektsopplysning>?,
        orgnummer: String
    ) = inntektshistorikk
        ?: listOf(
            Inntektsopplysning(
                1.desember(2017),
                INNTEKT,
                orgnummer,
                true
            )
        )


    protected fun nyttVedtak(fom: LocalDate, tom: LocalDate, grad: Prosentdel = 100.prosent, førsteFraværsdag: LocalDate = fom) {
        val id = tilGodkjent(fom, tom, grad, førsteFraværsdag)
        håndterUtbetalt(id, status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
    }

    protected fun tilGodkjent(
        fom: LocalDate,
        tom: LocalDate,
        grad: Prosentdel,
        førsteFraværsdag: LocalDate
    ): UUID {
        val id = tilSimulert(fom, tom, grad, førsteFraværsdag)
        håndterUtbetalingsgodkjenning(id, true)
        return id
    }

    protected fun tilSimulert(
        fom: LocalDate,
        tom: LocalDate,
        grad: Prosentdel,
        førsteFraværsdag: LocalDate
    ): UUID {
        val id = tilYtelser(fom, tom, grad, førsteFraværsdag)
        håndterSimulering(id)
        return id
    }

    protected fun tilYtelser(
        fom: LocalDate,
        tom: LocalDate,
        grad: Prosentdel,
        førsteFraværsdag: LocalDate
    ): UUID {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, grad))
        val id = observatør.sisteVedtaksperiode()
        håndterInntektsmeldingMedValidering(
            id,
            listOf(Periode(fom, fom.plusDays(15))),
            førsteFraværsdag = førsteFraværsdag
        )
        håndterSøknadMedValidering(id, Søknad.Søknadsperiode.Sykdom(fom, tom, grad))
        håndterVilkårsgrunnlag(id, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                fom.minusYears(1) til fom.minusMonths(1) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(id)   // No history
        return id
    }


    protected fun forlengVedtak(fom: LocalDate, tom: LocalDate, grad: Prosentdel = 100.prosent) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, grad))
        val id = observatør.sisteVedtaksperiode()
        håndterSøknadMedValidering(id, Søknad.Søknadsperiode.Sykdom(fom, tom, grad))
        håndterYtelser(id)   // No history
        håndterSimulering(id)
        håndterUtbetalingsgodkjenning(id, true)
        håndterUtbetalt(id, status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
    }

    protected fun forlengPeriode(fom: LocalDate, tom: LocalDate, grad: Prosentdel = 100.prosent) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, grad))
        val id = observatør.sisteVedtaksperiode()
        håndterSøknadMedValidering(id, Søknad.Søknadsperiode.Sykdom(fom, tom, grad))
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
        automatiskBehandling: Boolean
    ) = Utbetalingsgodkjenning(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = AKTØRID,
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        utbetalingId = UUID.fromString(
            inspektør.sisteBehov(Godkjenning).kontekst()["utbetalingId"]
                ?: throw IllegalStateException("Finner ikke utbetalingId i: ${inspektør.sisteBehov(Godkjenning).kontekst()}")
        ),
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        saksbehandler = "Ola Nordmann",
        utbetalingGodkjent = utbetalingGodkjent,
        godkjenttidspunkt = LocalDateTime.now(),
        automatiskBehandling = automatiskBehandling,
        saksbehandlerEpost = "ola.nordmann@nav.no",
        makstidOppnådd = false,
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
            hendelseIder: List<UUID>,
            inntektsmeldingId: UUID?,
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
        person.håndter(this)
        ikkeBesvarteBehov += EtterspurtBehov.finnEtterspurteBehov(behov())
        return this
    }

    private fun <T : ArbeidstakerHendelse> assertEtterspurt(løsning: KClass<T>, type: Behovtype, vedtaksperiodeId: UUID, orgnummer: String) {
        val etterspurtBehov = EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeId, orgnummer)
        assertTrue(ikkeBesvarteBehov.remove(etterspurtBehov)) {
            "Forventer at $type skal være etterspurt før ${løsning.simpleName} håndteres. Perioden er i ${
                observatør.tilstander[vedtaksperiodeId]?.last()
            }.\nAktivitetsloggen:\n${inspektør.personLogg}"
        }
    }

    private fun <T : ArbeidstakerHendelse> assertIkkeEtterspurt(løsning: KClass<T>, type: Behovtype, vedtaksperiodeId: UUID, orgnummer: String) {
        val etterspurtBehov = EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeId, orgnummer)
        assertFalse(etterspurtBehov in ikkeBesvarteBehov) {
            "Forventer ikke at $type skal være etterspurt før ${løsning.simpleName} håndteres. Perioden er i ${
                observatør.tilstander[vedtaksperiodeId]?.last()
            }"
        }
    }

    protected fun assertAlleBehovBesvart() {
        assertTrue(ikkeBesvarteBehov.isEmpty()) {
            "Ikke alle behov er besvart. Mangler fortsatt svar på behovene $ikkeBesvarteBehov"
        }
    }

    private val ikkeBesvarteBehov = mutableListOf<EtterspurtBehov>()

    private class EtterspurtBehov(
        private val type: Behovtype,
        private val tilstand: TilstandType,
        private val orgnummer: String,
        private val vedtaksperiodeId: UUID
    ) {
        companion object {
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
        }

        override fun toString() = "$type ($tilstand)"
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
