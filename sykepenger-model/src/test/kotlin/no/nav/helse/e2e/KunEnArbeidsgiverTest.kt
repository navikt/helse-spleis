package no.nav.helse.e2e

import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.behov.Behovstype.*
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.ModelSendtSøknad.Periode.Sykdom
import no.nav.helse.hendelser.ModelSykepengehistorikk.Inntektsopplysning
import no.nav.helse.person.*
import no.nav.helse.person.TilstandType.*
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
    private lateinit var hendelselogger: Aktivitetslogger
    private var forventetEndringTeller = 0

    @BeforeEach
    internal fun setup() {
        person = Person(UNG_PERSON_FNR_2018, AKTØRID)
        observatør = TestObservatør().also { person.addObserver(it) }
    }

    @Test
    internal fun `ingen historie med SendtSøknad først`() {
        håndterNySøknad(Triple(3.januar, 26.januar, 100))
        håndterSendtSøknad(0, Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        inspektør.also {
            assertNoErrors(it)
            assertNoWarnings(it)
            assertMessages(it)
            assertEquals(INNTEKT.toBigDecimal(), it.inntektshistorikk.inntekt(2.januar))
            assertEquals(3, it.sykdomshistorikk.size)
            assertEquals(18, it.dagtelling[Sykedag::class])
            assertEquals(6, it.dagtelling[SykHelgedag::class])
        }
        assertTilstander(
            0,
            START, MOTTATT_NY_SØKNAD, UNDERSØKER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
    }

    @Test
    internal fun `Har tilstøtende perioder i historikk`() {
        håndterNySøknad(Triple(3.januar, 26.januar, 100))
        håndterSendtSøknad(0, Sykdom(3.januar, 26.januar, 100))
        håndterYtelser(0, Triple(1.januar, 2.januar, 15000))
        håndterManuellSaksbehandling(0, true)
        inspektør.also {
            assertNoErrors(it)
            assertNoWarnings(it)
            assertMessages(it)
            assertEquals(21000.toBigDecimal(), it.inntektshistorikk.inntekt(2.januar))
            assertEquals(2, it.sykdomshistorikk.size)
            assertEquals(18, it.dagtelling[Sykedag::class])
            assertEquals(6, it.dagtelling[SykHelgedag::class])
        }
        assertTilstander(0, START, MOTTATT_NY_SØKNAD, UNDERSØKER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING)
    }

    @Test
    internal fun `ingen historie med Inntektsmelding først`() {
        håndterNySøknad(Triple(3.januar, 26.januar, 100))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 18.januar)))
        håndterSendtSøknad(0, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        inspektør.also {
            assertNoErrors(it)
            assertNoWarnings(it)
            assertMessages(it)
            assertEquals(INNTEKT.toBigDecimal(), it.inntektshistorikk.inntekt(2.januar))
            assertEquals(3, it.sykdomshistorikk.size)
        }
        assertTilstander(
            0,
            START, MOTTATT_NY_SØKNAD, AVVENTER_SENDT_SØKNAD,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
    }

    @Test
    internal fun `ingen nav utbetaling kreves`() {
        håndterNySøknad(Triple(3.januar, 5.januar, 100))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 5.januar)))
        håndterSendtSøknad(0, Sykdom(3.januar, 5.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        inspektør.also {
            assertNoErrors(it)
            assertNoWarnings(it)
            assertMessages(it)
        }
        håndterYtelser(0)   // No history
        assertTrue(hendelselogger.hasErrors())
        println(hendelselogger)
        assertTilstander(
            0,
            START, MOTTATT_NY_SØKNAD, AVVENTER_SENDT_SØKNAD,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, TIL_INFOTRYGD
        )
    }

    @Test
    internal fun `To perioder med opphold`() {
        håndterNySøknad(Triple(3.januar, 26.januar, 100))
        håndterSendtSøknad(0, Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        håndterNySøknad(Triple(1.februar, 23.februar, 100))
        assertTrue(hendelselogger.hasMessages(), hendelselogger.toString())
        håndterSendtSøknad(1, Sykdom(1.februar, 23.februar, 100))
        håndterInntektsmelding(1, listOf(Periode(1.februar, 16.februar)))
        håndterVilkårsgrunnlag(1, INNTEKT)
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        inspektør.also {
            assertNoErrors(it)
            assertNoWarnings(it)
            assertMessages(it)
            println(it.personLogger)
            assertEquals(23, it.dagTeller(NavDag::class))
            assertEquals(16, it.dagTeller(ArbeidsgiverperiodeDag::class))
            assertEquals(8, it.dagTeller(NavHelgDag::class))
            assertEquals(3, it.dagTeller(Arbeidsdag::class))
        }
        assertTilstander(
            0,
            START, MOTTATT_NY_SØKNAD, UNDERSØKER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
        assertTilstander(
            1,
            START, MOTTATT_NY_SØKNAD, UNDERSØKER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
    }

    @Test
    internal fun `Sammenblandede hendelser fra forskjellige perioder med sendt søknad først`() {
        håndterNySøknad(Triple(3.januar, 26.januar, 100))
        håndterNySøknad(Triple(1.februar, 23.februar, 100))
        håndterSendtSøknad(1, Sykdom(1.februar, 23.februar, 100))
        håndterInntektsmelding(1, listOf(Periode(1.februar, 16.februar)))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 18.januar)))
        håndterSendtSøknad(0, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        forventetEndringTeller++
        håndterManuellSaksbehandling(0, true)
        assertTrue(hendelselogger.hasMessages(), hendelselogger.toString())
        håndterVilkårsgrunnlag(1, INNTEKT)
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        inspektør.also {
            assertNoErrors(it)
            assertNoWarnings(it)
            assertMessages(it)
            assertEquals(23, it.dagTeller(NavDag::class))
            assertEquals(16, it.dagTeller(ArbeidsgiverperiodeDag::class))
            assertEquals(8, it.dagTeller(NavHelgDag::class))
            assertEquals(3, it.dagTeller(Arbeidsdag::class))
        }
        assertTilstander(
            0,
            START, MOTTATT_NY_SØKNAD, AVVENTER_SENDT_SØKNAD,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
        assertTilstander(
            1,
            START, MOTTATT_NY_SØKNAD, AVVENTER_TIDLIGERE_PERIODE_ELLER_INNTEKTSMELDING, AVVENTER_TIDLIGERE_PERIODE,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
    }

    @Test
    internal fun `To tilstøtende perioder`() {
        håndterNySøknad(Triple(3.januar, 26.januar, 100))
        håndterNySøknad(Triple(29.januar, 23.februar, 100))
        håndterSendtSøknad(1, Sykdom(29.januar, 23.februar, 100))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 18.januar)))
        håndterSendtSøknad(0, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        forventetEndringTeller++
        håndterManuellSaksbehandling(0, true)
        val førsteUtbetalingsreferanse = observatør.utbetalingsreferanse
        assertTrue(hendelselogger.hasMessages(), hendelselogger.toString())
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        assertEquals(førsteUtbetalingsreferanse, observatør.utbetalingsreferanse)
        inspektør.also {
            assertNoErrors(it)
            assertNoWarnings(it)
            assertMessages(it)
            assertEquals(26, it.dagTeller(NavDag::class))
            assertEquals(16, it.dagTeller(ArbeidsgiverperiodeDag::class))
            assertEquals(8, it.dagTeller(NavHelgDag::class))
            assertEquals(0, it.dagTeller(Arbeidsdag::class))
        }
        assertTilstander(
            0,
            START, MOTTATT_NY_SØKNAD, AVVENTER_SENDT_SØKNAD,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
        assertTilstander(
            1,
            START, MOTTATT_NY_SØKNAD, AVVENTER_TIDLIGERE_PERIODE_ELLER_INNTEKTSMELDING, AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING, TIL_UTBETALING
        )
    }

    @Test
    internal fun `Sammenblandede hendelser fra forskjellige perioder med inntektsmelding først`() {
        håndterNySøknad(Triple(3.januar, 26.januar, 100))
        håndterNySøknad(Triple(1.februar, 23.februar, 100))
        håndterInntektsmelding(1, listOf(Periode(1.februar, 16.februar)))
        håndterSendtSøknad(1, Sykdom(1.februar, 23.februar, 100))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 18.januar)))
        håndterSendtSøknad(0, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        forventetEndringTeller++
        håndterManuellSaksbehandling(0, true)
        assertTrue(hendelselogger.hasMessages(), hendelselogger.toString())
        håndterVilkårsgrunnlag(1, INNTEKT)
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        inspektør.also {
            assertNoErrors(it)
            assertNoWarnings(it)
            assertMessages(it)
            assertEquals(23, it.dagTeller(NavDag::class))
            assertEquals(16, it.dagTeller(ArbeidsgiverperiodeDag::class))
            assertEquals(8, it.dagTeller(NavHelgDag::class))
            assertEquals(3, it.dagTeller(Arbeidsdag::class))
        }
        assertTilstander(
            0,
            START, MOTTATT_NY_SØKNAD, AVVENTER_SENDT_SØKNAD,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
        assertTilstander(
            1,
            START, MOTTATT_NY_SØKNAD, AVVENTER_SENDT_SØKNAD, AVVENTER_TIDLIGERE_PERIODE,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
    }

    @Test
    internal fun `Sammenblandede hendelser fra forskjellige perioder med inntektsmelding etter forrige periode`() {
        håndterNySøknad(Triple(3.januar, 26.januar, 100))
        håndterNySøknad(Triple(1.februar, 23.februar, 100))
        håndterSendtSøknad(1, Sykdom(1.februar, 23.februar, 100))
        håndterInntektsmelding(0, listOf(Periode(3.januar, 18.januar)))
        håndterSendtSøknad(0, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        forventetEndringTeller++
        håndterManuellSaksbehandling(0, true)
        val førsteUtbetalingsreferanse = observatør.utbetalingsreferanse
        håndterInntektsmelding(1, listOf(Periode(1.februar, 16.februar)))
        assertTrue(hendelselogger.hasMessages(), hendelselogger.toString())
        håndterVilkårsgrunnlag(1, INNTEKT)
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        assertNotEquals(førsteUtbetalingsreferanse, observatør.utbetalingsreferanse)
        inspektør.also {
            assertNoErrors(it)
            assertNoWarnings(it)
            assertMessages(it)
            assertEquals(23, it.dagTeller(NavDag::class))
            assertEquals(16, it.dagTeller(ArbeidsgiverperiodeDag::class))
            assertEquals(8, it.dagTeller(NavHelgDag::class))
            assertEquals(3, it.dagTeller(Arbeidsdag::class))
        }
        assertTilstander(
            0,
            START, MOTTATT_NY_SØKNAD, AVVENTER_SENDT_SØKNAD,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
        assertTilstander(
            1,
            START, MOTTATT_NY_SØKNAD, AVVENTER_TIDLIGERE_PERIODE_ELLER_INNTEKTSMELDING, AVVENTER_INNTEKTSMELDING,
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
        assertFalse(inspektør.personLogger.hasErrors())
        assertFalse(inspektør.arbeidsgiverLogger.hasErrors())
        assertFalse(inspektør.periodeLogger.hasErrors())
    }

    private fun assertNoWarnings(inspektør: TestPersonInspektør) {
        assertFalse(inspektør.personLogger.hasWarnings())
        assertFalse(inspektør.arbeidsgiverLogger.hasWarnings())
        assertFalse(inspektør.periodeLogger.hasWarnings())
    }

    private fun assertMessages(inspektør: TestPersonInspektør) {
        assertTrue(inspektør.personLogger.hasMessages())
        assertTrue(inspektør.arbeidsgiverLogger.hasMessages())
        assertTrue(inspektør.periodeLogger.hasMessages())
    }

    private fun håndterNySøknad(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>) {
        person.håndter(nySøknad(*sykeperioder))
        assertEndringTeller()
    }

    private fun håndterSendtSøknad(vedtaksperiodeIndex: Int, vararg perioder: ModelSendtSøknad.Periode) {
        assertFalse(observatør.etterspurteBehov(vedtaksperiodeIndex, Inntektsberegning))
        assertFalse(observatør.etterspurteBehov(vedtaksperiodeIndex, EgenAnsatt))
        person.håndter(sendtSøknad(*perioder))
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
        assertFalse(observatør.etterspurteBehov(vedtaksperiodeIndex, GodkjenningFraSaksbehandler))
        person.håndter(ytelser(vedtaksperiodeIndex, utbetalinger.toList()))
        assertEndringTeller()
    }

    private fun håndterManuellSaksbehandling(vedtaksperiodeIndex: Int, utbetalingGodkjent: Boolean) {
        assertTrue(observatør.etterspurteBehov(vedtaksperiodeIndex, GodkjenningFraSaksbehandler))
        person.håndter(manuellSaksbehandling(vedtaksperiodeIndex, utbetalingGodkjent))
        assertEndringTeller()
    }

    private fun nySøknad(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>): ModelNySøknad {
        hendelselogger = Aktivitetslogger()
        return ModelNySøknad(
            hendelseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            rapportertdato = rapportertdato,
            sykeperioder = listOf(*sykeperioder),
            aktivitetslogger = hendelselogger
        )
    }

    private fun sendtSøknad(vararg perioder: ModelSendtSøknad.Periode): ModelSendtSøknad {
        hendelselogger = Aktivitetslogger()
        return ModelSendtSøknad(
            hendelseId = UUID.randomUUID(),
            fnr = ModelNySøknadTest.UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            sendtNav = rapportertdato,
            perioder = listOf(*perioder),
            aktivitetslogger = hendelselogger,
            harAndreInntektskilder = false
        )
    }

    private fun inntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        ferieperioder: List<Periode> = emptyList(),
        refusjonBeløp: Double = INNTEKT,
        beregnetInntekt: Double = INNTEKT,
        førsteFraværsdag: LocalDate = 1.januar,
        refusjonOpphørsdato: LocalDate = 31.desember,  // Employer paid
        endringerIRefusjon: List<LocalDate> = emptyList()
    ): ModelInntektsmelding {
        hendelselogger = Aktivitetslogger()
        return ModelInntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = ModelInntektsmelding.Refusjon(refusjonOpphørsdato, refusjonBeløp, endringerIRefusjon),
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            mottattDato = rapportertdato,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = arbeidsgiverperioder,
            ferieperioder = ferieperioder,
            aktivitetslogger = hendelselogger
        )
    }

    private fun vilkårsgrunnlag(vedtaksperiodeIndex: Int, inntekt: Double): ModelVilkårsgrunnlag {
        hendelselogger = Aktivitetslogger()
        return ModelVilkårsgrunnlag(
            hendelseId = UUID.randomUUID(),
            vedtaksperiodeId = observatør.vedtaksperiodeIder(vedtaksperiodeIndex),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
            rapportertDato = rapportertdato,
            inntektsmåneder = (1..12).map {
                ModelVilkårsgrunnlag.Måned(
                    YearMonth.of(2017, it),
                    listOf(ModelVilkårsgrunnlag.Inntekt(inntekt))
                )
            },
            erEgenAnsatt = false,
            aktivitetslogger = hendelselogger,
            arbeidsforhold = ModelVilkårsgrunnlag.ModelArbeidsforhold(listOf(ModelVilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 1.januar(2017))))
        )
    }

    private fun ytelser(
        vedtaksperiodeIndex: Int,
        utbetalinger: List<Triple<LocalDate, LocalDate, Int>> = listOf(),
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null
    ): ModelYtelser {
        hendelselogger = Aktivitetslogger()
        return ModelYtelser(
            hendelseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = observatør.vedtaksperiodeIder(vedtaksperiodeIndex),
            sykepengehistorikk = ModelSykepengehistorikk(
                utbetalinger = utbetalinger.map {
                    ModelSykepengehistorikk.Periode.RefusjonTilArbeidsgiver(
                        it.first,
                        it.second,
                        it.third
                    )
                },
                inntektshistorikk = listOf(Inntektsopplysning(1.desember(2017), INNTEKT.toInt() - 10000, ORGNUMMER)),
                aktivitetslogger = hendelselogger
            ),
            foreldrepenger = ModelForeldrepenger(foreldrepenger, svangerskapspenger, Aktivitetslogger()),
            rapportertdato = rapportertdato,
            aktivitetslogger = hendelselogger
        )
    }

    private fun manuellSaksbehandling(
        vedtaksperiodeIndex: Int,
        utbetalingGodkjent: Boolean
    ): ModelManuellSaksbehandling {
        hendelselogger = Aktivitetslogger()
        return ModelManuellSaksbehandling(
            hendelseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = observatør.vedtaksperiodeIder(vedtaksperiodeIndex),
            saksbehandler = "Ola Nordmann",
            utbetalingGodkjent = utbetalingGodkjent,
            rapportertdato = rapportertdato,
            aktivitetslogger = hendelselogger
        )
    }

    private inner class TestObservatør : PersonObserver {
        internal var endreTeller = 0
        private val etterspurteBehov = mutableMapOf<Int, MutableMap<String, Boolean>>()
        private var periodeIndek = -1
        private val periodeIndekser = mutableMapOf<String, Int>()
        private val vedtaksperiodeIder = mutableMapOf<Int, String>()
        internal val tilstander = mutableMapOf<Int, MutableList<TilstandType>>()
        internal var utbetalingsreferanse: Long = 0

        internal fun etterspurteBehov(vedtaksperiodeIndex: Int, key: Behovstype) =
            etterspurteBehov[vedtaksperiodeIndex]?.getOrDefault(key.name, false) ?: false

        internal fun vedtaksperiodeIder(indeks: Int) = vedtaksperiodeIder[indeks] ?: fail("Missing vedtaksperiodeId")

        override fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretTilstandEvent) {
            endreTeller += 1
            val indeks = periodeIndeks(event.id.toString())
            tilstander[indeks]?.add(event.gjeldendeTilstand) ?: fail("Missing collection initialization")
        }

        override fun vedtaksperiodeTrengerLøsning(behov: Behov) {
            val indeks = periodeIndeks(behov.vedtaksperiodeId())
            behov.behovType().forEach { etterspurteBehov[indeks]?.put(it, true) }
        }

        override fun vedtaksperiodeTilUtbetaling(event: PersonObserver.UtbetalingEvent) {
            utbetalingsreferanse = event.utbetalingsreferanse.toLong()
        }

        private fun periodeIndeks(vedtaksperiodeId: String): Int {
            return periodeIndekser.getOrPut(vedtaksperiodeId, {
                periodeIndek++
                etterspurteBehov[periodeIndek] = mutableMapOf()
                tilstander[periodeIndek] = mutableListOf(START)
                vedtaksperiodeIder[periodeIndek] = vedtaksperiodeId.toString()
                periodeIndek
            })
        }
    }

    private inner class TestPersonInspektør(person: Person) : PersonVisitor {
        private var vedtaksperiodeindeks: Int = -1
        private val tilstander = mutableMapOf<Int, MutableList<TilstandType>>()
        internal lateinit var personLogger: Aktivitetslogger
        internal lateinit var arbeidsgiver: Arbeidsgiver
        internal lateinit var arbeidsgiverLogger: Aktivitetslogger
        internal lateinit var periodeLogger: Aktivitetslogger
        internal lateinit var inntektshistorikk: Inntekthistorikk
        internal lateinit var sykdomshistorikk: Sykdomshistorikk
        internal val dagtelling = mutableMapOf<KClass<out Dag>, Int>()

        init {
            person.accept(this)
        }

        override fun visitPersonAktivitetslogger(aktivitetslogger: Aktivitetslogger) {
            personLogger = aktivitetslogger
        }

        override fun preVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String
        ) {
            this.arbeidsgiver = arbeidsgiver
        }

        override fun visitArbeidsgiverAktivitetslogger(aktivitetslogger: Aktivitetslogger) {
            arbeidsgiverLogger = aktivitetslogger
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
            override fun visitSykedag(sykedag: Sykedag) = inkrementer(Sykedag::class)

            override fun visitSykHelgedag(sykHelgedag: SykHelgedag) = inkrementer(SykHelgedag::class)

            private fun inkrementer(klasse: KClass<out Dag>) {
                dagtelling.compute(klasse) { _, value ->
                    1 + (value ?: 0)
                }
            }
        }

        override fun visitVedtaksperiodeAktivitetslogger(aktivitetslogger: Aktivitetslogger) {
            periodeLogger = aktivitetslogger
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
