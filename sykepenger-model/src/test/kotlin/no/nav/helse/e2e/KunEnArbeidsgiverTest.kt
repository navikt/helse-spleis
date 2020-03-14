package no.nav.helse.e2e

import no.nav.helse.FeatureToggle
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Periode.Sykdom
import no.nav.helse.hendelser.Utbetaling
import no.nav.helse.hendelser.Utbetalingshistorikk.Inntektsopplysning
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Person
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.*
import no.nav.helse.sykdomstidslinje.dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class KunEnArbeidsgiverTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
        private const val INNTEKT = 31000.00
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
    internal fun `ingen historie med inntektsmelding først`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, Utbetaling.Status.FERDIG)
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
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTrue(observatør.utbetalteVedtaksperioder.contains(observatør.vedtaksperiodeIder(0)))
    }

    @Test
    internal fun `ingen historie med Søknad først`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, Utbetaling.Status.FERDIG)
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
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTrue(observatør.utbetalteVedtaksperioder.contains(observatør.vedtaksperiodeIder(0)))
    }

    @Test
    internal fun `gap historie før inntektsmelding`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterYtelser(0, Triple(1.januar, 1.januar, 15000))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, Utbetaling.Status.FERDIG)
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
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, AVVENTER_INNTEKTSMELDING_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTrue(observatør.utbetalteVedtaksperioder.contains(observatør.vedtaksperiodeIder(0)))
    }

    @Test
    internal fun `no-gap historie før inntektsmelding`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterYtelser(0, Triple(1.januar, 2.januar, 15000))
        inspektør.also {
            assertTrue(it.personLogg.hasErrors())
            assertMessages(it)
            assertTrue(it.inntekter.isEmpty())
            assertNull(it.inntektshistorikk.inntekt(2.januar))
            assertEquals(2, it.sykdomshistorikk.size)
            assertEquals(18, it.dagtelling[Sykedag::class])
            assertEquals(6, it.dagtelling[SykHelgedag::class])
        }
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, TIL_INFOTRYGD)
    }

    @Test
    internal fun `ingen nav utbetaling kreves`() {
        håndterSykmelding(Triple(3.januar, 5.januar, 100))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 5.januar, 100))
        håndterYtelser(0)   // No history
        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
        }
        assertFalse(hendelselogg.hasErrors())
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, AVVENTER_INNTEKTSMELDING_FERDIG_GAP
        )
    }


    @Test
    internal fun `To perioder med opphold`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSykmelding(Triple(1.februar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, Utbetaling.Status.FERDIG)

        assertTrue(hendelselogg.hasMessages(), hendelselogg.toString())
        håndterInntektsmeldingMedValidering(1, listOf(Periode(1.februar, 16.februar)))
        håndterSøknadMedValidering(1, Sykdom(1.februar, 23.februar, 100))
        håndterVilkårsgrunnlag(1, INNTEKT)
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        håndterUtbetalt(1, Utbetaling.Status.FERDIG)

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            1,
            START, MOTTATT_SYKMELDING_UFERDIG_GAP, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
    }

    @Test
    internal fun `Kiler tilstand i uferdig venter for inntektsmelding`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSykmelding(Triple(1.februar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1, Sykdom(1.februar, 23.februar, 100))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, Utbetaling.Status.FERDIG)

        assertTrue(hendelselogg.hasMessages(), hendelselogg.toString())
        håndterYtelser(1)   // No history
        håndterInntektsmeldingMedValidering(1, listOf(Periode(1.februar, 16.februar)))
        håndterVilkårsgrunnlag(1, INNTEKT)
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        håndterUtbetalt(1, Utbetaling.Status.FERDIG)

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_INNTEKTSMELDING_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    internal fun `Kilt etter søknad og inntektsmelding`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSykmelding(Triple(1.februar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1, Sykdom(1.februar, 23.februar, 100))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1, listOf(Periode(1.februar, 16.februar)))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, Utbetaling.Status.FERDIG)

        assertTrue(hendelselogg.hasMessages(), hendelselogg.toString())
        håndterVilkårsgrunnlag(1, INNTEKT)
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        håndterUtbetalt(1, Utbetaling.Status.FERDIG)

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            1,
            START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP, AVVENTER_UFERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
    }

    @Test
    internal fun `Kilt etter inntektsmelding og søknad`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSykmelding(Triple(1.februar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterInntektsmeldingMedValidering(1, listOf(Periode(1.februar, 16.februar)))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterSøknadMedValidering(1, Sykdom(1.februar, 23.februar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, Utbetaling.Status.FERDIG)

        assertTrue(hendelselogg.hasMessages(), hendelselogg.toString())
        håndterVilkårsgrunnlag(1, INNTEKT)
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        håndterUtbetalt(1, Utbetaling.Status.FERDIG)

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            1,
            START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_SØKNAD_UFERDIG_GAP, AVVENTER_UFERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
    }

    @Test
    internal fun `Kilt etter inntektsmelding`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSykmelding(Triple(1.februar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterInntektsmeldingMedValidering(1, listOf(Periode(1.februar, 16.februar)))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, Utbetaling.Status.FERDIG)

        assertTrue(hendelselogg.hasMessages(), hendelselogg.toString())
        håndterSøknadMedValidering(1, Sykdom(1.februar, 23.februar, 100))
        håndterVilkårsgrunnlag(1, INNTEKT)
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        håndterUtbetalt(1, Utbetaling.Status.FERDIG)

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            1,
            START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_SØKNAD_UFERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
    }

    @Test
    internal fun `beregning av utbetaling ignorerer tidligere ugyldige perioder`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterYtelser(0, Triple(1.januar, 2.januar, 15000)) // -> TIL_INFOTRYGD

        håndterSykmelding(Triple(1.februar, 23.februar, 100))
        håndterSøknadMedValidering(1, Sykdom(1.februar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(1, listOf(Periode(1.februar, 16.februar)), 1.februar)
        håndterVilkårsgrunnlag(1, INNTEKT)
        håndterYtelser(1)   // No history

        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, TIL_INFOTRYGD)
        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING
        )
    }

    @Test
    internal fun `første fraværsdato fra inntektsmelding er ulik utregnet første fraværsdato`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)), 4.januar)
        inspektør.also { assertTrue(it.personLogg.hasWarnings()) }
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP)
    }

    @Test
    internal fun `første fraværsdato i inntektsmelding er utenfor perioden`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)), 27.januar)
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, AVVENTER_VILKÅRSPRØVING_GAP)
    }

    @Test
    internal fun `første fraværsdato i inntektsmelding, før søknad, er utenfor perioden`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)), 27.januar)
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP
        )
    }

    @Test
    internal fun `Sammenblandede hendelser fra forskjellige perioder med søknad først`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSykmelding(Triple(1.februar, 23.februar, 100))
        håndterSøknadMedValidering(1, Sykdom(1.februar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(1, listOf(Periode(1.februar, 16.februar)))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        forventetEndringTeller++
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, Utbetaling.Status.FERDIG)
        assertTrue(hendelselogg.hasMessages(), hendelselogg.toString())
        håndterVilkårsgrunnlag(1, INNTEKT)
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        håndterUtbetalt(1, Utbetaling.Status.FERDIG)
        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            1,
            START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP, AVVENTER_UFERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
    }

    @Test
    internal fun `To tilstøtende perioder søknad først`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSykmelding(Triple(29.januar, 23.februar, 100))
        håndterSøknadMedValidering(1, Sykdom(29.januar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history

        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, Utbetaling.Status.FERDIG)
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        håndterUtbetalt(1, Utbetaling.Status.FERDIG)

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }


    @Test
    internal fun `Venter å på bli kilt etter søknad og inntektsmelding`() {
        håndterSykmelding(Triple(3.januar, 7.januar, 100))
        håndterSykmelding(Triple(8.januar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 7.januar, 100))
        håndterSøknadMedValidering(1, Sykdom(8.januar, 23.februar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history

        håndterManuellSaksbehandling(0, true)
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        håndterUtbetalt(1, Utbetaling.Status.FERDIG)

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, AVSLUTTET
        )
        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    internal fun `Venter på å bli kilt etter inntektsmelding`() {
        håndterSykmelding(Triple(3.januar, 7.januar, 100))
        håndterSykmelding(Triple(8.januar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 7.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)

        håndterSøknadMedValidering(1, Sykdom(8.januar, 23.februar, 100))
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        håndterUtbetalt(1, Utbetaling.Status.FERDIG)

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, AVSLUTTET
        )
        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_UFERDIG_FORLENGELSE,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    internal fun `Fortsetter før andre søknad`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSykmelding(Triple(29.januar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(
            vedtaksperiodeIndex = 0,
            arbeidsgiverperioder = listOf(Periode(3.januar, 18.januar)),
            førsteFraværsdag = 3.januar
        )
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, Utbetaling.Status.FERDIG)

        håndterSøknadMedValidering(1, Sykdom(29.januar, 23.februar, 100))
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        håndterUtbetalt(1, Utbetaling.Status.FERDIG)

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            assertEquals(3.januar, it.førsteFraværsdager[0])
            assertEquals(3.januar, it.førsteFraværsdager[1])
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            1,
            START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
    }

    @Test
    internal fun `To tilstøtende perioder der den første er utbetalt`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, Utbetaling.Status.FERDIG)

        håndterSykmelding(Triple(29.januar, 23.februar, 100))
        håndterSøknadMedValidering(1, Sykdom(29.januar, 23.februar, 100))
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        håndterUtbetalt(1, Utbetaling.Status.FERDIG)

        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    internal fun `To tilstøtende perioder inntektsmelding først`() {
        håndterSykmelding(Triple(3.januar, 7.januar, 100))
        håndterSykmelding(Triple(8.januar, 23.februar, 100))
        håndterSøknadMedValidering(1, Sykdom(8.januar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 7.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history

        håndterManuellSaksbehandling(0, true)
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        håndterUtbetalt(1, Utbetaling.Status.FERDIG)

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, AVSLUTTET
        )
        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    internal fun `To tilstøtende perioder der den første er i utbetaling feilet`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, Utbetaling.Status.FEIL)

        håndterSykmelding(Triple(29.januar, 23.februar, 100))
        håndterSøknadMedValidering(1, Sykdom(29.januar, 23.februar, 100))

        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, UTBETALING_FEILET
        )
        assertTilstander(
            1,
            START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        )
    }

    @Test
    internal fun `ignorer inntektsmeldinger på påfølgende perioder`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, Utbetaling.Status.FERDIG)

        håndterSykmelding(Triple(29.januar, 23.februar, 100))
        håndterSøknadMedValidering(1, Sykdom(29.januar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(1, listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        håndterUtbetalt(1, Utbetaling.Status.FERDIG)


        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            1,
            START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
    }

    @Test
    internal fun `kiler bare 2nd og ikke 3nd periode i en rekke`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSykmelding(Triple(1.februar, 23.februar, 100))
        håndterSykmelding(Triple(1.mars, 28.mars, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1, Sykdom(1.februar, 23.februar, 100))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, Utbetaling.Status.FERDIG)

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            1,
            START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP, AVVENTER_GAP
        )

        assertTilstander(
            2,
            START, MOTTATT_SYKMELDING_UFERDIG_GAP
        )
    }

    @Test
    internal fun `Sykmelding med gradering`() {
        FeatureToggle.støtterGradertSykdom = true
        håndterSykmelding(Triple(3.januar, 26.januar, 50))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 50, 50.00))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
    }

    @Test
    internal fun `forlenger ikke vedtaksperiode som har gått til infotrygd`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterPåminnelse(0, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSykmelding(Triple(29.januar, 23.februar, 100))
        håndterSøknadMedValidering(1, Sykdom(29.januar, 23.februar, 100))

        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD
        )
        assertTilstander(
            1,
            START, TIL_INFOTRYGD
        )
    }

    @Test
    internal fun `dupliserte hendelser produserer bare advarsler`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100))
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, Utbetaling.Status.FERDIG)
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100))
        inspektør.also {
            assertNoErrors(it)
            assertWarnings(it)
            assertEquals(INNTEKT.toBigDecimal(), it.inntektshistorikk.inntekt(2.januar))
            assertEquals(3, it.sykdomshistorikk.size)
            assertEquals(18, it.dagtelling[Sykedag::class])
            assertEquals(6, it.dagtelling[SykHelgedag::class])
        }
        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTrue(observatør.utbetalteVedtaksperioder.contains(observatør.vedtaksperiodeIder(0)))
    }

    @Test
    internal fun `gradert sykmelding først`() {
        // ugyldig sykmelding lager en tom vedtaksperiode uten tidslinje, som overlapper med alt
        håndterSykmelding(Triple(3.januar(2020), 3.januar(2020), 50))
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)

        håndterSykmelding(Triple(13.januar(2020), 17.januar(2020), 100))
        håndterSøknad(Sykdom(13.januar(2020), 17.januar(2020), 100))
        assertTilstander(1, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP)
    }

    @Test
    internal fun `Søknad treffer flere perioder`() {
        håndterSykmelding(Triple(1.januar(2020), 5.januar(2020), 100))
        håndterSykmelding(Triple(6.januar(2020), 10.januar(2020), 100))
        håndterSykmelding(Triple(13.januar(2020), 17.januar(2020), 100))
        håndterSøknad(
            Sykdom(13.januar(2020), 17.januar(2020), 100),
            Søknad.Periode.Egenmelding(30.desember(2019), 31.desember(2019))
        )
        håndterSykmelding(Triple(18.januar(2020), 26.januar(2020), 100))
        håndterSøknad(Sykdom(18.januar(2020), 26.januar(2020), 100))
        håndterSykmelding(Triple(27.januar(2020), 30.januar(2020), 100))
        håndterSøknad(Sykdom(27.januar(2020), 30.januar(2020), 100))
        håndterSykmelding(Triple(30.januar(2020), 14.februar(2020), 100))
        håndterSykmelding(Triple(30.januar(2020), 14.februar(2020), 100))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(
                Periode(30.desember(2019), 31.desember(2019)),
                Periode(1.januar(2020), 5.januar(2020)),
                Periode(6.januar(2020), 10.januar(2020)),
                Periode(13.januar(2020), 16.januar(2020))
            ), førsteFraværsdag = 13.januar(2020)
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(
                Periode(30.desember(2019), 31.desember(2019)),
                Periode(1.januar(2020), 5.januar(2020)),
                Periode(6.januar(2020), 10.januar(2020)),
                Periode(13.januar(2020), 16.januar(2020))
            ), førsteFraværsdag = 13.januar(2020)
        )

        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, AVVENTER_VILKÅRSPRØVING_GAP)
        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE
        )
        assertTilstander(
            2,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE
        )
        assertTilstander(3, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(4, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)
        assertEquals(5, observatør.tilstander.size)
    }

    @Test
    internal fun `Sykmelding i omvendt rekkefølge`() {
        håndterSykmelding(Triple(10.januar, 20.januar, 100))
        håndterSykmelding(Triple(3.januar, 5.januar, 100))
        håndterInntektsmelding(listOf(
            Periode(4.januar, 5.januar),
            Periode(9.januar, 23.januar)
        ))
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP)
        assertTilstander(1, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP)
    }

    @Test
    internal fun `Ingen sykedager i tidslinjen - første fraværsdag bug`() {
        håndterSykmelding(Triple(6.januar(2020), 7.januar(2020), 100))
        håndterSykmelding(Triple(8.januar(2020), 10.januar(2020), 100))
        håndterSykmelding(Triple(27.januar(2020), 28.januar(2020), 100))

        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP)
        assertTilstander(1, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(2, START, MOTTATT_SYKMELDING_UFERDIG_GAP)

        håndterInntektsmelding(listOf(
            Periode(18.november(2019), 23.november(2019)),
            Periode(14.oktober(2019), 18.oktober(2019)),
            Periode(1.november(2019), 5.november(2019))
        ), 18.november(2019), listOf(
            Periode(5.desember(2019), 6.desember(2019)),
            Periode(30.desember(2019), 30.desember(2019)),
            Periode(2.januar(2020), 3.januar(2020)),
            Periode(22.januar(2020), 22.januar(2020))
        ))

        // TODO: Which state should the period go to after Inntektsmelding?
        // assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP)
    }

    @Test
    internal fun `Inntektsmelding vil ikke utvide vedtaksperiode til tidligere vedtaksperiode`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)), 3.januar)
        inspektør.also {
            assertNoErrors(it)
            assertNoWarnings(it)
        }
        håndterSøknadMedValidering(0, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        forventetEndringTeller++
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, Utbetaling.Status.FERDIG)

        håndterSykmelding(Triple(1.februar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(1, listOf(Periode(16.januar, 16.februar))) // Touches prior periode
        assertNoErrors(inspektør)

        håndterSøknadMedValidering(1, Sykdom(1.februar, 23.februar, 100))
        håndterVilkårsgrunnlag(1, INNTEKT)
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)
        håndterUtbetalt(1, Utbetaling.Status.FERDIG)
        assertNoErrors(inspektør)

        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            1,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
    }

    private fun assertTilstander(indeks: Int, vararg tilstander: TilstandType) {
        assertEquals(tilstander.asList(), observatør.tilstander[indeks])
    }

    private fun assertNoErrors(inspektør: TestPersonInspektør) {
        assertFalse(inspektør.personLogg.hasErrors(), inspektør.personLogg.toString())
    }

    private fun assertNoWarnings(inspektør: TestPersonInspektør) {
        assertFalse(inspektør.personLogg.hasWarnings(), inspektør.personLogg.toString())
    }

    private fun assertWarnings(inspektør: TestPersonInspektør) {
        assertTrue(inspektør.personLogg.hasWarnings(), inspektør.personLogg.toString())
    }

    private fun assertMessages(inspektør: TestPersonInspektør) {
        assertTrue(inspektør.personLogg.hasMessages(), inspektør.personLogg.toString())
    }

    private fun håndterSykmelding(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>) {
        person.håndter(sykmelding(*sykeperioder))
    }

    private fun håndterSøknadMedValidering(
        vedtaksperiodeIndex: Int,
        vararg perioder: Søknad.Periode,
        harAndreInntektskilder: Boolean = false
    ) {
        assertFalse(inspektør.etterspurteBehov(vedtaksperiodeIndex, Inntektsberegning))
        assertFalse(inspektør.etterspurteBehov(vedtaksperiodeIndex, EgenAnsatt))
        håndterSøknad(*perioder, harAndreInntektskilder = harAndreInntektskilder)
    }

    private fun håndterSøknad(vararg perioder: Søknad.Periode, harAndreInntektskilder: Boolean = false) {
        person.håndter(søknad(perioder = *perioder, harAndreInntektskilder = harAndreInntektskilder))
    }

    private fun håndterInntektsmeldingMedValidering(
        vedtaksperiodeIndex: Int,
        arbeidsgiverperioder: List<Periode>,
        førsteFraværsdag: LocalDate = 1.januar
    ) {
        assertFalse(inspektør.etterspurteBehov(vedtaksperiodeIndex, Inntektsberegning))
        assertFalse(inspektør.etterspurteBehov(vedtaksperiodeIndex, EgenAnsatt))
        håndterInntektsmelding(arbeidsgiverperioder, førsteFraværsdag)
    }

    private fun håndterInntektsmelding(arbeidsgiverperioder: List<Periode>, førsteFraværsdag: LocalDate = 1.januar, ferieperioder: List<Periode> = emptyList()) {
        person.håndter(inntektsmelding(arbeidsgiverperioder, ferieperioder = ferieperioder, førsteFraværsdag = førsteFraværsdag))
    }

    private fun håndterVilkårsgrunnlag(vedtaksperiodeIndex: Int, inntekt: Double) {
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, Inntektsberegning))
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, EgenAnsatt))
        person.håndter(vilkårsgrunnlag(vedtaksperiodeIndex, INNTEKT))
    }

    private fun håndterYtelser(vedtaksperiodeIndex: Int, vararg utbetalinger: Triple<LocalDate, LocalDate, Int>) {
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, Sykepengehistorikk))
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, Foreldrepenger))
        assertFalse(inspektør.etterspurteBehov(vedtaksperiodeIndex, Godkjenning))
        person.håndter(ytelser(vedtaksperiodeIndex, utbetalinger.toList()))
    }

    private fun håndterPåminnelse(vedtaksperiodeIndex: Int, påminnetTilstand: TilstandType) {
        person.håndter(påminnelse(vedtaksperiodeIndex, påminnetTilstand))
    }

    private fun håndterManuellSaksbehandling(vedtaksperiodeIndex: Int, utbetalingGodkjent: Boolean) {
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, Godkjenning))
        person.håndter(manuellSaksbehandling(vedtaksperiodeIndex, utbetalingGodkjent))
    }

    private fun håndterUtbetalt(vedtaksperiodeIndex: Int, status: Utbetaling.Status) {
        person.håndter(utbetaling(vedtaksperiodeIndex, status))
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
        }
    }

    private fun søknad(vararg perioder: Søknad.Periode, harAndreInntektskilder: Boolean): Søknad {
        return Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            perioder = listOf(*perioder),
            harAndreInntektskilder = harAndreInntektskilder,
            sendtTilNAV = perioder.last().tom.atStartOfDay()
        ).apply {
            hendelselogg = this
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
            arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 1.januar(2017)))
        ).apply {
            hendelselogg = this
        }
    }

    private fun påminnelse(
        vedtaksperiodeIndex: Int,
        påminnetTilstand: TilstandType
    ): Påminnelse {
        return Påminnelse(
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = observatør.vedtaksperiodeIder(vedtaksperiodeIndex),
            antallGangerPåminnet = 0,
            tilstand = påminnetTilstand,
            tilstandsendringstidspunkt = LocalDateTime.now(),
            påminnelsestidspunkt = LocalDateTime.now(),
            nestePåminnelsestidspunkt = LocalDateTime.now()
        )
    }

    private fun ytelser(
        vedtaksperiodeIndex: Int,
        utbetalinger: List<Triple<LocalDate, LocalDate, Int>> = listOf(),
        inntektshistorikk: List<Inntektsopplysning> = listOf(
            Inntektsopplysning(
                1.desember(2017),
                INNTEKT.toInt() - 10000,
                ORGNUMMER
            )
        ),
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
                inntektshistorikk = inntektshistorikk,
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
            utbetalingGodkjent = utbetalingGodkjent,
            godkjenttidspunkt = LocalDateTime.now()
        ).apply {
            hendelselogg = this
        }
    }
}
