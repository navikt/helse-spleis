package no.nav.helse.spleis.e2e.overstyring

import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterMinimumSykdomsgradVurdert
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class MinimumSykdomsgradVurdertTest : AbstractEndToEndTest() {

    @Test
    fun `Saksbehandler overstyrer avslag pga minimum sykdomsgrad`() {
        settOppAvslagPåMinimumSykdomsgrad()

        val avvistedager = inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager

        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertEquals(11, avvistedager.size)
        assertTrue(avvistedager.all { it.begrunnelser == listOf(Begrunnelse.MinimumSykdomsgrad) })
        assertVarsel(Varselkode.RV_VV_4, 1.vedtaksperiode.filter())

        håndterMinimumSykdomsgradVurdert(perioderMedMinimumSykdomsgradVurdertOK = listOf(januar))
        this@MinimumSykdomsgradVurdertTest.håndterYtelser(1.vedtaksperiode)
        assertVarsel(Varselkode.RV_VV_17, 1.vedtaksperiode.filter(orgnummer = a1))
        håndterSimulering(1.vedtaksperiode)

        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertEquals(0, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)
        assertEquals(11, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.size)
        assertTrue(inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.all { it.økonomi.inspektør.totalGrad == 10 })
    }

    @Test
    fun `Får søknad fra ghost etter at minimum sykdomsgrad er vurdert`()  {
        settOppAvslagPåMinimumSykdomsgrad()

        håndterMinimumSykdomsgradVurdert(perioderMedMinimumSykdomsgradVurdertOK = listOf(januar))
        this@MinimumSykdomsgradVurdertTest.håndterYtelser()
        assertVarsel(Varselkode.RV_VV_17, 1.vedtaksperiode.filter(orgnummer = a1))
        håndterSimulering()
        this@MinimumSykdomsgradVurdertTest.håndterUtbetalingsgodkjenning()
        håndterUtbetalt()

        assertEquals(10, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[17.januar].økonomi.inspektør.totalGrad)

        nyPeriode(januar, orgnummer = a2, grad = 10.prosent)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 81000.månedlig,
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        this@MinimumSykdomsgradVurdertTest.håndterYtelser(orgnummer = a1)

        assertEquals(19, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[17.januar].økonomi.inspektør.totalGrad)

        this@MinimumSykdomsgradVurdertTest.håndterUtbetalingsgodkjenning(orgnummer = a1)

        this@MinimumSykdomsgradVurdertTest.håndterYtelser(orgnummer = a2)
        håndterSimulering(orgnummer = a2)
        this@MinimumSykdomsgradVurdertTest.håndterUtbetalingsgodkjenning(orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        assertEquals(0, inspektør(a1).utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)
        assertEquals(0, inspektør(a2).utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)
        assertVarsel(Varselkode.RV_VV_17, 1.vedtaksperiode.filter(orgnummer = a2))
    }

    @Test
    fun `Vurderer samme måned til å være både ok og ikke ok`() {
        settOppAvslagPåMinimumSykdomsgrad()
        assertThrows<IllegalStateException> {
            håndterMinimumSykdomsgradVurdert(perioderMedMinimumSykdomsgradVurdertOK = listOf(januar), perioderMedMinimumSykdomsgradVurdertIkkeOK = listOf(januar))
        }
    }

    @Test
    fun `Saksbehandler angrer vurdering`() {
        settOppAvslagPåMinimumSykdomsgrad()
        assertEquals(11, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)
        assertEquals(0, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.size)
        håndterMinimumSykdomsgradVurdert(perioderMedMinimumSykdomsgradVurdertOK = listOf(januar))
        this@MinimumSykdomsgradVurdertTest.håndterYtelser()
        assertVarsel(Varselkode.RV_VV_17, 1.vedtaksperiode.filter(orgnummer = a1))
        assertEquals(0, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)
        assertEquals(11, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.size)
        håndterMinimumSykdomsgradVurdert(perioderMedMinimumSykdomsgradVurdertOK = emptyList(), perioderMedMinimumSykdomsgradVurdertIkkeOK = listOf(januar))
        this@MinimumSykdomsgradVurdertTest.håndterYtelser()
        assertEquals(11, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)
        assertEquals(0, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.size)
    }

    @Test
    fun `bare enkeltdager i vedtaksperioden er vurdert ok`() {
        settOppAvslagPåMinimumSykdomsgrad()
        assertEquals(11, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)
        assertEquals(0, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.size)
        håndterMinimumSykdomsgradVurdert(perioderMedMinimumSykdomsgradVurdertOK = listOf(1.januar til 20.januar))
        this@MinimumSykdomsgradVurdertTest.håndterYtelser()
        assertEquals(8, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)
        assertEquals(3, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.size)
        assertVarsel(Varselkode.RV_VV_17, 1.vedtaksperiode.filter(orgnummer = a1))
    }

    private fun settOppAvslagPåMinimumSykdomsgrad() {
        nyPeriode(januar, orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 10000.månedlig,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a1 to 10000.månedlig, a2 to 81000.månedlig),
            orgnummer = a1
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))
        this@MinimumSykdomsgradVurdertTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertVarsel(Varselkode.RV_VV_4, 1.vedtaksperiode.filter(orgnummer = a1))
    }
}
