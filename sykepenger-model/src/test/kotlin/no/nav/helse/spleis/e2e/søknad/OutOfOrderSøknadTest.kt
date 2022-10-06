package no.nav.helse.spleis.e2e.søknad

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.tilSimulering
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OutOfOrderSøknadTest : AbstractEndToEndTest() {

    @Test
    fun `ny tidligere periode - senere i avventer simulering - forkaster utbetalingen`() {
        tilSimulering(3.mars, 26.mars, 100.prosent, 3.mars)
        nullstillTilstandsendringer()
        håndterSøknad(Sykdom(5.januar, 19.januar, 80.prosent))
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.tilstand)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertTilstander(1.vedtaksperiode, AVVENTER_SIMULERING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `ny tidligere periode - senere i avventer godkjenning - vilkårsprøver på nytt`() {
        håndterSykmelding(Sykmeldingsperiode(3.mars, 31.mars, 100.prosent))
        val søknadId = håndterSøknad(Sykdom(3.mars, 31.mars, 100.prosent))
        håndterInntektsmelding(listOf(3.mars til 18.mars))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(1, observatør.manglendeInntektsmeldingVedtaksperioder.toList().filter { it.søknadIder == setOf(søknadId) }.size)
        nullstillTilstandsendringer()
        håndterSøknad(Sykdom(1.mars, 2.mars, 80.prosent))
        håndterInntektsmelding(listOf(1.mars til 16.mars))
        håndterYtelser(1.vedtaksperiode)

        assertEquals(Utbetaling.Forkastet, inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.tilstand)
        assertEquals(2, observatør.manglendeInntektsmeldingVedtaksperioder.toList().filter { it.søknadIder == setOf(søknadId) }.size)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING)
    }

}
