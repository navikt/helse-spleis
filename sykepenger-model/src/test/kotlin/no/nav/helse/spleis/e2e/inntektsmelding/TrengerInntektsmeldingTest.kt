package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.nyPeriode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TrengerInntektsmeldingTest : AbstractEndToEndTest() {

    @Test
    fun `Kort periode ber om inntektsmelding når den går tilbake, og sier fra om at inntektsmelding ikke trengs etter at den er mottatt`() {
        nyPeriode(5.januar til 17.januar)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        nyPeriode(20.januar til 22.januar)
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        assertTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertEquals(2, observatør.trengerIkkeInntektsmeldingVedtaksperioder.size)
        nullstillTilstandsendringer()

        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertEquals(2, observatør.manglendeInntektsmeldingVedtaksperioder.size)

        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 20.januar)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        assertEquals(4, observatør.trengerIkkeInntektsmeldingVedtaksperioder.size)
    }
}