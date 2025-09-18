package no.nav.helse.spleis.e2e

import no.nav.helse.april
import no.nav.helse.dsl.a1
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_3
import no.nav.helse.person.tilstandsmaskin.TilstandType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VedtaksperiodeAnnullertEventTest : AbstractEndToEndTest() {

    @Test
    fun `vi sender vedtaksperiode annullert-hendelse når saksbehandler annullerer en vedtaksperiode`() {
        nyttVedtak(januar)
        håndterAnnullerUtbetaling()
        håndterUtbetalt()

        assertTrue(observatør.vedtaksperiodeAnnullertEventer.isNotEmpty())
    }

    @Test
    fun `sender bare vedtaksperiode annullert-hendelse på vedtaksperioden vi faktisk annullerer`() {
        nyttVedtak(januar)
        forlengVedtak(februar)

        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(2.vedtaksperiode))
        håndterUtbetalt()

        assertEquals(1, observatør.vedtaksperiodeAnnullertEventer.size)
        assertEquals(1.februar til 28.februar, observatør.vedtaksperiodeAnnullertEventer[0].fom til observatør.vedtaksperiodeAnnullertEventer[0].tom)
    }

    @Test
    fun `vi sender ikke ut vedtaksperiode annullert-hendelse for vedtaksperioder som ikke er utbetalt`() {
        tilGodkjenning(januar, organisasjonsnummere = arrayOf(a1))
        håndterAnnullerUtbetaling()

        assertEquals(0, observatør.vedtaksperiodeAnnullertEventer.size)
    }

    @Test
    fun `også langt gap`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        nyttVedtak(april, vedtaksperiodeIdInnhenter = 3.vedtaksperiode)
        håndterAnnullerUtbetaling()
        håndterUtbetalt()

        assertEquals(1, observatør.vedtaksperiodeAnnullertEventer.size)
        assertEquals(
            april,
            observatør.vedtaksperiodeAnnullertEventer[0].fom til observatør.vedtaksperiodeAnnullertEventer[0].tom
        )
    }

    @Test
    fun `arbeid ikke gjenopptatt`() {
        nyttVedtak(januar)

        håndterSøknad(mars)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.mars,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "FerieEllerAvspasering"
        )
        assertVarsler(listOf(RV_IM_3, Varselkode.RV_IM_25), 2.vedtaksperiode.filter())
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@VedtaksperiodeAnnullertEventTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        nullstillTilstandsendringer()

        this@VedtaksperiodeAnnullertEventTest.håndterOverstyrTidslinje((februar).map {
            ManuellOverskrivingDag(
                it,
                Dagtype.ArbeidIkkeGjenopptattDag
            )
        })
        this@VedtaksperiodeAnnullertEventTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        this@VedtaksperiodeAnnullertEventTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertEquals(0, observatør.vedtaksperiodeAnnullertEventer.size)
    }

    @Test
    fun `revurdering uten endring som siden annulleres skal sende melding om annullert`() {
        nyttVedtak(januar)

        håndterInntektsmelding(listOf(1.januar til 16.januar))
        this@VedtaksperiodeAnnullertEventTest.håndterYtelser(1.vedtaksperiode)
        this@VedtaksperiodeAnnullertEventTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        håndterUtbetalt()
        håndterAnnullerUtbetaling()
        håndterUtbetalt()

        assertEquals(1, observatør.vedtaksperiodeAnnullertEventer.size)
    }

    @Test
    fun `Pågående revurdering uten endring som siden annulleres skal sende melding om annullert`() {
        nyttVedtak(januar)
        this@VedtaksperiodeAnnullertEventTest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(1.januar, Dagtype.Sykedag, 100)))
        assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_HISTORIKK_REVURDERING)
        håndterAnnullerUtbetaling()
        håndterUtbetalt()
        assertEquals(1, observatør.vedtaksperiodeAnnullertEventer.size)
    }
}
