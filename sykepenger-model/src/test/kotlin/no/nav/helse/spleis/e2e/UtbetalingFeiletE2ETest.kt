package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.Toggle.Companion.enable
import no.nav.helse.hendelser.*
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class UtbetalingFeiletE2ETest : AbstractEndToEndTest() {

    @Test
    fun `revurdering feilet med ett oppdrag status avvist som bygger på tidligere`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        håndterOverstyrTidslinje(listOf(
            ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AVVIST)
        håndterPåminnelse(2.vedtaksperiode, UTBETALING_FEILET)
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)
        val første = inspektør.utbetaling(0)
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(2))
        inspektør.utbetaling(3).inspektør.also { utbetalingInspektør ->
            assertEquals(utbetalingInspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
            assertEquals(Endringskode.ENDR, utbetalingInspektør.arbeidsgiverOppdrag.inspektør.endringskode)
            assertEquals(2, utbetalingInspektør.arbeidsgiverOppdrag.size)
            assertTrue(utbetalingInspektør.arbeidsgiverOppdrag.harUtbetalinger())
            assertEquals(17.januar, utbetalingInspektør.arbeidsgiverOppdrag[0].fom)
            assertEquals(30.januar, utbetalingInspektør.arbeidsgiverOppdrag[0].tom)
            assertEquals(1.februar, utbetalingInspektør.arbeidsgiverOppdrag[1].fom)
            assertEquals(28.februar, utbetalingInspektør.arbeidsgiverOppdrag[1].tom)
            assertTrue(utbetalingInspektør.arbeidsgiverOppdrag[0].erForskjell())
            assertTrue(utbetalingInspektør.arbeidsgiverOppdrag[1].erForskjell())
        }
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET,
            AVVENTER_ARBEIDSGIVERE_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, UTBETALING_FEILET, AVVENTER_ARBEIDSGIVERE_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
    }

    @Test
    fun `utbetaling feilet med ett oppdrag status avvist`() {
        tilGodkjent(1.januar, 31.januar, 100.prosent, 1.januar)
        håndterUtbetalt(1.vedtaksperiode, status = Oppdragstatus.AVVIST)
        håndterPåminnelse(1.vedtaksperiode, UTBETALING_FEILET)
        håndterYtelser(1.vedtaksperiode)
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(0))
        inspektør.utbetaling(1).inspektør.also { utbetalingInspektør ->
            assertEquals(
                utbetalingInspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(),
                inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId()
            )
            assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
            assertTrue(utbetalingInspektør.arbeidsgiverOppdrag.harUtbetalinger())
            assertEquals(17.januar, utbetalingInspektør.arbeidsgiverOppdrag[0].fom)
            assertEquals(31.januar, utbetalingInspektør.arbeidsgiverOppdrag[0].tom)
            assertTrue(utbetalingInspektør.arbeidsgiverOppdrag[0].erForskjell())
        }
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, UTBETALING_FEILET, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `utbetaling feilet med ett oppdrag status avvist som bygger på tidligere`() {
        nyttVedtak(1.januar, 31.januar)
        forlengTilGodkjentVedtak(1.februar, 28.februar)
        håndterUtbetalt(2.vedtaksperiode, status = Oppdragstatus.AVVIST)
        håndterPåminnelse(2.vedtaksperiode, UTBETALING_FEILET)
        håndterYtelser(2.vedtaksperiode)
        val første = inspektør.utbetaling(0)
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(1))
        inspektør.utbetaling(2).inspektør.also { utbetalingInspektør ->
            assertEquals(utbetalingInspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
            assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
            assertTrue(utbetalingInspektør.arbeidsgiverOppdrag.harUtbetalinger())
            assertEquals(17.januar, utbetalingInspektør.arbeidsgiverOppdrag[0].fom)
            assertEquals(28.februar, utbetalingInspektør.arbeidsgiverOppdrag[0].tom)
            assertTrue(utbetalingInspektør.arbeidsgiverOppdrag[0].erForskjell())
        }
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, UTBETALING_FEILET, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `utbetaling feilet med ett oppdrag status avvist og ett som er ok`() = listOf(Toggle.DelvisRefusjon, Toggle.LageBrukerutbetaling).enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(INNTEKT/2, null, emptyList()))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode, status = Oppdragstatus.AKSEPTERT, fagsystemId = inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        håndterUtbetalt(1.vedtaksperiode, status = Oppdragstatus.AVVIST, fagsystemId = inspektør.utbetaling(0).inspektør.personOppdrag.inspektør.fagsystemId())
        håndterPåminnelse(1.vedtaksperiode, UTBETALING_FEILET)
        håndterYtelser(1.vedtaksperiode)
        val første = inspektør.utbetaling(0)
        val andre = inspektør.utbetaling(1)
        håndterSimulering(1.vedtaksperiode, andre.inspektør.utbetalingId, andre.inspektør.personOppdrag.inspektør.fagsystemId(), Fagområde.Sykepenger)
        assertEquals(Utbetaling.Forkastet, første.inspektør.tilstand)
        andre.inspektør.also { utbetalingInspektør ->
            assertEquals(utbetalingInspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
            assertEquals(utbetalingInspektør.personOppdrag.inspektør.fagsystemId(), første.inspektør.personOppdrag.inspektør.fagsystemId())
            assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
            assertEquals(1, utbetalingInspektør.personOppdrag.size)
            assertFalse(utbetalingInspektør.arbeidsgiverOppdrag.harUtbetalinger())
            assertTrue(utbetalingInspektør.personOppdrag.harUtbetalinger())
            assertEquals(17.januar, utbetalingInspektør.arbeidsgiverOppdrag[0].fom)
            assertEquals(31.januar, utbetalingInspektør.arbeidsgiverOppdrag[0].tom)
            assertFalse(utbetalingInspektør.arbeidsgiverOppdrag[0].erForskjell())
            assertEquals(17.januar, utbetalingInspektør.personOppdrag[0].fom)
            assertEquals(31.januar, utbetalingInspektør.personOppdrag[0].tom)
            assertTrue(utbetalingInspektør.personOppdrag[0].erForskjell())
        }
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, UTBETALING_FEILET, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING)
    }

    @Test
    fun `utbetaling feilet med ett oppdrag status ok og ett som er avvist`() = listOf(Toggle.DelvisRefusjon, Toggle.LageBrukerutbetaling).enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(INNTEKT/2, null, emptyList()))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode, status = Oppdragstatus.AVVIST, fagsystemId = inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        håndterUtbetalt(1.vedtaksperiode, status = Oppdragstatus.AKSEPTERT, fagsystemId = inspektør.utbetaling(0).inspektør.personOppdrag.inspektør.fagsystemId())
        håndterPåminnelse(1.vedtaksperiode, UTBETALING_FEILET)
        håndterYtelser(1.vedtaksperiode)
        val første = inspektør.utbetaling(0)
        val andre = inspektør.utbetaling(1)
        håndterSimulering(1.vedtaksperiode, andre.inspektør.utbetalingId, andre.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), Fagområde.SykepengerRefusjon)
        assertEquals(Utbetaling.Forkastet, første.inspektør.tilstand)
        andre.inspektør.also { utbetalingInspektør ->
            assertEquals(utbetalingInspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
            assertEquals(utbetalingInspektør.personOppdrag.inspektør.fagsystemId(), første.inspektør.personOppdrag.inspektør.fagsystemId())
            assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
            assertEquals(1, utbetalingInspektør.personOppdrag.size)
            assertTrue(utbetalingInspektør.arbeidsgiverOppdrag.harUtbetalinger())
            assertFalse(utbetalingInspektør.personOppdrag.harUtbetalinger())
            assertEquals(17.januar, utbetalingInspektør.arbeidsgiverOppdrag[0].fom)
            assertEquals(31.januar, utbetalingInspektør.arbeidsgiverOppdrag[0].tom)
            assertTrue(utbetalingInspektør.arbeidsgiverOppdrag[0].erForskjell())
            assertEquals(17.januar, utbetalingInspektør.personOppdrag[0].fom)
            assertEquals(31.januar, utbetalingInspektør.personOppdrag[0].tom)
            assertFalse(utbetalingInspektør.personOppdrag[0].erForskjell())
        }
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, UTBETALING_FEILET, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING)
    }

    @Test
    fun `nyere perioder må vente til periode med feilet utbetaling er ok`() {
        tilGodkjent(1.januar, 31.januar, 100.prosent, 1.januar)
        håndterUtbetalt(1.vedtaksperiode, status = Oppdragstatus.AVVIST)

        gapPeriode(1.mars til 31.mars, ORGNUMMER)

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, UTBETALING_FEILET)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP, AVVENTER_UFERDIG_GAP)
    }
}
