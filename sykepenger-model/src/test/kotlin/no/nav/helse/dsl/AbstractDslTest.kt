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
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.inspectors.PersonInspektør
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.TilstandType
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.spleis.e2e.lagInntektperioder
import no.nav.helse.testhelpers.Inntektperioder
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach

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
    }
    protected lateinit var observatør: TestObservatør
    private lateinit var testperson: TestPerson

    protected fun Int.vedtaksperiode(orgnummer: String) = orgnummer { vedtaksperiode }
    protected val Int.vedtaksperiode get() = vedtaksperiode(bareÈnArbeidsgiver(a1))

    protected val String.inspektør get() = inspektør(this)
    protected val inspektør: TestArbeidsgiverInspektør get() = bareÈnArbeidsgiver(a1).inspektør

    private val TestPerson.TestArbeidsgiver.asserter get() = TestAssertions(observatør, inspektør, testperson.inspiser(personInspektør))

    protected fun forkastAlle() = testperson.forkastAlle()

    protected fun <INSPEKTØR : PersonVisitor> inspiser(inspektør: (Person) -> INSPEKTØR) = testperson.inspiser(inspektør)
    protected fun inspektør(orgnummer: String) = inspiser(agInspektør(orgnummer))

    protected operator fun <R> String.invoke(testblokk: TestPerson.TestArbeidsgiver.() -> R) =
        testperson.arbeidsgiver(this, testblokk)

    protected fun TestPerson.TestArbeidsgiver.assertTilstander(id: UUID, vararg tilstander: TilstandType, orgnummer: String = a1) {
        asserter.assertTilstander(id, *tilstander)
    }
    protected fun TestPerson.TestArbeidsgiver.assertTilstand(id: UUID, tilstand: TilstandType, orgnummer: String = a1) {
        assertSisteTilstand(id, tilstand)
    }
    protected fun TestPerson.TestArbeidsgiver.assertSisteTilstand(id: UUID, tilstand: TilstandType, orgnummer: String = a1) {
        asserter.assertSisteTilstand(id, tilstand)
    }
    protected fun TestPerson.TestArbeidsgiver.assertForkastetPeriodeTilstander(id: UUID, vararg tilstand: TilstandType, orgnummer: String = a1) {
        asserter.assertForkastetPeriodeTilstander(id, *tilstand)
    }
    protected fun TestPerson.TestArbeidsgiver.assertArbeidsgivereIVilkårsgrunnlag(skjæringstidspunkt: LocalDate, vararg arbeidsgivere: String) {
        asserter.assertArbeidsgivereISykepengegrunnlag(skjæringstidspunkt, *arbeidsgivere)
    }
    protected fun TestPerson.TestArbeidsgiver.assertNoErrors(vararg filtre: AktivitetsloggFilter) =
        asserter.assertNoErrors(*filtre)
    protected fun TestPerson.TestArbeidsgiver.assertWarnings(vararg filtre: AktivitetsloggFilter) =
        asserter.assertWarnings(*filtre)
    protected fun TestPerson.TestArbeidsgiver.assertWarning(warning: String, vararg filtre: AktivitetsloggFilter) =
        asserter.assertWarning(warning, *filtre)
    protected fun TestPerson.TestArbeidsgiver.assertNoWarnings(vararg filtre: AktivitetsloggFilter) =
        asserter.assertNoWarnings(*filtre)
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
    protected fun String.håndterInntektsmeldingReplay(inntektsmeldingId: UUID, vedtaksperiodeId: UUID) =
        this { håndterInntektsmeldingReplay(inntektsmeldingId, vedtaksperiodeId) }
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
    protected fun String.assertNoErrors(vararg filtre: AktivitetsloggFilter) =
        this { assertNoErrors(*filtre) }
    protected fun String.assertWarnings(vararg filtre: AktivitetsloggFilter) =
        this { assertWarnings(*filtre) }
    protected fun String.assertWarning(warning: String, vararg filtre: AktivitetsloggFilter) =
        this { assertWarning(warning, *filtre) }
    protected fun String.assertNoWarnings(vararg filtre: AktivitetsloggFilter) =
        this { assertNoWarnings(*filtre) }
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
    protected fun håndterInntektsmeldingReplay(inntektsmeldingId: UUID, vedtaksperiodeId: UUID) =
        bareÈnArbeidsgiver(a1).håndterInntektsmeldingReplay(inntektsmeldingId, vedtaksperiodeId)

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

    protected fun assertTilstander(id: UUID, vararg tilstander: TilstandType) =
        bareÈnArbeidsgiver(a1).assertTilstander(id, *tilstander)
    protected fun assertSisteTilstand(id: UUID, tilstand: TilstandType, orgnummer: String = a1) =
        bareÈnArbeidsgiver(a1).assertSisteTilstand(id, tilstand)
    protected fun assertNoErrors(vararg filtre: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertNoErrors(*filtre)
    protected fun assertWarnings(vararg filtre: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertWarnings(*filtre)
    protected fun assertWarning(warning: String, vararg filtre: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertWarning(warning, *filtre)
    protected fun assertNoWarnings(vararg filtre: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertNoWarnings(*filtre)
    protected fun assertActivities() {
        val inspektør = inspiser(personInspektør)
        assertTrue(inspektør.aktivitetslogg.hasActivities()) { inspektør.aktivitetslogg.toString() }
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
        testperson = TestPerson(observatør)
    }

    @AfterEach
    fun verify() {
        testperson.bekreftBehovOppfylt()
    }
}
