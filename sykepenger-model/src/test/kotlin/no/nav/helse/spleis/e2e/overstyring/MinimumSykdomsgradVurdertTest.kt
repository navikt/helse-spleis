package no.nav.helse.spleis.e2e.overstyring

import java.time.LocalDate
import no.nav.helse.dsl.lagStandardSykepengegrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterMinimumSykdomsgradVurdert
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class MinimumSykdomsgradVurdertTest : AbstractEndToEndTest() {

    @Disabled
    @Test
    fun `Saksbehandler overstyrer avslag pga minimum sykdomsgrad`() {
        nyPeriode(januar, orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 19000.månedlig, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(
                listOf(a1 to 19000.månedlig, a2 to 81000.månedlig),
                1.januar
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null, ORDINÆRT),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null, ORDINÆRT)
            )
        )
        håndterYtelser(1.vedtaksperiode)

        val avvistedager =
            inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.utbetalingstidslinje.inspektør.avvistedager

        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertEquals(11, avvistedager.size)
        assertTrue(avvistedager.all { it.begrunnelser == listOf(Begrunnelse.MinimumSykdomsgrad) })
        assertVarsel(Varselkode.RV_VV_4)

        håndterMinimumSykdomsgradVurdert(listOf(januar))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertEquals(0, inspektør.utbetalinger(1.vedtaksperiode).last().inspektør.utbetalingstidslinje.inspektør.avvistedager)
    }
}