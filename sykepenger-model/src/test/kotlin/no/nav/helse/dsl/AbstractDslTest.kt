package no.nav.helse.dsl

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.dsl.Varslersamler.AssertetVarsler
import no.nav.helse.dto.serialisering.PersonUtDto
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.gjenopprettFraJSON
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.PersonInspektør
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.serde.assertPersonEquals
import no.nav.helse.serde.tilPersonData
import no.nav.helse.serde.tilSerialisertPerson
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith

@Tag("e2e")
@ExtendWith(DeferredLogging::class)
internal abstract class AbstractDslTest {
    internal companion object {
        @JvmStatic
        protected val personInspektør = { person: Person -> PersonInspektør(person) }

        @JvmStatic
        protected val personView = { person: Person -> person.view() }

        @JvmStatic
        protected val agInspektør = { orgnummer: String -> { person: Person -> TestArbeidsgiverInspektør(person, orgnummer) } }

        @JvmStatic
        protected infix fun String.og(annen: String) = listOf(this, annen)

        @JvmStatic
        protected infix fun List<String>.og(annen: String) = this.plus(annen)
    }

    private val assertetVarsler = AssertetVarsler()

    protected lateinit var jurist: SubsumsjonsListLog
    protected lateinit var observatør: TestObservatør
    internal lateinit var testperson: TestPerson
    private lateinit var deferredLog: DeferredLog
    protected fun Int.vedtaksperiode(orgnummer: String) = orgnummer { vedtaksperiode }
    protected val Int.vedtaksperiode get() = vedtaksperiode(bareÈnArbeidsgiver(a1))

    protected val String.inspektør get() = inspektør(this)
    protected val inspektør: TestArbeidsgiverInspektør get() = bareÈnArbeidsgiver(a1).inspektør

    private val TestPerson.TestArbeidsgiver.testArbeidsgiverAsserter
        get() = TestArbeidsgiverAssertions(
            observatør = observatør,
            inspektør = inspektør,
            personInspektør = testperson.inspiser(personInspektør),
            aktivitetsloggAsserts = AktivitetsloggAsserts(testperson.personlogg, assertetVarsler)
        )
    private val testPersonAsserter get() = TestPersonAssertions(testperson.inspiser(personInspektør), jurist)
    protected fun personView() = testperson.view()
    protected fun <INSPEKTØR> inspiser(inspektør: (Person) -> INSPEKTØR) = testperson.inspiser(inspektør)
    internal fun inspektør(orgnummer: String) = inspiser(agInspektør(orgnummer))
    protected fun inspektør(vedtaksperiodeId: UUID) = inspiser(personInspektør).vedtaksperiode(vedtaksperiodeId).inspektør
    protected fun inspektørForkastet(vedtaksperiodeId: UUID) = inspiser(personInspektør).forkastetVedtaksperiode(vedtaksperiodeId).inspektør
    protected operator fun <R> String.invoke(testblokk: TestPerson.TestArbeidsgiver.() -> R) =
        testperson.arbeidsgiver(this, testblokk)

    protected operator fun <R> List<String>.invoke(testblokk: TestPerson.TestArbeidsgiver.() -> R) =
        forEach { organisasjonsnummer -> organisasjonsnummer { testblokk() } }

