package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Arbeidsavklaringspenger
import no.nav.helse.hendelser.Dagpenger
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.Institusjonsopphold.Institusjonsoppholdsperiode
import no.nav.helse.hendelser.Omsorgspenger
import no.nav.helse.hendelser.Opplæringspenger
import no.nav.helse.hendelser.Pleiepenger
import no.nav.helse.hendelser.Simulering.*
import no.nav.helse.hendelser.Utbetalingshistorikk.Inntektsopplysning
import no.nav.helse.person.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering
import no.nav.helse.serde.api.serializePersonForSpeil
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.inntektperioder
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal abstract class AbstractEndToEndTest {

    protected companion object {
        const val UNG_PERSON_FNR_2018 = "12020052345"
        const val AKTØRID = "42"
        const val ORGNUMMER = "987654321"
        val INNTEKT = 31000.00.månedlig
    }

    protected lateinit var person: Person
    protected lateinit var observatør: TestObservatør
    protected val inspektør get() = TestArbeidsgiverInspektør(person)
    fun speilApi () = serializePersonForSpeil(person)
    protected lateinit var hendelselogg: ArbeidstakerHendelse
    protected var forventetEndringTeller = 0
    private val sykmeldinger = mutableMapOf<UUID, Array<out Sykmeldingsperiode>>()
    private val søknader = mutableMapOf<UUID, Triple<LocalDate, Boolean, Array<out Søknad.Søknadsperiode>>>()
    private val inntektsmeldinger = mutableMapOf<UUID, InntektsmeldingData>()

    protected val Int.vedtaksperiode get() = vedtaksperiodeId(this - 1)

    private data class InntektsmeldingData(
        val arbeidsgiverperioder: List<Periode>,
        val førsteFraværsdag: LocalDate,
        val ferieperioder: List<Periode>,
        val refusjon: Triple<LocalDate?, Inntekt, List<LocalDate>>
    )

    fun <T>sjekkAt(t: T, init: T.() -> Unit) {
        t.init()
    }

    @BeforeEach
    internal fun abstractSetup() {
        person = Person(AKTØRID, UNG_PERSON_FNR_2018)
        observatør = TestObservatør().also { person.addObserver(it) }
        sykmeldinger.clear()
        søknader.clear()
        inntektsmeldinger.clear()
    }

    @AfterEach
    fun teardown() {
        Toggles.replayEnabled = false
        Toggles.mottattInntektsmeldingEventEnabled = false
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

    private fun vedtaksperiodeId(indeks: Int) = observatør.vedtaksperioder.toList()[indeks]

    private fun vedtaksperiodeIndeks(id: UUID): String {
        val index = observatør.vedtaksperioder.indexOf(id)
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
        harAndreInntektskilder = requireNotNull(søknader[hendelseId]).second,
        perioder = requireNotNull(søknader[hendelseId]).third
    )

    protected fun replayInntektsmelding(hendelseId: UUID): UUID {
        return håndterInntektsmelding(
            requireNotNull(inntektsmeldinger[hendelseId]).arbeidsgiverperioder,
            requireNotNull(inntektsmeldinger[hendelseId]).førsteFraværsdag,
            requireNotNull(inntektsmeldinger[hendelseId]).ferieperioder,
            requireNotNull(inntektsmeldinger[hendelseId]).refusjon
        )
    }

    protected fun replaySøknadArbeidsgiver(søknadArbeidsgiver: SøknadArbeidsgiver): Unit =
        person.håndter(søknadArbeidsgiver)

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
        ).also(person::håndter)
        sykmeldinger[id] = sykeperioder
        return id
    }

    protected fun håndterSøknadMedValidering(
        vedtaksperiodeId: UUID,
        vararg perioder: Søknad.Søknadsperiode,
        harAndreInntektskilder: Boolean = false
    ) {
        assertFalse(inspektør.etterspurteBehov(vedtaksperiodeId, InntekterForSammenligningsgrunnlag))
        håndterSøknad(*perioder, harAndreInntektskilder = harAndreInntektskilder)
    }

    protected fun håndterSøknad(
        vararg perioder: Søknad.Søknadsperiode,
        harAndreInntektskilder: Boolean = false,
        sendtTilNav: LocalDate = Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
        id: UUID = UUID.randomUUID()
    ): UUID {
        søknad(
            id = id,
            perioder = perioder,
            harAndreInntektskilder = harAndreInntektskilder,
            sendtTilNav = sendtTilNav
        ).also(person::håndter)
        søknader[id] = Triple(sendtTilNav, harAndreInntektskilder, perioder)
        return id
    }

    protected fun håndterSøknadArbeidsgiver(
        vararg perioder: SøknadArbeidsgiver.Søknadsperiode,
        orgnummer: String = ORGNUMMER
    ) = søknadArbeidsgiver(perioder = perioder, orgnummer = orgnummer).also(person::håndter)

    protected fun håndterInntektsmeldingMedValidering(
        vedtaksperiodeId: UUID,
        arbeidsgiverperioder: List<Periode>,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
        ferieperioder: List<Periode> = emptyList(),
        refusjon: Triple<LocalDate?, Inntekt, List<LocalDate>> = Triple(null, INNTEKT, emptyList())
    ) {
        assertFalse(inspektør.etterspurteBehov(vedtaksperiodeId, InntekterForSammenligningsgrunnlag))
        håndterInntektsmelding(arbeidsgiverperioder, førsteFraværsdag, ferieperioder, refusjon)
    }

    protected fun håndterInntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
        ferieperioder: List<Periode> = emptyList(),
        refusjon: Triple<LocalDate?, Inntekt, List<LocalDate>> = Triple(null, INNTEKT, emptyList()),
        id: UUID = UUID.randomUUID(),
        beregnetInntekt: Inntekt = refusjon.second
    ): UUID {
        inntektsmelding(
            id,
            arbeidsgiverperioder,
            ferieperioder = ferieperioder,
            førsteFraværsdag = førsteFraværsdag,
            refusjon = refusjon,
            beregnetInntekt = beregnetInntekt
        ).also(person::håndter)
        inntektsmeldinger[id] = InntektsmeldingData(
            arbeidsgiverperioder,
            førsteFraværsdag,
            ferieperioder,
            refusjon
        )
        return id
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
        ),
        arbeidsavklaringspenger: List<Periode> = emptyList()
    ) {
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeId, InntekterForSammenligningsgrunnlag))
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeId, Behovtype.Dagpenger))
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeId, Behovtype.Arbeidsavklaringspenger))
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeId, Medlemskap))
        person.håndter(
            vilkårsgrunnlag(
                vedtaksperiodeId,
                arbeidsforhold,
                medlemskapstatus,
                orgnummer,
                inntektsvurdering,
                arbeidsavklaringspenger
            )
        )
    }

    protected fun håndterSimulering(vedtaksperiodeId: UUID = 1.vedtaksperiode) {
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeId, Simulering))
        person.håndter(simulering(vedtaksperiodeId))
    }

    protected fun håndterUtbetalingshistorikk(
        vedtaksperiodeId: UUID,
        vararg utbetalinger: Utbetalingshistorikk.Periode,
        inntektshistorikk: List<Inntektsopplysning>? = null,
        orgnummer: String = ORGNUMMER
    ) {
        person.håndter(
            utbetalingshistorikk(
                vedtaksperiodeId,
                utbetalinger.toList(),
                inntektshistorikk(inntektshistorikk, orgnummer)
            )
        )
    }

    protected fun håndterYtelser(
        vedtaksperiodeId: UUID = 1.vedtaksperiode,
        vararg utbetalinger: Utbetalingshistorikk.Periode,
        inntektshistorikk: List<Inntektsopplysning>? = null,
        foreldrepenger: Periode? = null,
        pleiepenger: List<Periode> = emptyList(),
        omsorgspenger: List<Periode> = emptyList(),
        opplæringspenger: List<Periode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsoppholdsperiode> = emptyList(),
        orgnummer: String = ORGNUMMER
    ) {
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeId, Sykepengehistorikk))
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeId, Foreldrepenger))
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeId, Behovtype.Pleiepenger))
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeId, Behovtype.Omsorgspenger))
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeId, Behovtype.Opplæringspenger))
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeId, Behovtype.Institusjonsopphold))
        person.håndter(
            ytelser(
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalinger = utbetalinger.toList(),
                inntektshistorikk = inntektshistorikk(inntektshistorikk, orgnummer),
                foreldrepenger = foreldrepenger,
                pleiepenger = pleiepenger,
                omsorgspenger = omsorgspenger,
                opplæringspenger = opplæringspenger,
                institusjonsoppholdsperioder = institusjonsoppholdsperioder
            )
        )
    }

    protected fun håndterAnnullering(
        fom: LocalDate = 3.januar,
        tom: LocalDate = 26.januar
    ) {
        person.håndter(
            Annullering(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = AKTØRID,
                fødselsnummer = UNG_PERSON_FNR_2018,
                organisasjonsnummer = ORGNUMMER,
                fom = fom,
                tom = tom,
                saksbehandlerIdent = "Z999999",
                saksbehandler = "Ola Nordmann",
                saksbehandlerEpost = "tbd@nav.no",
                opprettet = LocalDateTime.now()
            )
        )
    }

    protected fun håndterPåminnelse(
        vedtaksperiodeId: UUID,
        påminnetTilstand: TilstandType,
        tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now()
    ) {
        person.håndter(påminnelse(vedtaksperiodeId, påminnetTilstand, tilstandsendringstidspunkt))
    }

    protected fun håndterUtbetalingsgodkjenning(
        vedtaksperiodeId: UUID = 1.vedtaksperiode,
        utbetalingGodkjent: Boolean = true,
        orgnummer: String = ORGNUMMER,
        automatiskBehandling: Boolean = false
    ) {
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeId, Godkjenning))
        person.håndter(utbetalingsgodkjenning(vedtaksperiodeId, utbetalingGodkjent, orgnummer, automatiskBehandling))
    }

    protected fun håndterUtbetalt(
        vedtaksperiodeId: UUID = 1.vedtaksperiode,
        status: UtbetalingHendelse.Oppdragstatus = UtbetalingHendelse.Oppdragstatus.AKSEPTERT,
        saksbehandlerEpost: String = "siri.saksbehanlder@nav.no",
        annullert: Boolean = false,
        fagsystemId: String = inspektør.fagsystemId(vedtaksperiodeId)

    ) {
        person.håndter(
            utbetaling(
                vedtaksperiodeId = vedtaksperiodeId,
                fagsystemId = fagsystemId,
                status = status,
                saksbehandlerEpost = saksbehandlerEpost,
                annullert = annullert
            )
        )
    }

    protected fun håndterGrunnbeløpsregulering(
        orgnummer: String = ORGNUMMER,
        fagsystemId: String = inspektør.arbeidsgiverOppdrag.last().fagsystemId(),
        gyldighetsdato: LocalDate
    ) {
        person.håndter(
            Grunnbeløpsregulering(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = AKTØRID,
                fødselsnummer = UNG_PERSON_FNR_2018,
                organisasjonsnummer = orgnummer,
                gyldighetsdato = gyldighetsdato,
                fagsystemId = fagsystemId
            )
        )
    }

    protected fun håndterKansellerUtbetaling(
        orgnummer: String = ORGNUMMER,
        fagsystemId: String = inspektør.arbeidsgiverOppdrag.last().fagsystemId()
    ) {
        person.håndter(
            KansellerUtbetaling(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = AKTØRID,
                fødselsnummer = UNG_PERSON_FNR_2018,
                organisasjonsnummer = orgnummer,
                fagsystemId = fagsystemId,
                saksbehandler = "Ola Nordmann",
                saksbehandlerEpost = "tbd@nav.no",
                opprettet = LocalDateTime.now()
            )
        )
    }

    protected fun håndterOverstyring(overstyringsdager: List<ManuellOverskrivingDag>) {
        person.håndter(
            OverstyrTidslinje(
                meldingsreferanseId = UUID.randomUUID(),
                fødselsnummer = UNG_PERSON_FNR_2018,
                aktørId = AKTØRID,
                organisasjonsnummer = ORGNUMMER,
                dager = overstyringsdager
            )
        )
    }

    protected fun utbetaling(
        vedtaksperiodeId: UUID,
        fagsystemId: String,
        status: UtbetalingHendelse.Oppdragstatus,
        orgnummer: String = ORGNUMMER,
        saksbehandlerEpost: String = "siri.saksbehanlder@nav.no",
        annullert: Boolean = false
    ) =
        UtbetalingHendelse(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            utbetalingsreferanse = fagsystemId,
            status = status,
            melding = "hei",
            saksbehandler = "Z999999",
            saksbehandlerEpost = saksbehandlerEpost,
            godkjenttidspunkt = LocalDateTime.now(),
            annullert = annullert
        )

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
            mottatt = mottatt ?: sykeperioder.minOfOrNull { it.fom }?.atStartOfDay() ?: LocalDateTime.now()
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
            sykeperioder = listOf(*sykeperioder),
            mottatt = sykeperioder.minOfOrNull { it.fom }?.plusYears(2)?.atStartOfDay() ?: LocalDateTime.now()
        ).apply {
            hendelselogg = this
        }
    }

    protected fun søknad(
        id: UUID,
        vararg perioder: Søknad.Søknadsperiode,
        harAndreInntektskilder: Boolean = false,
        sendtTilNav: LocalDate = Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
        orgnummer: String = ORGNUMMER
    ): Søknad {
        return Søknad(
            meldingsreferanseId = id,
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = orgnummer,
            perioder = listOf(*perioder),
            harAndreInntektskilder = harAndreInntektskilder,
            sendtTilNAV = sendtTilNav.atStartOfDay(),
            permittert = false
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

    protected fun inntektsmelding(
        id: UUID,
        arbeidsgiverperioder: List<Periode>,
        ferieperioder: List<Periode> = emptyList(),
        beregnetInntekt: Inntekt = INNTEKT,
        førsteFraværsdag: LocalDate = 1.januar,
        refusjon: Triple<LocalDate?, Inntekt, List<LocalDate>> = Triple(null, beregnetInntekt, emptyList()),
        orgnummer: String = ORGNUMMER
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
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        ).apply {
            hendelselogg = this
        }
    }

    protected fun vilkårsgrunnlag(
        vedtaksperiodeId: UUID,
        arbeidsforhold: List<Opptjeningvurdering.Arbeidsforhold> = emptyList(),
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        orgnummer: String = ORGNUMMER,
        inntektsvurdering: Inntektsvurdering,
        arbeidsavklaringspenger: List<Periode> = emptyList()
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
            ),
            dagpenger = Dagpenger(emptyList()),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(arbeidsavklaringspenger)
        ).apply {
            hendelselogg = this
        }
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
        utbetalinger: List<Utbetalingshistorikk.Periode> = listOf(),
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
        utbetalinger: List<Utbetalingshistorikk.Periode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning>? = null,
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null,
        pleiepenger: List<Periode> = emptyList(),
        omsorgspenger: List<Periode> = emptyList(),
        opplæringspenger: List<Periode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsoppholdsperiode> = emptyList(),
        orgnummer: String = ORGNUMMER
    ): Ytelser {
        val aktivitetslogg = Aktivitetslogg()
        val meldingsreferanseId = UUID.randomUUID()
        return Ytelser(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
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
            aktivitetslogg = aktivitetslogg
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


    protected fun nyttVedtak(fom: LocalDate, tom: LocalDate, grad: Int = 100, førsteFraværsdag: LocalDate = fom) {
        val id = tilGodkjent(fom, tom, grad, førsteFraværsdag)
        håndterUtbetalt(id, status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
    }

    protected fun tilGodkjent(
        fom: LocalDate,
        tom: LocalDate,
        grad: Int,
        førsteFraværsdag: LocalDate
    ): UUID {
        val id = tilSimulert(fom, tom, grad, førsteFraværsdag)
        håndterUtbetalingsgodkjenning(id, true)
        return id
    }

    protected fun tilSimulert(
        fom: LocalDate,
        tom: LocalDate,
        grad: Int,
        førsteFraværsdag: LocalDate
    ): UUID {
        val id = tilYtelser(fom, tom, grad, førsteFraværsdag)
        håndterSimulering(id)
        return id
    }

    protected fun tilYtelser(
        fom: LocalDate,
        tom: LocalDate,
        grad: Int,
        førsteFraværsdag: LocalDate
    ): UUID {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, grad))
        val id = observatør.vedtaksperioder.toList().last()
        håndterInntektsmeldingMedValidering(
            id,
            listOf(Periode(fom, fom.plusDays(15))),
            førsteFraværsdag = førsteFraværsdag
        )
        håndterSøknadMedValidering(id, Søknad.Søknadsperiode.Sykdom(fom, tom, grad))
        håndterVilkårsgrunnlag(id, INNTEKT)
        håndterYtelser(id)   // No history
        return id
    }


    protected fun forlengVedtak(fom: LocalDate, tom: LocalDate, grad: Int = 100) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, grad))
        val id = observatør.vedtaksperioder.toList().last()
        håndterSøknadMedValidering(id, Søknad.Søknadsperiode.Sykdom(fom, tom, grad))
        håndterYtelser(id)   // No history
        håndterSimulering(id)
        håndterUtbetalingsgodkjenning(id, true)
        håndterUtbetalt(id, status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
    }

    protected fun forlengPeriode(fom: LocalDate, tom: LocalDate, grad: Int = 100) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, grad))
        val id = observatør.vedtaksperioder.toList().last()
        håndterSøknadMedValidering(id, Søknad.Søknadsperiode.Sykdom(fom, tom, grad))
    }

    protected fun simulering(
        vedtaksperiodeId: UUID,
        simuleringOK: Boolean = true,
        orgnummer: String = ORGNUMMER
    ) =
        no.nav.helse.hendelser.Simulering(
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
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        saksbehandler = "Ola Nordmann",
        utbetalingGodkjent = utbetalingGodkjent,
        godkjenttidspunkt = LocalDateTime.now(),
        automatiskBehandling = automatiskBehandling,
        saksbehandlerEpost = "ola.nordmann@nav.no"
    ).apply {
        hendelselogg = this
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
            arbeidsgiverNettoBeløp: Int,
            personNettoBeløp: Int,
            periode: Periode,
            opprinneligPeriode: Periode
        ) {
            vedtaksperioderIder[orgnummer to indeks] = id
            indeks++
        }
    }

    internal fun String.id(indeks: Int): UUID {
        if (vedtaksperioderIder[this to indeks] == null) VedtaksperioderFinder(person)
        return requireNotNull(vedtaksperioderIder[this to indeks])
    }

}

const val sant = true

const val usant = false

infix fun <T>T?.er(expected: T?) =
    assertEquals(expected, this)

infix fun <T>T?.skalVære(expected: T?) =
    if (expected == null) {
        this == null
    } else {
        expected == this
    }

infix fun Boolean.ellers(message: String) {
    if (!this) fail(message)
}
