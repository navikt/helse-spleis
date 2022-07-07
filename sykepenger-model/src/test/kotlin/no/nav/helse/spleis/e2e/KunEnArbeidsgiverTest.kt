package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Permisjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Utdanning
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Utlandsopphold
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.TilstandType.UTBETALING_FEILET
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.september
import no.nav.helse.sisteBehov
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class KunEnArbeidsgiverTest : AbstractEndToEndTest() {

    @Test
    fun `ingen historie med inntektsmelding først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        inspektør.sykdomstidslinje.inspektør.låstePerioder.also {
            assertEquals(0, it.size)
        }
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        assertNoErrors()
        assertActivities(person)
        inspektør.also {
            assertInntektForDato(INNTEKT, 3.januar, inspektør = it)
            assertEquals(2, it.sykdomshistorikk.size)
            assertEquals(18, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(6, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTrue(1.vedtaksperiode.id(ORGNUMMER) in observatør.utbetalteVedtaksperioder)
        inspektør.sykdomstidslinje.inspektør.låstePerioder.also {
            assertEquals(1, it.size)
            assertEquals(Periode(3.januar, 26.januar), it.first())
        }
    }

    @Test
    fun `ingen historie med søknad til arbeidsgiver først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 8.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 8.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertNoWarnings()
        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        assertNoErrors()
        assertActivities(person)
        inspektør.also {
            assertInntektForDato(INNTEKT, 3.januar, inspektør = it)
            assertEquals(2, it.sykdomshistorikk.size)
            assertEquals(4, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(2, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
        }
        assertTilstander(
            0,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING
        )
    }

    @Test
    fun `ingen historie med to søknader til arbeidsgiver før inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 5.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(8.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(8.januar, 10.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(11.januar, 22.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 22.januar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)

        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode, INNTEKT)

        assertNoErrors()
        assertActivities(person)
        inspektør.also {
            assertInntektForDato(INNTEKT, 3.januar, inspektør = it)
            assertEquals(4, it.sykdomshistorikk.size)
            assertEquals(4, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
            assertEquals(14, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `ingen historie med to søknader (med gap mellom) til arbeidsgiver først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 4.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 4.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(8.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(8.januar, 10.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(11.januar, 22.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 22.januar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(3.januar, 4.januar), Periode(8.januar, 21.januar)),
            førsteFraværsdag = 8.januar
        )

        assertNoErrors()
        assertActivities(person)
        inspektør.also {
            assertInntektForDato(INNTEKT, 8.januar, inspektør = it)
            assertEquals(4, it.sykdomshistorikk.size)
            assertEquals(4, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
            assertEquals(13, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `ingen historie med inntektsmelding, så søknad til arbeidsgiver`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 8.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        assertNoWarnings()
        håndterSøknad(Sykdom(3.januar, 8.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode.id(ORGNUMMER))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertNoErrors(1.vedtaksperiode.filter())
        assertActivities(person)
        inspektør.also {
            assertInntektForDato(INNTEKT, 3.januar, inspektør = it)
            assertEquals(2, it.sykdomshistorikk.size)
            assertEquals(4, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(2, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
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
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        assertNoErrors()
        assertActivities(person)
        inspektør.also {
            assertInntektForDato(INNTEKT, 3.januar, inspektør = it)
            assertEquals(2, it.sykdomshistorikk.size)
            assertEquals(18, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(6, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTrue(1.vedtaksperiode.id(ORGNUMMER) in observatør.utbetalteVedtaksperioder)
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

        assertWarnings()
        inspektør.also {
            assertWarning("Utdanning oppgitt i perioden i søknaden.", 1.vedtaksperiode.filter())
            assertWarning("Utenlandsopphold oppgitt i perioden i søknaden.", 1.vedtaksperiode.filter())
            assertEquals(2, it.sykdomshistorikk.size)
            assertEquals(18, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(6, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
        }
    }

    @Test
    fun `søknad sendt etter 3 mnd`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.mai)
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        assertNoErrors()
        assertActivities(person)
        inspektør.also {
            assertInntektForDato(INNTEKT, 3.januar, inspektør = it)
            assertEquals(2, it.sykdomshistorikk.size)
            assertNull(it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(6, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
            assertEquals(0, it.utbetalinger(1.vedtaksperiode).size)
            assertThrows<IllegalStateException> { it.arbeidsgiver.nåværendeTidslinje() }
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING
        )
    }

    @Test
    fun `gap-historie før inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.desember(2017), 15.desember(2017), 100.prosent, 15000.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.desember(2017), INNTEKT, true))
        )
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        assertNoErrors()
        assertActivities(person)
        inspektør.also {
            assertInntektForDato(INNTEKT, 3.januar, inspektør = it)
            assertEquals(2, it.sykdomshistorikk.size)
            assertEquals(18, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(6, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
        }
        assertNotNull(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTrue(1.vedtaksperiode.id(ORGNUMMER) in observatør.utbetalteVedtaksperioder)
    }

    @Test
    fun `gap-historie uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.desember(2017), 16.desember(2017), 100.prosent, 15000.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.desember(2017), INNTEKT, true))
        )
        assertNoErrors()
        assertFalse(person.personLogg.hasWarningsOrWorse())
        assertActivities(person)
        inspektør.also {
            assertInntektForDato(null, 2.januar, inspektør = it)
            assertEquals(1, it.sykdomshistorikk.size)
            assertEquals(18, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(6, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
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

        assertNoErrors()
        assertActivities(person)
        inspektør.also {
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
        håndterUtbetalt()

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
        håndterUtbetalt()

        assertNoErrors()
        assertActivities(person)
        inspektør.also {
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
    fun `forlengelseperioden avsluttes ikke automatisk hvis warnings`() {
        nyttVedtak(1.januar, 20.januar, 100.prosent)
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent), Ferie(21.januar, 31.januar))
        håndterYtelser(2.vedtaksperiode, arbeidsavklaringspenger = listOf(3.januar.minusDays(60) til 5.januar.minusDays(60)))
        assertTrue(person.aktivitetslogg.logg(inspektør.vedtaksperioder(2.vedtaksperiode)).hasWarningsOrWorse())
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING
        )
    }

    @Test
    fun `To perioder med opphold`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertActivities(person)
        håndterSøknad(Sykdom(1.februar, 23.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.februar, 16.februar)))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertNoErrors()
        assertActivities(person)
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
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
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
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
    fun `Sammenblandede hendelser fra forskjellige perioder med søknad først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 23.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)))
        håndterInntektsmelding(listOf(Periode(1.februar, 16.februar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        assertActivities(person)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        assertNoErrors()
        assertActivities(person)
        inspektør.also {
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
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
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
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
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
    fun `Sender ikke avsluttet periode til infotrygd når man mottar en ugyldig søknad i etterkant`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 21.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertNoErrors()
        assertActivities(person)
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        håndterSøknad(Sykdom(1.januar, 21.januar, 100.prosent), Permisjon(21.januar, 21.januar))
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
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
    fun `Sender ikke periode som allerede har behandlet inntektsmelding til infotrygd når man mottar en ny ugyldig inntektsmelding i etterkant`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 21.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertNoErrors()
        assertActivities(person)
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(INNTEKT, 15.januar, emptyList())
        )
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
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
    fun `Venter på å bli kilt etter inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 7.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 23.februar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 7.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)))
        håndterSøknad(Sykdom(8.januar, 23.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertNoErrors()
        assertActivities(person)
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_BLOKKERENDE_PERIODE,
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
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSøknad(Sykdom(29.januar, 23.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertNoErrors()
        assertActivities(person)
        inspektør.also {
            assertEquals(3.januar, it.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(3.januar, it.skjæringstidspunkt(2.vedtaksperiode))
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
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
            AVVENTER_BLOKKERENDE_PERIODE,
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
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AVVIST)

        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 23.februar, 100.prosent))

        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
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
            START,
            AVVENTER_BLOKKERENDE_PERIODE
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

        assertNoErrors()
        assertActivities(person)
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING
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
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(5.februar, 10.februar, 100.prosent))
        håndterSøknad(Sykdom(5.februar, 10.februar, 100.prosent))
        håndterAnnullerUtbetaling(fagsystemId = inspektør.utbetalinger[0].inspektør.arbeidsgiverOppdrag.fagsystemId())
        håndterAnnullerUtbetaling(fagsystemId = inspektør.utbetalinger[0].inspektør.arbeidsgiverOppdrag.fagsystemId())
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        håndterAnnullerUtbetaling(fagsystemId = inspektør.utbetalinger[0].inspektør.arbeidsgiverOppdrag.fagsystemId())

        assertTrue(inspektør.utbetalinger[0].inspektør.erUtbetalt)
        assertTrue(inspektør.utbetalinger[1].inspektør.erAnnullering)

        assertEquals(2, inspektør.utbetalinger.size)
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
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `håndterer fler arbeidsgivere så lenge kun én har sykdomshistorikk`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)))
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
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        håndterSykmelding(Sykmeldingsperiode(1.mars, 28.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 28.mars, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.mars, 16.mars)))
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE
        )
    }

    @Test
    fun `Skal ikke g reguleres hvis virkning fra før virkningsdato`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.januar(2020))
        håndterInntektsmelding(listOf(Periode(1.januar(2020), 16.januar(2020))))
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
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterGrunnbeløpsregulering(gyldighetsdato = 20.juli(2020))

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
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
        håndterSøknad(Sykdom(24.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())

        håndterSykmelding(Sykmeldingsperiode(1.juli(2020), 9.juli(2020), 100.prosent))
        håndterSøknad(Sykdom(1.juli(2020), 9.juli(2020), 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())

        håndterSykmelding(Sykmeldingsperiode(16.oktober(2020), 23.oktober(2020), 100.prosent))
        håndterSøknad(Sykdom(16.oktober(2020), 23.oktober(2020), 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())

        håndterSykmelding(Sykmeldingsperiode(28.oktober(2020), 3.november(2020), 100.prosent))
        håndterSøknad(Sykdom(28.oktober(2020), 3.november(2020), 100.prosent))
        håndterUtbetalingshistorikk(4.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())

        håndterSykmelding(Sykmeldingsperiode(4.november(2020), 13.november(2020), 100.prosent))
        håndterSøknad(Sykdom(4.november(2020), 13.november(2020), 100.prosent))
        håndterUtbetalingshistorikk(5.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())
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
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(
            4.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            5.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )

        assertEquals(24.juni(2020), inspektør.utbetalinger.first().utbetalingstidslinje().periode().start)
    }

    @Test
    fun `Ny utbetalingsbuilder feiler ikke når IT har kontinuerlig sykdom på tvers av arbeidsgivere`() {
        håndterSykmelding(Sykmeldingsperiode(28.september(2020), 18.oktober(2020), 100.prosent))
        håndterSøknad(Sykdom(28.september(2020), 18.oktober(2020), 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar(2019), 22.februar(2019), 100.prosent, 200.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 23.mars(2020), 5.juli(2020), 100.prosent, 200.daglig),
            Friperiode(6.juli(2020), 27.september(2020)),
            ArbeidsgiverUtbetalingsperiode("123455433", 5.desember(2018), 31.desember(2018), 100.prosent, 200.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar(2019), 20000.månedlig, true),
                Inntektsopplysning(ORGNUMMER, 23.mars(2020), 21000.månedlig, true),
                Inntektsopplysning("123455433", 5.desember(2018), 18000.månedlig, true)
            )
        )
        assertDoesNotThrow { håndterYtelser(1.vedtaksperiode) }
    }

    @Test
    fun `Person uten skjæringstidspunkt feiler ikke i validering av Utbetalingshistorikk`() {
        håndterSykmelding(Sykmeldingsperiode(23.oktober(2020), 18.november(2020), 100.prosent))
        håndterSøknad(Sykdom(23.oktober(2020), 18.november(2020), 100.prosent), Ferie(23.oktober(2020), 18.november(2020)))
        val inntektsopplysning = listOf(
            Inntektsopplysning(ORGNUMMER, 3.september(2020), INNTEKT, true)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = inntektsopplysning)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `oppretter ikke ny vedtaksperiode ved søknad som overlapper med forkastet periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        person.invaliderAllePerioder(hendelselogg, null)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))

        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
    }

    @Test
    fun `forsikrer oss om at vi plukker opp forlengelser fra infotrygd ved sen behandling`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24))
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        håndterUtbetalingshistorikk(
            1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))
        )
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK
        )
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
        assertEquals("EN_ARBEIDSGIVER", person.personLogg.sisteBehov(1.vedtaksperiode).detaljer()["inntektskilde"])
    }

    @Test
    fun `Starter ikke ny arbeidsgiverperiode dersom flere opphold til sammen utgjør over 16 dager når hvert opphold er under 16 dager - opphold starter på helg`() {
        håndterSykmelding(Sykmeldingsperiode(27.januar(2021), 2.februar(2021), 100.prosent))
        håndterSøknad(Sykdom(27.januar(2021), 2.februar(2021), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(3.februar(2021), 7.februar(2021), 100.prosent))
        håndterSøknad(Sykdom(3.februar(2021), 7.februar(2021), 100.prosent))

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
        håndterSøknad(Sykdom(27.januar(2021), 2.februar(2021), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(3.februar(2021), 7.februar(2021), 100.prosent))
        håndterSøknad(Sykdom(3.februar(2021), 7.februar(2021), 100.prosent))

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
        håndterSøknad(Sykdom(1.januar(2021), 10.januar(2021), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(20.januar(2021), 25.januar(2021), 100.prosent))
        håndterSøknad(Sykdom(20.januar(2021), 25.januar(2021), 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(5.februar(2021), 12.februar(2021), 100.prosent))
        håndterSøknad(Sykdom(5.februar(2021), 12.februar(2021), 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)

        håndterInntektsmelding(
            listOf(
                1.januar(2021) til 10.januar(2021),
                20.januar(2021) til 25.januar(2021)
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

        val godkjenningsbehov = person.personLogg.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning)
        assertEquals(1.januar.toString(), godkjenningsbehov.detaljer()["skjæringstidspunkt"])
    }

    @Test
    fun `Skal ikke få warning for opptjening av sykedager etter nådd maksdato for irrelevante perioder`() {
        // Gir det noe mening å sette refusjonsbeløp når det kun er infotrygd-utbetalinger?
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterInntektsmelding(listOf(1.januar(2020) til 16.januar(2020)))

        val utbetalinger = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 28.desember, 100.prosent, INNTEKT)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))

        håndterYtelser(1.vedtaksperiode, utbetalinger, inntektshistorikk = inntektshistorikk)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertNoWarnings(1.vedtaksperiode.filter())
    }

    @Test
    fun `foreldet sykdomsdag etter opphold skal ikke bli til navdag`() {
        nyttVedtak(15.januar, 7.februar)

        håndterSykmelding(
            Sykmeldingsperiode(22.februar, 14.mars, 50.prosent),
            mottatt = 6.august.atStartOfDay(),
            sykmeldingSkrevet = 6.august.atStartOfDay()
        )
        håndterSøknad(Sykdom(22.februar, 14.mars, 50.prosent), sendtTilNAVEllerArbeidsgiver = 8.august)

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
        håndterSøknad(Sykdom(12.oktober(2021), 22.oktober(2021), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(23.oktober(2021), 29.oktober(2021), 100.prosent))
        håndterSøknad(Sykdom(23.oktober(2021), 29.oktober(2021), 100.prosent))

        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(
                12.oktober(2021) til 27.oktober(2021)),
            førsteFraværsdag = 12.oktober(2021)
        )

        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertTrue(inspektør.utbetalinger.last().inspektør.utbetalingstidslinje.inspektør.erNavdag(18.oktober(2021)))
    }

    @Test
    fun `hopper videre uten å validere inntektsmelding dersom vi har inntekt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), orgnummer = a2)

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
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1, refusjon = Refusjon(Inntekt.INGEN, null))


        //håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar, 100.prosent), orgnummer = a2)
        //håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), orgnummer = a2)
    }
}