    protected fun List<String>.forlengVedtak(periode: Periode, grad: Prosentdel = 100.prosent) {
        forEach {
            it {
                håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive))
                håndterSøknad(Sykdom(periode.start, periode.endInclusive, grad))
            }
        }
        (first()){
            håndterYtelser(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterSimulering(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterUtbetalt()
        }
        drop(1).forEach {
            it {
                håndterYtelser(observatør.sisteVedtaksperiodeId(orgnummer))
                håndterSimulering(observatør.sisteVedtaksperiodeId(orgnummer))
                håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiodeId(orgnummer))
                håndterUtbetalt()
            }
        }
    }

    protected fun List<String>.nyeVedtak(
        periode: Periode, grad: Prosentdel = 100.prosent, inntekt: Inntekt = 20000.månedlig,
        ghosts: List<String> = emptyList()
    ) {
        forEach {
            it {
                håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive))
                håndterSøknad(Sykdom(periode.start, periode.endInclusive, grad))
            }
        }
        forEach {
            it {
                håndterInntektsmelding(listOf(periode.start til periode.start.plusDays(15)), beregnetInntekt = inntekt)
            }
        }
        (first()){
            val arbeidsgivere = this@nyeVedtak + ghosts
            håndterVilkårsgrunnlagFlereArbeidsgivere(observatør.sisteVedtaksperiodeId(orgnummer), *arbeidsgivere.toTypedArray())
            håndterYtelser(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterSimulering(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterUtbetalt()
        }
        drop(1).forEach {
            it {
                håndterYtelser(observatør.sisteVedtaksperiodeId(orgnummer))
                håndterSimulering(observatør.sisteVedtaksperiodeId(orgnummer))
                håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiodeId(orgnummer))
                håndterUtbetalt()
            }
        }
    }

    protected fun <R> assertSubsumsjoner(block: SubsumsjonInspektør.() -> R): R {
        return testPersonAsserter.assertSubsumsjoner(block)
    }

    protected fun TestPerson.TestArbeidsgiver.assertTilstander(id: UUID, vararg tilstander: TilstandType) {
        testArbeidsgiverAsserter.assertTilstander(id, *tilstander)
    }

    protected fun TestPerson.TestArbeidsgiver.assertTilstand(id: UUID, tilstand: TilstandType) {
        assertSisteTilstand(id, tilstand)
    }

    protected fun TestPerson.TestArbeidsgiver.assertSisteTilstand(id: UUID, tilstand: TilstandType, errortekst: (() -> String)? = null) {
        testArbeidsgiverAsserter.assertSisteTilstand(id, tilstand, errortekst)
    }

    protected fun TestPerson.TestArbeidsgiver.assertUtbetalingsbeløp(
        vedtaksperiodeId: UUID,
        forventetArbeidsgiverbeløp: Int,
        forventetArbeidsgiverRefusjonsbeløp: Int,
        forventetPersonbeløp: Int = 0,
        subset: Periode? = null
    ) {
        testArbeidsgiverAsserter.assertUtbetalingsbeløp(vedtaksperiodeId, forventetArbeidsgiverbeløp, forventetArbeidsgiverRefusjonsbeløp, forventetPersonbeløp, subset)
    }

    protected fun TestPerson.TestArbeidsgiver.assertSisteForkastetTilstand(id: UUID, tilstand: TilstandType) {
        testArbeidsgiverAsserter.assertSisteForkastetTilstand(id, tilstand)
    }

    protected fun TestPerson.TestArbeidsgiver.assertForkastetPeriodeTilstander(id: UUID, vararg tilstand: TilstandType, varselkode: Varselkode? = null) {
        testArbeidsgiverAsserter.assertForkastetPeriodeTilstander(id, *tilstand, varselkode = varselkode)
    }

    protected fun TestPerson.TestArbeidsgiver.assertAntallOpptjeningsdager(forventet: Int, skjæringstidspunkt: LocalDate = 1.januar) {
        testArbeidsgiverAsserter.assertAntallOpptjeningsdager(forventet, skjæringstidspunkt)
    }

    protected fun TestPerson.TestArbeidsgiver.assertVilkårsgrunnlagFraSpleisFor(vararg skjæringstidspunkt: LocalDate) {
        testArbeidsgiverAsserter.assertSkjæringstidspunkt(*skjæringstidspunkt)
    }

    protected fun TestPerson.TestArbeidsgiver.assertIngenVilkårsgrunnlagFraSpleis() {
        assertVilkårsgrunnlagFraSpleisFor()
    }

    protected fun TestPerson.TestArbeidsgiver.assertErOppfylt(skjæringstidspunkt: LocalDate = 1.januar) {
        testArbeidsgiverAsserter.assertErOppfylt(skjæringstidspunkt)
    }

    protected fun TestPerson.TestArbeidsgiver.assertErIkkeOppfylt(skjæringstidspunkt: LocalDate = 1.januar) {
        testArbeidsgiverAsserter.assertErIkkeOppfylt(skjæringstidspunkt)
    }

    protected fun assertHarIkkeArbeidsforhold(skjæringstidspunkt: LocalDate, orgnummer: String) {
        testPersonAsserter.assertHarIkkeArbeidsforhold(skjæringstidspunkt, orgnummer)
    }

    protected fun assertHarArbeidsforhold(skjæringstidspunkt: LocalDate, orgnummer: String) {
        testPersonAsserter.assertHarArbeidsforhold(skjæringstidspunkt, orgnummer)
    }

    protected fun TestPerson.TestArbeidsgiver.assertHarHendelseIder(vedtaksperiodeId: UUID, vararg hendelseIder: UUID) =
        testArbeidsgiverAsserter.assertHarHendelseIder(vedtaksperiodeId, *hendelseIder)

    protected fun TestPerson.TestArbeidsgiver.assertHarIkkeHendelseIder(vedtaksperiodeId: UUID, vararg hendelseIder: UUID) =
        testArbeidsgiverAsserter.assertHarIkkeHendelseIder(vedtaksperiodeId, *hendelseIder)

    protected fun TestPerson.TestArbeidsgiver.assertIngenFunksjonelleFeil(filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle) =
        testArbeidsgiverAsserter.assertIngenFunksjonelleFeil(filter)

    protected fun TestPerson.TestArbeidsgiver.assertFunksjonelleFeil(filter: AktivitetsloggFilter = AktivitetsloggFilter.person()) =
        testArbeidsgiverAsserter.assertFunksjonelleFeil(filter)

    protected fun TestPerson.TestArbeidsgiver.assertFunksjonellFeil(funksjonellFeil: String, filter: AktivitetsloggFilter) =
        testArbeidsgiverAsserter.assertFunksjonellFeil(funksjonellFeil, filter)

    protected fun TestPerson.TestArbeidsgiver.assertFunksjonellFeil(funksjonellFeil: Varselkode, filter: AktivitetsloggFilter) =
        testArbeidsgiverAsserter.assertFunksjonellFeil(funksjonellFeil.varseltekst, filter)

    protected fun TestPerson.TestArbeidsgiver.ingenNyeFunksjonelleFeil(block: () -> Unit) =
        testArbeidsgiverAsserter.ingenNyeFunksjonelleFeil(block)

    protected fun TestPerson.TestArbeidsgiver.nyeFunksjonelleFeil(block: () -> Unit) =
        testArbeidsgiverAsserter.nyeFunksjonelleFeil(block)

    protected fun TestPerson.TestArbeidsgiver.assertVarsler(varsler: Collection<Varselkode>, filter: AktivitetsloggFilter) =
        testArbeidsgiverAsserter.assertVarsler(varsler, filter)

    protected fun TestPerson.TestArbeidsgiver.assertVarsler(vedtaksperiodeId: UUID, vararg varsler: Varselkode) =
        testArbeidsgiverAsserter.assertVarsler(varsler.toSet(), vedtaksperiodeId.filter())

    protected fun TestPerson.TestArbeidsgiver.assertVarsel(warning: String, filter: AktivitetsloggFilter) =
        testArbeidsgiverAsserter.assertVarsel(warning, filter)

    protected fun TestPerson.TestArbeidsgiver.assertVarsel(kode: Varselkode, filter: AktivitetsloggFilter) =
        testArbeidsgiverAsserter.assertVarsel(kode, filter)

    protected fun TestPerson.TestArbeidsgiver.assertInfo(forventet: String, filter: AktivitetsloggFilter) =
        testArbeidsgiverAsserter.assertInfo(forventet, filter)

    protected fun TestPerson.TestArbeidsgiver.assertInfo(forventet: String) =
        testArbeidsgiverAsserter.assertInfo(forventet, AktivitetsloggFilter.Alle)

    protected fun TestPerson.TestArbeidsgiver.assertIngenInfo(forventet: String, filter: AktivitetsloggFilter) =
        testArbeidsgiverAsserter.assertIngenInfo(forventet, filter)

    protected fun TestPerson.TestArbeidsgiver.assertIngenInfoSomInneholder(forventet: String, filter: AktivitetsloggFilter) =
        testArbeidsgiverAsserter.assertIngenInfoSomInneholder(forventet, filter)

    protected fun TestPerson.TestArbeidsgiver.assertIngenBehov(vedtaksperiodeId: UUID, behovtype: Aktivitet.Behov.Behovtype) =
        testArbeidsgiverAsserter.assertIngenBehov(vedtaksperiodeId, behovtype)

    protected fun TestPerson.TestArbeidsgiver.assertBehov(vedtaksperiodeId: UUID, behovtype: Aktivitet.Behov.Behovtype) =
        testArbeidsgiverAsserter.assertBehov(vedtaksperiodeId, behovtype)

    protected fun nyPeriode(periode: Periode, vararg orgnummer: String, grad: Prosentdel = 100.prosent) {
        testperson.nyPeriode(periode, *orgnummer, grad = grad)
    }

    /* alternative metoder fremfor å lage en arbeidsgiver-blokk hver gang */
    protected fun String.håndterSykmelding(vararg sykmeldingsperiode: Sykmeldingsperiode, sykmeldingSkrevet: LocalDateTime? = null, mottatt: LocalDateTime? = null) =
        this { håndterSykmelding(*sykmeldingsperiode, sykmeldingSkrevet = sykmeldingSkrevet, mottatt = mottatt) }

    protected fun String.håndterSøknad(
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: Boolean = false,
        arbeidUtenforNorge: Boolean = false,
        sendtTilNAVEllerArbeidsgiver: LocalDate? = null,
        sykmeldingSkrevet: LocalDateTime? = null,
        sendTilGosys: Boolean = false,
        inntekterFraNyeArbeidsforhold: Boolean = false
    ) =
        this { håndterSøknad(*perioder, andreInntektskilder = andreInntektskilder, arbeidUtenforNorge = arbeidUtenforNorge, sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver, sykmeldingSkrevet = sykmeldingSkrevet, sendTilGosys = sendTilGosys, inntekterFraNyeArbeidsforhold = inntekterFraNyeArbeidsforhold) }

    protected fun String.håndterInntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        beregnetInntekt: Inntekt,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOf { it.start },
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        opphørAvNaturalytelser: List<Inntektsmelding.OpphørAvNaturalytelse> = emptyList(),
        begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
        id: UUID = UUID.randomUUID(),
        mottatt: LocalDateTime = LocalDateTime.now()
    ) =
        this {
            håndterInntektsmelding(
                arbeidsgiverperioder,
                beregnetInntekt,
                førsteFraværsdag,
                refusjon,
                opphørAvNaturalytelser,
                begrunnelseForReduksjonEllerIkkeUtbetalt,
                id,
                mottatt = mottatt
            )
        }

    protected fun String.håndterInntektsmeldingPortal(
        arbeidsgiverperioder: List<Periode>,
        beregnetInntekt: Inntekt,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOf { it.start },
        vedtaksperiodeId: UUID,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        opphørAvNaturalytelser: List<Inntektsmelding.OpphørAvNaturalytelse> = emptyList(),
        begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
        id: UUID = UUID.randomUUID()
    ) =
        this {
            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder,
                beregnetInntekt,
                vedtaksperiodeId,
                refusjon,
                opphørAvNaturalytelser,
                begrunnelseForReduksjonEllerIkkeUtbetalt,
                id
            )
        }

    protected fun String.håndterVilkårsgrunnlag(vedtaksperiodeId: UUID) =
        this { håndterVilkårsgrunnlag(vedtaksperiodeId) }

    protected fun String.håndterVilkårsgrunnlag(vedtaksperiodeId: UUID, medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus) =
        this { håndterVilkårsgrunnlag(vedtaksperiodeId, medlemskapstatus) }

    protected fun String.håndterYtelser(
        vedtaksperiodeId: UUID,
        foreldrepenger: List<GradertPeriode> = emptyList(),
        svangerskapspenger: List<GradertPeriode> = emptyList(),
        pleiepenger: List<GradertPeriode> = emptyList(),
        omsorgspenger: List<GradertPeriode> = emptyList(),
        opplæringspenger: List<GradertPeriode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList(),
    ) =
        this { håndterYtelser(vedtaksperiodeId, foreldrepenger, svangerskapspenger, pleiepenger, omsorgspenger, opplæringspenger, institusjonsoppholdsperioder, arbeidsavklaringspenger, dagpenger) }

    protected fun String.håndterSimulering(vedtaksperiodeId: UUID) =
        this { håndterSimulering(vedtaksperiodeId) }

    protected fun String.håndterUtbetalingsgodkjenning(vedtaksperiodeId: UUID, godkjent: Boolean = true) =
        this { håndterUtbetalingsgodkjenning(vedtaksperiodeId, godkjent) }

    protected fun String.håndterUtbetalingshistorikkEtterInfotrygdendring(vararg utbetalinger: Infotrygdperiode) =
        this { håndterUtbetalingshistorikkEtterInfotrygdendring(*utbetalinger) }

    protected fun String.håndterUtbetalt(status: Oppdragstatus) =
        this { håndterUtbetalt(status) }

    protected fun String.håndterAnnullering(utbetalingId: UUID) =
        this { håndterAnnullering(utbetalingId) }

    protected fun String.håndterIdentOpphørt(nyttFnr: Personidentifikator) =
        this { håndterIdentOpphørt(nyttFnr) }

    protected fun String.håndterPåminnelse(vedtaksperiodeId: UUID, tilstand: TilstandType, tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now()) =
        this { håndterPåminnelse(vedtaksperiodeId, tilstand, tilstandsendringstidspunkt) }

    protected fun String.håndterGrunnbeløpsregulering(skjæringstidspunkt: LocalDate) =
        this { håndterGrunnbeløpsregulering(skjæringstidspunkt) }

    protected fun nullstillTilstandsendringer() = observatør.nullstillTilstandsendringer()

    protected fun String.assertTilstander(id: UUID, vararg tilstander: TilstandType) =
        this { assertTilstander(id, *tilstander) }

    protected fun String.assertSisteTilstand(id: UUID, tilstand: TilstandType) =
        this { assertSisteTilstand(id, tilstand) }

    protected fun String.assertIngenFunksjonelleFeil(filter: AktivitetsloggFilter) =
        this { assertIngenFunksjonelleFeil(filter) }

    protected fun String.assertVarsler(varsler: Collection<Varselkode>, filter: AktivitetsloggFilter) =
        this { assertVarsler(varsler, filter) }

    protected fun String.assertVarsel(warning: String, filter: AktivitetsloggFilter) =
        this { assertVarsel(warning, filter) }

    protected fun String.assertVarsel(kode: Varselkode, filter: AktivitetsloggFilter) =
        this { assertVarsel(kode, filter) }

    protected fun String.assertFunksjonellFeil(kode: Varselkode, filter: AktivitetsloggFilter) =
        this { assertFunksjonellFeil(kode, filter) }

    protected fun String.nyttVedtak(
        periode: Periode,
        grad: Prosentdel = 100.prosent,
        førsteFraværsdag: LocalDate = periode.start,
        beregnetInntekt: Inntekt = INNTEKT,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        arbeidsgiverperiode: List<Periode> = emptyList(),
        status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
        ghosts: List<String>
    ) =
        this { nyttVedtak(periode, grad, førsteFraværsdag, beregnetInntekt, refusjon, arbeidsgiverperiode, status, ghosts) }

    protected fun String.tilGodkjenning(
        periode: Periode,
        grad: Prosentdel = 100.prosent,
        førsteFraværsdag: LocalDate = periode.start,
        beregnetInntekt: Inntekt = INNTEKT,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        arbeidsgiverperiode: List<Periode> = emptyList(),
        status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
        sykepengegrunnlagSkatt: InntektForSykepengegrunnlag = lagStandardSykepengegrunnlag(this, beregnetInntekt, førsteFraværsdag)
    ) =
        this { tilGodkjenning(periode, grad, førsteFraværsdag, beregnetInntekt, refusjon, arbeidsgiverperiode, status) }

    /* dsl for å gå direkte på arbeidsgiver1, eksempelvis i tester for det ikke er andre arbeidsgivere */
    private fun bareÈnArbeidsgiver(orgnr: String): String {
        check(testperson.view().arbeidsgivere.size < 2) {
            "Kan ikke bruke forenklet API for én arbeidsgivere når det finnes flere! Det er ikke trygt og kommer til å lage feil!"
        }
        return orgnr
    }

    protected fun håndterSykmelding(periode: Periode) = håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive))
    protected fun håndterSykmelding(
        vararg sykmeldingsperiode: Sykmeldingsperiode,
        sykmeldingSkrevet: LocalDateTime? = null,
        mottatt: LocalDateTime? = null,
        orgnummer: String = a1
    ) =
        bareÈnArbeidsgiver(orgnummer).håndterSykmelding(*sykmeldingsperiode, sykmeldingSkrevet = sykmeldingSkrevet, mottatt = mottatt)

    protected fun håndterSøknad(periode: Periode) = håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent))
    protected fun håndterSøknad(
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: Boolean = false,
        arbeidUtenforNorge: Boolean = false,
        sendtTilNAVEllerArbeidsgiver: LocalDate? = null,
        sykmeldingSkrevet: LocalDateTime? = null,
        orgnummer: String = a1,
        sendTilGosys: Boolean = false,
        inntekterFraNyeArbeidsforhold: Boolean = false
    ) =
        bareÈnArbeidsgiver(orgnummer).håndterSøknad(*perioder, andreInntektskilder = andreInntektskilder, arbeidUtenforNorge = arbeidUtenforNorge, sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver, sykmeldingSkrevet = sykmeldingSkrevet, sendTilGosys = sendTilGosys, inntekterFraNyeArbeidsforhold = inntekterFraNyeArbeidsforhold)

    protected fun håndterInntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        beregnetInntekt: Inntekt,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOf { it.start },
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        opphørAvNaturalytelser: List<Inntektsmelding.OpphørAvNaturalytelse> = emptyList(),
        begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
        id: UUID = UUID.randomUUID(),
        orgnummer: String = a1,
        mottatt: LocalDateTime = LocalDateTime.now()
    ) =
        bareÈnArbeidsgiver(orgnummer).håndterInntektsmelding(
            arbeidsgiverperioder,
            beregnetInntekt,
            førsteFraværsdag,
            refusjon,
            opphørAvNaturalytelser,
            begrunnelseForReduksjonEllerIkkeUtbetalt,
            id,
            mottatt = mottatt
        )

    internal fun håndterVilkårsgrunnlag(vedtaksperiodeId: UUID, orgnummer: String = a1) =
        bareÈnArbeidsgiver(orgnummer).håndterVilkårsgrunnlag(vedtaksperiodeId)

    internal fun håndterVilkårsgrunnlag(vedtaksperiodeId: UUID, medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus, orgnummer: String = a1) =
        bareÈnArbeidsgiver(orgnummer).håndterVilkårsgrunnlag(vedtaksperiodeId, medlemskapstatus)

    internal fun håndterYtelser(
        vedtaksperiodeId: UUID,
        foreldrepenger: List<GradertPeriode> = emptyList(),
        svangerskapspenger: List<GradertPeriode> = emptyList(),
        pleiepenger: List<GradertPeriode> = emptyList(),
        omsorgspenger: List<GradertPeriode> = emptyList(),
        opplæringspenger: List<GradertPeriode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList(),
        orgnummer: String = a1
    ) =
        bareÈnArbeidsgiver(orgnummer).håndterYtelser(vedtaksperiodeId, foreldrepenger, svangerskapspenger, pleiepenger, omsorgspenger, opplæringspenger, institusjonsoppholdsperioder, arbeidsavklaringspenger, dagpenger)

    internal fun håndterSimulering(vedtaksperiodeId: UUID, orgnummer: String = a1) =
        bareÈnArbeidsgiver(orgnummer).håndterSimulering(vedtaksperiodeId)

    internal fun håndterUtbetalingsgodkjenning(vedtaksperiodeId: UUID, godkjent: Boolean = true, orgnummer: String = a1) =
        bareÈnArbeidsgiver(orgnummer).håndterUtbetalingsgodkjenning(vedtaksperiodeId, godkjent)

    internal fun håndterUtbetalt(status: Oppdragstatus = Oppdragstatus.AKSEPTERT, orgnummer: String = a1) =
        bareÈnArbeidsgiver(orgnummer).håndterUtbetalt(status)

    protected fun håndterAnnullering(utbetalingId: UUID, orgnummer: String = a1) =
        bareÈnArbeidsgiver(orgnummer).håndterAnnullering(utbetalingId)

    protected fun håndterIdentOpphørt(nyttFnr: Personidentifikator, nyAktørId: String) =
        bareÈnArbeidsgiver(a1).håndterIdentOpphørt(nyttFnr)

    protected fun håndterPåminnelse(vedtaksperiodeId: UUID, tilstand: TilstandType, tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now()) =
        bareÈnArbeidsgiver(a1).håndterPåminnelse(vedtaksperiodeId, tilstand, tilstandsendringstidspunkt)

    protected fun håndterGrunnbeløpsregulering(skjæringstidspunkt: LocalDate) =
        bareÈnArbeidsgiver(a1).håndterGrunnbeløpsregulering(skjæringstidspunkt)

    protected fun håndterOverstyrArbeidsforhold(skjæringstidspunkt: LocalDate, vararg overstyrteArbeidsforhold: OverstyrArbeidsforhold.ArbeidsforholdOverstyrt) =
        testperson { håndterOverstyrArbeidsforhold(skjæringstidspunkt, *overstyrteArbeidsforhold) }

    protected fun håndterSkjønnsmessigFastsettelse(skjæringstidspunkt: LocalDate, arbeidsgiveropplysninger: List<OverstyrtArbeidsgiveropplysning>, meldingsreferanseId: UUID = UUID.randomUUID(), tidsstempel: LocalDateTime = LocalDateTime.now()) =
        testperson { håndterSkjønnsmessigFastsettelse(skjæringstidspunkt, arbeidsgiveropplysninger, meldingsreferanseId, tidsstempel) }

    protected fun håndterOverstyrArbeidsgiveropplysninger(skjæringstidspunkt: LocalDate, arbeidsgiveropplysninger: List<OverstyrtArbeidsgiveropplysning>, meldingsreferanseId: UUID = UUID.randomUUID()) =
        testperson { håndterOverstyrArbeidsgiveropplysninger(skjæringstidspunkt, arbeidsgiveropplysninger, meldingsreferanseId) }

    protected fun assertTilstander(id: UUID, vararg tilstander: TilstandType) =
        bareÈnArbeidsgiver(a1).assertTilstander(id, *tilstander)

    protected fun assertSisteTilstand(id: UUID, tilstand: TilstandType, orgnummer: String = a1) =
        bareÈnArbeidsgiver(orgnummer).assertSisteTilstand(id, tilstand)

    protected fun assertIngenFunksjonelleFeil(filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle) =
        bareÈnArbeidsgiver(a1).assertIngenFunksjonelleFeil(filter)

    protected fun assertVarsler(varsler: Collection<Varselkode>, filter: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertVarsler(varsler, filter)

    protected fun assertVarsel(warning: String, filter: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertVarsel(warning, filter)

    protected fun assertVarsel(kode: Varselkode, filter: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertVarsel(kode, filter)

    protected fun assertFunksjonellFeil(kode: Varselkode, filter: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertFunksjonellFeil(kode, filter)

    protected fun assertActivities() {
        assertTrue(testperson.personlogg.aktiviteter.isNotEmpty()) { testperson.personlogg.toString() }
    }

    protected fun assertGjenoppbygget(dto: PersonUtDto) {
        val serialisertPerson = dto.tilPersonData().tilSerialisertPerson()
        val gjenoppbyggetPersonViaPersonData = Person.gjenopprett(EmptyLog, serialisertPerson.tilPersonDto())
        val gjenoppbyggetPersonViaPersonDto = Person.gjenopprett(EmptyLog, dto.tilPersonData().tilPersonDto())

        val dtoFraPersonViaPersonData = gjenoppbyggetPersonViaPersonData.dto()
        val dtoFraPersonViaPersonDto = gjenoppbyggetPersonViaPersonDto.dto()

        assertEquals(dto, dtoFraPersonViaPersonData)
        assertEquals(dto, dtoFraPersonViaPersonDto)
        assertPersonEquals(testperson.person, gjenoppbyggetPersonViaPersonData)
        assertPersonEquals(testperson.person, gjenoppbyggetPersonViaPersonDto)
    }

    protected fun håndterDødsmelding(dødsdato: LocalDate) {
        testperson.håndterDødsmelding(dødsdato)
    }

    protected fun nyttVedtak(
        vedtaksperiode: Periode,
        grad: Prosentdel = 100.prosent,
        førsteFraværsdag: LocalDate = vedtaksperiode.start,
        beregnetInntekt: Inntekt = INNTEKT,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        arbeidsgiverperiode: List<Periode> = emptyList(),
        status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
        ghosts: List<String> = emptyList()
    ) =
        bareÈnArbeidsgiver(a1).nyttVedtak(vedtaksperiode, grad, førsteFraværsdag, beregnetInntekt, refusjon, arbeidsgiverperiode, status, ghosts)

    protected fun dto() = testperson.dto()

    private fun regler(maksSykedager: Int): ArbeidsgiverRegler = object : ArbeidsgiverRegler {
        override fun burdeStarteNyArbeidsgiverperiode(oppholdsdagerBrukt: Int) = oppholdsdagerBrukt >= 16
        override fun arbeidsgiverperiodenGjennomført(arbeidsgiverperiodedagerBrukt: Int) = arbeidsgiverperiodedagerBrukt >= 16
        override fun maksSykepengedager() = maksSykedager
        override fun maksSykepengedagerOver67() = maksSykedager
    }

    protected fun medJSONPerson(filsti: String) {
        testperson = TestPerson(
            observatør = observatør,
            person = gjenopprettFraJSON(filsti, jurist),
            deferredLog = deferredLog
        )
    }

    protected fun medFødselsdato(fødselsdato: LocalDate) {
        testperson = TestPerson(observatør = observatør, fødselsdato = fødselsdato, deferredLog = deferredLog, jurist = jurist)
    }

    protected fun medPersonidentifikator(personidentifikator: Personidentifikator) {
        testperson = TestPerson(observatør = observatør, personidentifikator = personidentifikator, deferredLog = deferredLog, jurist = jurist)
    }

    protected fun medMaksSykedager(maksSykedager: Int) {
        testperson = TestPerson(observatør = observatør, deferredLog = deferredLog, jurist = jurist, regler = regler(maksSykedager))
    }

    @BeforeEach
    fun setup() {
        jurist = SubsumsjonsListLog()
        observatør = TestObservatør()
        deferredLog = DeferredLog()
        testperson = TestPerson(observatør = observatør, deferredLog = deferredLog, jurist = jurist)
    }

    @AfterEach
    fun verify() {
        testperson.bekreftBehovOppfylt(assertetVarsler)
    }

    fun dumpLog() = deferredLog.dumpLog()
}
