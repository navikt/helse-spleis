package no.nav.helse.e2e

import no.nav.helse.FeatureToggle
import no.nav.helse.behov.BehovType
import no.nav.helse.behov.Behovstype
import no.nav.helse.behov.Behovstype.*
import no.nav.helse.behov.partisjoner
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Periode.Sykdom
import no.nav.helse.hendelser.Utbetaling
import no.nav.helse.hendelser.Utbetalingshistorikk.Inntektsopplysning
import no.nav.helse.person.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.serde.api.serializePersonForSpeil
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import kotlin.reflect.KClass

internal class KunEnArbeidsgiverTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
        private const val INNTEKT = 31000.00
        private val rapportertdato = 1.februar.atStartOfDay()
    }

    private lateinit var person: Person
    private lateinit var observatør: TestObservatør
    private val inspektør get() = TestPersonInspektør(person)
    private lateinit var hendelselogg: ArbeidstakerHendelse
    private var forventetEndringTeller = 0

    @BeforeEach
    internal fun setup() {
        person = Person(UNG_PERSON_FNR_2018, AKTØRID)
        observatør = TestObservatør().also { person.addObserver(it) }
    }

    @AfterEach
    internal fun reset() {
        FeatureToggle.støtterGradertSykdom = false
    }

    @Test
    internal fun `ingen historie med Søknad først`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSøknad(0, Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            assertEquals(INNTEKT.toBigDecimal(), it.inntektshistorikk.inntekt(2.januar))
            assertEquals(3, it.sykdomshistorikk.size)
            assertEquals(18, it.dagtelling[Sykedag::class])
            assertEquals(6, it.dagtelling[SykHelgedag::class])
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING, UNDERSØKER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
    }

    @Test
    internal fun `Har tilstøtende perioder i historikk`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSøknad(0, Sykdom(3.januar, 26.januar, 100))
        håndterYtelser(0, Triple(1.januar, 2.januar, 15000))
        håndterManuellSaksbehandling(0, true)
        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            assertEquals(21000.toBigDecimal(), it.inntektshistorikk.inntekt(2.januar))
            assertEquals(2, it.sykdomshistorikk.size)
            assertEquals(18, it.dagtelling[Sykedag::class])
            assertEquals(6, it.dagtelling[SykHelgedag::class])
        }
        assertTilstander(0, START, MOTTATT_SYKMELDING, UNDERSØKER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING)
    }

    @Test
    internal fun `ingen historie med Inntektsmelding først`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 18.januar)))
        håndterSøknad(0, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            assertEquals(INNTEKT.toBigDecimal(), it.inntektshistorikk.inntekt(2.januar))
            assertEquals(3, it.sykdomshistorikk.size)
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING, AVVENTER_SØKNAD,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
    }

    @Test
    internal fun `ingen nav utbetaling kreves`() {
        håndterSykmelding(Triple(3.januar, 5.januar, 100))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 5.januar)))
        håndterSøknad(0, Sykdom(3.januar, 5.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
        }
        håndterYtelser(0)   // No history
        assertTrue(hendelselogg.hasErrors())
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING, AVVENTER_SØKNAD,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, TIL_INFOTRYGD
        )
    }

    @Test
    internal fun `To perioder med opphold`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSøknad(0, Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        val førsteUtbetalingsreferanse = observatør.utbetalingsreferanseFraUtbetalingEvent
        observatør.utbetalingsreferanseFraUtbetalingEvent = ""

        håndterSykmelding(Triple(1.februar, 23.februar, 100))
        assertTrue(hendelselogg.hasMessages(), hendelselogg.toString())
        håndterSøknad(1, Sykdom(1.februar, 23.februar, 100))
        håndterInntektsmelding(1, listOf(Periode(1.februar, 16.februar)))
        håndterVilkårsgrunnlag(1, INNTEKT)
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        val andreUtbetalingsreferanse = observatør.utbetalingsreferanseFraUtbetalingEvent

        // TODO: Sjekk / Fiks at ikke "gamle" utbetalingslinjer blir med på utbetalinger for
        //  ny periode/nytt sykdomsforløp slik at det ikke blir dobbelutbetalinger fra Spenn (!)

        assertNotEquals(førsteUtbetalingsreferanse, andreUtbetalingsreferanse)

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            assertEquals(23, it.dagTeller(NavDag::class))
            assertEquals(16, it.dagTeller(ArbeidsgiverperiodeDag::class))
            assertEquals(8, it.dagTeller(NavHelgDag::class))
            assertEquals(3, it.dagTeller(Arbeidsdag::class))
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING, UNDERSØKER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
        assertTilstander(
            1,
            START, MOTTATT_SYKMELDING, UNDERSØKER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
    }

    @Test
    internal fun `Sammenblandede hendelser fra forskjellige perioder med søknad først`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSykmelding(Triple(1.februar, 23.februar, 100))
        håndterSøknad(1, Sykdom(1.februar, 23.februar, 100))
        håndterInntektsmelding(1, listOf(Periode(1.februar, 16.februar)))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 18.januar)))
        håndterSøknad(0, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        forventetEndringTeller++
        håndterManuellSaksbehandling(0, true)
        assertTrue(hendelselogg.hasMessages(), hendelselogg.toString())
        håndterVilkårsgrunnlag(1, INNTEKT)
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            assertEquals(23, it.dagTeller(NavDag::class))
            assertEquals(16, it.dagTeller(ArbeidsgiverperiodeDag::class))
            assertEquals(8, it.dagTeller(NavHelgDag::class))
            assertEquals(3, it.dagTeller(Arbeidsdag::class))
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING, AVVENTER_SØKNAD,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
        assertTilstander(
            1,
            START, MOTTATT_SYKMELDING, AVVENTER_TIDLIGERE_PERIODE_ELLER_INNTEKTSMELDING, AVVENTER_TIDLIGERE_PERIODE,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
    }

    @Test
    internal fun `To tilstøtende perioder`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSykmelding(Triple(29.januar, 23.februar, 100))
        håndterSøknad(1, Sykdom(29.januar, 23.februar, 100))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 18.januar)))
        håndterSøknad(0, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        forventetEndringTeller++

        val utbetalingsreferanseISpeilOppslagFørstePeriode = serializePersonForSpeil(person)
            .first["arbeidsgivere"][0]["vedtaksperioder"][0]["utbetalingsreferanse"].asText()

        håndterManuellSaksbehandling(0, true)
        val førsteUtbetalingsreferanse = observatør.utbetalingsreferanseFraUtbetalingEvent
        observatør.utbetalingsreferanseFraUtbetalingEvent = ""
        assertEquals(utbetalingsreferanseISpeilOppslagFørstePeriode, førsteUtbetalingsreferanse)
        assertTrue(hendelselogg.hasMessages(), hendelselogg.toString())
        håndterYtelser(1)   // No history

        val utbetalingsreferanseISpeilOppslagAndrePeriode = serializePersonForSpeil(person)
            .first["arbeidsgivere"][0]["vedtaksperioder"][1]["utbetalingsreferanse"].asText()
        assertEquals(førsteUtbetalingsreferanse, utbetalingsreferanseISpeilOppslagAndrePeriode)

        håndterManuellSaksbehandling(1, true)
        println(serializePersonForSpeil(person))
        assertEquals(førsteUtbetalingsreferanse, observatør.utbetalingsreferanseFraUtbetalingEvent)
        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            assertEquals(26, it.dagTeller(NavDag::class))
            assertEquals(16, it.dagTeller(ArbeidsgiverperiodeDag::class))
            assertEquals(8, it.dagTeller(NavHelgDag::class))
            assertEquals(0, it.dagTeller(Arbeidsdag::class))
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING, AVVENTER_SØKNAD,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
        assertTilstander(
            1,
            START, MOTTATT_SYKMELDING, AVVENTER_TIDLIGERE_PERIODE_ELLER_INNTEKTSMELDING, AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING, TIL_UTBETALING
        )
    }

    @Test
    internal fun `To tilstøtende perioder der den første er utbetalt`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSøknad(0, Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, Utbetaling.Status.FERDIG)

        håndterSykmelding(Triple(29.januar, 23.februar, 100))
        håndterSøknad(1, Sykdom(29.januar, 23.februar, 100))

        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING, UNDERSØKER_HISTORIKK ,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, UTBETALT
        )
        assertTilstander(
            1,
            START, MOTTATT_SYKMELDING, AVVENTER_HISTORIKK
        )
    }

    @Test
    internal fun `To tilstøtende perioder der den første er i utbetaling feilet`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSøknad(0, Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, Utbetaling.Status.FEIL)

        håndterSykmelding(Triple(29.januar, 23.februar, 100))
        håndterSøknad(1, Sykdom(29.januar, 23.februar, 100))

        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING, UNDERSØKER_HISTORIKK ,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, UTBETALING_FEILET
        )
        assertTilstander(
            1,
            START, MOTTATT_SYKMELDING, AVVENTER_HISTORIKK
        )
    }

    @Test
    internal fun `tilstøtende periode arver første fraværsdag`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSøknad(0, Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)

        håndterSykmelding(Triple(29.januar, 23.februar, 100))
        håndterSøknad(1, Sykdom(29.januar, 23.februar, 100))
        håndterYtelser(1)   // No history

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            assertEquals(2, it.førsteFraværsdager.size)
            assertEquals(it.førsteFraværsdager[0], it.førsteFraværsdager[1])
        }
    }

    @Test
    internal fun `Sammenblandede hendelser fra forskjellige perioder med inntektsmelding først`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSykmelding(Triple(1.februar, 23.februar, 100))
        håndterInntektsmelding(1, listOf(Periode(1.februar, 16.februar)))
        håndterSøknad(1, Sykdom(1.februar, 23.februar, 100))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 18.januar)))
        håndterSøknad(0, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        forventetEndringTeller++
        håndterManuellSaksbehandling(0, true)
        assertTrue(hendelselogg.hasMessages(), hendelselogg.toString())
        håndterVilkårsgrunnlag(1, INNTEKT)
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            assertEquals(23, it.dagTeller(NavDag::class))
            assertEquals(16, it.dagTeller(ArbeidsgiverperiodeDag::class))
            assertEquals(8, it.dagTeller(NavHelgDag::class))
            assertEquals(3, it.dagTeller(Arbeidsdag::class))
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING, AVVENTER_SØKNAD,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
        assertTilstander(
            1,
            START, MOTTATT_SYKMELDING, AVVENTER_SØKNAD, AVVENTER_TIDLIGERE_PERIODE,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
    }

    @Test
    internal fun `Sammenblandede hendelser fra forskjellige perioder med inntektsmelding etter forrige periode`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSykmelding(Triple(1.februar, 23.februar, 100))
        håndterSøknad(1, Sykdom(1.februar, 23.februar, 100))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 18.januar)))
        håndterSøknad(0, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        forventetEndringTeller++
        håndterManuellSaksbehandling(0, true)
        val førsteUtbetalingsreferanse = observatør.utbetalingsreferanseFraUtbetalingEvent
        håndterInntektsmelding(1, listOf(Periode(1.februar, 16.februar)))
        assertTrue(hendelselogg.hasMessages(), hendelselogg.toString())
        håndterVilkårsgrunnlag(1, INNTEKT)
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        assertNotEquals(førsteUtbetalingsreferanse, observatør.utbetalingsreferanseFraUtbetalingEvent)
        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            assertEquals(23, it.dagTeller(NavDag::class))
            assertEquals(16, it.dagTeller(ArbeidsgiverperiodeDag::class))
            assertEquals(8, it.dagTeller(NavHelgDag::class))
            assertEquals(3, it.dagTeller(Arbeidsdag::class))
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING, AVVENTER_SØKNAD,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
        assertTilstander(
            1,
            START, MOTTATT_SYKMELDING, AVVENTER_TIDLIGERE_PERIODE_ELLER_INNTEKTSMELDING, AVVENTER_INNTEKTSMELDING,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
    }

    @Test
    internal fun `Sykmelding med gradering`() {
        FeatureToggle.støtterGradertSykdom = true
        håndterSykmelding(Triple(3.januar, 26.januar, 50))
        håndterSøknad(0, Sykdom(3.januar, 26.januar, 50, 50.00))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            assertEquals(6, it.dagTeller(NavDag::class))
            assertEquals(16, it.dagTeller(ArbeidsgiverperiodeDag::class))
            assertEquals(2, it.dagTeller(NavHelgDag::class))
            assertEquals(0, it.dagTeller(Arbeidsdag::class))
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING, UNDERSØKER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
    }

    private fun assertEndringTeller() {
        forventetEndringTeller += 1
        assertEquals(forventetEndringTeller, observatør.endreTeller)
    }

    private fun assertTilstander(indeks: Int, vararg tilstander: TilstandType) {
        assertEquals(tilstander.asList(), observatør.tilstander[indeks])
    }

    private fun assertNoErrors(inspektør: TestPersonInspektør) {
        assertFalse(inspektør.personLogg.hasErrors())
    }

    private fun assertMessages(inspektør: TestPersonInspektør) {
        assertTrue(inspektør.personLogg.hasMessages())
    }

    private fun håndterSykmelding(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>) {
        person.håndter(sykmelding(*sykeperioder))
        assertEndringTeller()
    }

    private fun håndterSøknad(vedtaksperiodeIndex: Int, vararg perioder: Søknad.Periode) {
        assertFalse(observatør.etterspurteBehov(vedtaksperiodeIndex, Inntektsberegning))
        assertFalse(observatør.etterspurteBehov(vedtaksperiodeIndex, EgenAnsatt))
        person.håndter(søknad(*perioder))
        assertEndringTeller()
    }

    private fun håndterInntektsmelding(vedtaksperiodeIndex: Int, arbeidsgiverperioder: List<Periode>) {
        assertFalse(observatør.etterspurteBehov(vedtaksperiodeIndex, Inntektsberegning))
        assertFalse(observatør.etterspurteBehov(vedtaksperiodeIndex, EgenAnsatt))
        person.håndter(inntektsmelding(arbeidsgiverperioder))
        assertEndringTeller()
    }

    private fun håndterVilkårsgrunnlag(vedtaksperiodeIndex: Int, inntekt: Double) {
        assertTrue(observatør.etterspurteBehov(vedtaksperiodeIndex, Inntektsberegning))
        assertTrue(observatør.etterspurteBehov(vedtaksperiodeIndex, EgenAnsatt))
        person.håndter(vilkårsgrunnlag(vedtaksperiodeIndex, INNTEKT))
        assertEndringTeller()
    }

    private fun håndterYtelser(vedtaksperiodeIndex: Int, vararg utbetalinger: Triple<LocalDate, LocalDate, Int>) {
        assertTrue(observatør.etterspurteBehov(vedtaksperiodeIndex, Sykepengehistorikk))
        assertTrue(observatør.etterspurteBehov(vedtaksperiodeIndex, Foreldrepenger))
        assertFalse(observatør.etterspurteBehov(vedtaksperiodeIndex, Godkjenning))
        person.håndter(ytelser(vedtaksperiodeIndex, utbetalinger.toList()))
        assertEndringTeller()
    }

    private fun håndterManuellSaksbehandling(vedtaksperiodeIndex: Int, utbetalingGodkjent: Boolean) {
        assertTrue(observatør.etterspurteBehov(vedtaksperiodeIndex, Godkjenning))
        person.håndter(manuellSaksbehandling(vedtaksperiodeIndex, utbetalingGodkjent))
        assertEndringTeller()
    }

    private fun håndterUtbetalt(vedtaksperiodeIndex: Int, status: Utbetaling.Status) {
        person.håndter(utbetaling(vedtaksperiodeIndex, status))
        assertEndringTeller()
    }

    private fun utbetaling(vedtaksperiodeIndex: Int, status: Utbetaling.Status) =
        Utbetaling(
            vedtaksperiodeId = observatør.vedtaksperiodeIder(vedtaksperiodeIndex),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
            utbetalingsreferanse = "ref",
            status = status,
            melding = "hei"
        )



    private fun sykmelding(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            sykeperioder = listOf(*sykeperioder)
        ).apply {
            hendelselogg = this
            addObserver(observatør)
        }
    }

    private fun søknad(vararg perioder: Søknad.Periode): Søknad {
        return Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            perioder = listOf(*perioder),
            harAndreInntektskilder = false
        ).apply {
            hendelselogg = this
            addObserver(observatør)
        }
    }

    private fun inntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        ferieperioder: List<Periode> = emptyList(),
        refusjonBeløp: Double = INNTEKT,
        beregnetInntekt: Double = INNTEKT,
        førsteFraværsdag: LocalDate = 1.januar,
        refusjonOpphørsdato: LocalDate = 31.desember,  // Employer paid
        endringerIRefusjon: List<LocalDate> = emptyList()
    ): Inntektsmelding {
        return Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(refusjonOpphørsdato, refusjonBeløp, endringerIRefusjon),
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = arbeidsgiverperioder,
            ferieperioder = ferieperioder
        ).apply {
            hendelselogg = this
            addObserver(observatør)
        }
    }

    private fun vilkårsgrunnlag(vedtaksperiodeIndex: Int, inntekt: Double): Vilkårsgrunnlag {
        return Vilkårsgrunnlag(
            vedtaksperiodeId = observatør.vedtaksperiodeIder(vedtaksperiodeIndex),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
            inntektsmåneder = (1..12).map {
                Vilkårsgrunnlag.Måned(
                    YearMonth.of(2017, it),
                    listOf(inntekt)
                )
            },
            erEgenAnsatt = false,
            arbeidsforhold = Vilkårsgrunnlag.MangeArbeidsforhold(listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 1.januar(2017))))
        ).apply {
            hendelselogg = this
            addObserver(observatør)
        }
    }

    private fun ytelser(
        vedtaksperiodeIndex: Int,
        utbetalinger: List<Triple<LocalDate, LocalDate, Int>> = listOf(),
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null
    ): Ytelser {
        val aktivitetslogg = Aktivitetslogg()
        return Ytelser(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = observatør.vedtaksperiodeIder(vedtaksperiodeIndex),
            utbetalingshistorikk = Utbetalingshistorikk(
                ukjentePerioder = emptyList(),
                utbetalinger = utbetalinger.map {
                    Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(
                        it.first,
                        it.second,
                        it.third
                    )
                },
                inntektshistorikk = listOf(Inntektsopplysning(1.desember(2017), INNTEKT.toInt() - 10000, ORGNUMMER)),
                graderingsliste = emptyList<Utbetalingshistorikk.Graderingsperiode>(),
                aktivitetslogg = aktivitetslogg
            ),
            foreldrepermisjon = Foreldrepermisjon(
                foreldrepenger,
                svangerskapspenger,
                aktivitetslogg
            ),
            aktivitetslogg = aktivitetslogg
        ).apply {
            hendelselogg = this
            addObserver(observatør)
        }
    }

    private fun manuellSaksbehandling(
        vedtaksperiodeIndex: Int,
        utbetalingGodkjent: Boolean
    ): ManuellSaksbehandling {
        return ManuellSaksbehandling(
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = observatør.vedtaksperiodeIder(vedtaksperiodeIndex),
            saksbehandler = "Ola Nordmann",
            utbetalingGodkjent = utbetalingGodkjent
        ).apply {
            hendelselogg = this
            addObserver(observatør)
        }
    }

    private inner class TestObservatør : PersonObserver, HendelseObserver {
        internal var endreTeller = 0
        private val etterspurteBehov = mutableListOf<BehovType>()
        private var periodeIndek = -1
        private val periodeIndekser = mutableMapOf<String, Int>()
        private val vedtaksperiodeIder = mutableMapOf<Int, String>()
        internal val tilstander = mutableMapOf<Int, MutableList<TilstandType>>()
        internal lateinit var utbetalingsreferanseFraUtbetalingEvent: String

        internal fun etterspurteBehov(vedtaksperiodeIndex: Int, key: Behovstype) =
            etterspurteBehov.partisjoner().let {
                it.filter { it["vedtaksperiodeId"] == UUID.fromString(vedtaksperiodeIder(vedtaksperiodeIndex)) }
                    .filter { key.name in (it["@behov"] as List<*>) }
                    .size == 1
            }

        internal fun vedtaksperiodeIder(indeks: Int) = vedtaksperiodeIder[indeks] ?: fail("Missing vedtaksperiodeId")

        override fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretTilstandEvent) {
            endreTeller += 1
            val indeks = periodeIndeks(event.id.toString())
            tilstander[indeks]?.add(event.gjeldendeTilstand) ?: fail("Missing collection initialization")
        }

        override fun onBehov(behov: BehovType) {
            etterspurteBehov.add(behov)
        }

        override fun vedtaksperiodeTilUtbetaling(event: PersonObserver.UtbetalingEvent) {
            utbetalingsreferanseFraUtbetalingEvent = event.utbetalingsreferanse
        }

        private fun periodeIndeks(vedtaksperiodeId: String): Int {
            return periodeIndekser.getOrPut(vedtaksperiodeId, {
                periodeIndek++
                tilstander[periodeIndek] = mutableListOf(START)
                vedtaksperiodeIder[periodeIndek] = vedtaksperiodeId
                periodeIndek
            })
        }
    }

    private inner class TestPersonInspektør(person: Person) : PersonVisitor {
        private var vedtaksperiodeindeks: Int = -1
        private val tilstander = mutableMapOf<Int, MutableList<TilstandType>>()
        internal lateinit var personLogg: Aktivitetslogg
        internal lateinit var arbeidsgiver: Arbeidsgiver
        internal lateinit var inntektshistorikk: Inntekthistorikk
        internal lateinit var sykdomshistorikk: Sykdomshistorikk
        internal val førsteFraværsdager: MutableList<LocalDate> = mutableListOf()
        internal val dagtelling = mutableMapOf<KClass<out Dag>, Int>()

        init {
            person.accept(this)
        }

        override fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
            personLogg = aktivitetslogg
        }

        override fun visitFørsteFraværsdag(førsteFraværsdag: LocalDate?) {
            if (førsteFraværsdag != null) {
                førsteFraværsdager.add(førsteFraværsdag)
            }
        }

        override fun preVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String
        ) {
            this.arbeidsgiver = arbeidsgiver
        }

        override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
            this.inntektshistorikk = inntekthistorikk
        }

        override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
            vedtaksperiodeindeks += 1
            tilstander[vedtaksperiodeindeks] = mutableListOf()
        }

        override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
            this.sykdomshistorikk = sykdomshistorikk
            this.sykdomshistorikk.sykdomstidslinje().accept(Dagteller())
        }

        private inner class Dagteller : SykdomstidslinjeVisitor {
            override fun visitSykedag(sykedag: Sykedag.Sykmelding) = inkrementer(Sykedag::class)
            override fun visitSykedag(sykedag: Sykedag.Søknad) = inkrementer(Sykedag::class)

            override fun visitSykHelgedag(sykHelgedag: SykHelgedag) = inkrementer(SykHelgedag::class)

            private fun inkrementer(klasse: KClass<out Dag>) {
                dagtelling.compute(klasse) { _, value ->
                    1 + (value ?: 0)
                }
            }
        }

        override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
            tilstander[vedtaksperiodeindeks]?.add(tilstand.type) ?: fail("Missing collection initialization")
        }

        internal val vedtaksperiodeTeller get() = tilstander.size

        internal fun tilstand(indeks: Int) = tilstander[indeks] ?: fail("Missing collection initialization")

        internal fun dagTeller(klasse: KClass<out Utbetalingsdag>) =
            TestTidslinjeInspektør(arbeidsgiver.peekTidslinje()).dagtelling[klasse] ?: 0
    }

    private class TestTidslinjeInspektør(tidslinje: Utbetalingstidslinje) :
        Utbetalingstidslinje.UtbetalingsdagVisitor {

        internal val dagtelling: MutableMap<KClass<out Utbetalingsdag>, Int> = mutableMapOf()
        internal val datoer = mutableMapOf<LocalDate, KClass<out Utbetalingsdag>>()

        init {
            tidslinje.accept(this)
        }

        override fun visitNavDag(dag: NavDag) {
            datoer[dag.dato] = NavDag::class
            inkrementer(NavDag::class)
        }

        override fun visitArbeidsdag(dag: Arbeidsdag) {
            datoer[dag.dato] = Arbeidsdag::class
            inkrementer(Arbeidsdag::class)
        }

        override fun visitNavHelgDag(dag: NavHelgDag) {
            datoer[dag.dato] = NavHelgDag::class
            inkrementer(NavHelgDag::class)
        }

        override fun visitUkjentDag(dag: UkjentDag) {
            datoer[dag.dato] = UkjentDag::class
            inkrementer(UkjentDag::class)
        }

        override fun visitArbeidsgiverperiodeDag(dag: ArbeidsgiverperiodeDag) {
            datoer[dag.dato] = ArbeidsgiverperiodeDag::class
            inkrementer(ArbeidsgiverperiodeDag::class)
        }

        override fun visitFridag(dag: Fridag) {
            datoer[dag.dato] = Fridag::class
            inkrementer(Fridag::class)
        }

        private fun inkrementer(klasse: KClass<out Utbetalingsdag>) {
            dagtelling.compute(klasse) { _, value -> 1 + (value ?: 0) }
        }
    }
}
