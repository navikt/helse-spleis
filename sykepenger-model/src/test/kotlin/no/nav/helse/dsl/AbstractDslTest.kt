package no.nav.helse.dsl

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.Kilde
import no.nav.helse.inspectors.PersonInspektør
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.TilstandType
import no.nav.helse.person.Varselkode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.spleis.e2e.lagInntektperioder
import no.nav.helse.testhelpers.Inntektperioder
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(DeferredLogging::class)
internal abstract class AbstractDslTest {
    internal companion object {
        @JvmStatic
        protected val a1 = "a1"
        @JvmStatic
        protected val a2 = "a2"
        @JvmStatic
        protected val a3 = "a3"
        @JvmStatic
        @Deprecated("må bruke a1")
        protected val ORGNUMMER = a1
        @JvmStatic
        protected val personInspektør = { person: Person -> PersonInspektør(person) }
        @JvmStatic
        protected val agInspektør = { orgnummer: String -> { person: Person -> TestArbeidsgiverInspektør(person, orgnummer) } }
        @JvmStatic
        protected infix fun String.og(annen: String) = listOf(this, annen)
        @JvmStatic
        protected infix fun List<String>.og(annen: String) = this.plus(annen)
    }
    protected lateinit var observatør: TestObservatør
    private lateinit var testperson: TestPerson
    private lateinit var deferredLog: DeferredLog

    protected fun Int.vedtaksperiode(orgnummer: String) = orgnummer { vedtaksperiode }
    protected val Int.vedtaksperiode get() = vedtaksperiode(bareÈnArbeidsgiver(a1))

    protected val String.inspektør get() = inspektør(this)
    protected val inspektør: TestArbeidsgiverInspektør get() = bareÈnArbeidsgiver(a1).inspektør

    private val TestPerson.TestArbeidsgiver.testArbeidsgiverAsserter get() = TestArbeidsgiverAssertions(observatør, inspektør, testperson.inspiser(personInspektør))
    private val testPersonAsserter get() = TestPersonAssertions(testperson.inspiser(personInspektør))

    protected fun forkastAlle() = testperson.forkastAlle()

    protected fun <INSPEKTØR : PersonVisitor> inspiser(inspektør: (Person) -> INSPEKTØR) = testperson.inspiser(inspektør)
    protected fun inspektør(orgnummer: String) = inspiser(agInspektør(orgnummer))

    protected operator fun <R> String.invoke(testblokk: TestPerson.TestArbeidsgiver.() -> R) =
        testperson.arbeidsgiver(this, testblokk)

    protected operator fun <R> List<String>.invoke(testblokk: TestPerson.TestArbeidsgiver.() -> R) =
        forEach { organisasjonsnummer -> organisasjonsnummer { testblokk() } }

