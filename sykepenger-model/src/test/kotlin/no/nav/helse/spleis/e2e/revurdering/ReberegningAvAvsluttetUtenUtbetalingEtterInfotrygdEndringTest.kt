package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.IdInnhenter
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ReberegningAvAvsluttetUtenUtbetalingEtterInfotrygdEndringTest : AbstractEndToEndTest() {

    @Test
    fun `AUU med infotrygdperiode rett før skal omgjøres`() {
        nyPeriode(5.januar til 20.januar)
        nullstillTilstandsendringer()
        this@ReberegningAvAvsluttetUtenUtbetalingEtterInfotrygdEndringTest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 4.januar))
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
        assertIngenOverlappendeInfotrygdutbetaling()
    }

    @Test
    fun `AUU med utbetalt forlengelse med infotrygdperiode rett før`() {
        nyPeriode(5.januar til 20.januar)
        nyttVedtak(21.januar til 31.januar, arbeidsgiverperiode = listOf(5.januar til 20.januar), vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        nullstillTilstandsendringer()
        this@ReberegningAvAvsluttetUtenUtbetalingEtterInfotrygdEndringTest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 4.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK)
        assertIngenOverlappendeInfotrygdutbetaling()
    }

    @Test
    fun `helt overlappende infotrygd`() {
        nyPeriode(5.januar til 20.januar)
        nullstillTilstandsendringer()
        this@ReberegningAvAvsluttetUtenUtbetalingEtterInfotrygdEndringTest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 5.januar, 20.januar))
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
        assertOverlappendeInfotrygdutbetalingIAUU(1.vedtaksperiode, "AVSLUTTET_UTEN_UTBETALING")
    }

    @Test
    fun `delvis overlappende infotrygd`() {
        nyPeriode(5.januar til 20.januar)
        nullstillTilstandsendringer()
        this@ReberegningAvAvsluttetUtenUtbetalingEtterInfotrygdEndringTest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 5.januar, 15.januar))
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
        assertOverlappendeInfotrygdutbetalingIAUU(1.vedtaksperiode, "AVSLUTTET_UTEN_UTBETALING")
    }

    @Test
    fun `delvis overlappende infotrygd med utbetalt forlengelse`() {
        nyPeriode(5.januar til 20.januar)
        nyttVedtak(21.januar til 31.januar, arbeidsgiverperiode = listOf(5.januar til 20.januar), vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        nullstillTilstandsendringer()
        this@ReberegningAvAvsluttetUtenUtbetalingEtterInfotrygdEndringTest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 5.januar, 15.januar))
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertOverlappendeInfotrygdutbetalingIAUU(1.vedtaksperiode, "AVSLUTTET_UTEN_UTBETALING")
    }

    @Test
    fun `overlappende infotrygd med utbetalt forlengelse`() {
        nyPeriode(5.januar til 20.januar)
        nyttVedtak(21.januar til 31.januar, arbeidsgiverperiode = listOf(5.januar til 20.januar), vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        nullstillTilstandsendringer()
        this@ReberegningAvAvsluttetUtenUtbetalingEtterInfotrygdEndringTest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 5.januar, 18.januar))
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        assertOverlappendeInfotrygdutbetalingIAUU(1.vedtaksperiode, "AVSLUTTET_UTEN_UTBETALING")
    }

    @Test
    fun `ny søknad medfører infotrygdhistorikk som overlapper helt med gammelt`() {
        nyPeriode(5.januar til 20.januar)
        nullstillTilstandsendringer()
        håndterSøknad(Sykdom(1.januar(2023), 31.januar(2023), 100.prosent))
        this@ReberegningAvAvsluttetUtenUtbetalingEtterInfotrygdEndringTest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 5.januar, 20.januar))
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)
        assertOverlappendeInfotrygdutbetalingIAUU(1.vedtaksperiode, "AVSLUTTET_UTEN_UTBETALING")
    }

    @Test
    fun `ny søknad medfører infotrygdhistorikk som overlapper med gammelt med utbetalt forlengelse`() {
        nyPeriode(5.januar til 20.januar)
        nyttVedtak(21.januar til 31.januar, arbeidsgiverperiode = listOf(5.januar til 20.januar), vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterSøknad(Sykdom(1.januar(2023), 31.januar(2023), 100.prosent))
        this@ReberegningAvAvsluttetUtenUtbetalingEtterInfotrygdEndringTest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 5.januar, 20.januar))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)
        assertOverlappendeInfotrygdutbetalingIAUU(1.vedtaksperiode, "AVSLUTTET_UTEN_UTBETALING")
    }

    @Test
    fun `ny søknad medfører infotrygdhistorikk som som er like før gammelt med utbetalt forlengelse`() {
        nyPeriode(5.januar til 20.januar)
        nyttVedtak(21.januar til 31.januar, arbeidsgiverperiode = listOf(5.januar til 20.januar), vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterSøknad(Sykdom(1.januar(2023), 31.januar(2023), 100.prosent))
        this@ReberegningAvAvsluttetUtenUtbetalingEtterInfotrygdEndringTest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 4.januar))
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)
        assertIngenOverlappendeInfotrygdutbetaling()
    }

    @Test
    fun `Periodene omgjøres i riktig rekkefølge`() {
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), orgnummer = a1) // Denne bare for at a1 skal være først i lista for arbeidsgivere

        håndterSøknad(Sykdom(1.mars, 16.mars, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            orgnummer = a2
        )
        håndterSøknad(Sykdom(1.mai, 16.mai, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.mai til 16.mai),
            orgnummer = a1
        )

        nullstillTilstandsendringer()

        this@ReberegningAvAvsluttetUtenUtbetalingEtterInfotrygdEndringTest.håndterUtbetalingshistorikkEtterInfotrygdendring(
            ArbeidsgiverUtbetalingsperiode(a2, 1.mars, 5.mars),
            ArbeidsgiverUtbetalingsperiode(a1, 1.mai, 5.mai)
        )

        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING, orgnummer = a2)
    }

    private fun assertOverlappendeInfotrygdutbetalingIAUU(vedtaksperiode: IdInnhenter, tilstand: String) {
        val overlapp = observatør.overlappendeInfotrygdperioder.last()
        val vedtaksperiodeId = vedtaksperiode.id(a1)
        assertEquals(vedtaksperiodeId, overlapp.overlappendeInfotrygdperioder.find { it.vedtaksperiodeId == vedtaksperiodeId }?.vedtaksperiodeId)
        assertEquals(tilstand, overlapp.overlappendeInfotrygdperioder.find { it.vedtaksperiodeId == vedtaksperiodeId }?.vedtaksperiodetilstand)
    }

    private fun assertIngenOverlappendeInfotrygdutbetaling() {
        assertTrue(observatør.overlappendeInfotrygdperioder.all { it.overlappendeInfotrygdperioder.isEmpty() })
    }
}
