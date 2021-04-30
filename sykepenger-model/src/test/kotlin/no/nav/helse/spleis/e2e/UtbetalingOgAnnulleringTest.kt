package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.aktive
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class UtbetalingOgAnnulleringTest : AbstractEndToEndTest() {

    @Test
    fun `annullerer første periode før andre periode starter i en ikke-sammenhengende utbetaling med mer enn 16 dager gap`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        val fagsystemId = inspektør.utbetalinger.aktive().last().arbeidsgiverOppdrag().fagsystemId()
        håndterAnnullerUtbetaling(fagsystemId = fagsystemId)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(15.februar, 15.mars, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(15.februar, 15.mars, 100.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(15.februar, 2.mars)), førsteFraværsdag = 15.februar)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertEquals(26.januar, observatør.annulleringer[0].utbetalingslinjer.last().tom)
        assertEquals(15.mars, observatør.utbetaltEventer.last().tom)
        assertEquals(1, observatør.utbetaltEventer.last().oppdrag.first().utbetalingslinjer.size)
        assertEquals(0, observatør.utbetaltEventer.last().oppdrag.last().utbetalingslinjer.size)
    }

    @Test
    fun `annullering kaster alle etterfølgende perioder`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(20.februar, 25.mars, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(20.februar, 25.mars, 100.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(20.februar, 7.mars)), førsteFraværsdag = 20.februar)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)

        val fagsystemIdFørsteVedtaksperiode = inspektør.utbetalinger.aktive().last().arbeidsgiverOppdrag().fagsystemId()
        håndterAnnullerUtbetaling(fagsystemId = fagsystemIdFørsteVedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertForkastetPeriodeTilstander(1.vedtaksperiode,
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

        assertForkastetPeriodeTilstander(2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `kan annullere selv om vi har en etterfølgende periode som har gått til infotrygd etter simulering`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(3.mars, 26.mars, 100.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(3.mars, 26.mars)))
        håndterSøknadMedValidering(2.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.mars, 26.mars, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, false)

        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        assertEquals(3, inspektør.utbetalinger.size)
        assertTrue(inspektør.utbetalinger[2].erAnnullering())
    }

    @Test
    fun `kan ikke annullere utbetaling etter sammenhengede periode TIL_UTBETALING`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(27.januar, 15.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(27.januar, 15.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(2.vedtaksperiode))
        assertEquals(2, inspektør.utbetalinger.size)
        assertFalse(inspektør.utbetalinger[1].erAnnullering())
    }

    @Test
    fun `utbetaling med teknisk feil blir stående i til utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.FEIL, sendOverførtKvittering = false)
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
            TIL_UTBETALING
        )
        assertEquals(Utbetaling.Sendt, inspektør.utbetalingtilstand(0))
    }
    @Test
    fun `utbetaling med teknisk feil blir stående i til utbetaling og prøver igjen`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.FEIL, sendOverførtKvittering = false)
        håndterUtbetalingpåminnelse(0, Utbetalingstatus.SENDT, LocalDateTime.now().minusDays(1))
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
            TIL_UTBETALING
        )
        assertEquals(Utbetaling.Sendt, inspektør.utbetalingtilstand(0))
    }

    @Test
    fun `utbetaling med teknisk feil for lenge går til Utbetaling feilet`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.FEIL, sendOverførtKvittering = false)
        håndterUtbetalingpåminnelse(0, Utbetalingstatus.SENDT, LocalDateTime.now().minusDays(8))
        håndterPåminnelse(1.vedtaksperiode, TIL_UTBETALING)
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
            UTBETALING_FEILET
        )
        assertEquals(Utbetaling.UtbetalingFeilet, inspektør.utbetalingtilstand(0))
    }

    @Test
    fun `utbetaling som blir avvist går til utbetaling feilet`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AVVIST)
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
            UTBETALING_FEILET
        )
    }

    @Test
    fun `kan forsøke utbetaling på nytt etter Utbetaling feilet`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AVVIST)
        assertEquals(Utbetaling.UtbetalingFeilet, inspektør.utbetalingtilstand(0))
        håndterUtbetalingpåminnelse(0, Utbetalingstatus.UTBETALING_FEILET)
        assertEquals(Utbetaling.Overført, inspektør.utbetalingtilstand(0))
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT, sendOverførtKvittering = false)
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
            UTBETALING_FEILET,
            AVSLUTTET
        )
    }

    @Test
    fun `utbetaling går videre dersom vi går glipp av Overført-kvittering`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT, sendOverførtKvittering = false)
        assertEquals(Utbetaling.Utbetalt, inspektør.utbetalingtilstand(0))
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
    fun `Kan ikke annullere over en fastlåst annullering`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode)) // Stale
        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode), opprettet = LocalDateTime.now().plusHours(3))
        håndterUtbetalt(1.vedtaksperiode, status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertTrue(inspektør.utbetalinger.last().erAnnullering())
        assertEquals(1, observatør.annulleringer.size)
        assertEquals(2,
            inspektør.personLogg.behov()
                .filter { it.detaljer()["fagsystemId"] == inspektør.fagsystemId(1.vedtaksperiode) }
                .filter { it.type == Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling }
                .size
        )
        assertEquals(2, inspektør.utbetalinger.size)
    }

    @Test
    fun `utbetaling med ubetalt periode etterpå inkluderer ikke dager etter perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        val utbetalingUtbetalingstidslinje = inspektør.utbetalingUtbetalingstidslinje(0)
        assertEquals(3.januar til 26.januar, utbetalingUtbetalingstidslinje.periode())
    }

    @Test
    fun `ubetalt periode, etter utbetalt, etterutbetales ikke`() {
        (10.juni to 30.juni).also { (fom, tom) ->
            håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(fom, tom, 100.prosent), sendtTilNav = tom)
            håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(fom til fom.plusDays(15)))
        }
        (1.juli to 31.juli).also { (fom, tom) ->
            håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent))
        }

        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.juni(2017) til 1.mai(2018) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)
        håndterPåminnelse(2.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, LocalDateTime.now().minusMonths(13)) // forkast

        (1.august to 31.august).also { (fom, tom) ->
            håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(fom, tom, 100.prosent))
        }
        val historikk = Utbetalingsperiode(ORGNUMMER, 1.juli til 31.juli, 100.prosent, 1000.daglig)
        håndterUtbetalingshistorikk(3.vedtaksperiode, historikk)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, true)
        håndterUtbetalt(3.vedtaksperiode)

        inspektør.utbetalingUtbetalingstidslinje(0).also { utbetalingUtbetalingstidslinje ->
            assertEquals(10.juni til 30.juni, utbetalingUtbetalingstidslinje.periode())
        }
        inspektør.utbetaling(0).also { utbetaling ->
            assertEquals(26.juni, utbetaling.arbeidsgiverOppdrag().førstedato)
            assertEquals(30.juni, utbetaling.arbeidsgiverOppdrag().sistedato)
            assertEquals(1, utbetaling.arbeidsgiverOppdrag().size)
        }
        inspektør.utbetalingUtbetalingstidslinje(1).also { utbetalingUtbetalingstidslinje ->
            assertEquals(10.juni til 31.august, utbetalingUtbetalingstidslinje.periode())
            assertEquals(Utbetalingstidslinje.Utbetalingsdag.UkjentDag::class, utbetalingUtbetalingstidslinje[1.juli]::class)
            assertEquals(Utbetalingstidslinje.Utbetalingsdag.UkjentDag::class, utbetalingUtbetalingstidslinje[31.juli]::class)
        }
        inspektør.utbetaling(1).also { utbetaling ->
            assertEquals(1.august, utbetaling.arbeidsgiverOppdrag().førstedato)
            assertEquals(31.august, utbetaling.arbeidsgiverOppdrag().sistedato)
            assertEquals(1, utbetaling.arbeidsgiverOppdrag().size)
        }
    }
}
