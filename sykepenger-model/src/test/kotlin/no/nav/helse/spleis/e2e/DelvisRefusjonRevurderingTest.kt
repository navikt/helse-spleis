package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.til
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class DelvisRefusjonRevurderingTest : AbstractEndToEndTest() {

    @Test
    fun `ny IM med inntekt lik refusjonsbeløp bruker informasjon fra ny IM`() = Toggles.RefusjonPerDag.enable {
        nyttVedtak(1.januar, 31.januar, 100.prosent, refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()))
        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)

        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT/2, refusjon = Inntektsmelding.Refusjon(INNTEKT/2, null, emptyList()))
        håndterOverstyrInntekt(INNTEKT/2, skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 715, subset = 1.januar til 16.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 715, 715, subset = 17.januar til 31.januar)

    }

}
