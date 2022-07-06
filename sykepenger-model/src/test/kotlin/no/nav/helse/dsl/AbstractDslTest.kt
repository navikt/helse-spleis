package no.nav.helse.dsl

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.inspectors.PersonInspektør
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.TilstandType
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach

internal abstract class AbstractDslTest {
    internal companion object {
        @JvmStatic
        protected val a1 = "a1"
        @JvmStatic
        protected val a2 = "a2"
        @JvmStatic
        protected val INNTEKT = 31000.00.månedlig
        @JvmStatic
        protected val personInspektør = { person: Person -> PersonInspektør(person) }
        @JvmStatic
        protected val agInspektør = { orgnummer: String -> { person: Person -> TestArbeidsgiverInspektør(person, orgnummer) } }
    }
    protected lateinit var observatør: TestObservatør
    private lateinit var testperson: TestPerson

    protected val Int.vedtaksperiode get() = testperson.arbeidsgiver(a1) { vedtaksperiode }

    protected val String.inspektør get() = inspektør(this)
    protected val inspektør: TestArbeidsgiverInspektør get() = a1.inspektør

    private val TestPerson.TestArbeidsgiver.asserter get() = TestAssertions(observatør, inspektør, testperson.inspiser(personInspektør))

    protected fun <INSPEKTØR : PersonVisitor> inspiser(inspektør: (Person) -> INSPEKTØR) = testperson.inspiser(inspektør)
    protected fun inspektør(orgnummer: String) = inspiser(agInspektør(orgnummer))

    protected operator fun String.invoke(testblokk: TestPerson.TestArbeidsgiver.() -> Any) =
        testperson.arbeidsgiver(this, testblokk)

    protected fun TestPerson.TestArbeidsgiver.assertTilstander(id: UUID, vararg tilstander: TilstandType) {
        asserter.assertTilstander(id, *tilstander)
    }
    protected fun TestPerson.TestArbeidsgiver.assertSisteTilstand(id: UUID, tilstand: TilstandType, errortekst: (() -> String)? = null) {
        asserter.assertSisteTilstand(id, tilstand, errortekst)
    }

    /* alternative metoder fremfor å lage en arbeidsgiver-blokk hver gang */
    protected fun String.håndterSykmelding(vararg sykmeldingsperiode: Sykmeldingsperiode) =
        this { håndterSykmelding(*sykmeldingsperiode) }
    protected fun String.håndterSøknad(vararg perioder: Søknad.Søknadsperiode) =
        this { håndterSøknad(*perioder) }
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
    internal fun String.håndterUtbetalingshistorikk(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Infotrygdperiode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        harStatslønn: Boolean = false,
        besvart: LocalDateTime = LocalDateTime.now()
    ) =
        this { håndterUtbetalingshistorikk(vedtaksperiodeId, utbetalinger, inntektshistorikk, harStatslønn) }
    internal fun String.håndterVilkårsgrunnlag(vedtaksperiodeId: UUID = 1.vedtaksperiode, inntekt: Inntekt = INNTEKT) =
        this { håndterVilkårsgrunnlag(vedtaksperiodeId, inntekt) }
    internal fun String.håndterYtelser(
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
    internal fun String.håndterSimulering(vedtaksperiodeId: UUID) =
        this { håndterSimulering(vedtaksperiodeId) }
    internal fun String.håndterUtbetalingsgodkjenning(vedtaksperiodeId: UUID, godkjent: Boolean) =
        this { håndterUtbetalingsgodkjenning(vedtaksperiodeId, godkjent) }
    internal fun String.håndterUtbetalt(status: Oppdragstatus) =
        this { håndterUtbetalt(status) }
    protected fun String.assertTilstander(id: UUID, vararg tilstander: TilstandType) =
        this { assertTilstander(id, *tilstander) }
    protected fun String.assertSisteTilstand(id: UUID, tilstand: TilstandType) =
        this { assertSisteTilstand(id, tilstand) }
    protected fun String.assertNoErrors() =
        this { asserter.assertNoErrors() }
    protected fun String.assertNoWarnings(vararg filtre: AktivitetsloggFilter) =
        this { asserter.assertNoWarnings(*filtre) }

    /* dsl for å gå direkte på arbeidsgiver1, eksempelvis i tester for det ikke er andre arbeidsgivere */
    protected fun håndterSykmelding(vararg sykmeldingsperiode: Sykmeldingsperiode) =
        a1.håndterSykmelding(*sykmeldingsperiode)
    protected fun håndterSøknad(vararg perioder: Søknad.Søknadsperiode) =
        a1.håndterSøknad(*perioder)
    protected fun håndterInntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        beregnetInntekt: Inntekt,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOf { it.start },
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        harOpphørAvNaturalytelser: Boolean = false,
        arbeidsforholdId: String? = null,
        begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
        id: UUID = UUID.randomUUID()
    ) =
        a1.håndterInntektsmelding(arbeidsgiverperioder, beregnetInntekt, førsteFraværsdag, refusjon, harOpphørAvNaturalytelser, arbeidsforholdId, begrunnelseForReduksjonEllerIkkeUtbetalt, id)
    internal fun håndterUtbetalingshistorikk(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Infotrygdperiode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        harStatslønn: Boolean = false,
        besvart: LocalDateTime = LocalDateTime.now()
    ) =
        a1.håndterUtbetalingshistorikk(vedtaksperiodeId, utbetalinger, inntektshistorikk, harStatslønn, besvart)
    internal fun håndterVilkårsgrunnlag(vedtaksperiodeId: UUID = 1.vedtaksperiode, inntekt: Inntekt = INNTEKT) =
        a1.håndterVilkårsgrunnlag(vedtaksperiodeId, inntekt)
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
    ) =
        a1.håndterYtelser(vedtaksperiodeId, utbetalinger, inntektshistorikk, foreldrepenger, svangerskapspenger, pleiepenger, omsorgspenger, opplæringspenger, institusjonsoppholdsperioder, dødsdato, statslønn, arbeidskategorikoder, arbeidsavklaringspenger, dagpenger, besvart)
    internal fun håndterSimulering(vedtaksperiodeId: UUID) =
        a1.håndterSimulering(vedtaksperiodeId)
    internal fun håndterUtbetalingsgodkjenning(vedtaksperiodeId: UUID, godkjent: Boolean) =
        a1.håndterUtbetalingsgodkjenning(vedtaksperiodeId, godkjent)
    internal fun håndterUtbetalt(status: Oppdragstatus) =
        a1.håndterUtbetalt(status)
    protected fun assertTilstander(id: UUID, vararg tilstander: TilstandType) =
        a1.assertTilstander(id, *tilstander)
    protected fun assertSisteTilstand(id: UUID, tilstand: TilstandType) =
        a1.assertSisteTilstand(id, tilstand)
    protected fun assertNoErrors() =
        a1.assertNoErrors()
    protected fun assertNoWarnings(vararg filtre: AktivitetsloggFilter) =
        a1.assertNoWarnings(*filtre)
    protected fun assertActivities() {
        val inspektør = inspiser(personInspektør)
        assertTrue(inspektør.aktivitetslogg.hasActivities()) { inspektør.aktivitetslogg.toString() }
    }

    @BeforeEach
    fun setup() {
        observatør = TestObservatør()
        testperson = TestPerson(observatør)
    }
}