    protected fun List<String>.forlengVedtak(periode: Periode, grad: Prosentdel = 100.prosent) {
        forEach {
            it {
                håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, grad))
                håndterSøknad(Sykdom(periode.start, periode.endInclusive, grad))
            }
        }
        (first()){
            håndterYtelser(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterSimulering(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterUtbetalt()
        }
        drop(1).forEach { it {
            håndterYtelser(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterSimulering(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterUtbetalt()
        }}
    }
    protected fun List<String>.nyeVedtak(
        periode: Periode, grad: Prosentdel = 100.prosent, inntekt: Inntekt = 20000.månedlig,
        inntekterBlock: Inntektperioder.() -> Unit = {
            this@nyeVedtak.forEach {
                lagInntektperioder(it, periode.start, inntekt)
            }
        }
    ) {
        forEach {
            it {
                håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, grad))
                håndterSøknad(Sykdom(periode.start, periode.endInclusive, grad))
            }
        }
        forEach { it {
            håndterInntektsmelding(listOf(periode.start til periode.start.plusDays(15)), beregnetInntekt = inntekt)
        }}
        (first()){
            håndterYtelser(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterVilkårsgrunnlag(observatør.sisteVedtaksperiodeId(orgnummer), inntektsvurdering = Inntektsvurdering(
                inntektperioderForSammenligningsgrunnlag {
                    inntekterBlock()
                })
            )
            håndterYtelser(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterSimulering(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterUtbetalt()
        }
        drop(1).forEach { it {
            håndterYtelser(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterSimulering(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterUtbetalt()
        }}
    }

    protected fun TestPerson.TestArbeidsgiver.assertTilstander(id: UUID, vararg tilstander: TilstandType, orgnummer: String = a1) {
        testArbeidsgiverAsserter.assertTilstander(id, *tilstander)
    }
    protected fun TestPerson.TestArbeidsgiver.assertTilstand(id: UUID, tilstand: TilstandType, orgnummer: String = a1) {
        assertSisteTilstand(id, tilstand)
    }
    protected fun TestPerson.TestArbeidsgiver.assertSisteTilstand(id: UUID, tilstand: TilstandType, orgnummer: String = a1) {
        testArbeidsgiverAsserter.assertSisteTilstand(id, tilstand)
    }
    protected fun TestPerson.TestArbeidsgiver.assertForkastetPeriodeTilstander(id: UUID, vararg tilstand: TilstandType, orgnummer: String = a1) {
        testArbeidsgiverAsserter.assertForkastetPeriodeTilstander(id, *tilstand)
    }
    protected fun assertArbeidsgivereISykepengegrunnlag(skjæringstidspunkt: LocalDate, vararg arbeidsgivere: String) =
        testPersonAsserter.assertArbeidsgivereISykepengegrunnlag(skjæringstidspunkt, *arbeidsgivere)
    protected fun TestPerson.TestArbeidsgiver.assertHarHendelseIder(vedtaksperiodeId: UUID, vararg hendelseIder: UUID) =
        testArbeidsgiverAsserter.assertHarHendelseIder(vedtaksperiodeId, *hendelseIder)
    protected fun TestPerson.TestArbeidsgiver.assertHarIkkeHendelseIder(vedtaksperiodeId: UUID, vararg hendelseIder: UUID) =
        testArbeidsgiverAsserter.assertHarIkkeHendelseIder(vedtaksperiodeId, *hendelseIder)
    protected fun TestPerson.TestArbeidsgiver.assertAntallInntektsopplysninger(antall: Int, inntektskilde: Kilde) =
        testArbeidsgiverAsserter.assertAntallInntektsopplysninger(antall, inntektskilde)
    protected fun TestPerson.TestArbeidsgiver.assertIngenFunksjonelleFeil(vararg filtre: AktivitetsloggFilter) =
        testArbeidsgiverAsserter.assertIngenFunksjonelleFeil(*filtre)
    protected fun TestPerson.TestArbeidsgiver.assertVarsler(vararg filtre: AktivitetsloggFilter) =
        testArbeidsgiverAsserter.assertVarsler(*filtre)
    protected fun TestPerson.TestArbeidsgiver.assertVarsel(warning: String, vararg filtre: AktivitetsloggFilter) =
        testArbeidsgiverAsserter.assertVarsel(warning, *filtre)
    protected fun TestPerson.TestArbeidsgiver.assertVarsel(kode: Varselkode, vararg filtre: AktivitetsloggFilter) =
        testArbeidsgiverAsserter.assertVarsel(kode, *filtre)
    protected fun TestPerson.TestArbeidsgiver.assertIngenVarsler(vararg filtre: AktivitetsloggFilter) =
        testArbeidsgiverAsserter.assertIngenVarsler(*filtre)
    protected fun TestPerson.TestArbeidsgiver.assertInfo(forventet: String, vararg filtre: AktivitetsloggFilter) =
        testArbeidsgiverAsserter.assertInfo(forventet, *filtre)
    protected fun TestPerson.TestArbeidsgiver.assertIngenInfo(forventet: String, vararg filtre: AktivitetsloggFilter) =
        testArbeidsgiverAsserter.assertIngenInfo(forventet, *filtre)

    protected fun nyPeriode(periode: Periode, vararg orgnummer: String, grad: Prosentdel = 100.prosent) {
        testperson.nyPeriode(periode, *orgnummer, grad = grad)
    }

    /* alternative metoder fremfor å lage en arbeidsgiver-blokk hver gang */
    protected fun String.håndterSykmelding(vararg sykmeldingsperiode: Sykmeldingsperiode, sykmeldingSkrevet: LocalDateTime? = null, mottatt: LocalDateTime? = null) =
        this { håndterSykmelding(*sykmeldingsperiode, sykmeldingSkrevet = sykmeldingSkrevet, mottatt = mottatt) }
    protected fun String.håndterSøknad(
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(),
        sendtTilNAVEllerArbeidsgiver: LocalDate? = null,
        sykmeldingSkrevet: LocalDateTime? = null,
    ) =
        this { håndterSøknad(*perioder, andreInntektskilder = andreInntektskilder, sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver, sykmeldingSkrevet = sykmeldingSkrevet) }
    protected fun String.håndterInntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        beregnetInntekt: Inntekt,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOf { it.start },
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        harOpphørAvNaturalytelser: Boolean = false,
        arbeidsforholdId: String? = null,
        begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
        id: UUID = UUID.randomUUID()
    ) =
        this { håndterInntektsmelding(arbeidsgiverperioder, beregnetInntekt, førsteFraværsdag, refusjon, harOpphørAvNaturalytelser, arbeidsforholdId, begrunnelseForReduksjonEllerIkkeUtbetalt, id) }
    protected fun String.håndterUtbetalingshistorikk(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Infotrygdperiode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        harStatslønn: Boolean = false,
        besvart: LocalDateTime = LocalDateTime.now()
    ) =
        this { håndterUtbetalingshistorikk(vedtaksperiodeId, utbetalinger, inntektshistorikk, harStatslønn, besvart) }
    protected fun String.håndterVilkårsgrunnlag(
        vedtaksperiodeId: UUID,
        inntekt: Inntekt = INNTEKT,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        inntektsvurdering: Inntektsvurdering? = null,
        inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag? = null,
        arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>? = null
    ) =
        this { håndterVilkårsgrunnlag(vedtaksperiodeId, inntekt, medlemskapstatus, inntektsvurdering, inntektsvurderingForSykepengegrunnlag, arbeidsforhold) }
    protected fun String.håndterYtelser(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Infotrygdperiode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null,
        pleiepenger: List<Periode> = emptyList(),
        omsorgspenger: List<Periode> = emptyList(),
        opplæringspenger: List<Periode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
        dødsdato: LocalDate? = null,
        statslønn: Boolean = false,
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList(),
        besvart: LocalDateTime = LocalDateTime.now(),
    ) =
        this { håndterYtelser(vedtaksperiodeId, utbetalinger, inntektshistorikk, foreldrepenger, svangerskapspenger, pleiepenger, omsorgspenger, opplæringspenger, institusjonsoppholdsperioder, dødsdato, statslønn, arbeidskategorikoder, arbeidsavklaringspenger, dagpenger, besvart) }
    protected fun String.håndterSimulering(vedtaksperiodeId: UUID) =
        this { håndterSimulering(vedtaksperiodeId) }
    protected fun String.håndterUtbetalingsgodkjenning(vedtaksperiodeId: UUID, godkjent: Boolean = true) =
        this { håndterUtbetalingsgodkjenning(vedtaksperiodeId, godkjent) }
    protected fun String.håndterUtbetalt(status: Oppdragstatus) =
        this { håndterUtbetalt(status) }
    protected fun String.håndterAnnullering(fagsystemId: String) =
        this { håndterAnnullering(fagsystemId) }
    protected fun String.håndterPåminnelse(vedtaksperiodeId: UUID, tilstand: TilstandType, tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now()) =
        this { håndterPåminnelse(vedtaksperiodeId, tilstand, tilstandsendringstidspunkt) }
    protected fun nullstillTilstandsendringer() = observatør.nullstillTilstandsendringer()
    protected fun String.assertTilstander(id: UUID, vararg tilstander: TilstandType) =
        this { assertTilstander(id, *tilstander) }
    protected fun String.assertSisteTilstand(id: UUID, tilstand: TilstandType) =
        this { assertSisteTilstand(id, tilstand) }
    protected fun String.assertIngenFunksjonelleFeil(vararg filtre: AktivitetsloggFilter) =
        this { assertIngenFunksjonelleFeil(*filtre) }
    protected fun String.assertVarsler(vararg filtre: AktivitetsloggFilter) =
        this { assertVarsler(*filtre) }
    protected fun String.assertVarsel(warning: String, vararg filtre: AktivitetsloggFilter) =
        this { assertVarsel(warning, *filtre) }
    protected fun String.assertVarsel(kode: Varselkode, vararg filtre: AktivitetsloggFilter) =
        this { assertVarsel(kode, *filtre) }
    protected fun String.assertIngenVarsler(vararg filtre: AktivitetsloggFilter) =
        this { assertIngenVarsler(*filtre) }
    protected fun String.nyttVedtak(
        fom: LocalDate,
        tom: LocalDate,
        grad: Prosentdel = 100.prosent,
        førsteFraværsdag: LocalDate = fom,
        beregnetInntekt: Inntekt = INNTEKT,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        arbeidsgiverperiode: List<Periode> = emptyList(),
        status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
        inntekterBlock: Inntektperioder.() -> Unit = { lagInntektperioder(this@nyttVedtak, fom, beregnetInntekt) }
    ) =
        this { nyttVedtak(fom, tom, grad, førsteFraværsdag, beregnetInntekt, refusjon, arbeidsgiverperiode, status, inntekterBlock) }


    /* dsl for å gå direkte på arbeidsgiver1, eksempelvis i tester for det ikke er andre arbeidsgivere */
    private fun bareÈnArbeidsgiver(orgnr: String): String {
        check(inspiser(personInspektør).arbeidsgiverteller < 2) {
            "Kan ikke bruke forenklet API for én arbeidsgivere når det finnes flere! Det er ikke trygt og kommer til å lage feil!"
        }
        return orgnr
    }
    protected fun håndterSykmelding(
        vararg sykmeldingsperiode: Sykmeldingsperiode,
        sykmeldingSkrevet: LocalDateTime? = null,
        mottatt: LocalDateTime? = null,
        orgnummer: String = a1
    ) =
        bareÈnArbeidsgiver(a1).håndterSykmelding(*sykmeldingsperiode, sykmeldingSkrevet = sykmeldingSkrevet, mottatt = mottatt)
    protected fun håndterSøknad(
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(),
        sendtTilNAVEllerArbeidsgiver: LocalDate? = null,
        sykmeldingSkrevet: LocalDateTime? = null,
        orgnummer: String = a1
    ) =
        bareÈnArbeidsgiver(a1).håndterSøknad(*perioder, andreInntektskilder = andreInntektskilder, sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver, sykmeldingSkrevet = sykmeldingSkrevet)
    protected fun håndterInntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        beregnetInntekt: Inntekt,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOf { it.start },
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        harOpphørAvNaturalytelser: Boolean = false,
        arbeidsforholdId: String? = null,
        begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
        id: UUID = UUID.randomUUID(),
        orgnummer: String = a1
    ) =
        bareÈnArbeidsgiver(a1).håndterInntektsmelding(arbeidsgiverperioder, beregnetInntekt, førsteFraværsdag, refusjon, harOpphørAvNaturalytelser, arbeidsforholdId, begrunnelseForReduksjonEllerIkkeUtbetalt, id)

    internal fun håndterUtbetalingshistorikk(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Infotrygdperiode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        harStatslønn: Boolean = false,
        besvart: LocalDateTime = LocalDateTime.now(),
        orgnummer: String = a1
    ) =
        bareÈnArbeidsgiver(a1).håndterUtbetalingshistorikk(vedtaksperiodeId, utbetalinger, inntektshistorikk, harStatslønn, besvart)
    internal fun håndterVilkårsgrunnlag(
        vedtaksperiodeId: UUID,
        inntekt: Inntekt = INNTEKT,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        inntektsvurdering: Inntektsvurdering? = null,
        inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag? = null,
        arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>? = null,
        orgnummer: String = a1
    ) =
        bareÈnArbeidsgiver(a1).håndterVilkårsgrunnlag(vedtaksperiodeId, inntekt, medlemskapstatus, inntektsvurdering, inntektsvurderingForSykepengegrunnlag, arbeidsforhold)
    internal fun håndterYtelser(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Infotrygdperiode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null,
        pleiepenger: List<Periode> = emptyList(),
        omsorgspenger: List<Periode> = emptyList(),
        opplæringspenger: List<Periode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
        dødsdato: LocalDate? = null,
        statslønn: Boolean = false,
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList(),
        besvart: LocalDateTime = LocalDateTime.now(),
        orgnummer: String = a1
    ) =
        bareÈnArbeidsgiver(a1).håndterYtelser(vedtaksperiodeId, utbetalinger, inntektshistorikk, foreldrepenger, svangerskapspenger, pleiepenger, omsorgspenger, opplæringspenger, institusjonsoppholdsperioder, dødsdato, statslønn, arbeidskategorikoder, arbeidsavklaringspenger, dagpenger, besvart)
    internal fun håndterSimulering(vedtaksperiodeId: UUID, orgnummer: String = a1) =
        bareÈnArbeidsgiver(a1).håndterSimulering(vedtaksperiodeId)
    internal fun håndterUtbetalingsgodkjenning(vedtaksperiodeId: UUID, godkjent: Boolean = true, orgnummer: String = a1) =
        bareÈnArbeidsgiver(a1).håndterUtbetalingsgodkjenning(vedtaksperiodeId, godkjent)
    internal fun håndterUtbetalt(status: Oppdragstatus = Oppdragstatus.AKSEPTERT, orgnummer: String = a1) =
        bareÈnArbeidsgiver(a1).håndterUtbetalt(status)
    protected fun håndterAnnullering(fagsystemId: String) =
        bareÈnArbeidsgiver(a1).håndterAnnullering(fagsystemId)
    protected fun håndterPåminnelse(vedtaksperiodeId: UUID, tilstand: TilstandType, tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now()) =
        bareÈnArbeidsgiver(a1).håndterPåminnelse(vedtaksperiodeId, tilstand, tilstandsendringstidspunkt)

    protected fun håndterOverstyrArbeidsforhold(skjæringstidspunkt: LocalDate, vararg overstyrteArbeidsforhold: OverstyrArbeidsforhold.ArbeidsforholdOverstyrt) =
        testperson { håndterOverstyrArbeidsforhold(skjæringstidspunkt, *overstyrteArbeidsforhold) }

    protected fun assertTilstander(id: UUID, vararg tilstander: TilstandType) =
        bareÈnArbeidsgiver(a1).assertTilstander(id, *tilstander)
    protected fun assertSisteTilstand(id: UUID, tilstand: TilstandType, orgnummer: String = a1) =
        bareÈnArbeidsgiver(a1).assertSisteTilstand(id, tilstand)
    protected fun assertIngenFunksjonelleFeil(vararg filtre: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertIngenFunksjonelleFeil(*filtre)
    protected fun assertVarsler(vararg filtre: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertVarsler(*filtre)
    protected fun assertVarsel(warning: String, vararg filtre: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertVarsel(warning, *filtre)
    protected fun assertVarsel(kode: Varselkode, vararg filtre: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertVarsel(kode, *filtre)
    protected fun assertIngenVarsler(vararg filtre: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertIngenVarsler(*filtre)
    protected fun assertActivities() {
        val inspektør = inspiser(personInspektør)
        assertTrue(inspektør.aktivitetslogg.harAktiviteter()) { inspektør.aktivitetslogg.toString() }
    }

    protected fun nyttVedtak(
        fom: LocalDate,
        tom: LocalDate,
        grad: Prosentdel = 100.prosent,
        førsteFraværsdag: LocalDate = fom,
        beregnetInntekt: Inntekt = INNTEKT,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        arbeidsgiverperiode: List<Periode> = emptyList(),
        status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
        inntekterBlock: Inntektperioder.() -> Unit = { lagInntektperioder(a1, fom, beregnetInntekt) }
    ) =
        bareÈnArbeidsgiver(a1).nyttVedtak(fom, tom, grad, førsteFraværsdag, beregnetInntekt, refusjon, arbeidsgiverperiode, status, inntekterBlock)


    @BeforeEach
    fun setup() {
        observatør = TestObservatør()
        deferredLog = DeferredLog()
        testperson = TestPerson(observatør = observatør, deferredLog = deferredLog)
    }

    @AfterEach
    fun verify() {
        testperson.bekreftBehovOppfylt()
        testperson.bekreftIngenOverlappende()
        testperson.validerInntektshistorikk()
        testperson.validerSykdomshistorikk()
    }

    fun dumpLog() = deferredLog.dumpLog()
}
