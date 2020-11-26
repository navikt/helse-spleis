package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.*
import no.nav.helse.hendelser.Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType.*
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters.firstDayOfMonth
import java.time.temporal.TemporalAdjusters.lastDayOfMonth

internal class KunEnArbeidsgiverTest : AbstractEndToEndTest() {

    //FIXME: Fjernes når alle Doubles byttes ut med Inntekt
    private val INNTEKT_AS_INT = 31000

    @Test
    fun `ingen historie med inntektsmelding først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        inspektør.låstePerioder.also {
            assertEquals(0, it.size)
        }
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 2.januar, it)
            assertEquals(3, it.sykdomshistorikk.size)
            assertEquals(18, it.dagtelling[Sykedag::class])
            assertEquals(6, it.dagtelling[SykHelgedag::class])
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTrue(observatør.utbetalteVedtaksperioder.contains(1.vedtaksperiode))
        inspektør.låstePerioder.also {
            assertEquals(1, it.size)
            assertEquals(Periode(3.januar, 26.januar), it.first())
        }
    }

    @Test
    fun `ingen historie med søknad til arbeidsgiver først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 8.januar, 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(3.januar, 8.januar, 100))
        assertNoWarnings(inspektør)
        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 2.januar, it)
            assertEquals(3, it.sykdomshistorikk.size)
            assertEquals(4, it.dagtelling[Sykedag::class])
            assertEquals(2, it.dagtelling[SykHelgedag::class])
        }
        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
    }

    @Test
    fun `ingen historie med to søknader til arbeidsgiver før inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 5.januar, 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(3.januar, 5.januar, 100))

        håndterSykmelding(Sykmeldingsperiode(8.januar, 10.januar, 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(8.januar, 10.januar, 100))

        håndterSykmelding(Sykmeldingsperiode(11.januar, 22.januar, 100))
        håndterSøknad(Sykdom(11.januar, 22.januar, 100))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 2.januar, it)
            assertEquals(7, it.sykdomshistorikk.size)
            assertEquals(4, it.dagtelling[SykHelgedag::class])
            assertEquals(14, it.dagtelling[Sykedag::class])
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `ingen historie med to søknader til arbeidsgiver først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 5.januar, 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(3.januar, 5.januar, 100))

        håndterSykmelding(Sykmeldingsperiode(8.januar, 10.januar, 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(8.januar, 10.januar, 100))

        håndterSykmelding(Sykmeldingsperiode(11.januar, 22.januar, 100))
        håndterSøknad(Sykdom(11.januar, 22.januar, 100))

        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 2.januar, it)
            assertEquals(7, it.sykdomshistorikk.size)
            assertEquals(4, it.dagtelling[SykHelgedag::class])
            assertEquals(14, it.dagtelling[Sykedag::class])
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `ingen historie med to søknader (med gap mellom) til arbeidsgiver først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 4.januar, 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(3.januar, 4.januar, 100))

        håndterSykmelding(Sykmeldingsperiode(8.januar, 10.januar, 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(8.januar, 10.januar, 100))

        håndterSykmelding(Sykmeldingsperiode(11.januar, 22.januar, 100))
        håndterSøknad(Sykdom(11.januar, 22.januar, 100))

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(3.januar, 4.januar), Periode(8.januar, 21.januar)),
            førsteFraværsdag = 8.januar
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 2.januar, it)
            assertEquals(7, it.sykdomshistorikk.size)
            assertEquals(4, it.dagtelling[SykHelgedag::class])
            assertEquals(13, it.dagtelling[Sykedag::class])
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `ingen historie med inntektsmelding, så søknad til arbeidsgiver`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 8.januar, 100))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        assertNoWarnings(inspektør)
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(3.januar, 8.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 2.januar, it)
            assertEquals(3, it.sykdomshistorikk.size)
            assertEquals(4, it.dagtelling[Sykedag::class])
            assertEquals(2, it.dagtelling[SykHelgedag::class])
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
    }

    @Test
    fun `ingen historie med Søknad først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 2.januar, it)
            assertEquals(3, it.sykdomshistorikk.size)
            assertEquals(18, it.dagtelling[Sykedag::class])
            assertEquals(6, it.dagtelling[SykHelgedag::class])
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTrue(observatør.utbetalteVedtaksperioder.contains(1.vedtaksperiode))
    }

    @Test
    fun `søknad sendt etter 3 mnd`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100), sendtTilNav = 1.mai)
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 2.januar, it)
            assertEquals(3, it.sykdomshistorikk.size)
            assertNull(it.dagtelling[Sykedag::class])
            assertEquals(6, it.dagtelling[SykHelgedag::class])
            assertDoesNotThrow { it.arbeidsgiver.nåværendeTidslinje() }
            assertTrue(it.utbetalingslinjer(0).isEmpty())
            TestTidslinjeInspektør(it.utbetalingstidslinjer(1.vedtaksperiode)).also { tidslinjeInspektør ->
                assertEquals(6, tidslinjeInspektør.dagtelling[ForeldetDag::class])
                assertEquals(2, tidslinjeInspektør.dagtelling[NavHelgDag::class])
                assertEquals(16, tidslinjeInspektør.dagtelling[ArbeidsgiverperiodeDag::class])
            }
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, AVSLUTTET
        )
    }

    @Test
    fun `gap-historie før inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.desember(2017), 15.desember(2017), 15000, 100, ORGNUMMER)
        )
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 2.januar, it)
            assertEquals(3, it.sykdomshistorikk.size)
            assertEquals(18, it.dagtelling[Sykedag::class])
            assertEquals(6, it.dagtelling[SykHelgedag::class])
        }
        assertNotNull(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_INNTEKTSMELDING_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTrue(observatør.utbetalteVedtaksperioder.contains(1.vedtaksperiode))
    }

    @Test
    fun `gap-historie uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.desember(2017), 16.desember(2017), 15000, 100, ORGNUMMER)
        )
        inspektør.also {
            assertNoErrors(it)
            assertFalse(it.personLogg.hasWarningsOrWorse())
            assertActivities(it)
            assertInntektForDato(null, 2.januar, it)
            assertEquals(2, it.sykdomshistorikk.size)
            assertEquals(18, it.dagtelling[Sykedag::class])
            assertEquals(6, it.dagtelling[SykHelgedag::class])
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_INNTEKTSMELDING_FERDIG_GAP
        )
    }

    @Test
    fun `fremtidig test av utbetalingstidslinjeBuilderVol2, historikk fra flere arbeidsgivere`() {
        håndterSykmelding(Sykmeldingsperiode(20.september(2020), 19.oktober(2020), 100))
        håndterSøknadMedValidering(
            1.vedtaksperiode,
            Sykdom(20.september(2020), 19.oktober(2020), 100)
        )

        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(27.juli(2020), 20.august(2020), 2077, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(21.august(2020), 19.september(2020), 2077, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(21.august(2019), 19.september(2019), 1043, 100, "12345789"),
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(27.juli(2020), 45000.månedlig, ORGNUMMER, true),
                Utbetalingshistorikk.Inntektsopplysning(21.august(2019), 22600.månedlig, "12345789", true)
            )
        )
        håndterYtelser(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(27.juli(2020), 20.august(2020), 2077, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(21.august(2020), 19.september(2020), 2077, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(21.august(2019), 19.september(2019), 1043, 100, "12345789"),
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(27.juli(2020), 45000.månedlig, ORGNUMMER, true),
                Utbetalingshistorikk.Inntektsopplysning(21.august(2019), 22600.månedlig, "12345789", true)
            )
        )

        inspektør.also {
            assertNoErrors(it)
            assertFalse(it.personLogg.hasWarningsOrWorse())
            assertActivities(it)
            assertInntektForDato(45000.månedlig, 27.juli(2020), it)
            assertEquals(2, it.sykdomshistorikk.size)
            assertEquals(21, it.dagtelling[Sykedag::class])
            assertEquals(9, it.dagtelling[SykHelgedag::class])
            assertEquals(43617, it.nettoBeløp[0])
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `setter riktig inntekt i utbetalingstidslinjebuilderVol2`() {
        håndterSykmelding(Sykmeldingsperiode(21.september(2020), 10.oktober(2020), 100))
        håndterSøknad(Sykdom(21.september(2020), 10.oktober(2020), 100))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(
                Periode(4.september(2020), 19.september(2020))
            ),
            førsteFraværsdag = 21.september(2020)
        ) // 20. september er en søndag
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 21.september(2020), it)
            assertEquals(21.september(2020), it.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(21465, it.nettoBeløp[0])
        }
    }


    @Test
    fun `inntektsmeldingen padder ikke senere vedtaksperioder med arbeidsdager`() {
        håndterSykmelding(Sykmeldingsperiode(4.januar, 22.januar, 100))
        håndterSøknad(Sykdom(4.januar, 22.januar, 100))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(4.januar, 19.januar)),
            førsteFraværsdag = 4.januar
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(24.januar, 31.januar, 100))
        håndterSøknad(Sykdom(24.januar, 31.januar, 100))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(4.januar, 19.januar)),
            førsteFraværsdag = 24.januar
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 4.januar, it)
            assertInntektForDato(INNTEKT, 24.januar, it)
            assertEquals(4.januar, it.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(24.januar, it.skjæringstidspunkt(2.vedtaksperiode))
            assertEquals(19, it.dagtelling[Sykedag::class])
            assertEquals(8, it.dagtelling[SykHelgedag::class])
            assertEquals(1, it.dagtelling[Dag.UkjentDag::class])
        }
    }

    @Test
    fun `ingen nav utbetaling kreves, blir automatisk behandlet og avsluttet`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 5.januar, 100))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 5.januar, 100))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertFalse(hendelselogg.hasErrorsOrWorse())

        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), 3.januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        assertFalse(person.aktivitetslogg.logg(inspektør.vedtaksperioder(1.vedtaksperiode)).hasWarningsOrWorse())
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_INNTEKTSMELDING_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
    }

    @Test
    fun `perioden avsluttes ikke automatisk hvis warnings`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 5.januar, 100))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 5.januar, 100))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertFalse(hendelselogg.hasErrorsOrWorse())

        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), 3.januar)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            INNTEKT,
            arbeidsavklaringspenger = listOf(3.januar.minusDays(60) til 5.januar.minusDays(60))
        ) // warning pga AAP
        håndterYtelser(1.vedtaksperiode)

        assertTrue(person.aktivitetslogg.logg(inspektør.vedtaksperioder(1.vedtaksperiode)).hasWarningsOrWorse())
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_INNTEKTSMELDING_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING
        )
    }

    @Test
    fun `To perioder med opphold`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertActivities(inspektør)
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.februar, 16.februar)))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.februar, 23.februar, 100))
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertNotNull(inspektør.maksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Kiler tilstand i uferdig venter for inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.februar, 23.februar, 100))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertActivities(inspektør)
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.februar, 16.februar)))
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertNotNull(inspektør.maksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_INNTEKTSMELDING_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Kilt etter søknad og inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.februar, 23.februar, 100))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.februar, 16.februar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertActivities(inspektør)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertNotNull(inspektør.maksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            AVVENTER_UFERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Kilt etter inntektsmelding og søknad`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.februar, 16.februar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.februar, 23.februar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertActivities(inspektør)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertNotNull(inspektør.maksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_SØKNAD_UFERDIG_GAP,
            AVVENTER_UFERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Kilt etter inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.februar, 16.februar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertActivities(inspektør)
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.februar, 23.februar, 100))
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertNotNull(inspektør.maksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_SØKNAD_UFERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `forlengelse av infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100))
        håndterSøknad(Sykdom(1.februar, 23.februar, 100))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.januar, 31.januar, INNTEKT_AS_INT, 100, ORGNUMMER)
        )
        håndterYtelser(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.januar, 31.januar, INNTEKT_AS_INT, 100, ORGNUMMER)
        )
        håndterSimulering(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING
        )
    }

    @Test
    fun `første fraværsdato fra inntektsmelding er ulik utregnet første fraværsdato`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), 4.januar)
        inspektør.also { assertTrue(it.personLogg.hasWarningsOrWorse()) }
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP)
    }

    @Test
    fun `første fraværsdato fra inntektsmelding er ulik utregnet første fraværsdato for påfølgende perioder`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(27.januar, 7.februar, 100))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(Periode(3.januar, 18.januar)),
            3.januar,
            listOf(Periode(27.januar, 27.januar))
        )
        inspektør.also { assertFalse(it.personLogg.hasWarningsOrWorse()) }
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE
        )
    }

    @Test
    fun `første fraværsdato i inntektsmelding er utenfor perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), 27.januar)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP
        )
    }

    @Test
    fun `første fraværsdato i inntektsmelding, før søknad, er utenfor perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), 27.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP
        )
    }

    @Test
    fun `Sammenblandede hendelser fra forskjellige perioder med søknad først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.februar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.februar, 16.februar)))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        forventetEndringTeller++
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        assertActivities(inspektør)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertNotNull(it.maksdato(1.vedtaksperiode))
            assertNotNull(it.maksdato(2.vedtaksperiode))
            assertEquals(8586, it.totalBeløp[0])
            assertEquals(8586, it.nettoBeløp[0])
            assertEquals(32913, it.totalBeløp[1])
            assertEquals(24327, it.nettoBeløp[1])
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            AVVENTER_UFERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `To forlengelser som forlenger utbetaling fra infotrygd skal ha samme maksdato`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 31.januar, 100))
        håndterSøknad(Sykdom(3.januar, 31.januar, 100))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.januar, 2.januar, INNTEKT_AS_INT, 100, ORGNUMMER)
        )
        håndterYtelser(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.januar, 2.januar, INNTEKT_AS_INT, 100, ORGNUMMER)
        )
        håndterSimulering(1.vedtaksperiode)
        forventetEndringTeller++
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100))
        håndterSøknad(Sykdom(1.februar, 23.februar, 100))
        håndterYtelser(
            2.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.januar, 2.januar, INNTEKT_AS_INT, 100, ORGNUMMER)
        )
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertEquals(inspektør.maksdato(1.vedtaksperiode), inspektør.maksdato(2.vedtaksperiode))
    }

    @Test
    fun `fortsettelse av Infotrygd-perioder skal ikke generere utbetalingslinjer for Infotrygd-periode`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 31.januar, 100))
        håndterSøknad(Sykdom(3.januar, 31.januar, 100))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.januar, 2.januar, INNTEKT_AS_INT, 100, ORGNUMMER)
        )
        håndterYtelser(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.januar, 2.januar, INNTEKT_AS_INT, 100, ORGNUMMER)
        )

        inspektør.also {
            assertEquals(3.januar, it.arbeidsgiverOppdrag[0][0].fom)
        }
    }

    @Test
    fun `To tilstøtende perioder søknad først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertNotNull(inspektør.maksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }


    @Test
    fun `Venter å på bli kilt etter søknad og inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 7.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 7.januar, 100))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(8.januar, 23.februar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history

        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertNotNull(inspektør.maksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }


    @Test
    fun `Bytter fra forlengelse til gap-state når tidligere tilstøtende periodes søknad avsluttes med arbeidsdager - andre periode har mottatt sykmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 22.februar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100), Arbeid(18.januar, 21.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            MOTTATT_SYKMELDING_FERDIG_GAP
        )
        assertEquals(INNTEKT, inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.beregnetÅrsinntektFraInntektskomponenten)
        assertNull(inspektør.vilkårsgrunnlag(2.vedtaksperiode))
    }

    @Test
    fun `Bytter fra forlengelse til gap-state når tidligere tilstøtende periodes søknad avsluttes med arbeidsdager - andre periode har mottatt søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 22.februar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100), Arbeid(18.januar, 21.januar))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(22.januar, 22.februar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_FERDIG_GAP
        )
    }

    @Test
    fun `Bytter fra forlengelse til gap-state når tidligere tilstøtende periodes søknad avsluttes med arbeidsdager - andre periode har mottatt inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 22.februar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100), Arbeid(18.januar, 21.januar))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.januar, 16.januar)), 22.januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_FERDIG_GAP
        )
    }

    @Test
    fun `Bytter fra forlengelse til gap-state når tidligere tilstøtende periodes søknad avsluttes med arbeidsdager - andre periode har mottatt søknad og inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 22.februar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100), Arbeid(18.januar, 21.januar))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(22.januar, 22.februar, 100))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.januar, 16.januar)), 22.januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_VILKÅRSPRØVING_GAP
        )
    }

    @Test
    fun `Sender ikke avsluttet periode til infotrygd når man mottar en ugyldig søknad i etterkant`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        håndterSøknad(
            perioder = arrayOf(Sykdom(1.januar, 21.januar, 100), Permisjon(21.januar, 21.januar)),
            id = 1.vedtaksperiode
        )
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
    }

    @Test
    fun `Sender ikke periode som allerede har behandlet inntektsmelding til infotrygd når man mottar en ny ugyldig inntektsmelding i etterkant`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Triple(15.januar, INNTEKT, emptyList())
        )
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
    }

    @Test
    fun `søknad til arbeidsgiver etter inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 7.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(8.januar, 23.februar, 100))

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertEquals(3, it.dagtelling[Dag.Arbeidsgiverdag::class])
            assertEquals(2, it.dagtelling[Dag.ArbeidsgiverHelgedag::class])
            assertEquals(35, it.dagtelling[Sykedag::class])
            assertEquals(12, it.dagtelling[SykHelgedag::class])
        }
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_UFERDIG_FORLENGELSE,
            AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD
        )
    }

    @Test
    fun `Venter på å bli kilt etter inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 7.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 7.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history

        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(8.januar, 23.februar, 100))
        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertNotNull(inspektør.maksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_UFERDIG_FORLENGELSE,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Fortsetter før andre søknad`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(
            vedtaksperiodeId = 1.vedtaksperiode,
            arbeidsgiverperioder = listOf(Periode(3.januar, 18.januar)),
            førsteFraværsdag = 3.januar
        )
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100))
        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertEquals(3.januar, it.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(3.januar, it.skjæringstidspunkt(2.vedtaksperiode))
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertNotNull(inspektør.maksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
    }

    @Test
    fun `To tilstøtende perioder der den første er utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100))
        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertNotNull(inspektør.maksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `To tilstøtende perioder inntektsmelding først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 7.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 23.februar, 100))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(8.januar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 7.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history

        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertNotNull(inspektør.maksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Disabled
    @Test
    fun `To tilstøtende perioder, inntektsmelding 2 med arbeidsdager i starten`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 7.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(
            2.vedtaksperiode,
            listOf(Periode(3.januar, 7.januar), Periode(15.januar, 20.januar), Periode(23.januar, 28.januar))
        )
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 7.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_FERDIG_GAP
        )
    }

    @Test
    fun `To tilstøtende perioder der den første er i utbetaling feilet`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AVVIST)

        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100))

        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, UTBETALING_FEILET
        )
        assertTilstander(
            2.vedtaksperiode,
            START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        )
    }

    @Test
    fun `ignorer inntektsmeldinger på påfølgende perioder`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)


        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertNotNull(inspektør.maksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
    }

    @Test
    fun `kiler bare andre periode og ikke tredje periode i en rekke`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 28.mars, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.februar, 23.februar, 100))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP, AVVENTER_GAP
        )

        assertTilstander(
            3.vedtaksperiode,
            START, MOTTATT_SYKMELDING_UFERDIG_GAP
        )
    }

    @Test
    fun `Sykmelding med gradering`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 50))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 50, 50))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING
        )
    }

    @Test
    fun `Inntektsmelding vil ikke utvide vedtaksperiode til tidligere vedtaksperiode`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), 3.januar)
        inspektør.also {
            assertNoErrors(it)
            assertNoWarnings(it)
        }
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        forventetEndringTeller++
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(
            2.vedtaksperiode,
            listOf(Periode(16.januar, 16.februar))
        ) // Touches prior periode
        assertNoErrors(inspektør)

        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.februar, 23.februar, 100))
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        assertNoErrors(inspektør)

        assertNotNull(inspektør.maksdato(1.vedtaksperiode))
        assertNotNull(inspektør.maksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
    }

    @Test
    fun `avvis sykmelding over 6 måneder gammel`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 15.februar, 100))
        person.håndter(sentSykmelding(Sykmeldingsperiode(1.januar, 15.januar, 100)))
        assertTrue(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.sykdomshistorikk.size)
    }

    @Test
    fun `Maksdato og antall gjenstående dager beregnes riktig når det er ferie sist i perioden`() {
        håndterSykmelding(Sykmeldingsperiode(22.juni(2020), 11.juli(2020), 100))
        håndterSøknad(Sykdom(22.juni(2020), 11.juli(2020), 100), Ferie(6.juli(2020), 11.juli(2020)))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(3.november(2019), 3.februar(2020), 2304, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(4.februar(2020), 28.februar(2020), 2304, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(29.februar(2020), 27.mars(2020), 2304, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(28.mars(2020), 26.april(2020), 2304, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(27.april(2020), 25.mai(2020), 2304, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(26.mai(2020), 21.juni(2020), 2304, 100, ORGNUMMER),
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(7.august(2019), 50005.månedlig, ORGNUMMER, true)
            )
        )
        håndterYtelser(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(7.august(2019), 7.august(2019), 2304, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(8.august(2019), 4.september(2019), 2304, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(5.september(2019), 20.september(2019), 2304, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(21.september(2019), 2.november(2019), 2304, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(3.november(2019), 3.februar(2020), 2304, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(4.februar(2020), 28.februar(2020), 2304, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(29.februar(2020), 27.mars(2020), 2304, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(28.mars(2020), 26.april(2020), 2304, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(27.april(2020), 25.mai(2020), 2304, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(26.mai(2020), 21.juni(2020), 2304, 100, ORGNUMMER),
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(7.august(2019), 50005.månedlig, ORGNUMMER, true)
            )
        )
        håndterSimulering(1.vedtaksperiode)
        assertEquals(10, inspektør.gjenståendeSykedager(1.vedtaksperiode))
        assertEquals(24.juli(2020), inspektør.maksdato(1.vedtaksperiode))
    }

    @Disabled
    @Test
    fun `Perioder hvor søknaden til vedtaksperiode 1 har dannet gap (friskmeldt) skal regnes som gap til påfølgende vedtaksperiode`() {
        // Første periode slutter på arbeiddager, og neste periode blir feilaktig bli markert som en forlengelse
        // Dette skyldes for at vi ikke sjekker for følgende arbeidsdager/ferie i slutten av forrige periode (som gjør at det egentlig skal være gap)
        håndterSykmelding(Sykmeldingsperiode(9.juni, 30.juni, 100))
        håndterSøknad(Sykdom(9.juni, 30.juni, 100), Arbeid(20.juni, 30.juni))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(3.juni, 8.juni, 15000, 100, ORGNUMMER)
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(3.juni, 8.juni, 15000, 100, ORGNUMMER)
        )
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(1.juli, 31.juli, 100))
        håndterSøknad(Sykdom(1.juli, 31.juli, 100))
        håndterYtelser(
            2.vedtaksperiode,
            RefusjonTilArbeidsgiver(3.juni, 8.juni, 15000, 100, ORGNUMMER)
        )

        assertNoErrors(inspektør)
        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_INNTEKTSMELDING_FERDIG_GAP
        )
    }

    @Disabled("Foreløpig usikkert hvordan vi skal håndere sammenhengende perioder fra IT, og hva vi skal gjøre med perioder som er helt utenfor alle sammenhengende perioder")
    @Test
    fun `Perioder hvor søknaden til vedtaksperiode 1 har dannet gap (friskmeldt) for hele perioden skal regnes som gap til påfølgende vedtaksperiode`() {
        håndterSykmelding(Sykmeldingsperiode(9.juni, 30.juni, 100))
        håndterSøknad(Sykdom(9.juni, 30.juni, 100), Arbeid(9.juni, 30.juni))
        håndterYtelser(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(3.juni, 8.juni, 15000, 100, ORGNUMMER)
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(3.juni, 8.juni, 15000, 100, ORGNUMMER)
        )

        håndterSykmelding(Sykmeldingsperiode(1.juli, 31.juli, 100))
        håndterSøknad(Sykdom(1.juli, 31.juli, 100))
        håndterYtelser(
            2.vedtaksperiode,
            RefusjonTilArbeidsgiver(3.juni, 8.juni, 15000, 100, ORGNUMMER)
        )

        assertNoErrors(inspektør)
        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )

        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_INNTEKTSMELDING_FERDIG_GAP
        )
    }

    @Test
    fun `Perioder hvor vedtaksperiode 1 avsluttes med ferie skal ikke regnes som gap til påfølgende vedtaksperiode`() {
        // Første periode slutter på arbeiddager, og neste periode blir feilaktig bli markert som en forlengelse
        // Dette skyldes for at vi ikke sjekker for følgende arbeidsdager/ferie i slutten av forrige periode (som gjør at det egentlig skal være gap)
        håndterSykmelding(Sykmeldingsperiode(9.juni, 30.juni, 100))
        håndterSøknad(Sykdom(9.juni, 30.juni, 100), Ferie(28.juni, 30.juni))
        håndterUtbetalingshistorikk(1.vedtaksperiode, RefusjonTilArbeidsgiver(3.juni, 8.juni, 15000, 100, ORGNUMMER))
        håndterYtelser(1.vedtaksperiode, RefusjonTilArbeidsgiver(3.juni, 8.juni, 15000, 100, ORGNUMMER))

        håndterSykmelding(Sykmeldingsperiode(1.juli, 31.juli, 100))
        håndterSøknad(Sykdom(1.juli, 31.juli, 100))

        assertNoErrors(inspektør)
        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )

        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        )
    }

    @Disabled("Håndtering av overlapp skal medføre at overlappende sykmeldinger blir delt opp i egne perioder")
    @Test
    fun `Overlapp-scenario fra prod`() {
        håndterSykmelding(Sykmeldingsperiode(27.mai(2020), 14.juni(2020), 100))
        håndterSøknad(Sykdom(27.mai(2020), 14.juni(2020), 100))
        håndterYtelser(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.mai(2020), 26.mai(2020), 2000, 100, ORGNUMMER)
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        håndterPåminnelse(
            1.vedtaksperiode,
            AVVENTER_SIMULERING,
            LocalDateTime.now().minusDays(200)
        ) // <-- TIL_INFOTRYGD
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))

        håndterSykmelding(Sykmeldingsperiode(29.mai(2020), 18.juni(2020), 100))
        håndterSykmelding(Sykmeldingsperiode(15.juni(2020), 3.juli(2020), 100))

        // Håndtering av overlapp skal medføre at sykmelding fra 15.juni til 3.juli blir en ny vedtaksperiode og at den foregående sykmeldingen blir trimmet
        // Blir p.t. ignorert
        assertEquals(3, inspektør.vedtaksperiodeTeller)

        håndterSøknad(Sykdom(15.juni(2020), 3.juli(2020), 100))

        håndterYtelser(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.mai(2020), 26.mai(2020), 2000, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(27.mai(2020), 14.juni(2020), 2000, 100, ORGNUMMER)
        )

        håndterSykmelding(Sykmeldingsperiode(4.juli(2020), 20.juli(2020), 100))
        håndterSøknad(Sykdom(4.juli(2020), 20.juli(2020), 100))

        håndterYtelser(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.mai(2020), 26.mai(2020), 2000, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(27.mai(2020), 14.juni(2020), 2000, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(15.juni(2020), 3.juli(2020), 2000, 100, ORGNUMMER)
        )

        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)

        håndterYtelser(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.mai(2020), 26.mai(2020), 2000, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(27.mai(2020), 14.juni(2020), 2000, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(15.juni(2020), 3.juli(2020), 2000, 100, ORGNUMMER)
        )
    }

    @Test
    fun `sykdomstidslinje tømmes helt når perioder blir forkastet, dersom det ikke finnes noen perioder igjen`() {
        håndterSykmelding(Sykmeldingsperiode(8.juni(2020), 21.juni(2020), 100))
        håndterSøknad(Sykdom(8.juni(2020), 21.juni(2020), 100))
        håndterInntektsmelding(listOf(Periode(8.juni(2020), 23.juni(2020))), førsteFraværsdag = 8.juni(2020))

        håndterPåminnelse(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_GAP, LocalDateTime.now().minusDays(200))
        assertEquals(0, inspektør.sykdomshistorikk.sykdomstidslinje().length())
    }

    @Test
    fun `gjentatt annullering av periode fører ikke til duplikate innslag i utbetalinger`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(5.februar, 10.februar, 100))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(5.februar, 10.februar, 100))
        håndterAnnullerUtbetaling(fagsystemId = inspektør.utbetalinger[0].arbeidsgiverOppdrag().fagsystemId())
        håndterAnnullerUtbetaling(fagsystemId = inspektør.utbetalinger[0].arbeidsgiverOppdrag().fagsystemId())
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT, annullert = true)
        håndterAnnullerUtbetaling(fagsystemId = inspektør.utbetalinger[0].arbeidsgiverOppdrag().fagsystemId())

        assertTrue(inspektør.utbetalinger[0].erUtbetalt())
        assertTrue(inspektør.utbetalinger[1].erAnnullering())

        assertEquals(2, inspektør.utbetalinger.size)
    }

    @Test
    fun `utbetalteventet får med seg arbeidsdager`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100), Arbeid(23.januar, 26.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertEquals(
            4, observatør.utbetaltEventer[0].ikkeUtbetalteDager
                .filter { it.type == PersonObserver.UtbetaltEvent.IkkeUtbetaltDag.Type.Arbeidsdag }.size
        )
    }

    @Test
    fun `inntekter på flere arbeidsgivere oppretter arbeidsgivere med tom sykdomshistorikk`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
                1.januar(2017) til 1.januar(2017) inntekter {
                    "123412344" inntekt 1
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)   // No history
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `håndterer fler arbeidsgivere så lenge kun én har sykdomshistorikk`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
                1.januar(2017) til 1.januar(2017) inntekter {
                    "123412344" inntekt 1
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)   // No history
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        håndterSykmelding(Sykmeldingsperiode(1.mars, 28.mars, 100))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.mars, 28.mars, 100))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.mars, 16.mars)))
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            AVVENTER_UFERDIG_GAP
        )
    }

    @Test
    fun `utbetalt event etter krysset maksdato inneholder kun utbetalte dager fra forrige periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2018), 31.januar(2018), 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar(2018), 16.januar(2018))))
        håndterSøknad(Sykdom(1.januar(2018), 31.januar(2018), gradFraSykmelding = 100), sendtTilNav = 1.januar(2018))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        repeat(11) {
            val fom = 1.januar(2018).plusMonths(it + 1L).with(firstDayOfMonth())
            val tom = fom.with(lastDayOfMonth())

            håndterSykmelding(Sykmeldingsperiode(fom, tom, 100))
            håndterSøknad(Sykdom(fom, tom, gradFraSykmelding = 100), sendtTilNav = tom)
            håndterYtelser((it + 2).vedtaksperiode)   // No history
            håndterSimulering((it + 2).vedtaksperiode)
            håndterUtbetalingsgodkjenning((it + 2).vedtaksperiode, true)
            håndterUtbetalt((it + 2).vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        }

        håndterSykmelding(Sykmeldingsperiode(1.januar(2019), 31.januar(2019), 100))
        håndterSøknad(Sykdom(1.januar(2019), 31.januar(2019), gradFraSykmelding = 100), sendtTilNav = 31.januar(2019))
        håndterYtelser(13.vedtaksperiode)   // No history
        håndterUtbetalingsgodkjenning(13.vedtaksperiode, true)

        val utbetaltEvent = observatør.utbetaltEventer.last()

        assertEquals(13, observatør.utbetaltEventer.size)
        assertEquals(30.desember, observatør.utbetaltEventer[11].oppdrag.first().utbetalingslinjer.first().tom)
        assertEquals(30.desember, utbetaltEvent.oppdrag.first().utbetalingslinjer.first().tom)
    }

    @Test
    fun `Skal ikke g reguleres hvis virkning fra før virkningsdato`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020), 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar(2020), 16.januar(2020))))
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), gradFraSykmelding = 100), sendtTilNav = 1.januar(2020))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterGrunnbeløpsregulering(gyldighetsdato = 20.juli(2020))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `overlappende inntektsmelding på grunn av ferie`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100))

        håndterPåminnelse(1.vedtaksperiode, AVVENTER_GAP, LocalDateTime.now().minusYears(1))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 16.februar, 100))
        håndterSøknad(Sykdom(1.februar, 16.februar, 100))
        håndterInntektsmelding(listOf(1.januar til 16.januar), ferieperioder = listOf(5.februar til 6.februar))

        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP)
    }

    @Test
    fun `Går ikke videre fra AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE hvis forrige periode ikke er ferdig behandlet`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100))

        håndterSykmelding(Sykmeldingsperiode(20.februar, 28.februar, 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(20.februar, 28.februar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100))

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            TIL_INFOTRYGD
        )
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        )
    }

    @Test
    fun `Går videre fra AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE hvis en gammel periode er i AVSLUTTET_UTEN_UTBETALING`() {
        håndterSykmelding(Sykmeldingsperiode(20.november(2017), 12.desember(2017), 100))
        håndterSøknad(Sykdom(20.november(2017), 12.desember(2017), 100))

        håndterSykmelding(Sykmeldingsperiode(1.januar, 12.januar, 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(1.januar, 12.januar, 100))

        håndterSykmelding(Sykmeldingsperiode(20.februar, 28.februar, 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(20.februar, 28.februar, 100))
        håndterInntektsmelding(listOf(Periode(20.februar, 8.mars)), 20.februar)
        håndterVilkårsgrunnlag(3.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100))

        håndterSykmelding(Sykmeldingsperiode(20.november(2017), 12.desember(2017), 100))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            TIL_INFOTRYGD
        )
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        assertTilstander(
            4.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `uavsluttet kort søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 12.januar, 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(1.januar, 12.januar, 100))

        håndterSykmelding(Sykmeldingsperiode(20.februar, 28.februar, 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(20.februar, 28.februar, 100))

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100))

        håndterSykmelding(Sykmeldingsperiode(5.april, 14.april, 100))
        håndterSøknad(Sykdom(5.april, 14.april, 100))

        håndterInntektsmelding(listOf(Periode(20.februar, 8.mars)), 20.februar)
        håndterVilkårsgrunnlag(2.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK
        )
        assertTilstander(4.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP)
    }

    @Test
    fun `Når inntektsmeldingens første fraværsdag er midt i en vedtaksperiode lagres inntekten på vedtaksperiodens skjæringstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 12.februar, 100))
        håndterSøknad(Sykdom(1.februar, 12.februar, 100))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.november(2017), 20.november(2017), 200, 100, ORGNUMMER),
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(1.november(2017), 20000.månedlig, ORGNUMMER, true)
            )
        )
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), 3.februar)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_INNTEKTSMELDING_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP
        )
        assertInntektForDato(INNTEKT, 1.januar, inspektør)
    }

    @Test
    fun `Opphør av naturalytelser kaster periode til infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar, harOpphørAvNaturalytelser = true)

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Historisk utbetaling til bruker skal ikke bli med i utbetalingstidslinje for arbeidsgiver`() {
        håndterSykmelding(Sykmeldingsperiode(24.juni(2020), 30.juni(2020), 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(24.juni(2020), 30.juni(2020), 100))

        håndterSykmelding(Sykmeldingsperiode(1.juli(2020), 9.juli(2020), 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(1.juli(2020), 9.juli(2020), 100))

        håndterSykmelding(Sykmeldingsperiode(16.oktober(2020), 23.oktober(2020), 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(16.oktober(2020), 23.oktober(2020), 100))

        håndterSykmelding(Sykmeldingsperiode(28.oktober(2020), 3.november(2020), 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(28.oktober(2020), 3.november(2020), 100))

        håndterSykmelding(Sykmeldingsperiode(4.november(2020), 13.november(2020), 100))
        håndterSøknad(Sykdom(4.november(2020), 13.november(2020), 100))
        håndterInntektsmelding(listOf(Periode(16.oktober(2020), 23.oktober(2020)), Periode(28.oktober(2020), 4.november(2020))), 28.oktober(2020))

        håndterVilkårsgrunnlag(4.vedtaksperiode)

        håndterYtelser(
            5.vedtaksperiode,
            Utbetalingshistorikk.Periode.Utbetaling(9.mai(2018), 31.mai(2018), 1621, 100, "0"),
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(9.mai(2018), 40000.månedlig, "0", true)
            )
        )
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING)
        assertTilstander(4.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD, AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING)
        assertTilstander(5.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, AVVENTER_UFERDIG_FORLENGELSE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)

        assertEquals(24.juni(2020), inspektør.utbetalinger.first().utbetalingstidslinje().førsteDato())

    }

    @Test
    fun `Ny utbetalingsbuilder feiler ikke når sykdomshistorikk inneholder arbeidsgiverperiode som hører til infotrygdperiode`() {
        Toggles.nyInntekt = true
        håndterSykmelding(Sykmeldingsperiode(28.september(2020), 14.oktober(2020), 30))
        håndterSøknad(Sykdom(28.september(2020), 14.oktober(2020), 30))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(3.september(2020), 7.september(2020), 200, 30, ORGNUMMER),
            RefusjonTilArbeidsgiver(8.september(2020), 27.september(2020), 200, 30, ORGNUMMER),
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(3.september(2020), 20000.månedlig, ORGNUMMER, true)
            )
        )
        håndterYtelser(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(3.september(2020), 7.september(2020), 200, 30, ORGNUMMER),
            RefusjonTilArbeidsgiver(8.september(2020), 27.september(2020), 200, 30, ORGNUMMER),
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(3.september(2020), 20000.månedlig, ORGNUMMER, true)
            )
        )
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(15.oktober(2020), 30.oktober(2020), 30))
        håndterSøknad(Sykdom(15.oktober(2020), 30.oktober(2020), 30))
        håndterYtelser(
            2.vedtaksperiode,
            RefusjonTilArbeidsgiver(3.september(2020), 7.september(2020), 200, 30, ORGNUMMER),
            RefusjonTilArbeidsgiver(8.september(2020), 27.september(2020), 200, 30, ORGNUMMER),
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(3.september(2020), 20000.månedlig, ORGNUMMER, true)
            )
        )
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(11.november(2020), 11.november(2020), 100))
        håndterSøknad(Sykdom(11.november(2020), 11.november(2020), 100))
        håndterInntektsmelding(listOf(Periode(18.august(2020), 2.september(2020))), 11.november(2020))
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        assertDoesNotThrow {
            håndterYtelser(
                3.vedtaksperiode,
                RefusjonTilArbeidsgiver(3.september(2020), 7.september(2020), 200, 30, ORGNUMMER),
                RefusjonTilArbeidsgiver(8.september(2020), 27.september(2020), 200, 30, ORGNUMMER),
                inntektshistorikk = listOf(
                    Utbetalingshistorikk.Inntektsopplysning(3.september(2020), 20000.månedlig, ORGNUMMER, true)
                )
            )
        }
        Toggles.nyInntekt = false
    }

    @Test
    fun `inntektsmelding uten relevant inntekt (fordi perioden er i agp) flytter perioden til ferdig-tilstand`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(1.januar, 5.januar, 100), orgnummer = ORGNUMMER)

        håndterSykmelding(Sykmeldingsperiode(9.januar, 10.januar, 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(9.januar, 10.januar, 100), orgnummer = ORGNUMMER)

        håndterSykmelding(Sykmeldingsperiode(12.januar, 24.januar, 100))
        håndterSøknad(Sykdom(12.januar, 24.januar, 100))

        håndterInntektsmelding(listOf(Periode(1.januar, 5.januar), Periode(9.januar, 10.januar), Periode(12.januar, 20.januar)), 12.januar)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, AVVENTER_VILKÅRSPRØVING_GAP)
    }

    @Test
    fun `Ny utbetalingsbuilder feiler ikke når IT har kontinuerlig sykdom på tvers av arbeidsgivere`() {
        Toggles.nyInntekt = true
        håndterSykmelding(Sykmeldingsperiode(28.september(2020), 18.oktober(2020), 100))
        håndterSøknad(Sykdom(28.september(2020), 18.oktober(2020), 100))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.januar(2019), 22.februar(2019), 200, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(23.mars(2020), 5.juli(2020), 200, 100, ORGNUMMER),
            Utbetalingshistorikk.Periode.Ferie(6.juli(2020), 27.september(2020)),
            RefusjonTilArbeidsgiver(5.desember(2018), 31.desember(2018), 200, 100, "123455433"),
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(1.januar(2019), 20000.månedlig, ORGNUMMER, true),
                Utbetalingshistorikk.Inntektsopplysning(23.mars(2020), 21000.månedlig, ORGNUMMER, true),
                Utbetalingshistorikk.Inntektsopplysning(5.desember(2018), 18000.månedlig, "123455433", true)
            )
        )
        assertDoesNotThrow {
            håndterYtelser(
                1.vedtaksperiode,
                RefusjonTilArbeidsgiver(1.januar(2019), 22.februar(2019), 200, 100, ORGNUMMER),
                RefusjonTilArbeidsgiver(23.mars(2020), 5.juli(2020), 200, 100, ORGNUMMER),
                Utbetalingshistorikk.Periode.Ferie(6.juli(2020), 27.september(2020)),
                RefusjonTilArbeidsgiver(5.desember(2018), 31.desember(2018), 200, 100, "123455433"),
                inntektshistorikk = listOf(
                    Utbetalingshistorikk.Inntektsopplysning(1.januar(2019), 20000.månedlig, ORGNUMMER, true),
                    Utbetalingshistorikk.Inntektsopplysning(23.mars(2020), 21000.månedlig, ORGNUMMER, true),
                    Utbetalingshistorikk.Inntektsopplysning(5.desember(2018), 18000.månedlig, "123455433", true)
                )
            )
        }
        Toggles.nyInntekt = false
    }

    @Test
    fun `beregner ikke skjæringstidspunktet på nytt for å finne sykepengegrunnlag`() {
        Toggles.nyInntekt = true
        håndterSykmelding(Sykmeldingsperiode(1.november(2020), 10.november(2020), 100))
        håndterSøknad(Sykdom(1.november(2020), 10.november(2020), gradFraSykmelding = 100))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(20.oktober(2020), 31.oktober(2020), 1000, 100, ORGNUMMER),
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(20.oktober(2020), 22000.månedlig, ORGNUMMER, true)
            )
        )
        håndterYtelser(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(20.oktober(2020), 31.oktober(2020), 1000, 100, ORGNUMMER),
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(20.oktober(2020), 22000.månedlig, ORGNUMMER, true)
            )
        )
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        val utbetaltEvent = observatør.utbetaltEventer.last()

        assertEquals(264000.0, utbetaltEvent.sykepengegrunnlag)
        Toggles.nyInntekt = false
    }
}
