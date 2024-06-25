package no.nav.helse.spleis.e2e.overstyring

import java.time.LocalDate
import no.nav.helse.dsl.lagStandardSykepengegrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.håndterInntektsmelding
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MinimumSykdomsgradVurdertTest : AbstractEndToEndTest() {

    @Test
    fun `Saksbehandler overstyrer avslag pga minimum sykdomsgrad`() {
        settOppAvslagPåMinimumSykdomsgrad()

        val avvistedager =
            inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.utbetalingstidslinje.inspektør.avvistedager

        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertEquals(11, avvistedager.size)
        assertTrue(avvistedager.all { it.begrunnelser == listOf(Begrunnelse.MinimumSykdomsgrad) })
        assertVarsel(Varselkode.RV_VV_4)

        håndterMinimumSykdomsgradVurdert(listOf(januar))
        håndterYtelser(1.vedtaksperiode)
        assertVarsel(Varselkode.RV_VV_17, 1.vedtaksperiode.filter(orgnummer = a1))
        håndterSimulering(1.vedtaksperiode)

        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertEquals(0, inspektør.utbetalinger(1.vedtaksperiode).last().inspektør.utbetalingstidslinje.inspektør.avvistedager.size)
    }

    @Test
    fun `Får søknad fra ghost etter at minimum sykdomsgrad er vurdert`() {
        settOppAvslagPåMinimumSykdomsgrad()

        håndterMinimumSykdomsgradVurdert(listOf(januar))
        håndterYtelser()
        assertVarsel(Varselkode.RV_VV_17, 1.vedtaksperiode.filter(orgnummer = a1))
        håndterSimulering()
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()

        assertEquals(10, inspektør.utbetalinger.last().inspektør.utbetalingstidslinje[17.januar].økonomi.inspektør.totalGrad)

        nyPeriode(januar, orgnummer = a2, grad = 10.prosent)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 81000.månedlig, orgnummer = a2)
        håndterYtelser(orgnummer = a1)

        assertEquals(19, inspektør.utbetalinger.last().inspektør.utbetalingstidslinje[17.januar].økonomi.inspektør.totalGrad)

        håndterSimulering(orgnummer = a1)
        håndterUtbetalingsgodkjenning(orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(orgnummer = a2)
        håndterSimulering(orgnummer = a2)
        håndterUtbetalingsgodkjenning(orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        assertEquals(0, inspektør(a1).utbetalinger(1.vedtaksperiode).last().inspektør.utbetalingstidslinje.inspektør.avvistedager.size)
        assertEquals(0, inspektør(a2).utbetalinger(1.vedtaksperiode).last().inspektør.utbetalingstidslinje.inspektør.avvistedager.size)
        assertVarsel(Varselkode.RV_VV_17, 1.vedtaksperiode.filter(orgnummer = a2))
    }

    private fun settOppAvslagPåMinimumSykdomsgrad() {
        nyPeriode(januar, orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 10000.månedlig, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(
                listOf(a1 to 10000.månedlig, a2 to 81000.månedlig),
                1.januar
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null, ORDINÆRT),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null, ORDINÆRT)
            )
        )
        håndterYtelser(1.vedtaksperiode)
    }
}