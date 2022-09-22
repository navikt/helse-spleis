package no.nav.helse.spleis.e2e.søknad

import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.tilSimulering
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OutOfOrderSøknadTest : AbstractEndToEndTest() {

    @Test
    fun `ny tidligere periode - senere i avventer simulering - forkaster utbetalingen`() {
        tilSimulering(3.mars, 26.mars, 100.prosent, 3.januar)
        nullstillTilstandsendringer()
        håndterSøknad(Sykdom(5.januar, 19.januar, 80.prosent))
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.tilstand)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertTilstander(1.vedtaksperiode, AVVENTER_SIMULERING, AVVENTER_BLOKKERENDE_PERIODE)
    }

}
