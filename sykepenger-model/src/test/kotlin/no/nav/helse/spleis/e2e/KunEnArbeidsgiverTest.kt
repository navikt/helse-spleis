package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.*
import no.nav.helse.inspectors.GrunnlagsdataInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters.firstDayOfMonth
import java.time.temporal.TemporalAdjusters.lastDayOfMonth
import java.util.*

internal class KunEnArbeidsgiverTest : AbstractEndToEndTest() {

    @Test
    fun `ingen historie med inntektsmelding først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        inspektør.sykdomstidslinje.inspektør.låstePerioder.also {
            assertEquals(0, it.size)
        }
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)
        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 3.januar, inspektør = it)
            assertEquals(3, it.sykdomshistorikk.size)
            assertEquals(18, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(6, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTrue(1.vedtaksperiode(ORGNUMMER) in observatør.utbetalteVedtaksperioder)
        inspektør.sykdomstidslinje.inspektør.låstePerioder.also {
            assertEquals(1, it.size)
            assertEquals(Periode(3.januar, 26.januar), it.first())
        }
    }

    @Test
    fun `ingen historie med søknad til arbeidsgiver først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 8.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(3.januar, 8.januar, 100.prosent))
        assertNoWarnings(inspektør)
        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 3.januar, inspektør = it)
            assertEquals(3, it.sykdomshistorikk.size)
            assertEquals(4, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(2, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
        }
        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )
    }

    @Test
    fun `ingen historie med to søknader til arbeidsgiver før inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 5.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(3.januar, 5.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(8.januar, 10.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(8.januar, 10.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(11.januar, 22.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 22.januar, 100.prosent))

        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode, INNTEKT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 3.januar, inspektør = it)
            assertEquals(7, it.sykdomshistorikk.size)
            assertEquals(4, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
            assertEquals(14, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `ingen historie med to søknader (med gap mellom) til arbeidsgiver først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 4.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(3.januar, 4.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(8.januar, 10.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(8.januar, 10.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(11.januar, 22.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 22.januar, 100.prosent))

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(3.januar, 4.januar), Periode(8.januar, 21.januar)),
            førsteFraværsdag = 8.januar
        )

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 8.januar, inspektør = it)
            assertEquals(7, it.sykdomshistorikk.size)
            assertEquals(4, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
            assertEquals(13, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `ingen historie med inntektsmelding, så søknad til arbeidsgiver`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 8.januar, 100.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        assertNoWarnings(inspektør)
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(3.januar, 8.januar, 100.prosent))
        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 3.januar, inspektør = it)
            assertEquals(3, it.sykdomshistorikk.size)
            assertEquals(4, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(2, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )
    }

    @Test
    fun `ingen historie med Søknad først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)
        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 3.januar, inspektør = it)
            assertEquals(3, it.sykdomshistorikk.size)
            assertEquals(18, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(6, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTrue(1.vedtaksperiode(ORGNUMMER) in observatør.utbetalteVedtaksperioder)
    }

    @Test
    fun `Søknad med utenlandsopphold og studieopphold gir warning`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSøknadMedValidering(
            1.vedtaksperiode,
            Sykdom(3.januar, 26.januar, 100.prosent),
            Utlandsopphold(11.januar, 15.januar),
            Utdanning(16.januar, 18.januar)
        )
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)))

        inspektør.also {
            assertWarnings(it)
            assertWarn("Utdanning oppgitt i perioden i søknaden.", it.personLogg)
            assertWarn("Utenlandsopphold oppgitt i perioden i søknaden.", it.personLogg)
            assertEquals(3, it.sykdomshistorikk.size)
            assertEquals(18, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(6, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
        }
    }

    @Test
    fun `søknad sendt etter 3 mnd`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent), sendtTilNav = 1.mai)
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 3.januar, inspektør = it)
            assertEquals(3, it.sykdomshistorikk.size)
            assertNull(it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(6, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
            assertDoesNotThrow { it.arbeidsgiver.nåværendeTidslinje() }
            assertTrue(it.utbetalingslinjer(0).isEmpty())
            it.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(6, tidslinjeInspektør.foreldetDagTeller)
                assertEquals(2, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
            }
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, AVSLUTTET
        )
    }

    @Test
    fun `gap-historie før inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.desember(2017), 15.desember(2017), 100.prosent, 15000.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER.toString(), 1.desember(2017), INNTEKT, true))
        )
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)
        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 3.januar, inspektør = it)
            assertEquals(3, it.sykdomshistorikk.size)
            assertEquals(18, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(6, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
        }
        assertNotNull(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTrue(1.vedtaksperiode(ORGNUMMER) in observatør.utbetalteVedtaksperioder)
    }

    @Test
    fun `gap-historie uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.desember(2017), 16.desember(2017), 100.prosent, 15000.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER.toString(), 1.desember(2017), INNTEKT, true))
        )
        inspektør.also {
            assertNoErrors(it)
            assertFalse(it.personLogg.hasWarningsOrWorse())
            assertActivities(it)
            assertInntektForDato(null, 2.januar, inspektør = it)
            assertEquals(2, it.sykdomshistorikk.size)
            assertEquals(18, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(6, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP
        )
    }

    // TODO: er denne testen relevant? Vi sender ikke med arbeidsgiverinfo for forlengelser
    @Test
    fun `fremtidig test av utbetalingstidslinjeBuilder, historikk fra flere arbeidsgivere`() {
        håndterInntektsmelding(listOf(11.juli(2020) til 26.juli(2020)), refusjon = Refusjon(2077.daglig, null))
        håndterSykmelding(Sykmeldingsperiode(20.september(2020), 19.oktober(2020), 100.prosent))
        håndterSøknadMedValidering(
            1.vedtaksperiode,
            Sykdom(20.september(2020), 19.oktober(2020), 100.prosent)
        )

        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 27.juli(2020), 20.august(2020), 100.prosent, 2077.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 21.august(2020), 19.september(2020), 100.prosent, 2077.daglig),
            ArbeidsgiverUtbetalingsperiode("12345789", 21.august(2019), 19.september(2019), 100.prosent, 1043.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER.toString(), 27.juli(2020), 45000.månedlig, true),
                Inntektsopplysning("12345789", 21.august(2019), 22600.månedlig, true)
            )
        )
        håndterYtelser(1.vedtaksperiode)

        inspektør.also {
            assertNoErrors(1.vedtaksperiode)
            assertNoWarnings(1.vedtaksperiode)
            assertActivities(it)
            assertInntektForDato(45000.månedlig, 27.juli(2020), inspektør = it)
            assertEquals(2, it.sykdomshistorikk.size)
            assertEquals(21, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(9, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
            assertEquals(43617, it.nettoBeløp[0])
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `setter riktig inntekt i utbetalingstidslinjebuilder`() {
        håndterSykmelding(Sykmeldingsperiode(21.september(2020), 10.oktober(2020), 100.prosent))
        håndterSøknad(Sykdom(21.september(2020), 10.oktober(2020), 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(
                Periode(4.september(2020), 19.september(2020))
            ),
            førsteFraværsdag = 21.september(2020)
        ) // 20. september er en søndag
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.september(2019) til 1.august(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 21.september(2020), inspektør = it)
            assertEquals(21.september(2020), it.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(21465, it.nettoBeløp[0])
        }
    }


    @Test
    fun `inntektsmeldingen padder ikke senere vedtaksperioder med arbeidsdager`() {
        håndterSykmelding(Sykmeldingsperiode(4.januar, 22.januar, 100.prosent))
        håndterSøknad(Sykdom(4.januar, 22.januar, 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(4.januar, 19.januar)),
            førsteFraværsdag = 4.januar
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(24.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(24.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(4.januar, 19.januar)),
            førsteFraværsdag = 24.januar
        )
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertInntektForDato(INNTEKT, 4.januar, inspektør = it)
            assertInntektForDato(INNTEKT, 24.januar, inspektør = it)
            assertEquals(4.januar, it.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(24.januar, it.skjæringstidspunkt(2.vedtaksperiode))
            assertEquals(19, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(8, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
            assertEquals(1, it.sykdomstidslinje.inspektør.dagteller[Dag.UkjentDag::class])
        }
    }

    @Test
    fun `ingen nav utbetaling kreves, blir automatisk behandlet og avsluttet`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 5.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 5.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertFalse(hendelselogg.hasErrorsOrWorse())

        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), 3.januar)
        assertFalse(person.aktivitetslogg.logg(inspektør.vedtaksperioder(1.vedtaksperiode)).hasWarningsOrWorse())
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )
    }

    @Test
    fun `perioden avsluttes ikke automatisk hvis warnings`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 19.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 18.januar, 100.prosent), Ferie(19.januar, 19.januar))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertFalse(hendelselogg.hasErrorsOrWorse())

        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), 3.januar)
        håndterYtelser(1.vedtaksperiode, arbeidsavklaringspenger = listOf(3.januar.minusDays(60) til 5.januar.minusDays(60)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode, arbeidsavklaringspenger = listOf(3.januar.minusDays(60) til 5.januar.minusDays(60)))

        assertTrue(person.aktivitetslogg.logg(inspektør.vedtaksperioder(1.vedtaksperiode)).hasWarningsOrWorse())
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING
        )
    }

    @Test
    fun `To perioder med opphold`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        assertActivities(inspektør)
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.februar, 16.februar)))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.februar, 23.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
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
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Kiler tilstand i uferdig venter for inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.februar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        assertActivities(inspektør)
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.februar, 16.februar)))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
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
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Kilt etter søknad og inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.februar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.februar, 16.februar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        assertActivities(inspektør)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
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
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Kilt etter inntektsmelding og søknad`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.februar, 16.februar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.februar, 23.februar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        assertActivities(inspektør)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
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
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Kilt etter inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.februar, 16.februar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        assertActivities(inspektør)
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.februar, 23.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
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
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `forlengelse av infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 23.februar, 100.prosent))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.januar, 31.januar, 100.prosent, INNTEKT)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER.toString(), 1.januar(2018), INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING
        )
    }

    @Test
    fun `Sammenblandede hendelser fra forskjellige perioder med søknad først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.februar, 23.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.februar, 16.februar)))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        forventetEndringTeller++
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)
        assertActivities(inspektør)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)
        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertNotNull(it.sisteMaksdato(1.vedtaksperiode))
            assertNotNull(it.sisteMaksdato(2.vedtaksperiode))
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
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
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
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `To forlengelser som forlenger utbetaling fra infotrygd skal ha samme maksdato`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 31.januar, 100.prosent))
        val historie = ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.januar, 2.januar, 100.prosent, INNTEKT)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER.toString(), 1.januar(2018), INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historie, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        forventetEndringTeller++
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 23.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        assertEquals(inspektør.sisteMaksdato(1.vedtaksperiode), inspektør.sisteMaksdato(2.vedtaksperiode))
    }

    @Test
    fun `fortsettelse av Infotrygd-perioder skal ikke generere utbetalingslinjer for Infotrygd-periode`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 31.januar, 100.prosent))
        val historie = ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.januar, 2.januar, 100.prosent, INNTEKT)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER.toString(), 1.januar, INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historie, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)

        inspektør.also {
            assertEquals(3.januar, it.arbeidsgiverOppdrag[0][0].fom)
        }
    }

    @Test
    fun `To tilstøtende perioder søknad først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
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
        håndterSykmelding(Sykmeldingsperiode(3.januar, 7.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 23.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 7.januar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(8.januar, 23.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Bytter fra forlengelse til gap-state når tidligere tilstøtende periodes søknad avsluttes med arbeidsdager - andre periode har mottatt sykmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 22.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100.prosent), Arbeid(18.januar, 21.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
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
            MOTTATT_SYKMELDING_FERDIG_GAP
        )
        val grunnlagsdataInspektør = GrunnlagsdataInspektør(inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!)
        assertEquals(INNTEKT, grunnlagsdataInspektør.sammenligningsgrunnlag)
        assertNull(inspektør.vilkårsgrunnlag(2.vedtaksperiode))
    }

    @Test
    fun `Bytter fra forlengelse til gap-state når tidligere tilstøtende periodes søknad avsluttes med arbeidsdager - andre periode har mottatt søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 22.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100.prosent), Arbeid(18.januar, 21.januar))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(22.januar, 22.februar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
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
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP
        )
    }

    @Test
    fun `Bytter fra forlengelse til gap-state når tidligere tilstøtende periodes søknad avsluttes med arbeidsdager - andre periode har mottatt inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 22.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100.prosent), Arbeid(18.januar, 21.januar))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.januar, 16.januar)), 22.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
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
            AVVENTER_SØKNAD_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_FERDIG_GAP
        )
    }

    @Test
    fun `Bytter fra forlengelse til gap-state når tidligere tilstøtende periodes søknad avsluttes med arbeidsdager - andre periode har mottatt søknad og inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 22.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100.prosent), Arbeid(18.januar, 21.januar))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(22.januar, 22.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.januar, 16.januar)), 22.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
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
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `Sender ikke avsluttet periode til infotrygd når man mottar en ugyldig søknad i etterkant`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        håndterSøknad(Sykdom(1.januar, 21.januar, 100.prosent), Permisjon(21.januar, 21.januar))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
    }

    @Test
    fun `Sender ikke periode som allerede har behandlet inntektsmelding til infotrygd når man mottar en ny ugyldig inntektsmelding i etterkant`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(INNTEKT, 15.januar, emptyList())
        )
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
    }

    @Test
    fun `søknad til arbeidsgiver etter inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 7.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 23.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(8.januar, 23.februar, 100.prosent))

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertEquals(3, it.sykdomstidslinje.inspektør.dagteller[Dag.Arbeidsgiverdag::class])
            assertEquals(2, it.sykdomstidslinje.inspektør.dagteller[Dag.ArbeidsgiverHelgedag::class])
            assertEquals(35, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(12, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
        }
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_UFERDIG_FORLENGELSE,
            AVSLUTTET_UTEN_UTBETALING
        )
    }

    @Test
    fun `Venter på å bli kilt etter inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 7.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 23.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 7.januar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(8.januar, 23.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Fortsetter før andre søknad`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            arbeidsgiverperioder = listOf(Periode(3.januar, 18.januar)),
            førsteFraværsdag = 3.januar
        )
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
            assertEquals(3.januar, it.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(3.januar, it.skjæringstidspunkt(2.vedtaksperiode))
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
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
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
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
    fun `To tilstøtende perioder der den første er i utbetaling feilet`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AVVIST)

        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))

        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            UTBETALING_FEILET
        )
        assertTilstander(
            2.vedtaksperiode,
            START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        )
    }

    @Test
    fun `kiler bare andre periode og ikke tredje periode i en rekke`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 28.mars, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.februar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
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
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP
        )

        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP
        )
    }

    @Test
    fun `Sykmelding med gradering`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 50.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 50.prosent, 50.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING
        )
    }

    @Test
    fun `avvis sykmelding over 6 måneder gammel`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 15.februar, 100.prosent))
        person.håndter(sentSykmelding(Sykmeldingsperiode(1.januar, 15.januar, 100.prosent)))
        assertTrue(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.sykdomshistorikk.size)
    }

    @Test
    fun `Maksdato og antall gjenstående dager beregnes riktig når det er fravær sist i perioden`() {
        håndterSykmelding(Sykmeldingsperiode(22.juni(2020), 11.juli(2020), 100.prosent))
        håndterSøknad(Sykdom(22.juni(2020), 11.juli(2020), 100.prosent), Permisjon(4.juli(2020), 5.juli(2020)), Ferie(6.juli(2020), 11.juli(2020)))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 7.august(2019), 7.august(2019), 100.prosent, 2304.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 8.august(2019), 4.september(2019), 100.prosent, 2304.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 5.september(2019), 20.september(2019), 100.prosent, 2304.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 21.september(2019), 2.november(2019), 100.prosent, 2304.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 3.november(2019), 3.februar(2020), 100.prosent, 2304.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 4.februar(2020), 28.februar(2020), 100.prosent, 2304.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 29.februar(2020), 27.mars(2020), 100.prosent, 2304.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 28.mars(2020), 26.april(2020), 100.prosent, 2304.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 27.april(2020), 25.mai(2020), 100.prosent, 2304.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 26.mai(2020), 21.juni(2020), 100.prosent, 2304.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER.toString(), 7.august(2019), 50005.månedlig, true)
            )
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertEquals(10, inspektør.gjenståendeSykedager(1.vedtaksperiode))
        assertEquals(24.juli(2020), inspektør.sisteMaksdato(1.vedtaksperiode))
    }

    @Test
    fun `Perioder hvor søknaden til vedtaksperiode 1 har dannet gap (friskmeldt) skal regnes som gap til påfølgende vedtaksperiode`() {
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.mai, 31.mai, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER.toString(), 1.mai, INNTEKT, true))

        håndterSykmelding(Sykmeldingsperiode(1.juni, 30.juni, 100.prosent))
        håndterSøknad(Sykdom(1.juni, 30.juni, 100.prosent), Arbeid(20.juni, 30.juni))

        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(1.juli, 31.juli, 100.prosent))

        assertNoErrors(inspektør)
        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP
        )
    }

    @Test
    fun `Perioder hvor søknaden til vedtaksperiode 1 har dannet gap (friskmeldt) for hele perioden skal regnes som gap til påfølgende vedtaksperiode`() {
        håndterInntektsmelding(listOf(16.april til 30.april))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.mai, 31.mai, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER.toString(), 1.mai, INNTEKT, true))

        håndterSykmelding(Sykmeldingsperiode(1.juni, 30.juni, 100.prosent))
        håndterSøknad(Sykdom(1.juni, 30.juni, 100.prosent), Arbeid(1.juni, 30.juni))

        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)
        håndterYtelser()

        håndterSykmelding(Sykmeldingsperiode(1.juli, 31.juli, 100.prosent))

        assertNoWarnings(1.vedtaksperiode)
        assertNoErrors(1.vedtaksperiode)
        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING
        )

        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP
        )
    }

    @Test
    fun `Perioder hvor vedtaksperiode 1 avsluttes med ferie skal ikke regnes som gap til påfølgende vedtaksperiode`() {
        // Første periode slutter på arbeidsdager, og neste periode blir feilaktig markert som en forlengelse.
        // Dette skyldes for at vi ikke sjekker for følgende arbeidsdager/ferie i slutten av forrige periode (som gjør at det egentlig skal være gap)
        håndterSykmelding(Sykmeldingsperiode(9.juni, 30.juni, 100.prosent))
        håndterSøknad(Sykdom(9.juni, 30.juni, 100.prosent), Ferie(28.juni, 30.juni))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 3.juni, 8.juni, 100.prosent, 15000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER.toString(), 3.juni(2018), 15000.daglig, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.juli, 31.juli, 100.prosent))
        håndterSøknad(Sykdom(1.juli, 31.juli, 100.prosent))

        assertNoErrors(inspektør)
        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
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

    @Test
    fun `Perioder hvor vedtaksperiode 1 avsluttes med permisjon skal ikke regnes som gap til påfølgende vedtaksperiode`() {
        // Første periode slutter på arbeidsdager, og neste periode blir feilaktig markert som en forlengelse.
        // Dette skyldes for at vi ikke sjekker for følgende arbeidsdager/permisjon i slutten av forrige periode (som gjør at det egentlig skal være gap)
        håndterSykmelding(Sykmeldingsperiode(9.juni, 30.juni, 100.prosent))
        håndterSøknad(Sykdom(9.juni, 30.juni, 100.prosent), Permisjon(28.juni, 30.juni))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 3.juni, 8.juni, 100.prosent, 15000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER.toString(), 3.juni(2018), 15000.daglig, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.juli, 31.juli, 100.prosent))
        håndterSøknad(Sykdom(1.juli, 31.juli, 100.prosent))

        assertNoErrors(inspektør)
        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
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
        håndterSykmelding(Sykmeldingsperiode(27.mai(2020), 14.juni(2020), 100.prosent))
        håndterSøknad(Sykdom(27.mai(2020), 14.juni(2020), 100.prosent))
        håndterYtelser(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.mai(2020), 26.mai(2020), 100.prosent, 2000.daglig)
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

        håndterSykmelding(Sykmeldingsperiode(29.mai(2020), 18.juni(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(15.juni(2020), 3.juli(2020), 100.prosent))

        // Håndtering av overlapp skal medføre at sykmelding fra 15.juni til 3.juli blir en ny vedtaksperiode og at den foregående sykmeldingen blir trimmet
        // Blir p.t. ignorert
        assertEquals(3, inspektør.vedtaksperiodeTeller)

        håndterSøknad(Sykdom(15.juni(2020), 3.juli(2020), 100.prosent))

        håndterYtelser(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.mai(2020), 26.mai(2020), 100.prosent, 2000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 27.mai(2020), 14.juni(2020), 100.prosent, 2000.daglig)
        )

        håndterSykmelding(Sykmeldingsperiode(4.juli(2020), 20.juli(2020), 100.prosent))
        håndterSøknad(Sykdom(4.juli(2020), 20.juli(2020), 100.prosent))

        håndterYtelser(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.mai(2020), 26.mai(2020), 100.prosent, 2000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 27.mai(2020), 14.juni(2020), 100.prosent, 2000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 15.juni(2020), 3.juli(2020), 100.prosent, 2000.daglig)
        )

        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)

        håndterYtelser(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.mai(2020), 26.mai(2020), 100.prosent, 2000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 27.mai(2020), 14.juni(2020), 100.prosent, 2000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 15.juni(2020), 3.juli(2020), 100.prosent, 2000.daglig)
        )
    }

    @Test
    fun `sykdomstidslinje tømmes helt når perioder blir forkastet, dersom det ikke finnes noen perioder igjen`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 21.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar)
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_HISTORIKK, LocalDateTime.now().minusDays(200))
        assertEquals(0, inspektør.sykdomshistorikk.sykdomstidslinje().count())
    }

    @Test
    fun `gjentatt annullering av periode fører ikke til duplikate innslag i utbetalinger`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(5.februar, 10.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(5.februar, 10.februar, 100.prosent))
        håndterAnnullerUtbetaling(fagsystemId = inspektør.utbetalinger[0].inspektør.arbeidsgiverOppdrag.fagsystemId())
        håndterAnnullerUtbetaling(fagsystemId = inspektør.utbetalinger[0].inspektør.arbeidsgiverOppdrag.fagsystemId())
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)
        håndterAnnullerUtbetaling(fagsystemId = inspektør.utbetalinger[0].inspektør.arbeidsgiverOppdrag.fagsystemId())

        assertTrue(inspektør.utbetalinger[0].inspektør.erUtbetalt)
        assertTrue(inspektør.utbetalinger[1].inspektør.erAnnullering)

        assertEquals(2, inspektør.utbetalinger.size)
    }

    @Test
    fun `utbetalteventet får med seg arbeidsdager`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent), Arbeid(23.januar, 26.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        assertEquals(
            4, observatør.utbetaltEventer[0].ikkeUtbetalteDager
                .filter { it.type == PersonObserver.UtbetaltEvent.IkkeUtbetaltDag.Type.Arbeidsdag }.size
        )
    }

    @Test
    fun `inntekter på flere arbeidsgivere oppretter arbeidsgivere med tom sykdomshistorikk`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
                1.januar(2017) til 1.januar(2017) inntekter {
                    "123412344" inntekt 1
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `håndterer fler arbeidsgivere så lenge kun én har sykdomshistorikk`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
                1.januar(2017) til 1.januar(2017) inntekter {
                    "123412344" inntekt 1
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        håndterSykmelding(Sykmeldingsperiode(1.mars, 28.mars, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.mars, 28.mars, 100.prosent))
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
        håndterSykmelding(Sykmeldingsperiode(1.januar(2018), 31.januar(2018), 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar(2018), 16.januar(2018))))
        håndterSøknad(Sykdom(1.januar(2018), 31.januar(2018), 100.prosent), sendtTilNav = 1.januar(2018))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        repeat(11) {
            val fom = 1.januar(2018).plusMonths(it + 1L).with(firstDayOfMonth())
            val tom = fom.with(lastDayOfMonth())

            håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent))
            håndterSøknad(Sykdom(fom, tom, 100.prosent), sendtTilNav = tom)

            håndterYtelser((it + 2).vedtaksperiode)
            håndterSimulering((it + 2).vedtaksperiode)
            håndterUtbetalingsgodkjenning((it + 2).vedtaksperiode, true)
            håndterUtbetalt((it + 2).vedtaksperiode, Oppdragstatus.AKSEPTERT)
        }

        håndterSykmelding(Sykmeldingsperiode(1.januar(2019), 31.januar(2019), 100.prosent))
        håndterSøknad(Sykdom(1.januar(2019), 31.januar(2019), 100.prosent), sendtTilNav = 31.januar(2019))
        håndterYtelser(13.vedtaksperiode)
        håndterUtbetalingsgodkjenning(13.vedtaksperiode, true)

        val utbetaltEvent = observatør.utbetaltEventer.last()

        assertEquals(13, observatør.utbetaltEventer.size)
        assertEquals(28.desember, observatør.utbetaltEventer[11].oppdrag.first().utbetalingslinjer.first().tom)
        assertEquals(28.desember, utbetaltEvent.oppdrag.first().utbetalingslinjer.first().tom)
    }

    @Test
    fun `Skal ikke g reguleres hvis virkning fra før virkningsdato`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar(2020), 16.januar(2020))))
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent), sendtTilNav = 1.januar(2020))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2019) til 1.desember(2019) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        håndterGrunnbeløpsregulering(gyldighetsdato = 20.juli(2020))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Historisk utbetaling til bruker skal ikke bli med i utbetalingstidslinje for arbeidsgiver`() {
        håndterSykmelding(Sykmeldingsperiode(24.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(24.juni(2020), 30.juni(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.juli(2020), 9.juli(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.juli(2020), 9.juli(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(16.oktober(2020), 23.oktober(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(16.oktober(2020), 23.oktober(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(28.oktober(2020), 3.november(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(28.oktober(2020), 3.november(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(4.november(2020), 13.november(2020), 100.prosent))
        håndterSøknad(Sykdom(4.november(2020), 13.november(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(16.oktober(2020), 23.oktober(2020)), Periode(28.oktober(2020), 4.november(2020))), 28.oktober(2020))

        val historikk = arrayOf(ArbeidsgiverUtbetalingsperiode(UNG_PERSON_FNR_2018.toString(), 9.mai(2018), 31.mai(2018), 100.prosent, 1621.daglig))
        val inntekter = listOf(Inntektsopplysning("0", 9.mai(2018), 40000.månedlig, true))
        håndterYtelser(5.vedtaksperiode, *historikk, inntektshistorikk = inntekter)
        håndterVilkårsgrunnlag(5.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.oktober(2019) til 1.september(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))

        håndterYtelser(5.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(
            4.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            5.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )

        assertEquals(24.juni(2020), inspektør.utbetalinger.first().utbetalingstidslinje().periode().start)
    }

    @Test
    fun `Ny utbetalingsbuilder feiler ikke når sykdomshistorikk inneholder arbeidsgiverperiode som hører til infotrygdperiode`() {
        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 3.september(2020), 7.september(2020), 30.prosent, 200.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 8.september(2020), 27.september(2020), 30.prosent, 200.daglig)
        )
        val inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER.toString(), 3.september(2020), 20000.månedlig, true)
        )
        håndterSykmelding(Sykmeldingsperiode(28.september(2020), 14.oktober(2020), 30.prosent))
        håndterSøknad(Sykdom(28.september(2020), 14.oktober(2020), 30.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(15.oktober(2020), 30.oktober(2020), 30.prosent))
        håndterSøknad(Sykdom(15.oktober(2020), 30.oktober(2020), 30.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(11.november(2020), 11.november(2020), 100.prosent))
        håndterSøknad(Sykdom(11.november(2020), 11.november(2020), 100.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(18.august(2020), 2.september(2020))), førsteFraværsdag = 11.november(2020))
        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.november(2019) til 1.oktober(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        assertDoesNotThrow { håndterYtelser(3.vedtaksperiode) }
    }

    @Test
    fun `Ny utbetalingsbuilder feiler ikke når IT har kontinuerlig sykdom på tvers av arbeidsgivere`() {
        håndterSykmelding(Sykmeldingsperiode(28.september(2020), 18.oktober(2020), 100.prosent))
        håndterSøknad(Sykdom(28.september(2020), 18.oktober(2020), 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.januar(2019), 22.februar(2019), 100.prosent, 200.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 23.mars(2020), 5.juli(2020), 100.prosent, 200.daglig),
            Friperiode(6.juli(2020), 27.september(2020)),
            ArbeidsgiverUtbetalingsperiode("123455433", 5.desember(2018), 31.desember(2018), 100.prosent, 200.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER.toString(), 1.januar(2019), 20000.månedlig, true),
                Inntektsopplysning(ORGNUMMER.toString(), 23.mars(2020), 21000.månedlig, true),
                Inntektsopplysning("123455433", 5.desember(2018), 18000.månedlig, true)
            )
        )
        assertDoesNotThrow { håndterYtelser(1.vedtaksperiode) }
    }

    @Test
    fun `beregner ikke skjæringstidspunktet på nytt for å finne sykepengegrunnlag`() {
        håndterSykmelding(Sykmeldingsperiode(1.november(2020), 10.november(2020), 100.prosent))
        håndterSøknad(Sykdom(1.november(2020), 10.november(2020), 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 20.oktober(2020), 31.oktober(2020), 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER.toString(), 20.oktober(2020), 22000.månedlig, true)
            )
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        val utbetaltEvent = observatør.utbetaltEventer.last()

        assertEquals(264000.0, utbetaltEvent.sykepengegrunnlag)
    }

    @Test
    fun `Person uten skjæringstidspunkt feiler ikke i validering av Utbetalingshistorikk`() {
        håndterSykmelding(Sykmeldingsperiode(23.oktober(2020), 18.november(2020), 100.prosent))
        håndterSøknad(Sykdom(23.oktober(2020), 18.november(2020), 100.prosent), Ferie(23.oktober(2020), 18.november(2020)))
        val inntektsopplysning = listOf(
            Inntektsopplysning(ORGNUMMER.toString(), 3.september(2020), INNTEKT, true)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = inntektsopplysning)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP
        )
    }

    @Test
    fun `oppretter ikke ny vedtaksperiode ved sykmelding som overlapper med forkastet periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 7.januar, 100.prosent), Utdanning(8.januar, 31.januar))
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))

        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
    }

    @Test
    fun `man kan opprette en vedtaksperiode før en forkastet periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent), Utdanning(21.februar, 28.februar))

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))

        assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertFalse(inspektør.periodeErForkastet(2.vedtaksperiode))
    }

    @Test
    fun `forsikrer oss om at vi plukker opp forlengelser fra infotrygd ved sen behandling`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24))
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        håndterUtbetalingshistorikk(
            1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.januar, 31.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER.toString(), 1.januar, INNTEKT, true)
            )
        )
        assertTilstander(
            1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `Overstyring treffer periode som har fått FOM flyttet bakover`() {
        val inntektsmeldingId = UUID.randomUUID()
        håndterSykmelding(Sykmeldingsperiode(1.desember(2020), 31.desember(2020), 100.prosent))
        håndterSøknad(Sykdom(1.desember(2020), 31.desember(2020), 100.prosent))
        håndterInntektsmelding(listOf(1.desember(2020) til 16.desember(2020)), id = inntektsmeldingId)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.desember(2019) til 1.november(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(20.januar(2021), 25.januar(2021), 100.prosent)) // Irrelevant, men tas med for completeness
        håndterSykmelding(Sykmeldingsperiode(1.januar(2021), 29.januar(2021), 20.prosent)) // Irrelevant, men tas med for completeness

        håndterSykmelding(Sykmeldingsperiode(29.januar(2021), 29.januar(2021), 100.prosent))
        håndterSøknad(Sykdom(1.januar(2021), 29.januar(2021), 20.prosent)) // Her strekkes vedtaksperioden tilbake til 1. januar, pga historikken.
        håndterUtbetalingshistorikk(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)

        assertEquals(null, inspektør.sykdomstidslinje.inspektør.dagteller[Dag.Feriedag::class])

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(14.januar(2021), Dagtype.Feriedag)))

        assertEquals(1, inspektør.sykdomstidslinje.inspektør.dagteller[Dag.Feriedag::class])
    }

    @Test
    fun `Inntektskilde i godkjenningsbehov for en arbeidsgiver`() {
        håndterSykmelding(Sykmeldingsperiode(1.desember(2020), 31.desember(2020), 100.prosent))
        håndterSøknad(Sykdom(1.desember(2020), 31.desember(2020), 100.prosent))
        håndterInntektsmelding(listOf(1.desember(2020) til 16.desember(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.desember(2019) til 1.november(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        assertEquals("EN_ARBEIDSGIVER", inspektør.sisteBehov(1.vedtaksperiode).detaljer()["inntektskilde"])
    }

    @Test
    fun `Starter ikke ny arbeidsgiverperiode dersom flere opphold til sammen utgjør over 16 dager når hvert opphold er under 16 dager - opphold starter på helg`() {
        håndterSykmelding(Sykmeldingsperiode(27.januar(2021), 2.februar(2021), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(27.januar(2021), 2.februar(2021), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(3.februar(2021), 7.februar(2021), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(3.februar(2021), 7.februar(2021), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(8.februar(2021), 12.februar(2021), 100.prosent))
        håndterSøknad(Sykdom(8.februar(2021), 12.februar(2021), 100.prosent))

        håndterInntektsmelding(
            listOf(
                28.desember(2020) til 28.desember(2020),
                13.januar(2021) til 15.januar(2021),
                27.januar(2021) til 7.februar(2021)
            ), førsteFraværsdag = 27.januar(2021)
        )

        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2020) til 1.desember(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))

        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)

        inspektør.utbetalingUtbetalingstidslinje(0).inspektør.also {
            assertEquals(5, it.navDagTeller)
            assertEquals(16, it.arbeidsgiverperiodeDagTeller)
        }
    }

    @Test
    fun `Starter ikke ny arbeidsgiverperiode dersom flere opphold til sammen utgjør over 16 dager når hvert opphold er under 16 dager`() {
        håndterSykmelding(Sykmeldingsperiode(27.januar(2021), 2.februar(2021), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(27.januar(2021), 2.februar(2021), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(3.februar(2021), 7.februar(2021), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(3.februar(2021), 7.februar(2021), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(8.februar(2021), 12.februar(2021), 100.prosent))
        håndterSøknad(Sykdom(8.februar(2021), 12.februar(2021), 100.prosent))

        håndterInntektsmelding(
            listOf(
                27.desember(2020) til 27.desember(2020),
                12.januar(2021) til 14.januar(2021),
                27.januar(2021) til 7.februar(2021)
            ), førsteFraværsdag = 27.januar(2021)
        )

        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2020) til 1.desember(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))

        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)

        inspektør.utbetalingUtbetalingstidslinje(0).inspektør.also {
            assertEquals(5, it.navDagTeller)
            assertEquals(16, it.arbeidsgiverperiodeDagTeller)
        }
    }

    @Test
    fun `Starter ikke ny arbeidsgiverperiode dersom flere opphold til sammen utgjør over 16 dager når hvert opphold er under 16 dager - opphold etter arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2021), 10.januar(2021), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.januar(2021), 10.januar(2021), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(20.januar(2021), 25.januar(2021), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(20.januar(2021), 25.januar(2021), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(5.februar(2021), 12.februar(2021), 100.prosent))
        håndterSøknad(Sykdom(5.februar(2021), 12.februar(2021), 100.prosent))

        håndterInntektsmelding(
            listOf(
                1.januar(2021) til 10.januar(2021),
                20.januar(2021) til 25.februar(2021)
            ), førsteFraværsdag = 5.februar(2021)
        )

        håndterYtelser(3.vedtaksperiode)

        håndterVilkårsgrunnlag(3.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2020) til 1.desember(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))

        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)

        inspektør.utbetalingUtbetalingstidslinje(0).inspektør.also {
            assertEquals(6, it.navDagTeller)
            assertEquals(16, it.arbeidsgiverperiodeDagTeller)
        }
    }

    @Test
    fun `sender med skjæringstidspunkt på godkjenningsbehov`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val godkjenningsbehov = inspektør.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning)
        assertEquals(1.januar.toString(), godkjenningsbehov.detaljer()["skjæringstidspunkt"])
    }

    @Test
    fun `Tar hensyn til forkastede perioder ved beregning av maks dato`() {
        val inntektshistorikkA1 = listOf(
            Inntektsopplysning(ORGNUMMER.toString(), 1.januar, INNTEKT, true),
        )
        val inntektshistorikkA2 = listOf(
            Inntektsopplysning(ORGNUMMER.toString(), 1.januar, INNTEKT, true),
            Inntektsopplysning(ORGNUMMER.toString(), 1.juli, INNTEKT, true),
        )
        val ITPeriode1 = 1.januar til 31.januar
        val ITPeriode2 = 1.juli til 31.juli

        val spleisPeriode1 = 1.februar til 28.februar
        val spleisPeriode2 = 1.august til 31.august
        val utbetalinger1 = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), ITPeriode1.start, ITPeriode1.endInclusive, 100.prosent, INNTEKT),
        )
        val utbetalinger2 = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), ITPeriode1.start, ITPeriode1.endInclusive, 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), ITPeriode2.start, ITPeriode2.endInclusive, 100.prosent, INNTEKT),
        )




        håndterSykmelding(Sykmeldingsperiode(spleisPeriode1.start, spleisPeriode1.endInclusive, 100.prosent), orgnummer = ORGNUMMER)
        håndterSøknad(Sykdom(spleisPeriode1.start, spleisPeriode1.endInclusive, 100.prosent), orgnummer = ORGNUMMER)

        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            *utbetalinger1,
            inntektshistorikk = inntektshistorikkA1,
            orgnummer = ORGNUMMER,
            besvart = LocalDateTime.MIN
        )
        håndterYtelser(1.vedtaksperiode, *utbetalinger1, inntektshistorikk = inntektshistorikkA1, orgnummer = ORGNUMMER, besvart = LocalDateTime.MIN)

        håndterSimulering(1.vedtaksperiode, orgnummer = ORGNUMMER)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = ORGNUMMER)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = ORGNUMMER)

        person.invaliderAllePerioder(inspektør.personLogg, feilmelding = "Feil med vilje")

        håndterSykmelding(Sykmeldingsperiode(spleisPeriode2.start, spleisPeriode2.endInclusive, 100.prosent), orgnummer = ORGNUMMER)
        håndterSøknad(Sykdom(spleisPeriode2.start, spleisPeriode2.endInclusive, 100.prosent), orgnummer = ORGNUMMER)

        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            *utbetalinger2,
            inntektshistorikk = inntektshistorikkA2,
            orgnummer = ORGNUMMER,
            besvart = LocalDateTime.MIN
        )
        håndterYtelser(2.vedtaksperiode, *utbetalinger2, inntektshistorikk = inntektshistorikkA2, orgnummer = ORGNUMMER, besvart = LocalDateTime.MIN)

        håndterSimulering(2.vedtaksperiode, orgnummer = ORGNUMMER)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = ORGNUMMER)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = ORGNUMMER)

        assertEquals(88, inspektør(ORGNUMMER).forbrukteSykedager(1))
    }

    @Test
    fun `forlengelse av vedtaksperiode hvor utbetalinger tidligere har nådd makstid, men ikke har mottatt ytelser i 26 uker - skal få warning om at vilkårsgrunnlag må etterspørres`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        val months2018 = (2..12).map {
            YearMonth.of(2018, it)
        }
        val months2019 = (1..6).map {
            YearMonth.of(2019, it)
        }

        (months2018 + months2019).forEachIndexed { index, yearMonth ->
            val vedtaksperiodeIndex = index + 2
            håndterSykmelding(
                Sykmeldingsperiode(
                    LocalDate.of(yearMonth.year, yearMonth.month, 1),
                    LocalDate.of(yearMonth.year, yearMonth.month, yearMonth.lengthOfMonth()),
                    100.prosent
                )
            )
            håndterSøknad(
                Sykdom(
                    LocalDate.of(yearMonth.year, yearMonth.month, 1),
                    LocalDate.of(yearMonth.year, yearMonth.month, yearMonth.lengthOfMonth()),
                    100.prosent
                )
            )
            håndterYtelser(vedtaksperiodeIndex.vedtaksperiode)
            if (inspektør.etterspurteBehov(vedtaksperiodeIndex.vedtaksperiode, Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering)) {
                håndterSimulering(vedtaksperiodeIndex.vedtaksperiode)
            }
            håndterUtbetalingsgodkjenning(vedtaksperiodeIndex.vedtaksperiode)
            håndterUtbetalt(vedtaksperiodeIndex.vedtaksperiode)
        }

        // Denne perioden skal egentlig ha nytt skjæringstidspunkt, få ny inntektsmelding og gå gjennom ny vilkårsprøving
        håndterSykmelding(Sykmeldingsperiode(1.juli(2019), 31.juli(2019), 100.prosent))
        håndterSøknad(Sykdom(1.juli(2019), 31.juli(2019), 100.prosent))
        håndterYtelser(19.vedtaksperiode)

        assertWarnings(inspektør)
        assertTrue(inspektør.personLogg.toString().contains("26 uker siden forrige utbetaling av sykepenger, vurder om vilkårene for sykepenger er oppfylt"))
    }

    @Test
    fun `Skal ikke få warning for opptjening av sykedager etter nådd maksdato for irrelevante perioder`() {
        // Gir det noe mening å sette refusjonsbeløp når det kun er infotrygd-utbetalinger?
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterInntektsmelding(listOf(1.januar(2020) til 16.januar(2020)))

        val utbetalinger = ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.januar, 28.desember, 100.prosent, INNTEKT)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER.toString(), 1.januar, INNTEKT, true))

        håndterYtelser(1.vedtaksperiode, utbetalinger, inntektshistorikk = inntektshistorikk)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        assertNoWarnings(1.vedtaksperiode)
    }

    @Test
    fun `foreldet sykdomsdag etter opphold skal ikke bli til navdag`() {
        nyttVedtak(15.januar, 7.februar)

        håndterSykmelding(
            Sykmeldingsperiode(22.februar, 14.mars, 50.prosent),
            mottatt = 6.august.atStartOfDay(),
            sykmeldingSkrevet = 6.august.atStartOfDay()
        )
        håndterSøknad(Sykdom(22.februar, 14.mars, 50.prosent), sendtTilNav = 8.august)

        håndterInntektsmelding(listOf(22.februar til 14.mars))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        inspektør.utbetalingstidslinjer(2.vedtaksperiode).inspektør.let {
            assertEquals(6, it.navHelgDagTeller)
            assertEquals(15, it.foreldetDagTeller)
        }
    }

    @Test
    fun `Periode som kommer inn som SøknadArbeidsgiver selv om det er mindre enn 16 dager gap til forrige periode`() {
        nyttVedtak(1.september(2021), 24.september(2021))

        håndterSykmelding(Sykmeldingsperiode(12.oktober(2021), 22.oktober(2021), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(12.oktober(2021), 22.oktober(2021), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(23.oktober(2021), 29.oktober(2021), 100.prosent))
        håndterSøknad(Sykdom(23.oktober(2021), 29.oktober(2021), 100.prosent))

        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(
                12.oktober(2021) til 27.oktober(2021)),
            førsteFraværsdag = 12.oktober(2021)
        )

        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_UFERDIG_FORLENGELSE)

        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        assertTrue(inspektør.utbetalinger.last().inspektør.utbetalingstidslinje.inspektør.erNavdag(18.oktober(2021)))
    }

    @Test
    fun `hopper videre uten å validere inntektsmelding dersom vi har inntekt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent), orgnummer = a1)
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.januar, 16.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent), orgnummer = a2)
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.januar, 16.januar, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1, refusjon = Refusjon(Inntekt.INGEN, null))


        //håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar, 100.prosent), orgnummer = a2)
        //håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), orgnummer = a2)
    }

    @Test
    fun `hei christian og david`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent), orgnummer = a1)
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.januar, 16.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent), orgnummer = a2)
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.januar, 16.januar, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1, refusjon = Refusjon(Inntekt.INGEN, null))


        //håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar, 100.prosent), orgnummer = a2)
        //håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), orgnummer = a2)
    }
}
