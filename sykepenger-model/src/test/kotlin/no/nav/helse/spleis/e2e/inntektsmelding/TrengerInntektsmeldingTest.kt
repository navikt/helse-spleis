package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.Toggle
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.nyPeriode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TrengerInntektsmeldingTest : AbstractEndToEndTest() {

    @Test
    fun `Kort periode ber om inntektsmelding når den går inn i AvventerRevurdering, og sier fra om at inntektsmelding ikke trengs etter at den er mottatt`() = Toggle.RevurderOutOfOrder.enable {
        nyPeriode(5.januar til 17.januar)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        nyPeriode(20.januar til 22.januar)
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        assertTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertEquals(2, observatør.trengerIkkeInntektsmeldingVedtaksperioder.size)

        håndterInntektsmelding(listOf(1.januar til 16.januar))

        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertEquals(1, observatør.manglendeInntektsmeldingVedtaksperioder.size)

        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 20.januar)
        assertEquals(3, observatør.trengerIkkeInntektsmeldingVedtaksperioder.size)
    }
}