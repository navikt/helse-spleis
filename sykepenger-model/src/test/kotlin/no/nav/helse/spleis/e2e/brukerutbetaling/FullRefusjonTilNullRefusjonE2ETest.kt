package no.nav.helse.spleis.e2e.brukerutbetaling

import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertIngenVarsler
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus.AKSEPTERT
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class  FullRefusjonTilNullRefusjonE2ETest : AbstractEndToEndTest() {

    @Test
    fun `starter med ingen refusjon`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            refusjon = Inntektsmelding.Refusjon(0.daglig, null),
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(AKSEPTERT)

        håndterSykmelding(februar)
        håndterSøknad(februar)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(AKSEPTERT)

        assertFalse(inspektør.utbetaling(0).arbeidsgiverOppdrag.harUtbetalinger())
        assertTrue(inspektør.utbetaling(0).personOppdrag.harUtbetalinger())
        assertEquals(17.januar til 31.januar, Oppdrag.periode(inspektør.utbetaling(0).personOppdrag))

        assertFalse(inspektør.utbetaling(1).arbeidsgiverOppdrag.harUtbetalinger())
        assertTrue(inspektør.utbetaling(1).personOppdrag.harUtbetalinger())
        assertEquals(17.januar til 28.februar, Oppdrag.periode(inspektør.utbetaling(1).personOppdrag))
    }

    @Test
    fun `starter med refusjon, så ingen refusjon`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar,
            refusjon = Inntektsmelding.Refusjon(
                INNTEKT, 31.januar),
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(AKSEPTERT)

        håndterSykmelding(februar)
        håndterSøknad(februar)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(AKSEPTERT)

        assertFalse(inspektør.utbetaling(1).arbeidsgiverOppdrag.harUtbetalinger())
        assertEquals(17.januar til 31.januar, Oppdrag.periode(inspektør.utbetaling(1).arbeidsgiverOppdrag))
        assertTrue(inspektør.utbetaling(1).personOppdrag.harUtbetalinger())
        assertEquals(februar, Oppdrag.periode(inspektør.utbetaling(1).personOppdrag))
    }

    @Test
    fun `starter med refusjon som opphører i neste periode`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar,
            refusjon = Inntektsmelding.Refusjon(
                INNTEKT, 3.februar),
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(AKSEPTERT)

        håndterSykmelding(februar)
        håndterSøknad(februar)
        håndterYtelser(2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_SIMULERING)
    }

    @Test
    fun `starter med refusjon, så korrigeres refusjonen til ingenting`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(AKSEPTERT)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar,
            refusjon = Inntektsmelding.Refusjon(
                INNTEKT, 31.januar),
        )

        håndterSykmelding(februar)
        håndterSøknad(februar)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(AKSEPTERT)

        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(AKSEPTERT)

        assertFalse(inspektør.utbetaling(1).arbeidsgiverOppdrag.harUtbetalinger())
        assertEquals(17.januar til 31.januar, Oppdrag.periode(inspektør.utbetaling(1).arbeidsgiverOppdrag))
        assertTrue(inspektør.utbetaling(2).personOppdrag.harUtbetalinger())
        assertEquals(februar, Oppdrag.periode(inspektør.utbetaling(2).personOppdrag))
    }

    @Test
    fun `starter med ingen refusjon, så korrigeres refusjonen til full`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            refusjon = Inntektsmelding.Refusjon(0.daglig, null),
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(AKSEPTERT)

        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar)

        håndterSykmelding(februar)
        håndterSøknad(februar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(AKSEPTERT)

        håndterYtelser(2.vedtaksperiode)

        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(AKSEPTERT)

        val januarutbetaling = inspektør.utbetaling(0)
        assertFalse(januarutbetaling.arbeidsgiverOppdrag.harUtbetalinger())
        assertTrue(januarutbetaling.personOppdrag.harUtbetalinger())
        assertEquals(17.januar til 31.januar, Oppdrag.periode(januarutbetaling.personOppdrag))

        val januarrevurdering = inspektør.utbetaling(1)
        assertTrue(januarrevurdering.arbeidsgiverOppdrag.harUtbetalinger())
        assertTrue(januarrevurdering.personOppdrag.harUtbetalinger())
        assertTrue(januarrevurdering.personOppdrag[0].erOpphør())
        assertEquals(17.januar til 31.januar, Oppdrag.periode(januarrevurdering.arbeidsgiverOppdrag))

        inspektør.utbetaling(2).also { utbetalingInspektør ->
            assertEquals(17.januar til 28.februar, Oppdrag.periode(utbetalingInspektør.arbeidsgiverOppdrag))
            assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
            utbetalingInspektør.arbeidsgiverOppdrag.also { oppdrag ->
                assertEquals(Endringskode.ENDR, oppdrag.inspektør.endringskode)
                oppdrag[0].inspektør.also { linjeInspektør ->
                    assertEquals(Endringskode.ENDR, linjeInspektør.endringskode)
                    assertEquals(17.januar til 28.februar, linjeInspektør.periode)
                    assertEquals(null, linjeInspektør.datoStatusFom)
                }
            }
            assertEquals(1, utbetalingInspektør.personOppdrag.size)
            utbetalingInspektør.personOppdrag.also { oppdrag ->
                assertEquals(Endringskode.UEND, oppdrag.inspektør.endringskode)
                oppdrag[0].inspektør.also { linjeInspektør ->
                    assertEquals(Endringskode.UEND, linjeInspektør.endringskode)
                    assertEquals(17.januar til 31.januar, linjeInspektør.periode)
                    assertEquals(17.januar, linjeInspektør.datoStatusFom)
                }
            }
        }
        assertIngenVarsler(2.vedtaksperiode.filter())
    }
}
